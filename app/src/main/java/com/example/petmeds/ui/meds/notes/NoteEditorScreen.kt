package com.example.petmeds.ui.meds.notes

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.petmeds.R
import com.example.petmeds.domain.model.NoteCategory
import com.example.petmeds.ui.meds.DatePickerDialog
import com.example.petmeds.ui.meds.TimePickerDialog
import com.example.petmeds.ui.theme.Brand
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File

@Composable
fun NoteEditorScreen(
    courseId: Long,
    noteId: Long?,
    onClose: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(courseId, noteId) { viewModel.init(courseId, noteId) }
    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onClose()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::setPhotoUri) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) pendingCameraUri?.let(viewModel::setPhotoUri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = createNoteCameraUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditing) stringResource(R.string.edit_note)
                        else stringResource(R.string.new_note),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (state.isEditing) {
                        IconButton(onClick = viewModel::delete, enabled = !state.deleting) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = stringResource(R.string.delete),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Button(
                    onClick = viewModel::submit,
                    enabled = !state.saving && !state.deleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Brand.SageDark,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (state.saving) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Text(text = " " + stringResource(R.string.save))
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NoteWhenRow(
                        occurredAtFormatted = formatNoteWhen(state.occurredAt),
                        onPickDate = { viewModel.setDate(it) },
                        onPickTime = { hour, minute -> viewModel.setTime(hour, minute) },
                        currentDate = state.occurredAt
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date,
                        currentTimeHour = state.occurredAt
                            .toLocalDateTime(TimeZone.currentSystemDefault()).hour,
                        currentTimeMinute = state.occurredAt
                            .toLocalDateTime(TimeZone.currentSystemDefault()).minute,
                    )
                    Text(
                        stringResource(R.string.note_category_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    NoteCategoryRow(
                        selected = state.category,
                        onSelected = { c -> viewModel.update { it.copy(category = c) } },
                    )
                    OutlinedTextField(
                        value = state.body,
                        onValueChange = { v ->
                            viewModel.update { it.copy(body = v, bodyError = false) }
                        },
                        label = { Text(stringResource(R.string.note_body_label)) },
                        placeholder = { Text(stringResource(R.string.note_body_placeholder)) },
                        isError = state.bodyError,
                        supportingText = if (state.bodyError) {
                            { Text(stringResource(R.string.error_required)) }
                        } else null,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp),
                    )
                    Text(
                        stringResource(R.string.note_photo_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    NotePhotoSection(
                        photoPath = state.photoPath,
                        onPickGallery = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                        onPickCamera = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onRemove = { viewModel.removePhoto() },
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteWhenRow(
    occurredAtFormatted: String,
    onPickDate: (kotlinx.datetime.LocalDate) -> Unit,
    onPickTime: (Int, Int) -> Unit,
    currentDate: kotlinx.datetime.LocalDate,
    currentTimeHour: Int,
    currentTimeMinute: Int,
) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.note_when_label), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { showDate = true },
                label = { Text(occurredAtFormatted.substringBefore(" · ")) },
                modifier = Modifier.weight(1f),
            )
            AssistChip(
                onClick = { showTime = true },
                label = { Text(occurredAtFormatted.substringAfter(" · ")) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    if (showDate) {
        DatePickerDialog(
            initial = currentDate,
            onConfirm = { d -> onPickDate(d); showDate = false },
            onDismiss = { showDate = false },
        )
    }
    if (showTime) {
        TimePickerDialog(
            initialHour = currentTimeHour,
            initialMinute = currentTimeMinute,
            onConfirm = { h, m -> onPickTime(h, m); showTime = false },
            onDismiss = { showTime = false },
        )
    }
}

@Composable
private fun NoteCategoryRow(
    selected: NoteCategory,
    onSelected: (NoteCategory) -> Unit,
) {
    val items = listOf(
        NoteCategory.OBSERVATION to R.string.note_category_observation,
        NoteCategory.SYMPTOM to R.string.note_category_symptom,
        NoteCategory.SIDE_EFFECT to R.string.note_category_side_effect,
        NoteCategory.VET_VISIT to R.string.note_category_vet_visit,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items.forEach { (cat, labelRes) ->
            FilterChip(
                selected = selected == cat,
                onClick = { onSelected(cat) },
                label = { Text(stringResource(labelRes)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun NotePhotoSection(
    photoPath: String?,
    onPickGallery: () -> Unit,
    onPickCamera: () -> Unit,
    onRemove: () -> Unit,
) {
    if (photoPath != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 0.dp),
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.6f),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.note_photo_remove),
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(6.dp),
                    )
                }
            }
        }
    } else {
        var menuOpen by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(
                onClick = { menuOpen = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Text(text = "  " + stringResource(R.string.note_photo_add), fontWeight = FontWeight.SemiBold)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Choose from gallery") },
                    leadingIcon = { Icon(Icons.Filled.PhotoLibrary, null) },
                    onClick = { menuOpen = false; onPickGallery() },
                )
                DropdownMenuItem(
                    text = { Text("Take a photo") },
                    leadingIcon = { Icon(Icons.Filled.CameraAlt, null) },
                    onClick = { menuOpen = false; onPickCamera() },
                )
            }
        }
    }
}

private fun formatNoteWhen(instant: kotlinx.datetime.Instant): String {
    val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val datePart = "%d %s %d".format(
        ldt.dayOfMonth,
        ldt.month.name.lowercase().replaceFirstChar { it.titlecase() }.take(3),
        ldt.year,
    )
    val timePart = "%02d:%02d".format(ldt.hour, ldt.minute)
    return "$datePart · $timePart"
}

internal fun createNoteCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "note_captures").apply { mkdirs() }
    val file = File.createTempFile("note_", ".jpg", dir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
