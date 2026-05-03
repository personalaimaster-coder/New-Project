package com.example.petmeds.ui.meds

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.petmeds.data.ocr.OcrMedResult
import com.example.petmeds.ui.theme.Brand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrReviewScreen(
    imageUri: Uri,
    onConfirm: (List<OcrMedResult>) -> Unit,
    onRetake: () -> Unit,
    onManual: () -> Unit,
    onClose: () -> Unit,
    viewModel: OcrReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(imageUri) { viewModel.processImage(imageUri) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Review Scan", style = MaterialTheme.typography.titleLarge) },
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
            val resultsState = state as? OcrReviewUiState.Results
            if (resultsState != null) {
                Surface(
                    tonalElevation = 4.dp,
                    modifier = Modifier.navigationBarsPadding().imePadding(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onRetake,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Retake")
                        }
                        Button(
                            onClick = { onConfirm(resultsState.medications) },
                            enabled = resultsState.medications.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Brand.SageDark,
                                contentColor = Color.White,
                            ),
                            modifier = Modifier.weight(2f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(Icons.Filled.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Confirm & Add", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        },
    ) { padding ->
        when (val s = state) {
            is OcrReviewUiState.Loading -> ScanningLoadingState(
                imageUri = imageUri,
                modifier = Modifier.padding(padding),
            )

            is OcrReviewUiState.Results -> ResultsContent(
                state = s,
                onUpdate = { index, med -> viewModel.updateMedication(index, med) },
                onRemove = { index -> viewModel.removeMedication(index) },
                modifier = Modifier.padding(padding),
            )

            is OcrReviewUiState.NoResults -> NoResultsState(
                reason = s.reason,
                rawText = s.rawText,
                onRetake = onRetake,
                onManual = onManual,
                modifier = Modifier.padding(padding),
            )

            is OcrReviewUiState.Error -> ErrorState(
                message = s.message,
                onRetake = onRetake,
                onManual = onManual,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

// ── Loading state with scan animation ─────────────────────────────────────────

@Composable
private fun ScanningLoadingState(imageUri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scanLine",
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(20.dp)),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(imageUri).crossfade(true).build(),
                contentDescription = "Prescription image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Semi-transparent overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
            )
            // Animated scan line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(horizontal = 16.dp)
                    .align(Alignment.TopStart)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Brand.GrassGreen, Color.Transparent),
                        ),
                    )
                    .padding(top = (scanY * 260).dp),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(320.dp))
            CircularProgressIndicator(
                color = Brand.SageDark,
                modifier = Modifier.size(36.dp),
            )
            Text(
                "Reading prescription…",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "This runs entirely on your device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Results list ──────────────────────────────────────────────────────────────

@Composable
private fun ResultsContent(
    state: OcrReviewUiState.Results,
    onUpdate: (Int, OcrMedResult) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            // Thumbnail of original image
            state.imageUri?.let { uri ->
                val context = LocalContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context).data(uri).crossfade(true).build(),
                    contentDescription = "Scanned prescription",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
                Spacer(Modifier.height(4.dp))
            }

            Text(
                "${state.medications.size} medication${if (state.medications.size != 1) "s" else ""} detected — review and edit before saving",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        itemsIndexed(state.medications) { index, med ->
            OcrMedCard(
                index = index,
                med = med,
                onUpdate = { updated -> onUpdate(index, updated) },
                onRemove = { onRemove(index) },
                canRemove = state.medications.size > 1,
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Single OCR medication card ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OcrMedCard(
    index: Int,
    med: OcrMedResult,
    onUpdate: (OcrMedResult) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Medication ${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (canRemove) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Name
            OutlinedTextField(
                value = med.name,
                onValueChange = { onUpdate(med.copy(name = it)) },
                label = { Text("Medication name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            // Confidence indicator
            if (med.confidenceHigh) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brand.Sage.copy(alpha = 0.35f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        Icons.Filled.Verified,
                        contentDescription = null,
                        tint = Brand.SageDark,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        "Name recognised",
                        style = MaterialTheme.typography.labelSmall,
                        color = Brand.SageDark,
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brand.WarningAmberLight)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = Brand.WarningAmber,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        "Verify name — not in known drug list",
                        style = MaterialTheme.typography.labelSmall,
                        color = Brand.WarningAmber,
                    )
                }
            }

            // Dosage amount + unit
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = med.dosageAmount,
                    onValueChange = { onUpdate(med.copy(dosageAmount = it)) },
                    label = { Text("Amount") },
                    placeholder = { Text("e.g. 10") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = med.dosageUnit,
                    onValueChange = { onUpdate(med.copy(dosageUnit = it)) },
                    label = { Text("Unit") },
                    placeholder = { Text("e.g. mg") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                )
            }

            // Form chips
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
                    "pill" to "💊 Pill",
                    "liquid" to "🧴 Liquid",
                    "eye_drop" to "👁️ Eye drops",
                    "ear_drop" to "👂 Ear drops",
                    "topical" to "🩹 Topical",
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = med.form == key,
                        onClick = { onUpdate(med.copy(form = if (med.form == key) "" else key)) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            // Frequency
            Text(
                "Frequency",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "prn" to "As needed",
                    "daily_times" to freqLabel(med.timesPerDay),
                    "interval" to intervalLabel(med.intervalHours.takeIf { it > 0 }),
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = med.frequencyType == key,
                        onClick = { onUpdate(med.copy(frequencyType = key)) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            // Times per day (shown when daily_times selected)
            AnimatedVisibility(
                visible = med.frequencyType == "daily_times",
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = "${med.timesPerDay}x per day",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Times per day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        (1..6).forEach { n ->
                            DropdownMenuItem(
                                text = { Text("$n time${if (n > 1) "s" else ""} per day") },
                                onClick = { onUpdate(med.copy(timesPerDay = n)); expanded = false },
                            )
                        }
                    }
                }
            }

            // Interval hours (shown when interval selected)
            AnimatedVisibility(
                visible = med.frequencyType == "interval",
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = "Every ${med.intervalHours}h",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Interval") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf(4, 6, 8, 12, 24).forEach { h ->
                            DropdownMenuItem(
                                text = { Text("Every ${h}h") },
                                onClick = { onUpdate(med.copy(intervalHours = h)); expanded = false },
                            )
                        }
                    }
                }
            }

            // Notes (if any)
            if (med.notes.isNotBlank()) {
                OutlinedTextField(
                    value = med.notes,
                    onValueChange = { onUpdate(med.copy(notes = it)) },
                    label = { Text("Notes") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Raw OCR reference
            if (med.rawText.isNotBlank()) {
                val preview = med.rawText.take(120) + if (med.rawText.length > 120) "…" else ""
                Text(
                    text = "Detected: \"$preview\"",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

// ── Empty / error states ───────────────────────────────────────────────────────

@Composable
private fun NoResultsState(
    reason: NoResultsReason,
    rawText: String,
    onRetake: () -> Unit,
    onManual: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("📋", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            if (reason == NoResultsReason.NO_TEXT) "No text detected"
            else "Couldn't parse medications",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (reason == NoResultsReason.NO_TEXT)
                "The image didn't contain readable text. Ensure the prescription is well-lit and in focus."
            else "Text was found but no medication entries could be extracted. Try a clearer image or enter manually.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRetake,
            colors = ButtonDefaults.buttonColors(
                containerColor = Brand.SageDark,
                contentColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Try Again")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onManual,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Enter Manually")
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetake: () -> Unit,
    onManual: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("⚠️", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "Scan failed",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Something went wrong while reading the image. Please try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRetake,
            colors = ButtonDefaults.buttonColors(
                containerColor = Brand.SageDark,
                contentColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("Try Again")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onManual,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("Enter Manually")
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

private fun freqLabel(timesPerDay: Int): String = when (timesPerDay) {
    1 -> "Once daily"
    2 -> "Twice daily"
    3 -> "3x daily"
    4 -> "4x daily"
    else -> "${timesPerDay}x daily"
}

private fun intervalLabel(hours: Int?): String =
    if (hours != null) "Every ${hours}h" else "Interval"
