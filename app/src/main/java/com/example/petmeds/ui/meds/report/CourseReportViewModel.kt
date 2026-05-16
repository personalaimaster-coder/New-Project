package com.example.petmeds.ui.meds.report

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petmeds.domain.report.CourseReportBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import javax.inject.Inject

data class CourseReportUiState(
    val generating: Boolean = false,
    val pdfFile: File? = null,
    val error: String? = null,
)

@HiltViewModel
class CourseReportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val builder: CourseReportBuilder,
    private val renderer: CourseReportPdfRenderer,
    private val imageEncoder: CourseReportImageEncoder,
) : ViewModel() {

    private val _state = MutableStateFlow(CourseReportUiState())
    val state: StateFlow<CourseReportUiState> = _state.asStateFlow()

    fun generate(courseId: Long) {
        if (_state.value.generating) return
        _state.value = _state.value.copy(generating = true, error = null, pdfFile = null)
        viewModelScope.launch {
            runCatching {
                val report = builder.build(courseId)
                    ?: error("Course not found")
                val html = renderCourseReportHtml(
                    report = report,
                    encodeImage = { path -> imageEncoder.encode(path) },
                )
                val outFile = withContext(Dispatchers.IO) {
                    val dir = File(context.cacheDir, "course_reports").apply { mkdirs() }
                    val ts = report.generatedAt
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                    val safeName = report.course.name.replace(Regex("[^A-Za-z0-9-]+"), "_")
                        .trim('_')
                        .ifBlank { "course" }
                    val fileName = "%s-%04d%02d%02d-%02d%02d.pdf".format(
                        safeName,
                        ts.year, ts.monthNumber, ts.dayOfMonth,
                        ts.hour, ts.minute,
                    )
                    File(dir, fileName)
                }
                renderer.render(html, outFile)
                outFile
            }.onSuccess { file ->
                _state.value = _state.value.copy(generating = false, pdfFile = file)
            }.onFailure { t ->
                _state.value = _state.value.copy(generating = false, error = t.message ?: "error")
            }
        }
    }

    fun consumeShareIntent() {
        _state.value = _state.value.copy(pdfFile = null)
    }

    fun consumeError() {
        _state.value = _state.value.copy(error = null)
    }
}
