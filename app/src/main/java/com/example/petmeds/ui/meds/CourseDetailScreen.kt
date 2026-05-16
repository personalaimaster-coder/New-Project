package com.example.petmeds.ui.meds

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.petmeds.R
import com.example.petmeds.domain.model.CourseNote
import com.example.petmeds.domain.model.NoteCategory
import com.example.petmeds.ui.common.formatDosage
import com.example.petmeds.ui.meds.report.CourseReportViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File

@Composable
fun CourseDetailScreen(
    courseId: Long?,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenMedication: (Long) -> Unit,
    onAddMedication: () -> Unit = {},
    onAddNote: (Long) -> Unit = {},
    onEditNote: (Long) -> Unit = {},
    viewModel: CourseDetailViewModel = hiltViewModel(),
    reportViewModel: CourseReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val reportState by reportViewModel.state.collectAsStateWithLifecycle()
    val course = state.course
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    val reportFailed = stringResource(R.string.report_failed)
    val shareTitle = stringResource(R.string.report_share_title)

    LaunchedEffect(courseId) { viewModel.load(courseId) }

    LaunchedEffect(reportState.pdfFile, reportState.error) {
        val pdf = reportState.pdfFile
        if (pdf != null) {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, pdf)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, shareTitle))
            reportViewModel.consumeShareIntent()
        }
        if (reportState.error != null) {
            scope.launch { snackbarHostState.showSnackbar(reportFailed) }
            reportViewModel.consumeError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMedication,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_medication)) },
            )
        },
        topBar = {
            TopAppBar(
                title = { Text(course?.name ?: "Course") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (course != null) {
                        IconButton(onClick = { onEdit(course.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit course")
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.add_note)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.NoteAdd, null) },
                                    onClick = {
                                        menuOpen = false
                                        onAddNote(course.id)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.generate_pdf_report)) },
                                    leadingIcon = { Icon(Icons.Filled.Description, null) },
                                    onClick = {
                                        menuOpen = false
                                        reportViewModel.generate(course.id)
                                    },
                                    enabled = !reportState.generating,
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (course != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                course.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (course.endDate != null)
                                    "${course.startDate} to ${course.endDate}"
                                else "Starts ${course.startDate}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            course.notes?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        }
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.progress_notes_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (course != null) {
                        AssistChip(
                            onClick = { onAddNote(course.id) },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            label = { Text(stringResource(R.string.add_note)) },
                        )
                    }
                }
            }
            if (state.notes.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                stringResource(R.string.notes_empty_title),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                stringResource(R.string.notes_empty_subtitle),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(state.notes, key = { it.id }) { note ->
                    NoteCard(note = note, onClick = { onEditNote(note.id) })
                }
            }

            item {
                Text(
                    "Medications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(state.medications, key = { it.id }) { med ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenMedication(med.id) },
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(med.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                formatDosage(med.dosageAmount, med.dosageUnit),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            med.status.name.lowercase().replaceFirstChar { it.titlecase() },
                        )
                    }
                }
            }
        }

        if (reportState.generating) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                        Text(stringResource(R.string.report_generating))
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: CourseNote, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = onClick,
                    label = { Text(noteCategoryLabel(note.category)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    ),
                )
                Text(
                    formatNoteTimestamp(note.occurredAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(note.body)
            note.photoPath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
            }
        }
    }
}

@Composable
private fun noteCategoryLabel(category: NoteCategory): String = when (category) {
    NoteCategory.OBSERVATION -> stringResource(R.string.note_category_observation)
    NoteCategory.SYMPTOM -> stringResource(R.string.note_category_symptom)
    NoteCategory.SIDE_EFFECT -> stringResource(R.string.note_category_side_effect)
    NoteCategory.VET_VISIT -> stringResource(R.string.note_category_vet_visit)
}

private fun formatNoteTimestamp(instant: kotlinx.datetime.Instant): String {
    val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val month = ldt.month.name.lowercase().replaceFirstChar { it.titlecase() }.take(3)
    return "${ldt.dayOfMonth} $month ${ldt.year} · %02d:%02d".format(ldt.hour, ldt.minute)
}
