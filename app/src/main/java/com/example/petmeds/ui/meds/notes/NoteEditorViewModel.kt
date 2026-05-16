package com.example.petmeds.ui.meds.notes

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petmeds.data.notes.NotePhotoStore
import com.example.petmeds.data.repo.CourseNoteRepository
import com.example.petmeds.domain.model.CourseNote
import com.example.petmeds.domain.model.NoteCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class NoteEditorUiState(
    val courseId: Long = 0L,
    val occurredAt: Instant = Clock.System.now(),
    val category: NoteCategory = NoteCategory.OBSERVATION,
    val body: String = "",
    val photoPath: String? = null,
    val bodyError: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val deleting: Boolean = false,
    val deleted: Boolean = false,
    val isEditing: Boolean = false,
)

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val notesRepository: CourseNoteRepository,
    private val photoStore: NotePhotoStore,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(NoteEditorUiState())
    val state: StateFlow<NoteEditorUiState> = _state.asStateFlow()

    private var editingId: Long = 0L
    private var createdAt: Instant = clock.now()
    private var originalPhotoPath: String? = null

    fun init(courseId: Long, noteId: Long?) {
        if (noteId != null && noteId > 0L) {
            loadExisting(noteId)
        } else {
            _state.value = _state.value.copy(
                courseId = courseId,
                occurredAt = clock.now(),
                isEditing = false,
            )
        }
    }

    private fun loadExisting(noteId: Long) {
        viewModelScope.launch {
            val note = notesRepository.findById(noteId) ?: return@launch
            editingId = note.id
            createdAt = note.createdAt
            originalPhotoPath = note.photoPath
            _state.value = _state.value.copy(
                courseId = note.courseId,
                occurredAt = note.occurredAt,
                category = note.category,
                body = note.body,
                photoPath = note.photoPath,
                isEditing = true,
            )
        }
    }

    fun update(transform: (NoteEditorUiState) -> NoteEditorUiState) {
        _state.value = transform(_state.value)
    }

    fun setDate(date: LocalDate) {
        val zone = TimeZone.currentSystemDefault()
        val current = _state.value.occurredAt.toLocalDateTime(zone)
        val updated = LocalDateTime(date, current.time).toInstant(zone)
        _state.value = _state.value.copy(occurredAt = updated)
    }

    fun setTime(hour: Int, minute: Int) {
        val zone = TimeZone.currentSystemDefault()
        val current = _state.value.occurredAt.toLocalDateTime(zone)
        val updated = LocalDateTime(current.date, LocalTime(hour, minute)).toInstant(zone)
        _state.value = _state.value.copy(occurredAt = updated)
    }

    fun setPhotoUri(uri: Uri) {
        viewModelScope.launch {
            val saved = photoStore.save(uri) ?: return@launch
            val previous = _state.value.photoPath
            _state.value = _state.value.copy(photoPath = saved)
            // Discard a previously chosen-but-unsaved photo so we don't leak files.
            if (previous != null && previous != originalPhotoPath) {
                photoStore.delete(previous)
            }
        }
    }

    fun removePhoto() {
        val previous = _state.value.photoPath
        _state.value = _state.value.copy(photoPath = null)
        if (previous != null && previous != originalPhotoPath) {
            photoStore.delete(previous)
        }
    }

    fun submit() {
        val s = _state.value
        if (s.body.isBlank()) {
            update { it.copy(bodyError = true) }
            return
        }
        if (s.saving || s.deleting) return
        update { it.copy(saving = true) }
        viewModelScope.launch {
            // If the user replaced the photo, clean up the old one once we're committing.
            val replacedOriginal = originalPhotoPath != null && originalPhotoPath != s.photoPath
            notesRepository.upsert(
                CourseNote(
                    id = editingId,
                    courseId = s.courseId,
                    occurredAt = s.occurredAt,
                    category = s.category,
                    body = s.body.trim(),
                    photoPath = s.photoPath,
                    createdAt = createdAt,
                    updatedAt = clock.now(),
                )
            )
            if (replacedOriginal) photoStore.delete(originalPhotoPath)
            update { it.copy(saving = false, saved = true) }
        }
    }

    fun delete() {
        if (editingId <= 0L) return
        if (_state.value.saving || _state.value.deleting) return
        update { it.copy(deleting = true) }
        viewModelScope.launch {
            notesRepository.delete(editingId)
            photoStore.delete(originalPhotoPath)
            update { it.copy(deleting = false, deleted = true) }
        }
    }
}
