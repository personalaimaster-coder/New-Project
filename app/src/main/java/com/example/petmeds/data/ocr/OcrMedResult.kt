package com.example.petmeds.data.ocr

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A single medication entry extracted from a prescription image by the OCR pipeline.
 * All string fields may be blank when the parser could not confidently extract a value.
 *
 * Implements [Parcelable] so it can be stored directly in [androidx.lifecycle.SavedStateHandle]
 * and passed between Navigation Compose back-stack entries.
 */
@Parcelize
data class OcrMedResult(
    val name: String,
    val dosageAmount: String = "",
    val dosageUnit: String = "",
    /** Lower-cased keyword: "pill", "liquid", "eye_drop", "ear_drop", "topical" */
    val form: String = "",
    /** "daily_times" | "interval" | "prn" */
    val frequencyType: String = "daily_times",
    val timesPerDay: Int = 1,
    val intervalHours: Int = 8,
    val notes: String = "",
    /** The raw OCR text lines that produced this result, shown as a reference to the user. */
    val rawText: String = "",
    /**
     * True when the extracted name was found in the bundled India pet drug dictionary.
     * False means the dosage anchor was confident but the name wasn't recognised —
     * the user should verify it. Never used to suppress results.
     */
    val confidenceHigh: Boolean = false,
) : Parcelable

sealed class OcrResult {
    data class Success(val medications: List<OcrMedResult>) : OcrResult()
    /** Image was processed but no text regions were detected at all. */
    data object NoTextFound : OcrResult()
    /** Text was found but it could not be parsed as any medication entry. */
    data class NoMedicationsFound(val rawText: String) : OcrResult()
    data class Error(val message: String) : OcrResult()
}
