package com.example.petmeds.data.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Orchestrates the full prescription OCR pipeline:
 *  1. Converts a [Bitmap] to an ML Kit [InputImage].
 *  2. Runs [TextRecognizer] (on-device, no network).
 *  3. Passes the raw [Text] output to [PrescriptionTextParser] for structured extraction.
 */
@Singleton
class PrescriptionOcrService @Inject constructor(
    private val textRecognizer: TextRecognizer,
    private val parser: PrescriptionTextParser,
) {

    suspend fun extractMedications(bitmap: Bitmap): OcrResult = try {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val visionText = textRecognizer.processImage(inputImage)

        if (visionText.text.isBlank()) {
            OcrResult.NoTextFound
        } else {
            val medications = parser.parse(visionText)
            if (medications.isEmpty()) {
                OcrResult.NoMedicationsFound(visionText.text)
            } else {
                OcrResult.Success(medications)
            }
        }
    } catch (e: Exception) {
        OcrResult.Error(e.message ?: "Unknown OCR error")
    }

    /**
     * Runs [TextRecognizer.process] as a suspending function using a cancellable coroutine.
     */
    private suspend fun TextRecognizer.processImage(
        image: InputImage,
    ): com.google.mlkit.vision.text.Text = suspendCancellableCoroutine { cont ->
        process(image)
            .addOnSuccessListener { result -> cont.resume(result) }
            .addOnFailureListener { e ->
                if (cont.isActive) cont.resumeWith(Result.failure(e))
            }
        cont.invokeOnCancellation { close() }
    }
}
