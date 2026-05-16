package com.example.petmeds.ui.meds.report

import android.content.Context
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.ceil

/**
 * Renders an HTML string into an A4 PDF file, headlessly, with no third-party
 * dependencies and no print-dialog UI.
 *
 * Approach:
 *   1. Build an offscreen [WebView] (no JS) and load the HTML.
 *   2. Wait for `onPageFinished` so layout and embedded images are stable.
 *   3. Measure the WebView at A4 width to determine its full content height.
 *   4. For each PDF page, draw the WebView into a [PdfDocument.Page] canvas
 *      with a vertical translation so the right slice ends up on the page.
 *
 * We avoid `WebView.createPrintDocumentAdapter()` because the Android
 * `PrintDocumentAdapter.LayoutResultCallback` / `WriteResultCallback`
 * constructors are package-private and cannot be subclassed from app code in
 * Kotlin or Java without reflection. Drawing manually onto [PdfDocument] is the
 * standard headless workaround.
 *
 * The render rate is 2x for crisper text (the WebView is laid out at 2× the
 * page's point dimensions, then scaled back down on the canvas).
 */
@Singleton
class CourseReportPdfRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun render(html: String, outFile: File) {
        outFile.parentFile?.mkdirs()
        withContext(Dispatchers.Main.immediate) {
            val webView = WebView(context).apply {
                settings.javaScriptEnabled = false
                // Honour the <meta viewport width=595> set by the HTML so layout
                // happens at exactly A4 width in CSS px (not device px). Without
                // this the WebView lays out at the device's pixel density and the
                // page contents are squeezed asymmetrically when drawn to PDF.
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.defaultTextEncodingName = "UTF-8"
                // Critical: HW-accelerated rendering cannot be captured by a software
                // Canvas (PdfDocument's page canvas is software). Force software layer
                // so webView.draw(canvas) actually paints content into the PDF.
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                setBackgroundColor(Color.WHITE)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
            }
            try {
                loadHtmlAndAwait(webView, html)
                // Yield so the WebView's layout/paint pass after page finish has
                // actually completed before we capture it. Can't use view.post
                // because the WebView is offscreen and never attached to a window.
                delay(150)
                drawToPdf(webView, outFile)
            } finally {
                webView.destroy()
            }
        }
    }

    private suspend fun loadHtmlAndAwait(webView: WebView, html: String) =
        suspendCancellableCoroutine<Unit> { cont ->
            var resumed = false
            // Wait for BOTH page finish and chrome progress=100 — onPageFinished can
            // fire before late-loading resources (data-URI images) have been laid out.
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) { /* no-op */ }
            }
            webView.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress >= 100 && !resumed && cont.isActive) {
                        resumed = true
                        cont.resume(Unit)
                    }
                }
            }
            cont.invokeOnCancellation { webView.stopLoading() }
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }

    private suspend fun drawToPdf(webView: WebView, outFile: File) =
        withContext(Dispatchers.Main.immediate) {
            val pageWidthPx = PAGE_WIDTH_PT * SCALE
            val pageHeightPx = PAGE_HEIGHT_PT * SCALE

            // 1) Lock the viewport width so CSS lays out at A4 width.
            webView.measure(
                View.MeasureSpec.makeMeasureSpec(pageWidthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED,
            )
            // contentHeight reports CSS-pixel content size; combine with the
            // device-pixel measuredHeight so we never under-paginate.
            val contentHeightDevicePx = webView.contentHeight * SCALE
            val totalHeightPx = maxOf(
                webView.measuredHeight,
                contentHeightDevicePx,
                pageHeightPx,
            )
            webView.layout(0, 0, pageWidthPx, totalHeightPx)

            // 2) Allow one more layout/paint pass at the new height before drawing.
            delay(80)

            val numPages = ceil(totalHeightPx.toDouble() / pageHeightPx).toInt()
                .coerceAtLeast(1)

            val pdf = PdfDocument()
            try {
                for (i in 0 until numPages) {
                    val pageInfo = PdfDocument.PageInfo
                        .Builder(PAGE_WIDTH_PT, PAGE_HEIGHT_PT, i + 1)
                        .create()
                    val page = pdf.startPage(pageInfo)
                    val canvas = page.canvas
                    val saved = canvas.save()
                    val inverseScale = 1f / SCALE.toFloat()
                    canvas.scale(inverseScale, inverseScale)
                    canvas.translate(0f, -(i.toFloat() * pageHeightPx))
                    webView.draw(canvas)
                    canvas.restoreToCount(saved)
                    pdf.finishPage(page)
                }
                withContext(Dispatchers.IO) {
                    FileOutputStream(outFile).use { os -> pdf.writeTo(os) }
                }
            } finally {
                pdf.close()
            }
        }

    companion object {
        // A4 in PostScript points (1/72 inch).
        private const val PAGE_WIDTH_PT = 595
        private const val PAGE_HEIGHT_PT = 842

        // 2× over-render to keep text crisp when the canvas scales back down.
        private const val SCALE = 2
    }
}
