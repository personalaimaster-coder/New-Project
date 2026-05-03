package com.example.petmeds.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petmeds.ui.common.PawPillWordmark
import com.example.petmeds.ui.common.formatDosage
import com.example.petmeds.ui.theme.Brand
import com.example.petmeds.ui.theme.BrandGradients

private enum class HistorySegment { MEDICATIONS, COURSES }

@Composable
fun HistoryScreen(
    onOpenMedication: (Long) -> Unit,
    onOpenCourse: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var segment by remember { mutableStateOf(HistorySegment.MEDICATIONS) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { PawPillWordmark(badgeSize = 30.dp, textSize = 20) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = segment == HistorySegment.MEDICATIONS,
                        onClick = { segment = HistorySegment.MEDICATIONS },
                        label = { Text("Medications") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                    FilterChip(
                        selected = segment == HistorySegment.COURSES,
                        onClick = { segment = HistorySegment.COURSES },
                        label = { Text("Courses") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
            if (segment == HistorySegment.MEDICATIONS) {
                if (state.medications.isEmpty()) {
                    item { EmptyHistoryState("No completed medications yet — completed treatments will appear here.") }
                } else {
                    state.medications.groupBy { it.monthLabel }.forEach { (month, rows) ->
                        item { HistoryMonthHeader(month) }
                        items(rows, key = { it.medication.id }) { row ->
                            MedicationHistoryCard(row = row, onClick = { onOpenMedication(row.medication.id) })
                        }
                    }
                }
            } else {
                if (state.courses.isEmpty()) {
                    item { EmptyHistoryState("No completed courses yet — completed treatments will appear here.") }
                } else {
                    state.courses.groupBy { it.monthLabel }.forEach { (month, rows) ->
                        item { HistoryMonthHeader(month) }
                        items(rows, key = { it.course.id }) { row ->
                            CourseHistoryCard(row = row, onClick = { onOpenCourse(row.course.id) })
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun HistoryMonthHeader(month: String) {
    Text(
        month,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun MedicationHistoryCard(row: MedicationHistoryRow, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(row.medication.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(formatDosage(row.medication.dosageAmount, row.medication.dosageUnit), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (row.courseName != null) {
                Text("Course: ${row.courseName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(row.doseStats, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun CourseHistoryCard(row: CourseHistoryRow, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(row.course.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${row.medCount} medications", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(row.doseStats, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun EmptyHistoryState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .size(108.dp)
                .clip(CircleShape)
                .background(BrandGradients.EmptyTile),
            contentAlignment = Alignment.Center,
        ) {
            Text("📒🐾", fontSize = 38.sp)
        }
        Text(
            "No history yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Brand.DarkBlue,
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
