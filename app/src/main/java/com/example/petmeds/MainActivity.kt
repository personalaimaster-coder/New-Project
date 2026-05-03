package com.example.petmeds

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.petmeds.ui.common.PawPillWordmark
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.petmeds.data.ocr.OcrMedResult
import com.example.petmeds.ui.meds.CourseEditorScreen
import com.example.petmeds.ui.meds.CourseDetailScreen
import com.example.petmeds.ui.meds.MedAddChooserScreen
import com.example.petmeds.ui.meds.MedEditorScreen
import com.example.petmeds.ui.meds.MedicationDetailScreen
import com.example.petmeds.ui.meds.MedsListScreen
import com.example.petmeds.ui.meds.OcrReviewScreen
import com.example.petmeds.ui.onboarding.OnboardingPager
import com.example.petmeds.ui.pets.PetEditorScreen
import com.example.petmeds.ui.pets.PetProfileScreen
import com.example.petmeds.ui.settings.SettingsScreen
import com.example.petmeds.ui.theme.PetMedsTheme
import com.example.petmeds.ui.timeline.TimelineScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PetMedsTheme {
                AppNav()
            }
        }
    }
}

// ── Route constants ──────────────────────────────────────────────────────────

private object Routes {
    const val ONBOARDING      = "onboarding"
    const val TODAY           = "today"
    const val MEDS            = "meds"
    const val MED_ADD_CHOOSER = "med_add_chooser"
    const val MED_NEW         = "med_new"
    const val MED_EDIT        = "med_edit/{id}"
    const val MED_DETAIL      = "med_detail/{id}"
    const val OCR_REVIEW      = "ocr_review/{imageUri}"
    const val COURSE_NEW      = "course_new"
    const val COURSE_EDIT     = "course_edit/{id}"
    const val COURSE_DETAIL   = "course_detail/{id}"
    const val HISTORY         = "history"
    const val PET             = "pet"
    const val PET_EDIT        = "pet_edit"
    const val SETTINGS        = "settings"

    fun medEdit(id: Long) = "med_edit/$id"
    fun medDetail(id: Long) = "med_detail/$id"
    fun courseEdit(id: Long) = "course_edit/$id"
    fun courseDetail(id: Long) = "course_detail/$id"
    fun ocrReview(imageUri: Uri) = "ocr_review/${Uri.encode(imageUri.toString())}"
}

private data class TabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val TABS = listOf(
    TabItem(Routes.TODAY, "Today",       Icons.Filled.Home,       Icons.Outlined.Home),
    TabItem(Routes.MEDS,  "Medications", Icons.Filled.Medication,  Icons.Outlined.Medication),
    TabItem(Routes.HISTORY, "History",    Icons.Filled.History,     Icons.Outlined.History),
    TabItem(Routes.PET,   "Pet",         Icons.Filled.Pets,        Icons.Outlined.Pets),
)

// ── Root navigation ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppNav(viewModel: AppNavViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val petCount by viewModel.petCount.collectAsState()

    val navBackStack by nav.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route
    val showBottomBar = currentRoute in TABS.map { it.route }
    val showTopBar = currentRoute in TABS.map { it.route }

    // Redirect to onboarding when there is no pet yet (only once count is known)
    androidx.compose.runtime.LaunchedEffect(petCount) {
        if (petCount == 0) {
            nav.navigate(Routes.ONBOARDING) {
                popUpTo(Routes.TODAY) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { PawPillWordmark(badgeSize = 30.dp, textSize = 20) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        nav.navigate(route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingPager(
                    onFinished = {
                        nav.navigate(Routes.TODAY) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.TODAY) {
                TimelineScreen(
                    onAddMedication = { nav.navigate(Routes.MED_ADD_CHOOSER) },
                    onEditMedication = { id -> nav.navigate(Routes.medEdit(id)) },
                    onOpenMedication = { id -> nav.navigate(Routes.medDetail(id)) },
                )
            }

            composable(Routes.MEDS) {
                MedsListScreen(
                    onAddMedication = { nav.navigate(Routes.MED_ADD_CHOOSER) },
                    onAddCourse = { nav.navigate(Routes.COURSE_NEW) },
                    onOpenMedication = { id -> nav.navigate(Routes.medDetail(id)) },
                )
            }

            composable(Routes.HISTORY) {
                com.example.petmeds.ui.history.HistoryScreen(
                    onOpenMedication = { id -> nav.navigate(Routes.medDetail(id)) },
                    onOpenCourse = { id -> nav.navigate(Routes.courseDetail(id)) },
                )
            }

            composable(Routes.MED_ADD_CHOOSER) {
                MedAddChooserScreen(
                    onManual = {
                        nav.navigate(Routes.MED_NEW) {
                            popUpTo(Routes.MED_ADD_CHOOSER) { inclusive = true }
                        }
                    },
                    onImageReady = { uri -> nav.navigate(Routes.ocrReview(uri)) },
                    onClose = { nav.popBackStack() },
                )
            }

            composable(
                route = Routes.OCR_REVIEW,
                arguments = listOf(navArgument("imageUri") { type = NavType.StringType }),
            ) { entry ->
                val uriString = entry.arguments?.getString("imageUri") ?: ""
                val imageUri = Uri.parse(Uri.decode(uriString))
                OcrReviewScreen(
                    imageUri = imageUri,
                    onConfirm = { results ->
                        nav.navigate(Routes.MED_NEW)
                        nav.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("ocr_results", ArrayList(results))
                    },
                    onRetake = { nav.popBackStack(Routes.MED_ADD_CHOOSER, inclusive = false) },
                    onManual = {
                        nav.navigate(Routes.MED_NEW) {
                            popUpTo(Routes.MED_ADD_CHOOSER) { inclusive = true }
                        }
                    },
                    onClose = { nav.popBackStack(Routes.MED_ADD_CHOOSER, inclusive = true) },
                )
            }

            composable(Routes.MED_NEW) { backStackEntry ->
                val viewModel: com.example.petmeds.ui.meds.MedEditorViewModel = hiltViewModel(backStackEntry)
                val ocrResults by backStackEntry.savedStateHandle
                    .getStateFlow<ArrayList<OcrMedResult>?>("ocr_results", null)
                    .collectAsState()
                LaunchedEffect(ocrResults) {
                    if (!ocrResults.isNullOrEmpty()) {
                        viewModel.loadFromOcrResults(ocrResults!!)
                        backStackEntry.savedStateHandle.remove<ArrayList<OcrMedResult>>("ocr_results")
                    }
                }
                MedEditorScreen(
                    medicationId = null,
                    onClose = { nav.popBackStack() },
                    viewModel = viewModel,
                )
            }

            composable(
                route = Routes.MED_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                MedEditorScreen(
                    medicationId = entry.arguments?.getLong("id"),
                    onClose = { nav.popBackStack() },
                )
            }

            composable(
                route = Routes.MED_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                MedicationDetailScreen(
                    medicationId = entry.arguments?.getLong("id"),
                    onBack = { nav.popBackStack() },
                    onEdit = { id -> nav.navigate(Routes.medEdit(id)) },
                )
            }

            composable(Routes.COURSE_NEW) {
                CourseEditorScreen(
                    courseId = null,
                    onClose = { nav.popBackStack() },
                )
            }

            composable(
                route = Routes.COURSE_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                CourseEditorScreen(
                    courseId = entry.arguments?.getLong("id"),
                    onClose = { nav.popBackStack() },
                )
            }

            composable(
                route = Routes.COURSE_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                CourseDetailScreen(
                    courseId = entry.arguments?.getLong("id"),
                    onBack = { nav.popBackStack() },
                    onEdit = { id -> nav.navigate(Routes.courseEdit(id)) },
                    onOpenMedication = { id -> nav.navigate(Routes.medDetail(id)) },
                )
            }

            composable(Routes.PET) {
                PetProfileScreen(
                    onNavigateToSettings = { nav.navigate(Routes.SETTINGS) },
                    onEditPet = { nav.navigate(Routes.PET_EDIT) },
                )
            }

            composable(Routes.PET_EDIT) {
                PetEditorScreen(onClose = { nav.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        TABS.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                    )
                },
                label = {
                    Text(
                        tab.label,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
