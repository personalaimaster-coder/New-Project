package com.example.petmeds.ui.meds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petmeds.ui.theme.Brand
import com.example.petmeds.ui.theme.MedicationAccents

@Composable
fun CourseEditorScreen(
    courseId: Long?,
    onClose: () -> Unit,
    viewModel: CourseEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(courseId) { viewModel.load(courseId) }
    LaunchedEffect(state.saved) { if (state.saved) onClose() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (courseId == null) "New course" else "Edit course") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                modifier = Modifier.navigationBarsPadding().imePadding(),
            ) {
                Button(
                    onClick = viewModel::submit,
                    enabled = !state.saving,
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
                        Text("Save course")
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
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { v -> viewModel.update { it.copy(name = v, nameError = false) } },
                        label = { Text("Course name") },
                        placeholder = { Text("e.g. Allergy treatment") },
                        isError = state.nameError,
                        supportingText = if (state.nameError) {{ Text("Required") }} else null,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Course color", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MedicationAccents.forEachIndexed { index, color ->
                            FilterChip(
                                selected = state.colorIndex == index,
                                onClick = { viewModel.update { it.copy(colorIndex = index) } },
                                label = { Text(" ") },
                                leadingIcon = {
                                    Surface(
                                        color = color,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.height(16.dp),
                                    ) { Spacer(Modifier.padding(horizontal = 8.dp)) }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                    }
                    CourseDateRow(
                        startDate = state.startDate,
                        endDate = state.endDate,
                        onStartChanged = { d -> viewModel.update { it.copy(startDate = d) } },
                        onEndChanged = { d -> viewModel.update { it.copy(endDate = d) } },
                    )
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
                        label = { Text("Notes") },
                        placeholder = { Text("Why this course was prescribed, vet instructions…") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseDateRow(
    startDate: kotlinx.datetime.LocalDate,
    endDate: kotlinx.datetime.LocalDate?,
    onStartChanged: (kotlinx.datetime.LocalDate) -> Unit,
    onEndChanged: (kotlinx.datetime.LocalDate?) -> Unit,
) {
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = { showStart = true },
            label = { Text("Start: $startDate") },
            modifier = Modifier.weight(1f),
        )
        AssistChip(
            onClick = { showEnd = true },
            label = { Text(if (endDate != null) "End: $endDate" else "No end date") },
            modifier = Modifier.weight(1f),
        )
    }
    if (showStart) {
        DatePickerDialog(
            initial = startDate,
            onConfirm = { d -> onStartChanged(d); showStart = false },
            onDismiss = { showStart = false },
        )
    }
    if (showEnd) {
        DatePickerDialog(
            initial = endDate ?: startDate,
            onConfirm = { d -> onEndChanged(d); showEnd = false },
            onDismiss = { showEnd = false },
            allowClear = true,
            onClear = { onEndChanged(null); showEnd = false },
        )
    }
}
