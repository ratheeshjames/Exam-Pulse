package com.example.ui.viewmodel

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ExamPulseRepository
import com.example.data.model.AppPreferencesEntity
import com.example.data.model.Exam
import com.example.data.model.ExamMilestone
import com.example.data.model.MockTestResult
import com.example.data.model.StudyLogEntity
import com.example.data.model.StudyTodoEntity
import com.example.data.model.SyllabusSubject
import com.example.data.model.SyllabusTopic
import com.example.data.presets.ExamPreset
import com.example.data.presets.ExamPresetsData
import com.example.domain.CountdownCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

enum class AppDestination {
    EXAMS,
    PLANNER,
    STUDY,
    MOCKS,
    CALENDAR
}

enum class SortOption(val label: String) {
    SOONEST("Soonest"),
    NAME("Name"),
    PROGRESS("Progress")
}

data class PomodoroState(
    val isRunning: Boolean = false,
    val isBreakMode: Boolean = false,
    val remainingSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val selectedSubject: String = "General",
    val completedSessionsToday: Int = 0
)

data class ExamPulseUiState(
    val exams: List<Exam> = emptyList(),
    val studyLogs: List<StudyLogEntity> = emptyList(),
    val studyTodos: List<StudyTodoEntity> = emptyList(),
    val preferences: AppPreferencesEntity = AppPreferencesEntity(),
    val nowMillis: Long = System.currentTimeMillis(),
    val currentDestination: AppDestination = AppDestination.EXAMS,
    val selectedExamId: String? = null,
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val sortOption: SortOption = SortOption.SOONEST,
    val focusOverlayExamId: String? = null,
    val pomodoroState: PomodoroState = PomodoroState(),
    val calendarYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val calendarMonth: Int = Calendar.getInstance().get(Calendar.MONTH), // 0..11
    val selectedCalendarDateIso: String = CountdownCalculator.todayIso(),
    val recentlyDeletedExam: Exam? = null,
    val statusMessage: String? = null
) {
    val activeExam: Exam?
        get() = exams.find { it.id == selectedExamId }
            ?: exams.find { it.id == preferences.pinnedExamId }
            ?: exams.firstOrNull()

    val heroExam: Exam?
        get() {
            val pinned = exams.find { it.id == preferences.pinnedExamId }
            if (pinned != null) return pinned
            return exams
                .filter { CountdownCalculator.parseTargetMillis(it.dateTimeIso, preferences.timeZoneOffset) > nowMillis }
                .minByOrNull { CountdownCalculator.parseTargetMillis(it.dateTimeIso, preferences.timeZoneOffset) }
                ?: exams.firstOrNull()
        }

    val focusExam: Exam?
        get() = focusOverlayExamId?.let { id -> exams.find { it.id == id } }
}

private data class LocalInteractionState(
    val currentDestination: AppDestination = AppDestination.EXAMS,
    val selectedExamId: String? = null,
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val sortOption: SortOption = SortOption.SOONEST,
    val focusOverlayExamId: String? = null,
    val pomodoroState: PomodoroState = PomodoroState(),
    val calendarYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val calendarMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedCalendarDateIso: String = CountdownCalculator.todayIso(),
    val recentlyDeletedExam: Exam? = null,
    val statusMessage: String? = null
)

class ExamPulseViewModel(
    private val repository: ExamPulseRepository
) : ViewModel() {

    private val nowMillisFlow = MutableStateFlow(System.currentTimeMillis())
    private val localStateFlow = MutableStateFlow(LocalInteractionState())

    val uiState: StateFlow<ExamPulseUiState> = combine(
        repository.examsFlow,
        repository.studyLogsFlow,
        repository.studyTodosFlow,
        repository.preferencesFlow,
        combine(nowMillisFlow, localStateFlow) { now, local -> now to local }
    ) { exams, logs, todos, prefs, (now, local) ->
        ExamPulseUiState(
            exams = exams,
            studyLogs = logs,
            studyTodos = todos,
            preferences = prefs,
            nowMillis = now,
            currentDestination = local.currentDestination,
            selectedExamId = local.selectedExamId ?: prefs.pinnedExamId ?: exams.firstOrNull()?.id,
            searchQuery = local.searchQuery,
            selectedCategory = local.selectedCategory,
            sortOption = local.sortOption,
            focusOverlayExamId = local.focusOverlayExamId,
            pomodoroState = local.pomodoroState,
            calendarYear = local.calendarYear,
            calendarMonth = local.calendarMonth,
            selectedCalendarDateIso = local.selectedCalendarDateIso,
            recentlyDeletedExam = local.recentlyDeletedExam,
            statusMessage = local.statusMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExamPulseUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeededIfFirstLaunch()
        }
        // Live 1-second ticker for real-time countdowns and Pomodoro timer
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val now = System.currentTimeMillis()
                nowMillisFlow.value = now
                tickPomodoroIfRunning(now)
            }
        }
    }

    private fun tickPomodoroIfRunning(nowMillis: Long) {
        val currentPom = localStateFlow.value.pomodoroState
        if (!currentPom.isRunning) return

        val nextRem = currentPom.remainingSeconds - 1
        if (nextRem > 0) {
            localStateFlow.update {
                it.copy(pomodoroState = currentPom.copy(remainingSeconds = nextRem))
            }
        } else {
            // Timer completed! Play tone and log session if in focus mode
            playCompletionBeep()
            val prefs = uiState.value.preferences
            val wasFocus = !currentPom.isBreakMode
            if (wasFocus) {
                val focusMins = prefs.pomodoroFocusMinutes.coerceAtLeast(1)
                val todayIso = CountdownCalculator.todayIso(nowMillis)
                val activeExamId = uiState.value.activeExam?.id
                viewModelScope.launch {
                    repository.logStudySession(
                        dateIso = todayIso,
                        minutes = focusMins,
                        subjectName = currentPom.selectedSubject.ifBlank { "General" },
                        examId = activeExamId
                    )
                }
            }
            val nextIsBreak = wasFocus
            val nextDurationSecs = (if (nextIsBreak) prefs.pomodoroBreakMinutes else prefs.pomodoroFocusMinutes)
                .coerceAtLeast(1) * 60
            localStateFlow.update {
                it.copy(
                    pomodoroState = currentPom.copy(
                        isRunning = false,
                        isBreakMode = nextIsBreak,
                        remainingSeconds = nextDurationSecs,
                        totalSeconds = nextDurationSecs,
                        completedSessionsToday = if (wasFocus) currentPom.completedSessionsToday + 1 else currentPom.completedSessionsToday
                    ),
                    statusMessage = if (wasFocus) {
                        "Pomodoro complete! Logged ${prefs.pomodoroFocusMinutes}m under ${currentPom.selectedSubject}."
                    } else {
                        "Break finished! Ready for your next focus sprint."
                    }
                )
            }
        }
    }

    private fun playCompletionBeep() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            toneGen.startTone(ToneGenerator.TONE_PROP_PROMPT, 500)
        } catch (_: Exception) {
            // Ignore in headless/unit test environments
        }
    }

    // Navigation & Filtering
    fun navigateTo(destination: AppDestination, examId: String? = null) {
        localStateFlow.update {
            it.copy(
                currentDestination = destination,
                selectedExamId = examId ?: it.selectedExamId
            )
        }
    }

    fun selectExam(examId: String) {
        localStateFlow.update { it.copy(selectedExamId = examId) }
    }

    fun updateSearchQuery(query: String) {
        localStateFlow.update { it.copy(searchQuery = query) }
    }

    fun selectCategory(category: String) {
        localStateFlow.update { it.copy(selectedCategory = category) }
    }

    fun selectSortOption(option: SortOption) {
        localStateFlow.update { it.copy(sortOption = option) }
    }

    fun openFocusOverlay(examId: String?) {
        val targetId = examId ?: uiState.value.heroExam?.id
        localStateFlow.update { it.copy(focusOverlayExamId = targetId) }
    }

    fun closeFocusOverlay() {
        localStateFlow.update { it.copy(focusOverlayExamId = null) }
    }

    fun clearStatusMessage() {
        localStateFlow.update { it.copy(statusMessage = null) }
    }

    // Welcome Banner & Pinning
    fun dismissWelcomeBanner() {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(hasSeenWelcome = true) }
        }
    }

    fun togglePinExam(examId: String) {
        viewModelScope.launch {
            repository.updatePreferences { prefs ->
                val nextPin = if (prefs.pinnedExamId == examId) null else examId
                prefs.copy(pinnedExamId = nextPin)
            }
        }
    }

    // Exam CRUD
    fun saveExam(exam: Exam) {
        viewModelScope.launch {
            repository.upsertExam(exam)
            localStateFlow.update {
                it.copy(
                    selectedExamId = exam.id,
                    statusMessage = "Saved ${exam.name}"
                )
            }
        }
    }

    fun addPresetExam(preset: ExamPreset) {
        viewModelScope.launch {
            val exam = ExamPresetsData.buildExamFromPreset(preset, nowMillisFlow.value)
            repository.upsertExam(exam)
            localStateFlow.update {
                it.copy(
                    selectedExamId = exam.id,
                    statusMessage = "Added ${preset.emoji} ${preset.name} with syllabus & countdown"
                )
            }
        }
    }

    fun deleteExamWithUndo(exam: Exam) {
        viewModelScope.launch {
            localStateFlow.update { it.copy(recentlyDeletedExam = exam) }
            repository.deleteExam(exam.id)
        }
    }

    fun undoDeleteExam() {
        val deleted = localStateFlow.value.recentlyDeletedExam ?: return
        viewModelScope.launch {
            repository.upsertExam(deleted)
            localStateFlow.update {
                it.copy(
                    recentlyDeletedExam = null,
                    statusMessage = "Restored ${deleted.name}"
                )
            }
        }
    }

    fun clearUndoExam() {
        localStateFlow.update { it.copy(recentlyDeletedExam = null) }
    }

    // Syllabus, Topics, Templates & Milestones
    fun updateHoursPerTopic(examId: String, hours: Float) {
        val exam = uiState.value.exams.find { it.id == examId } ?: return
        viewModelScope.launch {
            repository.upsertExam(exam.copy(hoursPerTopic = hours.coerceIn(0.5f, 24f)))
        }
    }

    fun applySyllabusTemplate(examId: String, templateKey: String) {
        val exam = uiState.value.exams.find { it.id == examId } ?: return
        val newSubjects = ExamPresetsData.parseTemplate(templateKey)
        if (newSubjects.isEmpty()) return
        viewModelScope.launch {
            repository.upsertExam(exam.copy(subjects = exam.subjects + newSubjects))
            localStateFlow.update {
                it.copy(statusMessage = "Loaded $templateKey syllabus template (${newSubjects.size} subjects)")
            }
        }
    }

    fun addSubject(examId: String, name: String, weightagePercent: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val exam = uiState.value.exams.find { it.id == examId } ?: return
        val updated = exam.subjects + SyllabusSubject(
            name = trimmed,
            weightagePercent = weightagePercent.coerceIn(0, 100)
        )
        viewModelScope.launch {
            repository.upsertExam(exam.copy(subjects = updated))
        }
    }

    fun updateSubjectWeightage(examId: String, subjectId: String, weightagePercent: Int) {
        val exam = uiState.value.exams.find { it.id == examId } ?: return
        val updated = exam.subjects.map { sub ->
            if (sub.id == subjectId) sub.copy(weightagePercent = weightagePercent.coerceIn(0, 100)) else sub
        }
        viewModelScope.launch {
            repository.upsertExam(exam.copy(subjects = updated))
        }
    }

    fun deleteSubject(examId: String, subjectId: String) {
        val exam = uiState.value.exams.find { it.id == examId } ?: return
        val updated = exam.subjects.filterNot { it.id == subjectId }
        viewModelScope.launch {
            repository.upsertExam(exam.copy(subjects = updated))
        }
    }

    fun addTopic(examId: String, subjectId: String, topicName: String) {
        val trimmed = topicName.trim()
        if (trimmed.isEmpty()) return
        val exam = uiState.value.exams.find { it.id == examId } ?: return
        val updated = exam.subjects.map { sub ->
            if (sub.id == subjectId) {
                sub.copy(topics = sub.topics + SyllabusTopic(name = trimmed))
            } else sub
        }
        viewModelScope.launch {
            repository.upsertExam(exam.copy(subjects = updated))
        }
    }

    fun toggleTopicDone(examId: String, subjectId: String, topicId: String) {
        val exam = uiState.value.exams.find { it.id == examId } ?: return
        val now = nowMillisFlow.value
        val updated = exam.subjects.map { sub ->
            if (sub.id == subjectId) {
                sub.copy(
                    topics = sub.topics.map { top ->
                        if (top.id == topicId) {
                            val nextDone = !top.isDone
                            top.copy(isDone = nextDone, doneAtMillis = if (nextDone) now else 0L)
                        } else top
                    }
                )
            } else sub
        }
        viewModelScope.launch {
            repository.upsertExam(exam.copy(subjects = updated))
        }
    }

    fun toggleTopicWeak(examId: String, subjectId: String, topicId: String) {
        val exam = uiState.value.exams.find { it.id == examId } ?: return
        val updated = exam.subjects.map { sub ->
            if (sub.id == subjectId) {
                sub.copy(
                    topics = sub.topics.map { top ->
                        if (top.id == topicId) top.copy(isWeak = !top.isWeak) else top
                    }
                )
            } else sub
        }
        viewModelScope.launch {
            repository.upsertExam(exam.copy(subjects = updated))
        }
    }

    fun deleteTopic(examId: String, subjectId: String, topicId: String) {
        val exam = uiState.value.exams.find { it.id == examId } ?: return
        val updated = exam.subjects.map { sub ->
            if (sub.id == subjectId) {
                sub.copy(topics = sub.topics.filterNot { it.id == topicId })
            } else sub
        }
        viewModelScope.launch {
            repository.upsertExam(exam.copy(subjects = updated))
        }
    }

    fun addMilestone(examId: String, title: String, dateTimeIso: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val exam = uiState.value.exams.find { it.id == examId } ?: return
        val updated = exam.milestones + ExamMilestone(title = trimmed, dateTimeIso = dateTimeIso)
        viewModelScope.launch {
            repository.upsertExam(exam.copy(milestones = updated))
        }
    }

    fun deleteMilestone(examId: String, milestoneId: String) {
        val exam = uiState.value.exams.find { it.id == examId } ?: return
        val updated = exam.milestones.filterNot { it.id == milestoneId }
        viewModelScope.launch {
            repository.upsertExam(exam.copy(milestones = updated))
        }
    }

    // Mocks
    fun addMockResult(
        examId: String,
        dateIso: String,
        score: Float,
        maxScore: Float,
        accuracyPercent: Float?,
        rank: Int?
    ) {
        val exam = uiState.value.exams.find { it.id == examId } ?: return
        val newMock = MockTestResult(
            id = UUID.randomUUID().toString().take(8),
            dateIso = dateIso,
            score = score,
            maxScore = maxScore.coerceAtLeast(1f),
            accuracyPercent = accuracyPercent,
            rank = rank
        )
        viewModelScope.launch {
            repository.upsertExam(exam.copy(mocks = exam.mocks + newMock))
            localStateFlow.update { it.copy(statusMessage = "Mock test score logged!") }
        }
    }

    fun deleteMockResult(examId: String, mockId: String) {
        val exam = uiState.value.exams.find { it.id == examId } ?: return
        viewModelScope.launch {
            repository.upsertExam(exam.copy(mocks = exam.mocks.filterNot { it.id == mockId }))
        }
    }

    // Pomodoro & Study Logs
    fun togglePomodoroStartPause() {
        localStateFlow.update { state ->
            state.copy(pomodoroState = state.pomodoroState.copy(isRunning = !state.pomodoroState.isRunning))
        }
    }

    fun resetPomodoro() {
        val prefs = uiState.value.preferences
        val focusSecs = prefs.pomodoroFocusMinutes.coerceAtLeast(1) * 60
        localStateFlow.update { state ->
            state.copy(
                pomodoroState = state.pomodoroState.copy(
                    isRunning = false,
                    isBreakMode = false,
                    remainingSeconds = focusSecs,
                    totalSeconds = focusSecs
                )
            )
        }
    }

    fun switchPomodoroMode(isBreak: Boolean) {
        val prefs = uiState.value.preferences
        val secs = (if (isBreak) prefs.pomodoroBreakMinutes else prefs.pomodoroFocusMinutes).coerceAtLeast(1) * 60
        localStateFlow.update { state ->
            state.copy(
                pomodoroState = state.pomodoroState.copy(
                    isRunning = false,
                    isBreakMode = isBreak,
                    remainingSeconds = secs,
                    totalSeconds = secs
                )
            )
        }
    }

    fun selectPomodoroSubject(subject: String) {
        localStateFlow.update { state ->
            state.copy(pomodoroState = state.pomodoroState.copy(selectedSubject = subject.ifBlank { "General" }))
        }
    }

    fun logManualStudySession(dateIso: String, minutes: Int, subjectName: String) {
        viewModelScope.launch {
            repository.logStudySession(
                dateIso = dateIso,
                minutes = minutes,
                subjectName = subjectName,
                examId = uiState.value.activeExam?.id
            )
            localStateFlow.update {
                it.copy(statusMessage = "Logged ${minutes}m for $subjectName")
            }
        }
    }

    fun updateStudyGoals(dailyHours: Float, weeklyHours: Float) {
        viewModelScope.launch {
            repository.updatePreferences {
                it.copy(
                    dailyGoalHours = dailyHours.coerceIn(0.5f, 18f),
                    weeklyGoalHours = weeklyHours.coerceIn(1f, 120f)
                )
            }
        }
    }

    fun updateMotivationWhy(whyText: String) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(motivationWhy = whyText) }
        }
    }

    // Todos
    fun addTodo(text: String) {
        viewModelScope.launch { repository.addTodo(text) }
    }

    fun toggleTodo(todo: StudyTodoEntity) {
        viewModelScope.launch { repository.toggleTodo(todo) }
    }

    fun deleteTodo(todoId: String) {
        viewModelScope.launch { repository.deleteTodo(todoId) }
    }

    // Calendar Navigation
    fun previousCalendarMonth() {
        localStateFlow.update { state ->
            val cal = Calendar.getInstance().apply {
                set(state.calendarYear, state.calendarMonth, 1)
                add(Calendar.MONTH, -1)
            }
            state.copy(
                calendarYear = cal.get(Calendar.YEAR),
                calendarMonth = cal.get(Calendar.MONTH)
            )
        }
    }

    fun nextCalendarMonth() {
        localStateFlow.update { state ->
            val cal = Calendar.getInstance().apply {
                set(state.calendarYear, state.calendarMonth, 1)
                add(Calendar.MONTH, 1)
            }
            state.copy(
                calendarYear = cal.get(Calendar.YEAR),
                calendarMonth = cal.get(Calendar.MONTH)
            )
        }
    }

    fun selectCalendarDate(dateIso: String) {
        localStateFlow.update { it.copy(selectedCalendarDateIso = dateIso) }
    }

    // Settings & Preferences
    fun updateThemeMode(mode: String) {
        viewModelScope.launch { repository.updatePreferences { it.copy(themeMode = mode) } }
    }

    fun updateAccentColor(colorHex: Long) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(accentColorHex = colorHex, useDynamicColor = false) }
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.updatePreferences { it.copy(useDynamicColor = enabled) } }
    }

    fun update24HourFormat(enabled: Boolean) {
        viewModelScope.launch { repository.updatePreferences { it.copy(use24HourFormat = enabled) } }
    }

    fun updateWeekStartsMonday(monday: Boolean) {
        viewModelScope.launch { repository.updatePreferences { it.copy(weekStartsMonday = monday) } }
    }

    fun updateViewStyle(style: String) {
        viewModelScope.launch { repository.updatePreferences { it.copy(viewStyle = style) } }
    }

    fun updateShowSleeps(enabled: Boolean) {
        viewModelScope.launch { repository.updatePreferences { it.copy(showSleepsInsteadOfDays = enabled) } }
    }

    fun updateAutoArchive(enabled: Boolean) {
        viewModelScope.launch { repository.updatePreferences { it.copy(autoArchivePastExams = enabled) } }
    }

    fun updateTimeZoneOffset(offset: String) {
        viewModelScope.launch { repository.updatePreferences { it.copy(timeZoneOffset = offset) } }
    }

    fun toggleRestDay(dayIndex0To6: Int) {
        viewModelScope.launch {
            repository.updatePreferences { prefs ->
                val current = prefs.restDays.toMutableSet()
                if (current.contains(dayIndex0To6)) current.remove(dayIndex0To6) else current.add(dayIndex0To6)
                prefs.copy(restDaysCsv = current.sorted().joinToString(","))
            }
        }
    }

    fun updatePomodoroDurations(focusMins: Int, breakMins: Int) {
        val safeFocus = focusMins.coerceIn(1, 180)
        val safeBreak = breakMins.coerceIn(1, 60)
        viewModelScope.launch {
            repository.updatePreferences {
                it.copy(pomodoroFocusMinutes = safeFocus, pomodoroBreakMinutes = safeBreak)
            }
            localStateFlow.update { state ->
                if (!state.pomodoroState.isRunning) {
                    val secs = (if (state.pomodoroState.isBreakMode) safeBreak else safeFocus) * 60
                    state.copy(pomodoroState = state.pomodoroState.copy(remainingSeconds = secs, totalSeconds = secs))
                } else state
            }
        }
    }

    // Export / Import / Reset
    fun generateBackupJson(): String {
        val s = uiState.value
        return repository.exportBackupJson(s.exams, s.studyLogs, s.studyTodos, s.preferences)
    }

    fun generateIcsText(): String {
        val s = uiState.value
        return repository.generateIcsCalendar(s.exams, s.preferences.timeZoneOffset)
    }

    fun importBackupJson(jsonText: String) {
        viewModelScope.launch {
            val ok = repository.importBackupJson(jsonText)
            localStateFlow.update {
                it.copy(
                    statusMessage = if (ok) "Backup imported successfully!" else "Invalid backup JSON format."
                )
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
            localStateFlow.update {
                it.copy(
                    selectedExamId = null,
                    statusMessage = "All data has been reset."
                )
            }
        }
    }

    companion object {
        fun provideFactory(repository: ExamPulseRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ExamPulseViewModel(repository) as T
                }
            }
    }
}
