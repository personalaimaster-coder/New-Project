package com.example.petmeds.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petmeds.domain.model.Species
import com.example.petmeds.ui.common.PawPillWordmark
import com.example.petmeds.ui.meds.DatePickerDialog
import com.example.petmeds.ui.pets.FirstRunPetViewModel
import com.example.petmeds.ui.theme.Brand
import com.example.petmeds.ui.theme.BrandGradients
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val PAGE_COUNT = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingPager(
    onFinished: () -> Unit,
    viewModel: FirstRunPetViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.saved) { if (state.saved) onFinished() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brand.Cream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Wordmark — fixed top-left so the brand stays present across pages
        PawPillWordmark(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 20.dp),
            badgeSize = 30.dp,
            textSize = 20,
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> OnboardingPage1()
                1 -> OnboardingPage2()
                2 -> OnboardingPage3(
                    parentName = state.parentName,
                    name = state.name,
                    species = state.species,
                    breed = state.breed,
                    weightKg = state.weightKg,
                    birthDate = state.birthDate,
                    nameError = state.nameError,
                    saving = state.saving,
                    onParentNameChanged = viewModel::onParentNameChanged,
                    onNameChanged = viewModel::onNameChanged,
                    onSpeciesChanged = viewModel::onSpeciesChanged,
                    onBreedChanged = viewModel::onBreedChanged,
                    onWeightChanged = viewModel::onWeightChanged,
                    onBirthDateChanged = viewModel::onBirthDateChanged,
                )
            }
        }

        // Dot indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 104.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(PAGE_COUNT) { i ->
                val selected = i == pagerState.currentPage
                val width by animateDpAsState(
                    targetValue = if (selected) 24.dp else 8.dp,
                    animationSpec = spring(),
                    label = "dot-width",
                )
                Box(
                    Modifier
                        .height(8.dp)
                        .width(width)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                        ),
                )
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (pagerState.currentPage > 0) {
                TextButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    modifier = Modifier.weight(1f),
                ) { Text("Back") }
            } else {
                Spacer(Modifier.weight(1f))
            }
            Button(
                onClick = {
                    if (pagerState.currentPage < PAGE_COUNT - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        viewModel.submit()
                    }
                },
                enabled = !state.saving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(56.dp),
            ) {
                if (state.saving && pagerState.currentPage == PAGE_COUNT - 1) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Text(
                        if (pagerState.currentPage < PAGE_COUNT - 1) "Next" else "Get started",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage1() {
    OnboardingPageTemplate(
        emoji = "⏰",
        title = "Reminders your\npet can count on",
        body = "Never miss a dose again. PawPill sends reliable\nalerts for every medication, every time.",
        circleBrush = BrandGradients.OnboardingWarm,
    )
}

@Composable
private fun OnboardingPage2() {
    OnboardingPageTemplate(
        emoji = "🔒",
        title = "Stays on your\nphone, always",
        body = "All data lives on your device — no accounts, no\ncloud, no tracking. 100% private.",
        circleBrush = BrandGradients.OnboardingCool,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingPage3(
    parentName: String,
    name: String,
    species: Species,
    breed: String,
    weightKg: String,
    birthDate: LocalDate?,
    nameError: Boolean,
    saving: Boolean,
    onParentNameChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onSpeciesChanged: (Species) -> Unit,
    onBreedChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onBirthDateChanged: (LocalDate?) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    if (showDatePicker) {
        DatePickerDialog(
            initial = birthDate ?: today,
            onConfirm = { date ->
                onBirthDateChanged(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            allowClear = birthDate != null,
            onClear = {
                onBirthDateChanged(null)
                showDatePicker = false
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp)
            .padding(top = 96.dp, bottom = 160.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(BrandGradients.OnboardingFresh),
            contentAlignment = Alignment.Center,
        ) {
            Text("🐾", fontSize = 40.sp)
        }
        Text(
            "Let's meet your pet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Brand.DarkBlue,
            textAlign = TextAlign.Center,
        )
        Text(
            "You can always update this later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedTextField(
            value = parentName,
            onValueChange = onParentNameChanged,
            label = { Text("Your name (optional)") },
            placeholder = { Text("e.g. Sarah") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            label = { Text("Pet's name") },
            placeholder = { Text("Buddy, Whiskers…") },
            isError = nameError,
            supportingText = if (nameError) {{ Text("Please enter a name") }} else null,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Species",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            listOf(Species.DOG to "🐶", Species.CAT to "🐱", Species.RABBIT to "🐰").forEach { (sp, emoji) ->
                FilterChip(
                    selected = species == sp,
                    onClick = { onSpeciesChanged(sp) },
                    label = { Text(emoji) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            listOf(Species.BIRD to "🐦", Species.OTHER to "🐾").forEach { (sp, emoji) ->
                FilterChip(
                    selected = species == sp,
                    onClick = { onSpeciesChanged(sp) },
                    label = { Text(emoji) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        OutlinedTextField(
            value = breed,
            onValueChange = onBreedChanged,
            label = { Text("Breed (optional)") },
            placeholder = { Text("e.g. Golden Retriever") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = weightKg,
            onValueChange = { v -> onWeightChanged(v.filter { it.isDigit() || it == '.' }) },
            label = { Text("Weight (optional)") },
            placeholder = { Text("e.g. 8.5") },
            trailingIcon = {
                Text(
                    "kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp),
                )
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
            ),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Surface(
            onClick = { showDatePicker = true },
            shape = RoundedCornerShape(14.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(14.dp),
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "Date of birth (optional)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = birthDate?.toString() ?: "Tap to select",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (birthDate != null)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Filled.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageTemplate(
    emoji: String,
    title: String,
    body: String,
    circleBrush: Brush = BrandGradients.OnboardingCool,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "emoji-scale",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp)
            .padding(top = 96.dp, bottom = 160.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            Modifier
                .size(200.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(circleBrush),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 88.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Brand.DarkBlue,
            textAlign = TextAlign.Center,
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
