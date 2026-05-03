package com.example.petmeds.ui.meds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petmeds.R
import com.example.petmeds.domain.model.LocalTimeStr
import com.example.petmeds.domain.model.MedForm
import com.example.petmeds.ui.theme.Brand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedEditorScreen(
    medicationId: Long?,
    onClose: () -> Unit,
    viewModel: MedEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(medicationId) { viewModel.load(medicationId) }
    LaunchedEffect(state.saved) { if (state.saved) onClose() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (medicationId == null) "New medication" else "Edit medication",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
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
            // Sticky save bar
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
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    } else {
                        Text(
                            if (state.isEditMode || state.drafts.size == 1) "Save medication" else "Save medications",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CoursePicker(
                    courses = state.courses,
                    selectedCourseId = state.selectedCourseId,
                    onSelected = { id -> viewModel.update { it.copy(selectedCourseId = id) } },
                )
            }

            state.drafts.forEachIndexed { index, draft ->
                item {
                    MedicationDraftEditor(
                        index = index,
                        draft = draft,
                        canRemove = !state.isEditMode && state.drafts.size > 1,
                        canShareSchedule = !state.isEditMode && index > 0,
                        firstMedicationName = state.drafts.firstOrNull()?.name.orEmpty(),
                        onUpdate = { transform -> viewModel.updateDraft(index, transform) },
                        onAddTime = { time -> viewModel.addDailyTime(index, time) },
                        onRemoveTime = { time -> viewModel.removeDailyTime(index, time) },
                        onRemoveDraft = { viewModel.removeDraft(index) },
                    )
                }
            }

            if (!state.isEditMode) {
                item {
                    OutlinedButton(
                        onClick = viewModel::addDraft,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add another medication")
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun CoursePicker(
    courses: List<com.example.petmeds.domain.model.Course>,
    selectedCourseId: Long?,
    onSelected: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = courses.firstOrNull { it.id == selectedCourseId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected?.name ?: "No course",
            onValueChange = {},
            readOnly = true,
            label = { Text("Course") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("No course") },
                onClick = { onSelected(null); expanded = false },
            )
            courses.forEach { course ->
                DropdownMenuItem(
                    text = { Text(course.name) },
                    onClick = { onSelected(course.id); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun MedicationDraftEditor(
    index: Int,
    draft: MedDraft,
    canRemove: Boolean,
    canShareSchedule: Boolean,
    firstMedicationName: String,
    onUpdate: ((MedDraft) -> MedDraft) -> Unit,
    onAddTime: (LocalTimeStr) -> Unit,
    onRemoveTime: (LocalTimeStr) -> Unit,
    onRemoveDraft: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Medication ${index + 1}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (canRemove) {
                IconButton(onClick = onRemoveDraft) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove medication")
                }
            }
        }
        EditorCard(title = "Essentials", icon = Icons.Filled.Tune) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { v -> onUpdate { it.copy(name = v, nameError = false) } },
                    label = { Text("Medication name") },
                    placeholder = { Text("e.g. Apoquel") },
                    isError = draft.nameError,
                    supportingText = if (draft.nameError) {{ Text("Required") }} else null,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = draft.dosageAmount,
                        onValueChange = { v -> onUpdate { it.copy(dosageAmount = v, dosageError = false) } },
                        label = { Text("Amount") },
                        placeholder = { Text("1.5") },
                        isError = draft.dosageError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    )
                    UnitDropdown(
                        draft = draft,
                        onUpdate = onUpdate,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    "Form",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        MedForm.PILL to "💊 Pill",
                        MedForm.LIQUID to "🧴 Liquid",
                        MedForm.DROP_EYE to "👁️ Eye drops",
                        MedForm.DROP_EAR to "👂 Ear drops",
                        MedForm.TOPICAL to "🩹 Topical",
                    ).forEach { (form, label) ->
                        FilterChip(
                            selected = draft.form == form,
                            onClick = {
                                onUpdate {
                                    val nextUnit = if (it.dosageUnitIsCustom) it.dosageUnit else unitOptionsFor(form).first()
                                    it.copy(form = form, dosageUnit = nextUnit)
                                }
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
        }
        EditorCard(title = "Schedule", icon = Icons.Filled.Repeat) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (canShareSchedule) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Use same schedule as ${firstMedicationName.ifBlank { "Medication 1" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = draft.useScheduleFromFirst,
                            onCheckedChange = { checked -> onUpdate { it.copy(useScheduleFromFirst = checked) } },
                        )
                    }
                }
                if (!draft.useScheduleFromFirst || !canShareSchedule) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FreqTile(
                            label = "Specific times",
                            emoji = "🕐",
                            selected = draft.frequencyOption == FrequencyOption.DAILY_TIMES,
                            onClick = { onUpdate { it.copy(frequencyOption = FrequencyOption.DAILY_TIMES) } },
                            modifier = Modifier.weight(1f),
                        )
                        FreqTile(
                            label = "Every N hours",
                            emoji = "🔁",
                            selected = draft.frequencyOption == FrequencyOption.INTERVAL_HOURS,
                            onClick = { onUpdate { it.copy(frequencyOption = FrequencyOption.INTERVAL_HOURS) } },
                            modifier = Modifier.weight(1f),
                        )
                        FreqTile(
                            label = "As needed",
                            emoji = "🔔",
                            selected = draft.frequencyOption == FrequencyOption.PRN,
                            onClick = { onUpdate { it.copy(frequencyOption = FrequencyOption.PRN) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    when (draft.frequencyOption) {
                        FrequencyOption.DAILY_TIMES -> DailyTimesEditor(
                            times = draft.dailyTimes,
                            onAdd = onAddTime,
                            onRemove = onRemoveTime,
                        )
                        FrequencyOption.INTERVAL_HOURS -> IntervalEditor(
                            draft = draft,
                            onUpdate = onUpdate,
                        )
                        FrequencyOption.PRN -> Text(
                            "Log doses manually when given — no scheduled reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "Date range",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DateRow(
                        startDate = draft.startDate,
                        endDate = draft.endDate,
                        onStartChanged = { d -> onUpdate { it.copy(startDate = d) } },
                        onEndChanged = { d -> onUpdate { it.copy(endDate = d) } },
                    )
                }
            }
        }
        EditorCard(title = "Notes", icon = Icons.Filled.Notes) {
            OutlinedTextField(
                value = draft.notes,
                onValueChange = { v -> onUpdate { it.copy(notes = v) } },
                label = { Text("Notes") },
                placeholder = { Text("Give with food, watch for drowsiness…") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )
        }
    }
}

@Composable
private fun UnitDropdown(
    draft: MedDraft,
    onUpdate: ((MedDraft) -> MedDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = unitOptionsFor(draft.form)
    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = if (draft.dosageUnitIsCustom) "Custom…" else draft.dosageUnit,
                onValueChange = {},
                readOnly = true,
                label = { Text("Unit") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            expanded = false
                            if (option == CUSTOM_UNIT_LABEL) {
                                onUpdate { it.copy(dosageUnitIsCustom = true, dosageUnit = "") }
                            } else {
                                onUpdate { it.copy(dosageUnitIsCustom = false, dosageUnit = option) }
                            }
                        },
                    )
                }
            }
        }
        if (draft.dosageUnitIsCustom) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.dosageUnit,
                onValueChange = { v -> onUpdate { it.copy(dosageUnit = v) } },
                label = { Text("Custom unit") },
                placeholder = { Text("e.g. sachet") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EditorCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            content()
        }
    }
}

@Composable
private fun FreqTile(
    label: String,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.height(84.dp),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyTimesEditor(
    times: List<LocalTimeStr>,
    onAdd: (LocalTimeStr) -> Unit,
    onRemove: (LocalTimeStr) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    Column {
        Text(
            "Dose times",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            times.forEach { t ->
                AssistChip(
                    onClick = { onRemove(t) },
                    label = { Text(t.value) },
                    leadingIcon = {
                        Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                    },
                    trailingIcon = {
                        Icon(Icons.Filled.Close, contentDescription = "Remove ${t.value}", modifier = Modifier.size(AssistChipDefaults.IconSize))
                    },
                )
            }
            AssistChip(
                onClick = { showPicker = true },
                label = { Text("Add time") },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
            )
        }
    }
    if (showPicker) {
        TimePickerDialog(
            initialHour = 8, initialMinute = 0,
            onConfirm = { h, m -> onAdd(LocalTimeStr.of(h, m)); showPicker = false },
            onDismiss = { showPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntervalEditor(
    draft: MedDraft,
    onUpdate: ((MedDraft) -> MedDraft) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Interval",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(4, 6, 8, 12, 24).forEach { h ->
                FilterChip(
                    selected = draft.intervalHours == h,
                    onClick = { onUpdate { it.copy(intervalHours = h) } },
                    label = { Text("Every ${h}h") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
        Text(
            "Anchor: ${draft.intervalAnchor.value}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRow(
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
