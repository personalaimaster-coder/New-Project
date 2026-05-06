package com.example.petmeds.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petmeds.R
import com.example.petmeds.domain.model.Species
import com.example.petmeds.ui.common.PawPillWordmark
import com.example.petmeds.ui.meds.DatePickerDialog
import com.example.petmeds.ui.permissions.PermissionChecks
import com.example.petmeds.ui.permissions.PermissionsState
import com.example.petmeds.ui.pets.FirstRunPetViewModel
import com.example.petmeds.ui.theme.Brand
import com.example.petmeds.ui.theme.BrandGradients
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val PAGE_COUNT = 4
private const val PAGE_PERMISSIONS = 2
private const val PAGE_PET_INFO = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingPager(
    onFinished: () -> Unit,
    viewModel: FirstRunPetViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Live permission state — re-read whenever the host returns from a system
    // settings activity (exact alarms / battery exemption) so the UI updates
    // without requiring the user to leave and re-enter onboarding.
    var perms by remember { mutableStateOf(PermissionChecks.read(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                perms = PermissionChecks.read(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { perms = PermissionChecks.read(context) }

    fun advance() {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
    }
    fun goBack() {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    LaunchedEffect(state.saved) { if (state.saved) onFinished() }

    val isPetInfoPage = pagerState.currentPage == PAGE_PET_INFO
    val hideFloatingControls = isPetInfoPage

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
                PAGE_PERMISSIONS -> OnboardingPermissionsPage(
                    perms = perms,
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onRequestExactAlarms = {
                        PermissionChecks.openExactAlarmSettings(context)
                    },
                    onRequestBattery = {
                        PermissionChecks.requestIgnoreBatteryOptimizations(context)
                    },
                    onSkip = ::advance,
                )
                PAGE_PET_INFO -> OnboardingPetInfoPage(
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
                    onBack = ::goBack,
                    onSubmit = viewModel::submit,
                )
            }
        }

        if (!hideFloatingControls) {
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
                        onClick = ::goBack,
                        modifier = Modifier.weight(1f),
                    ) { Text("Back") }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                val isPermissionsPage = pagerState.currentPage == PAGE_PERMISSIONS
                val isFinalPage = pagerState.currentPage == PAGE_COUNT - 1
                val ctaLabel = when {
                    isFinalPage -> "Get started"
                    isPermissionsPage && perms.allReady -> stringResource(R.string.onboarding_perm_continue)
                    isPermissionsPage -> stringResource(R.string.onboarding_perm_allow_continue)
                    else -> "Next"
                }
                Button(
                    onClick = {
                        when {
                            isFinalPage -> viewModel.submit()
                            isPermissionsPage && !perms.notificationsGranted -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    advance()
                                }
                            }
                            isPermissionsPage && !perms.exactAlarmsAllowed ->
                                PermissionChecks.openExactAlarmSettings(context)
                            isPermissionsPage && !perms.ignoringBatteryOptimizations ->
                                PermissionChecks.requestIgnoreBatteryOptimizations(context)
                            else -> advance()
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
                    if (state.saving && isFinalPage) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    } else {
                        Text(ctaLabel, style = MaterialTheme.typography.labelLarge)
                    }
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
private fun OnboardingPetInfoPage(
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
    onBack: () -> Unit,
    onSubmit: () -> Unit,
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
            .padding(top = 96.dp, bottom = 32.dp),
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

        // Inline dot indicators
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            repeat(PAGE_COUNT) { i ->
                val selected = i == PAGE_PET_INFO
                val dotWidth by animateDpAsState(
                    targetValue = if (selected) 24.dp else 8.dp,
                    animationSpec = spring(),
                    label = "inline-dot",
                )
                Box(
                    Modifier
                        .height(8.dp)
                        .width(dotWidth)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                        ),
                )
            }
        }

        // Inline navigation buttons
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
            ) { Text("Back") }
            Button(
                onClick = onSubmit,
                enabled = !saving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(56.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Text("Get started", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun OnboardingPermissionsPage(
    perms: PermissionsState,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
    onRequestBattery: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .padding(top = 96.dp, bottom = 160.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(BrandGradients.OnboardingFresh),
            contentAlignment = Alignment.Center,
        ) {
            Text("🔔", fontSize = 44.sp)
        }
        Text(
            stringResource(R.string.onboarding_perm_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Brand.DarkBlue,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.onboarding_perm_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(4.dp))

        PermissionExplainerRow(
            icon = Icons.Filled.Notifications,
            title = stringResource(R.string.perm_notifications_title),
            why = stringResource(R.string.onboarding_perm_notifications_why),
            granted = perms.notificationsGranted,
            onAllow = onRequestNotifications,
        )
        PermissionExplainerRow(
            icon = Icons.Filled.Schedule,
            title = stringResource(R.string.perm_exact_alarms_title),
            why = stringResource(R.string.onboarding_perm_exact_alarms_why),
            granted = perms.exactAlarmsAllowed,
            onAllow = onRequestExactAlarms,
        )
        PermissionExplainerRow(
            icon = Icons.Filled.Bolt,
            title = stringResource(R.string.perm_battery_title),
            why = stringResource(R.string.onboarding_perm_battery_why),
            granted = perms.ignoringBatteryOptimizations,
            onAllow = onRequestBattery,
        )

        Spacer(Modifier.height(4.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(R.string.onboarding_perm_skip),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionExplainerRow(
    icon: ImageVector,
    title: String,
    why: String,
    granted: Boolean,
    onAllow: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (granted) Brand.Sage else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp),
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (granted) Brand.Sage.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.primaryContainer,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (granted) Icons.Filled.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (granted) Brand.SageDark
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    why,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (granted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Brand.SageDark,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        stringResource(R.string.onboarding_perm_granted),
                        style = MaterialTheme.typography.labelMedium,
                        color = Brand.SageDark,
                    )
                }
            } else {
                TextButton(onClick = onAllow) {
                    Text(
                        stringResource(R.string.onboarding_perm_allow),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
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
