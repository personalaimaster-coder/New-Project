package com.example.petmeds.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petmeds.domain.model.DoseStatus
import com.example.petmeds.ui.common.PawPillWordmark
import com.example.petmeds.ui.common.formatTime
import com.example.petmeds.ui.permissions.PermissionsCard
import com.example.petmeds.ui.theme.Brand
import com.example.petmeds.ui.theme.BrandGradients
import com.example.petmeds.ui.theme.MedicationAccents
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Composable
fun TimelineScreen(
    onAddMedication: () -> Unit,
    onEditMedication: (Long) -> Unit,
    onOpenMedication: (Long) -> Unit,
    viewModel: TimelineViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsStateWithLifecycle()

    when {
        state.loading -> LoadingState()
        !state.hasPet -> NoPetState()
        else -> TodayContent(
            state = state,
            onMarkTaken = viewModel::markTaken,
            onMarkTakenAt = viewModel::markTakenAt,
            onMarkSkipped = viewModel::markSkipped,
            onEditMedication = onEditMedication,
            onOpenMedication = onOpenMedication,
            onAddMedication = onAddMedication,
        )
    }
}

@Composable
private fun TimelineViewModel.collectAsStateWithLifecycle() =
    state.collectAsStateWithLifecycle()

// ---------- Top-level states ----------

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}

@Composable
private fun NoPetState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}

// ---------- Main content ----------

@Composable
private fun TodayContent(
    state: TimelineUiState,
    onMarkTaken: (TimelineItem) -> Unit,
    onMarkTakenAt: (TimelineItem, Instant) -> Unit,
    onMarkSkipped: (TimelineItem) -> Unit,
    onEditMedication: (Long) -> Unit,
    onOpenMedication: (Long) -> Unit,
    onAddMedication: () -> Unit,
) {
    var takenSheetItem by remember { mutableStateOf<TimelineItem?>(null) }
    var contextSheetItem by remember { mutableStateOf<TimelineItem?>(null) }
    var doneSectionExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { PawPillWordmark(badgeSize = 30.dp, textSize = 20) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Brand.Canvas,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {
            // ── Header ──────────────────────────────────────────────────────
            item {
                TodayHeader(
                    parentName = state.parentName,
                    summary = summaryHeadline(state),
                    todayDone = state.todayDone,
                    todayTotal = state.todayTotal,
                )
            }

            // ── Permissions banner ───────────────────────────────────────────
            item { PermissionsCard(modifier = Modifier.padding(horizontal = 20.dp)) }

            if (state.empty && !state.hasTodaySections) {
                item {
                    EmptyTodayState(
                        modifier = Modifier.fillParentMaxHeight(0.75f),
                        onAdd = onAddMedication,
                    )
                }
            } else {
                // ── Overdue ─────────────────────────────────────────────────
                if (state.overdue.isNotEmpty()) {
                    item {
                        SectionLabel(
                            text = "Overdue",
                            icon = Icons.Filled.Warning,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                    items(state.overdue, key = { it.key }) { item ->
                        OverdueCard(
                            item = item,
                            onOpen = { onOpenMedication(item.medicationId) },
                            onMarkTaken = { takenSheetItem = item },
                            onLongPress = { contextSheetItem = item },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        )
                    }
                }

                // ── Due Now ─────────────────────────────────────────────────
                if (state.dueNow.isNotEmpty()) {
                    item {
                        SectionLabel(
                            text = "Due Now",
                            icon = Icons.Filled.Notifications,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(state.dueNow, key = { it.key }) { item ->
                        DueNowCard(
                            item = item,
                            onOpen = { onOpenMedication(item.medicationId) },
                            onMarkTaken = { takenSheetItem = item },
                            onSkip = { onMarkSkipped(item) },
                            onLongPress = { contextSheetItem = item },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        )
                    }
                }

                // ── Up Next ─────────────────────────────────────────────────
                if (state.upNext.isNotEmpty()) {
                    item {
                        SectionLabel(
                            text = "Up next today",
                            icon = Icons.Filled.Schedule,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(state.upNext, key = { it.key }) { item ->
                        UpcomingCard(
                            item = item,
                            onOpen = { onOpenMedication(item.medicationId) },
                            onLongPress = { contextSheetItem = item },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        )
                    }
                }

                // ── Tomorrow ────────────────────────────────────────────────
                if (state.tomorrow.isNotEmpty()) {
                    item {
                        SectionLabel(
                            text = "Tomorrow",
                            icon = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            muted = true,
                        )
                    }
                    items(state.tomorrow, key = { it.key }) { item ->
                        UpcomingCard(
                            item = item,
                            onOpen = { onOpenMedication(item.medicationId) },
                            onLongPress = { contextSheetItem = item },
                            muted = true,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        )
                    }
                }

                // ── Done today ──────────────────────────────────────────────
                if (state.doneToday.isNotEmpty()) {
                    item {
                        DoneSectionHeader(
                            count = state.doneToday.size,
                            expanded = doneSectionExpanded,
                            onToggle = { doneSectionExpanded = !doneSectionExpanded },
                        )
                    }
                    if (doneSectionExpanded) {
                        items(state.doneToday, key = { it.key }) { item ->
                            DoneCard(
                                item = item,
                                onOpen = { onOpenMedication(item.medicationId) },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                // All-done celebration
                if (state.todayTotal > 0 && state.todayDone >= state.todayTotal) {
                    item { AllDoneBanner(petName = state.petName) }
                }
            }
        }
    }

    // ── Mark-as-Taken sheet ──────────────────────────────────────────────
    takenSheetItem?.let { item ->
        MarkAsTakenSheet(
            item = item,
            onTakenNow = { onMarkTaken(item); takenSheetItem = null },
            onTakenAt = { at -> onMarkTakenAt(item, at); takenSheetItem = null },
            onDismiss = { takenSheetItem = null },
        )
    }

    // ── Context sheet ────────────────────────────────────────────────────
    contextSheetItem?.let { item ->
        DoseContextSheet(
            item = item,
            onMarkTaken = { takenSheetItem = item; contextSheetItem = null },
            onSkip = { onMarkSkipped(item); contextSheetItem = null },
            onEditMedication = { onEditMedication(item.medicationId); contextSheetItem = null },
            onDismiss = { contextSheetItem = null },
        )
    }
}

// ---------- Header ----------

@Composable
private fun TodayHeader(
    parentName: String?,
    summary: String,
    todayDone: Int,
    todayTotal: Int,
) {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = now.hour
    val (timeOfDay, todIcon, todTint) = when {
        hour < 12 -> Triple("Good morning", Icons.Filled.WbSunny, Brand.Coral)
        hour < 18 -> Triple("Good afternoon", Icons.Filled.WbSunny, Brand.Apricot)
        else      -> Triple("Good evening", Icons.Filled.NightsStay, Brand.Lavender)
    }
    val greeting = if (!parentName.isNullOrBlank()) "$timeOfDay, $parentName" else timeOfDay
    val dateLine = "${now.dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase() }}, " +
        "${now.dayOfMonth} ${now.month.name.lowercase().replaceFirstChar { it.titlecase() }}"
    val progress = if (todayTotal > 0) todayDone.toFloat() / todayTotal else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandGradients.Hero),
    ) {
        // Decorative blob — soft sun peeking from the top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 32.dp, y = 12.dp)
                .size(160.dp)
                .clip(CircleShape)
                .background(todTint.copy(alpha = 0.18f)),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    todIcon,
                    contentDescription = null,
                    tint = todTint,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "$greeting  ·  $dateLine",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                summary,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Brand.DarkBlue,
            )

            if (todayTotal > 0) {
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(50)),
                        color = Brand.SageDark,
                        trackColor = Brand.Sage.copy(alpha = 0.28f),
                        strokeCap = StrokeCap.Round,
                    )
                    Text(
                        "$todayDone of $todayTotal done",
                        style = MaterialTheme.typography.labelMedium,
                        color = Brand.SageDark,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

private fun summaryHeadline(state: TimelineUiState): String {
    val left = state.overdue.size + state.dueNow.size + state.upNext.size
    return when {
        !state.hasTodaySections && state.empty -> "Let's set up the first med"
        state.todayTotal == 0                  -> "No doses today — relax!"
        left == 0                              -> "All paws up today!"
        left == 1                              -> "Just 1 dose to go"
        else                                   -> "$left doses left today"
    }
}

// ---------- Section label ----------

@Composable
private fun SectionLabel(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    tint: Color,
    muted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else tint,
        )
    }
}

@Composable
private fun DoneSectionHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Brand.Sage,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "Done today · $count",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(
            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ---------- Dose card variants ----------

@Composable
private fun OverdueCard(
    item: TimelineItem,
    onOpen: () -> Unit,
    onMarkTaken: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpen,
                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress() },
            ),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.error),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatTime(item.scheduledAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.weight(1f))
                    MedColorDot(item.colorIndex)
                }
                Text(item.medName, style = MaterialTheme.typography.titleMedium)
                Text(
                    item.dosage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        Button(
            onClick = onMarkTaken,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 14.dp)
                .semantics { contentDescription = "Mark ${item.medName} as taken" },
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Mark as taken now")
        }
    }
}

@Composable
private fun DueNowCard(
    item: TimelineItem,
    onOpen: () -> Unit,
    onMarkTaken: () -> Unit,
    onSkip: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val accent = MedicationAccents[item.colorIndex]
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = accent.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
            )
            .combinedClickable(
                onClick = onOpen,
                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress() },
            ),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brand.Sage),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatTime(item.scheduledAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = Brand.SageDark,
                    )
                    Spacer(Modifier.weight(1f))
                    MedColorDot(item.colorIndex)
                }
                Text(item.medName, style = MaterialTheme.typography.titleMedium)
                Text(
                    item.dosage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onMarkTaken,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Brand.SageDark,
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Mark ${item.medName} as taken" },
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Taken")
            }
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Skip") }
        }
    }
}

@Composable
private fun UpcomingCard(
    item: TimelineItem,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    muted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val alpha = if (muted) 0.55f else 1f
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (muted) 0.5f else 1f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpen,
                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress() },
            ),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MedColorDot(item.colorIndex, size = 10.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    item.medName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    ),
                )
                Text(
                    item.dosage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    ),
                )
            }
            Text(
                formatTime(item.scheduledAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
    }
}

@Composable
private fun DoneCard(
    item: TimelineItem,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            if (item.status == DoseStatus.TAKEN) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
            contentDescription = null,
            tint = if (item.status == DoseStatus.TAKEN) Brand.Sage else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                item.medName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                ),
            )
            Text(
                item.dosage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        }
        Text(
            formatTime(item.scheduledAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        )
    }
}

// ---------- Empty state ----------

@Composable
private fun EmptyTodayState(
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = 40.dp),
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
            Text("🐾💊", fontSize = 44.sp)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Ready when you are",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Brand.DarkBlue,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Add your pet's first medicine — we'll handle the reminders so you can keep cuddling.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(
                containerColor = Brand.Coral,
                contentColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add medication", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ---------- All-done banner ----------

@Composable
private fun AllDoneBanner(petName: String = "") {
    val title = if (petName.isNotBlank()) "$petName is all set today!" else "All set for today!"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(BrandGradients.Celebrate),
    ) {
        // Decorative confetti dots
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 18.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.55f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 28.dp, end = 38.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(Brand.Coral.copy(alpha = 0.7f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 14.dp, start = 14.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.5f)),
        )

        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("🎉", fontSize = 36.sp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Brand.SageOn,
                )
                Text(
                    "Every dose on track. Give a treat — you've earned it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Brand.SageOn.copy(alpha = 0.85f),
                )
            }
        }
    }
}

// ---------- Shared helpers ----------

@Composable
private fun MedColorDot(colorIndex: Int, size: androidx.compose.ui.unit.Dp = 8.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MedicationAccents[colorIndex]),
    )
}

// ---------- MarkAsTakenSheet ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkAsTakenSheet(
    item: TimelineItem,
    onTakenNow: () -> Unit,
    onTakenAt: (Instant) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showTimePicker by remember { mutableStateOf(false) }
    val nowLdt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val timeState = rememberTimePickerState(initialHour = nowLdt.hour, initialMinute = nowLdt.minute)
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Pill indicator
            Box(
                Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )

            // Animated checkmark
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Brand.Sage.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Brand.SageDark,
                    modifier = Modifier.size(40.dp),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Mark as taken?", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${item.medName} · ${item.dosage}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!showTimePicker) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTakenNow()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Brand.SageDark,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Yes, taken now", style = MaterialTheme.typography.labelLarge)
                }
                TextButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Different time…")
                }
            } else {
                TimePicker(state = timeState)
                Button(
                    onClick = {
                        val tz = TimeZone.currentSystemDefault()
                        val today = Clock.System.now().toLocalDateTime(tz).date
                        val at = LocalDateTime(
                            date = today,
                            time = LocalTime(timeState.hour, timeState.minute),
                        ).toInstant(tz)
                        onTakenAt(at)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Brand.SageDark,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) { Text("Confirm time") }
                TextButton(onClick = { showTimePicker = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}

// ---------- Context sheet ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoseContextSheet(
    item: TimelineItem,
    onMarkTaken: () -> Unit,
    onSkip: () -> Unit,
    onEditMedication: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
        ) {
            Text(
                item.medName,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Text(
                "${item.dosage} · ${formatTime(item.scheduledAt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            if (item.status == DoseStatus.PENDING) {
                ContextAction(label = "Mark as taken at a different time…") { onMarkTaken() }
                ContextAction(label = "Skip this dose") { onSkip() }
            }
            ContextAction(
                icon = Icons.Filled.Edit,
                label = "Edit medication",
            ) { onEditMedication() }
        }
    }
}

@Composable
private fun ContextAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    label: String,
    tint: Color = Color.Unspecified,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint.takeUnless { it == Color.Unspecified } ?: MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.weight(1f))
    }
}
