package com.example.petmeds.ui.meds

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.petmeds.domain.model.MedForm
import com.example.petmeds.domain.model.Medication
import com.example.petmeds.domain.model.ScheduleConfig
import com.example.petmeds.ui.common.PawPillWordmark
import com.example.petmeds.ui.common.formatDosage
import com.example.petmeds.ui.theme.Brand
import com.example.petmeds.ui.theme.BrandGradients
import com.example.petmeds.ui.theme.MedicationAccents
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MedsListScreen(
    onAddMedication: (courseId: Long?) -> Unit,
    onAddCourse: () -> Unit,
    onOpenMedication: (Long) -> Unit,
    viewModel: MedsListViewModel = hiltViewModel(),
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var fabExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (fabExpanded) {
                    ExtendedFloatingActionButton(
                        onClick = { fabExpanded = false; onAddCourse() },
                        icon = { Icon(Icons.Filled.AddCircle, contentDescription = null) },
                        text = { Text("Add course") },
                        elevation = FloatingActionButtonDefaults.elevation(),
                    )
                    ExtendedFloatingActionButton(
                        onClick = { fabExpanded = false; onAddMedication(null) },
                        icon = { Icon(Icons.Filled.Medication, contentDescription = null) },
                        text = { Text("Add medication") },
                        elevation = FloatingActionButtonDefaults.elevation(),
                    )
                }
                LargeFloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(28.dp))
                }
            }
        },
    ) { padding ->
        if (sections.isEmpty()) {
            EmptyMedsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onAdd = { onAddMedication(null) },
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sections.forEach { section ->
                    stickyHeader(key = "course-${section.course?.id ?: 0L}") {
                        CourseHeader(
                            section = section,
                            onCompleteCourse = { id -> viewModel.markCourseComplete(id) },
                        )
                    }
                    if (section.medications.isEmpty()) {
                        item(key = "empty-course-${section.course?.id ?: 0L}") {
                            EmptyCourseCard(onAdd = { onAddMedication(section.course?.id) })
                        }
                    }
                    itemsIndexed(section.medications, key = { _, m -> m.id }) { idx, med ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    scope.launch {
                                        viewModel.deleteMedication(med.id)
                                        snackbarHostState.showSnackbar(
                                            message = "${med.name} removed",
                                        )
                                    }
                                    true
                                } else false
                            },
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    targetValue = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                                        MaterialTheme.colorScheme.errorContainer
                                    else androidx.compose.ui.graphics.Color.Transparent,
                                    animationSpec = spring(),
                                    label = "swipe-bg",
                                )
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            },
                            content = {
                                MedCard(
                                    med = med,
                                    colorIndex = (idx + (section.course?.colorIndex ?: 0)) % MedicationAccents.size,
                                    onOpen = { onOpenMedication(med.id) },
                                )
                            },
                        )
                    }
                }
                item { Spacer(Modifier.height(120.dp)) }
            }
        }
    }
}

@Composable
private fun CourseHeader(
    section: CourseSection,
    onCompleteCourse: (Long) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val course = section.course
    SurfaceHeader {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MedicationAccents[course?.colorIndex ?: 0]),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    course?.name ?: "Other medications",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    buildString {
                        append("${section.medications.size} medication")
                        if (section.medications.size != 1) append("s")
                        course?.endDate?.let { append(" · Until $it") }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (course != null) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Course actions")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mark course complete") },
                            leadingIcon = { Icon(Icons.Filled.DoneAll, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onCompleteCourse(course.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurfaceHeader(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        content()
    }
}

@Composable
private fun MedCard(
    med: Medication,
    colorIndex: Int,
    onOpen: () -> Unit,
) {
    val accent = MedicationAccents[colorIndex]
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent),
            )
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Text(
                    formEmoji(med.form),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    med.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    formatDosage(med.dosageAmount, med.dosageUnit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    scheduleLabel(med.schedule),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (med.endDate != null) {
                    Text(
                        "Until ${med.endDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCourseCard(onAdd: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onAdd)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column {
                Text(
                    "No medications yet",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Tap to add a medication to this course",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyMedsState(modifier: Modifier, onAdd: () -> Unit) {
    Column(
        modifier = modifier.padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(124.dp)
                .clip(CircleShape)
                .background(BrandGradients.EmptyTile),
            contentAlignment = Alignment.Center,
        ) {
            Text("💊✨", fontSize = 44.sp)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Your medicine cabinet awaits",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Brand.DarkBlue,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap + to add the first medication. Swipe a card left later to remove it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
    }
}

private fun formEmoji(form: MedForm): String = when (form) {
    MedForm.PILL     -> "💊"
    MedForm.LIQUID   -> "🧴"
    MedForm.DROP_EYE -> "👁️"
    MedForm.DROP_EAR -> "👂"
    MedForm.TOPICAL  -> "🩹"
}

private fun scheduleLabel(schedule: ScheduleConfig): String = when (schedule) {
    is ScheduleConfig.DailyTimes -> {
        val times = schedule.times.joinToString(" · ") { it.value }
        "$times, daily"
    }
    is ScheduleConfig.IntervalHours -> "Every ${schedule.intervalHours}h"
    is ScheduleConfig.SpecificDays -> {
        val days = schedule.daysOfWeek.sorted().joinToString(", ") { dayNum ->
            listOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").getOrNull(dayNum) ?: "$dayNum"
        }
        "${schedule.times.firstOrNull()?.value ?: ""} · $days"
    }
    ScheduleConfig.Prn -> "As needed"
}
