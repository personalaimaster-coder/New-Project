package com.example.petmeds.ui.meds.report

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads an on-disk image, downscales it to keep the embedded PDF reasonable,
 * and returns a `data:image/jpeg;base64,...` URI suitable for inlining into
 * the report HTML. Returns `null` if the file is missing or unreadable.
 */
@Singleton
class CourseReportImageEncoder @Inject constructor() {

    fun encode(absolutePath: String): String? {
        val file = File(absolutePath)
        if (!file.exists()) return null
        val bitmap = decodeDownsampled(absolutePath, MAX_EDGE) ?: return null
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        bitmap.recycle()
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$b64"
    }

    private fun decodeDownsampled(path: String, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxEdge || bounds.outHeight / sample > maxEdge) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(path, opts)
    }

    companion object {
        private const val MAX_EDGE = 1024
        private const val JPEG_QUALITY = 75
    }
}
