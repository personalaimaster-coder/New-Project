package com.example.petmeds.ui.meds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petmeds.domain.model.DoseLog
import com.example.petmeds.domain.model.DoseStatus
import com.example.petmeds.domain.model.LifecycleStatus
import com.example.petmeds.domain.model.MedForm
import com.example.petmeds.domain.model.Medication
import com.example.petmeds.domain.model.MedicineReference
import com.example.petmeds.domain.model.ScheduleConfig
import com.example.petmeds.ui.common.formatDosage
import com.example.petmeds.ui.common.formatTime
import com.example.petmeds.ui.theme.Brand

@Composable
fun MedicationDetailScreen(
    medicationId: Long?,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: MedicationDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val med = state.medication
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmComplete by remember { mutableStateOf(false) }

    LaunchedEffect(medicationId) { viewModel.load(medicationId) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(med?.name ?: "Medication") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (med != null && med.status == LifecycleStatus.ACTIVE) {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Medication actions")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onEdit(med.id)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.delete()
                                        onBack()
                                    },
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
        bottomBar = {
            if (med != null) {
                Surface(
                    tonalElevation = 4.dp,
                    modifier = Modifier.navigationBarsPadding().imePadding(),
                ) {
                    if (med.status == LifecycleStatus.ACTIVE) {
                        Button(
                            onClick = { confirmComplete = true },
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
                            Text("Mark complete")
                        }
                    } else if (med.status == LifecycleStatus.COMPLETED) {
                        OutlinedButton(
                            onClick = viewModel::reactivate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null)
                            Text("Reactivate")
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (med == null && !state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Medication not found")
            }
        } else if (med != null) {
            MedicationDetailContent(
                med = med,
                courseName = state.course?.name,
                logs = state.logs,
                medicineInfo = state.medicineInfo,
                medicineInfoLoaded = state.medicineInfoLoaded,
                onTakenNow = viewModel::logTakenNow,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (confirmComplete) {
        AlertDialog(
            onDismissRequest = { confirmComplete = false },
            title = { Text("Mark complete?") },
            text = { Text("This stops future reminders for ${med?.name.orEmpty()} and moves it to History.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmComplete = false
                        viewModel.markComplete()
                    },
                ) { Text("Mark complete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmComplete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun MedicationDetailContent(
    med: Medication,
    courseName: String?,
    logs: List<DoseLog>,
    medicineInfo: MedicineReference?,
    medicineInfoLoaded: Boolean,
    onTakenNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(formEmojiDetail(med.form), style = MaterialTheme.typography.headlineMedium)
                        Column(Modifier.weight(1f)) {
                            Text(med.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(formatDosage(med.dosageAmount, med.dosageUnit), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(statusLabel(med), style = MaterialTheme.typography.labelLarge, color = Brand.SageDark)
                    if (courseName != null) Text("Course: $courseName", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            DetailCard(title = "Schedule") {
                Text(scheduleLabelDetail(med.schedule))
                Text(
                    if (med.endDate != null) "${med.startDate} to ${med.endDate}" else "Starts ${med.startDate}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!med.notes.isNullOrBlank()) {
            item {
                DetailCard(title = "Notes") { Text(med.notes) }
            }
        }
        if (med.status == LifecycleStatus.ACTIVE) {
            item {
                Button(
                    onClick = onTakenNow,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) { Text("Mark taken now") }
            }
        }
        item {
            Text("Dose history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (logs.isEmpty()) {
            item { Text("No doses logged yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(logs, key = { it.id }) { log ->
                DoseLogRow(log)
            }
        }
        if (medicineInfoLoaded) {
            item {
                MedicineInfoSection(medicineInfo)
            }
        }
    }
}

@Composable
private fun MedicineInfoSection(info: MedicineReference?) {
    if (info == null) {
        Text(
            "No medicine information available.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    DetailCard(title = "Medicine Information") {
        MedicineInfoRow("Generic Name", info.genericName)
        MedicineInfoRow("Composition", info.composition)
        MedicineInfoRow("Manufacturer", info.manufacturer)
        MedicineInfoRow("Dosage Form", info.dosageForm)
        MedicineInfoRow("Category", info.category)
        Spacer(Modifier.height(4.dp))
        Text(
            "Clinical Indication",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            info.indication,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun MedicineInfoRow(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun DoseLogRow(log: DoseLog) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(log.status.name.lowercase().replaceFirstChar { it.titlecase() }, modifier = Modifier.weight(1f))
        Text(
            formatTime(log.takenAt ?: log.scheduledAt ?: log.createdAt),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun statusLabel(med: Medication): String = when (med.status) {
    LifecycleStatus.ACTIVE -> "Active"
    LifecycleStatus.COMPLETED -> "Completed${med.completedAt?.let { " on ${formatTime(it)}" }.orEmpty()}"
    LifecycleStatus.DELETED -> "Deleted"
}

private fun formEmojiDetail(form: MedForm): String = when (form) {
    MedForm.PILL -> "💊"
    MedForm.LIQUID -> "🧴"
    MedForm.DROP_EYE -> "👁️"
    MedForm.DROP_EAR -> "👂"
    MedForm.TOPICAL -> "🩹"
}

fun scheduleLabelDetail(schedule: ScheduleConfig): String = when (schedule) {
    is ScheduleConfig.DailyTimes -> "${schedule.times.joinToString(" · ") { it.value }}, daily"
    is ScheduleConfig.IntervalHours -> "Every ${schedule.intervalHours}h from ${schedule.anchor.value}"
    is ScheduleConfig.SpecificDays -> {
        val days = schedule.daysOfWeek.sorted().joinToString(", ") { dayNum ->
            listOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").getOrNull(dayNum) ?: "$dayNum"
        }
        "${schedule.times.joinToString(" · ") { it.value }} · $days"
    }
    ScheduleConfig.Prn -> "As needed"
}
