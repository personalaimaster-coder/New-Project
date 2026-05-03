package com.example.petmeds.ui.meds

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petmeds.data.ocr.OcrMedResult
import com.example.petmeds.data.ocr.OcrResult
import com.example.petmeds.data.ocr.PrescriptionOcrService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OcrReviewUiState {
    data object Loading : OcrReviewUiState()
    data class Results(
        val medications: List<OcrMedResult>,
        val imageUri: Uri?,
    ) : OcrReviewUiState()
    data class NoResults(
        val rawText: String,
        val imageUri: Uri?,
        val reason: NoResultsReason,
    ) : OcrReviewUiState()
    data class Error(val message: String) : OcrReviewUiState()
}

enum class NoResultsReason { NO_TEXT, NO_MEDICATIONS }

@HiltViewModel
class OcrReviewViewModel @Inject constructor(
    private val ocrService: PrescriptionOcrService,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<OcrReviewUiState>(OcrReviewUiState.Loading)
    val state: StateFlow<OcrReviewUiState> = _state.asStateFlow()

    fun processImage(imageUri: Uri) {
        _state.value = OcrReviewUiState.Loading
        viewModelScope.launch {
            val bitmap = loadBitmap(imageUri)
            if (bitmap == null) {
                _state.value = OcrReviewUiState.Error("Could not load image")
                return@launch
            }
            val result = ocrService.extractMedications(bitmap)
            _state.value = when (result) {
                is OcrResult.Success -> OcrReviewUiState.Results(
                    medications = result.medications,
                    imageUri = imageUri,
                )
                is OcrResult.NoTextFound -> OcrReviewUiState.NoResults(
                    rawText = "",
                    imageUri = imageUri,
                    reason = NoResultsReason.NO_TEXT,
                )
                is OcrResult.NoMedicationsFound -> OcrReviewUiState.NoResults(
                    rawText = result.rawText,
                    imageUri = imageUri,
                    reason = NoResultsReason.NO_MEDICATIONS,
                )
                is OcrResult.Error -> OcrReviewUiState.Error(result.message)
            }
        }
    }

    fun updateMedication(index: Int, updated: OcrMedResult) {
        val current = _state.value as? OcrReviewUiState.Results ?: return
        _state.value = current.copy(
            medications = current.medications.toMutableList().also { it[index] = updated },
        )
    }

    fun removeMedication(index: Int) {
        val current = _state.value as? OcrReviewUiState.Results ?: return
        val updated = current.medications.toMutableList().also { it.removeAt(index) }
        _state.value = if (updated.isEmpty()) {
            OcrReviewUiState.NoResults("", current.imageUri, NoResultsReason.NO_MEDICATIONS)
        } else {
            current.copy(medications = updated)
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap? = try {
        appContext.contentResolver.openInputStream(uri)?.use { stream ->
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeStream(stream, null, options)
        }
    } catch (e: Exception) {
        null
    }
}
