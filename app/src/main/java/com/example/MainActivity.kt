package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ExamPulseDatabase
import com.example.data.local.ExamPulseRepository
import com.example.data.model.Exam
import com.example.domain.CountdownCalculator
import com.example.ui.components.ExamEditorSheet
import com.example.ui.components.ImmersiveFocusOverlay
import com.example.ui.components.PresetsBrowserSheet
import com.example.ui.screens.CalendarScheduleScreen
import com.example.ui.screens.ExamsDashboardScreen
import com.example.ui.screens.MockAnalyticsScreen
import com.example.ui.screens.SettingsAndBackupSheet
import com.example.ui.screens.StudyFocusScreen
import com.example.ui.screens.SyllabusPlannerScreen
import com.example.ui.theme.ExamPulseTheme
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.ExamPulseUiState
import com.example.ui.viewmodel.ExamPulseViewModel
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = ExamPulseDatabase.getInstance(applicationContext)
        val repository = ExamPulseRepository(database.examPulseDao())

        setContent {
            val viewModel: ExamPulseViewModel = viewModel(
                factory = ExamPulseViewModel.provideFactory(repository)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            ExamPulseTheme(
                themeMode = uiState.preferences.themeMode,
                accentColorHex = uiState.preferences.accentColorHex,
                dynamicColor = uiState.preferences.useDynamicColor
            ) {
                ExamPulseApp(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

private data class NavTabItem(
    val destination: AppDestination,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamPulseApp(
    uiState: ExamPulseUiState,
    viewModel: ExamPulseViewModel
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var editingExam by remember { mutableStateOf<Exam?>(null) }
    var showExamEditor by remember { mutableStateOf(false) }
    var showPresetsSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // BackHandler for secondary tabs
    BackHandler(enabled = uiState.currentDestination != AppDestination.EXAMS) {
        viewModel.navigateTo(AppDestination.EXAMS)
    }

    // Snackbar for Deleted Exam with Undo
    LaunchedEffect(uiState.recentlyDeletedExam) {
        val deleted = uiState.recentlyDeletedExam ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Deleted ${deleted.name}",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoDeleteExam()
        } else {
            viewModel.clearUndoExam()
        }
    }

    // Snackbar for Status Messages
    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = msg,
            duration = SnackbarDuration.Short
        )
        viewModel.clearStatusMessage()
    }

    val navItems = remember {
        listOf(
            NavTabItem(
                destination = AppDestination.EXAMS,
                label = "Exams",
                selectedIcon = Icons.Filled.Schedule,
                unselectedIcon = Icons.Outlined.Schedule,
                testTag = "nav_tab_exams"
            ),
            NavTabItem(
                destination = AppDestination.PLANNER,
                label = "Planner",
                selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
                unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
                testTag = "nav_tab_planner"
            ),
            NavTabItem(
                destination = AppDestination.STUDY,
                label = "Study",
                selectedIcon = Icons.Filled.Timer,
                unselectedIcon = Icons.Outlined.Timer,
                testTag = "nav_tab_study"
            ),
            NavTabItem(
                destination = AppDestination.MOCKS,
                label = "Mocks",
                selectedIcon = Icons.Filled.Analytics,
                unselectedIcon = Icons.Outlined.Analytics,
                testTag = "nav_tab_mocks"
            ),
            NavTabItem(
                destination = AppDestination.CALENDAR,
                label = "Calendar",
                selectedIcon = Icons.Filled.CalendarMonth,
                unselectedIcon = Icons.Outlined.CalendarMonth,
                testTag = "nav_tab_calendar"
            )
        )
    }

    val shareCountdown: (Exam) -> Unit = { exam ->
        val cd = CountdownCalculator.computeLiveCountdown(
            exam = exam,
            nowMillis = uiState.nowMillis,
            timeZoneOffset = uiState.preferences.timeZoneOffset,
            use24Hour = uiState.preferences.use24HourFormat,
            restDays = uiState.preferences.restDays,
            autoArchive = uiState.preferences.autoArchivePastExams
        )
        val shareText = "${exam.emoji} ${exam.name}: ${max(0, cd.daysCeil)} days to go (${cd.formattedTargetDate}). #ExamPulse"
        clipboard.setText(AnnotatedString(shareText))
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Countdown"))
        } catch (_: Exception) {
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 640.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (uiState.currentDestination) {
                                AppDestination.EXAMS -> "Exam Pulse"
                                AppDestination.PLANNER -> "Syllabus & Velocity"
                                AppDestination.STUDY -> "Pomodoro & Study Log"
                                AppDestination.MOCKS -> "Mock Test Analytics"
                                AppDestination.CALENDAR -> "Exam Schedule"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.openFocusOverlay(null) },
                            modifier = Modifier.testTag("top_focus_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CenterFocusStrong,
                                contentDescription = "Immersive Focus Mode"
                            )
                        }
                    },
                    actions = {
                        FilledTonalButton(
                            onClick = { showPresetsSheet = true },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("top_presets_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesomeMotion,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Presets")
                        }
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier.testTag("top_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings and Backup"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar {
                        navItems.forEach { item ->
                            val selected = uiState.currentDestination == item.destination
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(item.destination) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (uiState.currentDestination == AppDestination.EXAMS) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingExam = null
                            showExamEditor = true
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null
                            )
                        },
                        text = { Text("New Exam") },
                        modifier = Modifier.testTag("new_exam_fab")
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail {
                        navItems.forEach { item ->
                            val selected = uiState.currentDestination == item.destination
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(item.destination) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.currentDestination) {
                        AppDestination.EXAMS -> {
                            ExamsDashboardScreen(
                                uiState = uiState,
                                onDismissWelcome = viewModel::dismissWelcomeBanner,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onSelectCategory = viewModel::selectCategory,
                                onSelectSortOption = viewModel::selectSortOption,
                                onChangeViewStyle = viewModel::updateViewStyle,
                                onPinToggle = viewModel::togglePinExam,
                                onOpenFocus = viewModel::openFocusOverlay,
                                onShareExam = shareCountdown,
                                onOpenPlanner = { examId ->
                                    viewModel.navigateTo(AppDestination.PLANNER, examId)
                                },
                                onEditExam = { exam ->
                                    editingExam = exam
                                    showExamEditor = true
                                },
                                onDeleteExam = viewModel::deleteExamWithUndo,
                                onOpenNewExam = {
                                    editingExam = null
                                    showExamEditor = true
                                },
                                onOpenPresets = { showPresetsSheet = true }
                            )
                        }

                        AppDestination.PLANNER -> {
                            SyllabusPlannerScreen(
                                uiState = uiState,
                                onSelectExam = viewModel::selectExam,
                                onUpdateHoursPerTopic = viewModel::updateHoursPerTopic,
                                onApplyTemplate = viewModel::applySyllabusTemplate,
                                onAddSubject = viewModel::addSubject,
                                onUpdateSubjectWeightage = viewModel::updateSubjectWeightage,
                                onDeleteSubject = viewModel::deleteSubject,
                                onAddTopic = viewModel::addTopic,
                                onToggleTopicDone = viewModel::toggleTopicDone,
                                onToggleTopicWeak = viewModel::toggleTopicWeak,
                                onDeleteTopic = viewModel::deleteTopic,
                                onAddMilestone = viewModel::addMilestone,
                                onDeleteMilestone = viewModel::deleteMilestone,
                                onOpenNewExam = {
                                    editingExam = null
                                    showExamEditor = true
                                }
                            )
                        }

                        AppDestination.STUDY -> {
                            StudyFocusScreen(
                                uiState = uiState,
                                onTogglePomodoroStartPause = viewModel::togglePomodoroStartPause,
                                onResetPomodoro = viewModel::resetPomodoro,
                                onSwitchPomodoroMode = viewModel::switchPomodoroMode,
                                onSelectPomodoroSubject = viewModel::selectPomodoroSubject,
                                onLogManualStudySession = viewModel::logManualStudySession,
                                onUpdateStudyGoals = viewModel::updateStudyGoals,
                                onUpdateMotivationWhy = viewModel::updateMotivationWhy,
                                onAddTodo = viewModel::addTodo,
                                onToggleTodo = viewModel::toggleTodo,
                                onDeleteTodo = viewModel::deleteTodo
                            )
                        }

                        AppDestination.MOCKS -> {
                            MockAnalyticsScreen(
                                uiState = uiState,
                                onSelectExam = viewModel::selectExam,
                                onAddMockResult = viewModel::addMockResult,
                                onDeleteMockResult = viewModel::deleteMockResult
                            )
                        }

                        AppDestination.CALENDAR -> {
                            CalendarScheduleScreen(
                                uiState = uiState,
                                onPrevMonth = viewModel::previousCalendarMonth,
                                onNextMonth = viewModel::nextCalendarMonth,
                                onSelectDate = viewModel::selectCalendarDate,
                                onOpenSettingsSheet = { showSettingsSheet = true }
                            )
                        }
                    }
                }
            }
        }

        // Full-Screen Immersive Focus Overlay
        val focusExam = uiState.focusExam
        if (focusExam != null) {
            val focusCountdown = CountdownCalculator.computeLiveCountdown(
                exam = focusExam,
                nowMillis = uiState.nowMillis,
                timeZoneOffset = uiState.preferences.timeZoneOffset,
                use24Hour = uiState.preferences.use24HourFormat,
                restDays = uiState.preferences.restDays,
                autoArchive = uiState.preferences.autoArchivePastExams
            )
            ImmersiveFocusOverlay(
                exam = focusExam,
                countdown = focusCountdown,
                motivationWhy = uiState.preferences.motivationWhy,
                showSleeps = uiState.preferences.showSleepsInsteadOfDays,
                onDismiss = viewModel::closeFocusOverlay
            )
        }

        // Modals & Bottom Sheets
        if (showExamEditor) {
            ExamEditorSheet(
                initialExam = editingExam,
                onDismiss = { showExamEditor = false },
                onSave = viewModel::saveExam
            )
        }

        if (showPresetsSheet) {
            PresetsBrowserSheet(
                onDismiss = { showPresetsSheet = false },
                onAddPreset = viewModel::addPresetExam
            )
        }

        if (showSettingsSheet) {
            SettingsAndBackupSheet(
                uiState = uiState,
                onDismiss = { showSettingsSheet = false },
                onUpdateThemeMode = viewModel::updateThemeMode,
                onUpdateAccentColor = viewModel::updateAccentColor,
                onUpdateDynamicColor = viewModel::updateDynamicColor,
                onUpdate24Hour = viewModel::update24HourFormat,
                onUpdateWeekStartsMonday = viewModel::updateWeekStartsMonday,
                onUpdateShowSleeps = viewModel::updateShowSleeps,
                onUpdateAutoArchive = viewModel::updateAutoArchive,
                onUpdateTimeZoneOffset = viewModel::updateTimeZoneOffset,
                onToggleRestDay = viewModel::toggleRestDay,
                onUpdatePomodoroDurations = viewModel::updatePomodoroDurations,
                onExportBackupJson = viewModel::generateBackupJson,
                onExportIcsText = viewModel::generateIcsText,
                onImportBackupJson = viewModel::importBackupJson,
                onResetAllData = viewModel::resetAllData
            )
        }
    }
}
