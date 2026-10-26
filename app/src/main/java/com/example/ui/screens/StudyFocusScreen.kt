package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyTodoEntity
import com.example.domain.CountdownCalculator
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.ExamPulseUiState
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyFocusScreen(
    uiState: ExamPulseUiState,
    onTogglePomodoroStartPause: () -> Unit,
    onResetPomodoro: () -> Unit,
    onSwitchPomodoroMode: (Boolean) -> Unit,
    onSelectPomodoroSubject: (String) -> Unit,
    onLogManualStudySession: (String, Int, String) -> Unit,
    onUpdateStudyGoals: (Float, Float) -> Unit,
    onUpdateMotivationWhy: (String) -> Unit,
    onAddTodo: (String) -> Unit,
    onToggleTodo: (StudyTodoEntity) -> Unit,
    onDeleteTodo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pom = uiState.pomodoroState
    val prefs = uiState.preferences
    val now = uiState.nowMillis
    val todayIso = remember(now) { CountdownCalculator.todayIso(now) }

    val availableSubjects = remember(uiState.activeExam) {
        val examSubjects = uiState.activeExam?.subjects?.map { it.name } ?: emptyList()
        (listOf("General") + examSubjects).distinct()
    }

    val streakDays = remember(uiState.studyLogs, now) {
        CountdownCalculator.calculateStreakDays(uiState.studyLogs, now)
    }
    val totalHours = remember(uiState.studyLogs) {
        uiState.studyLogs.sumOf { it.minutes } / 60f
    }
    val todayMinutes = remember(uiState.studyLogs, todayIso) {
        uiState.studyLogs.filter { it.dateIso == todayIso }.sumOf { it.minutes }
    }
    val last7Days = remember(uiState.studyLogs, now) {
        CountdownCalculator.buildLast7DaysMinutes(uiState.studyLogs, now)
    }
    val weekMinutes = remember(last7Days) {
        last7Days.sumOf { it.second }
    }
    val heatmapDays = remember(uiState.studyLogs, now) {
        CountdownCalculator.build84DayHeatmap(uiState.studyLogs, now)
    }
    val subjectTotals = remember(uiState.studyLogs) {
        uiState.studyLogs
            .groupBy { it.subjectName.ifBlank { "General" } }
            .mapValues { entry -> entry.value.sumOf { it.minutes } }
            .entries
            .sortedByDescending { it.value }
    }
    val badges = remember(uiState.exams, uiState.studyLogs, now, prefs.timeZoneOffset) {
        CountdownCalculator.computeEarnedBadges(uiState.exams, uiState.studyLogs, now, prefs.timeZoneOffset)
    }

    var showLogSessionDialog by remember { mutableStateOf(false) }
    var showEditGoalsDialog by remember { mutableStateOf(false) }
    var newTodoText by remember { mutableStateOf("") }
    var selectedHeatmapTooltip by remember { mutableStateOf<String?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("study_focus_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Pomodoro Focus Timer Card
        item(key = "pomodoro_card") {
            ElevatedCard(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pomodoro_timer_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = !pom.isBreakMode,
                            onClick = { onSwitchPomodoroMode(false) },
                            label = { Text("🍅 Focus (${prefs.pomodoroFocusMinutes}m)") }
                        )
                        FilterChip(
                            selected = pom.isBreakMode,
                            onClick = { onSwitchPomodoroMode(true) },
                            label = { Text("☕ Break (${prefs.pomodoroBreakMinutes}m)") }
                        )
                    }

                    // Circular Timer Gauge
                    val fraction = if (pom.totalSeconds > 0) {
                        pom.remainingSeconds.toFloat() / pom.totalSeconds.toFloat()
                    } else 1f
                    val animatedFraction by animateFloatAsState(
                        targetValue = fraction.coerceIn(0f, 1f),
                        animationSpec = spring(),
                        label = "pom_arc"
                    )
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                    val activeArcColor = if (pom.isBreakMode) secondaryColor else primaryColor

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(196.dp)
                    ) {
                        Canvas(modifier = Modifier.size(196.dp)) {
                            val strokePx = 12.dp.toPx()
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = activeArcColor,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedFraction,
                                useCenter = false,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%02d:%02d",
                                    pom.remainingSeconds / 60,
                                    pom.remainingSeconds % 60
                                ),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (pom.isBreakMode) "RECHARGE BREAK" else pom.selectedSubject.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Subject Tag Selector
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableSubjects) { subj ->
                            FilterChip(
                                selected = pom.selectedSubject == subj,
                                onClick = { onSelectPomodoroSubject(subj) },
                                label = { Text(subj) }
                            )
                        }
                    }

                    // Start / Pause / Reset Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onTogglePomodoroStartPause,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("pomodoro_start_pause_button")
                        ) {
                            Icon(
                                imageVector = if (pom.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (pom.isRunning) "Pause" else "Start Sprint")
                        }

                        FilledTonalIconButton(
                            onClick = onResetPomodoro,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("pomodoro_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Timer"
                            )
                        }
                    }
                }
            }
        }

        // 2. Today & Weekly Goals + Streak Card
        item(key = "streak_and_goals_card") {
            OutlinedCard(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B)
                            )
                            Column {
                                Text(
                                    text = "$streakDays-Day Study Streak",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Total logged: ${String.format(Locale.US, "%.1f", totalHours)} hours",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(onClick = { showEditGoalsDialog = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = "Edit Study Goals"
                                )
                            }
                            FilledTonalButton(
                                onClick = { showLogSessionDialog = true },
                                modifier = Modifier.testTag("log_study_time_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log Time")
                            }
                        }
                    }

                    // Today Goal Bar
                    val todayHours = todayMinutes / 60f
                    val dailyFraction = min(1f, todayHours / max(0.5f, prefs.dailyGoalHours))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Today's Progress",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", todayHours)}h / ${String.format(Locale.US, "%.1f", prefs.dailyGoalHours)}h",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = primaryColor
                            )
                        }
                        LinearProgressIndicator(
                            progress = { dailyFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = primaryColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    // Weekly Goal Bar
                    val weekHours = weekMinutes / 60f
                    val weeklyFraction = min(1f, weekHours / max(1f, prefs.weeklyGoalHours))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Last 7 Days",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", weekHours)}h / ${String.format(Locale.US, "%.1f", prefs.weeklyGoalHours)}h",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = secondaryColor
                            )
                        }
                        LinearProgressIndicator(
                            progress = { weeklyFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = secondaryColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // 3. 12-Week Contribution Heatmap
        item(key = "heatmap_card") {
            OutlinedCard(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Last 12 Weeks Consistency",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = selectedHeatmapTooltip ?: "Tap a square to inspect",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    val weeks = remember(heatmapDays) { heatmapDays.chunked(7) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        weeks.forEach { weekColumn ->
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                weekColumn.forEach { cell ->
                                    val cellColor = if (cell.minutes == 0) {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    } else {
                                        primaryColor.copy(alpha = (0.28f + 0.72f * cell.intensityFraction).coerceIn(0.28f, 1f))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(cellColor)
                                            .clickable {
                                                selectedHeatmapTooltip = "${cell.dateIso}: ${cell.minutes}m"
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. This Week Bar Chart & Subject Distribution
        item(key = "weekly_chart_and_subjects") {
            OutlinedCard(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "This Week's Output",
                        style = MaterialTheme.typography.titleMedium
                    )

                    val maxDayMins = max(60, last7Days.maxOfOrNull { it.second } ?: 60)
                    val barColor = MaterialTheme.colorScheme.primary
                    val emptyBarColor = MaterialTheme.colorScheme.surfaceContainerHighest

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                        ) {
                            val count = last7Days.size
                            val slotWidth = size.width / count
                            val barWidth = min(slotWidth * 0.56f, 32.dp.toPx())
                            last7Days.forEachIndexed { idx, (_, mins) ->
                                val ratio = (mins.toFloat() / maxDayMins.toFloat()).coerceIn(0.06f, 1f)
                                val barHeight = size.height * ratio
                                val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                                drawRoundRect(
                                    color = if (mins > 0) barColor else emptyBarColor,
                                    topLeft = Offset(left, size.height - barHeight),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            last7Days.forEach { (dayLabel, mins) ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = dayLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${mins}m",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Time by Subject",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (subjectTotals.isEmpty()) {
                        Text(
                            text = "No study sessions logged yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val maxSubMins = max(1, subjectTotals.maxOf { it.value })
                        subjectTotals.forEach { (subjectName, mins) ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = subjectName,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", mins / 60f)}h",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { mins.toFloat() / maxSubMins.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = secondaryColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Daily Study To-Do List
        item(key = "study_todos_card") {
            OutlinedCard(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TaskAlt,
                            contentDescription = null,
                            tint = primaryColor
                        )
                        Text(
                            text = "Daily Study Checklist",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTodoText,
                            onValueChange = { newTodoText = it },
                            placeholder = { Text("Add today's study goal…") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_todo_input")
                        )
                        Button(
                            onClick = {
                                if (newTodoText.isNotBlank()) {
                                    onAddTodo(newTodoText)
                                    newTodoText = ""
                                }
                            },
                            modifier = Modifier.testTag("add_todo_button")
                        ) {
                            Text("Add")
                        }
                    }

                    uiState.studyTodos.forEach { todo ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = todo.isDone,
                                onCheckedChange = { onToggleTodo(todo) }
                            )
                            Text(
                                text = todo.text,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = if (todo.isDone) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (todo.isDone) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onDeleteTodo(todo.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Delete Task",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. My Why & Earned Badges
        item(key = "why_and_badges_card") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedCard(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "My Why (Motivation Anchor)",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        OutlinedTextField(
                            value = prefs.motivationWhy,
                            onValueChange = onUpdateMotivationWhy,
                            placeholder = { Text("Why am I working toward this rank?") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                OutlinedCard(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B)
                            )
                            Text(
                                text = "Earned Milestones & Badges (${badges.size})",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (badges.isEmpty()) {
                            Text(
                                text = "Keep logging study sessions and completing topics to unlock badges.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                badges.forEach { badge ->
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(text = badge.emoji, fontSize = 18.sp)
                                            Column {
                                                Text(
                                                    text = badge.title,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Text(
                                                    text = badge.subtitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showLogSessionDialog) {
        var minsStr by remember { mutableStateOf("60") }
        var selectedSub by remember { mutableStateOf(availableSubjects.first()) }
        var dateStr by remember { mutableStateOf(todayIso) }

        AlertDialog(
            onDismissRequest = { showLogSessionDialog = false },
            title = { Text("Log Study Session") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = minsStr,
                        onValueChange = { minsStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Duration (Minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true
                    )
                    Text(
                        text = "Subject",
                        style = MaterialTheme.typography.labelMedium
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableSubjects) { sub ->
                            FilterChip(
                                selected = selectedSub == sub,
                                onClick = { selectedSub = sub },
                                label = { Text(sub) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = minsStr.toIntOrNull() ?: 0
                        if (mins > 0) {
                            onLogManualStudySession(dateStr, mins, selectedSub)
                        }
                        showLogSessionDialog = false
                    }
                ) {
                    Text("Save Log")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogSessionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditGoalsDialog) {
        var dailyStr by remember { mutableStateOf(prefs.dailyGoalHours.toString()) }
        var weeklyStr by remember { mutableStateOf(prefs.weeklyGoalHours.toString()) }
        AlertDialog(
            onDismissRequest = { showEditGoalsDialog = false },
            title = { Text("Daily & Weekly Study Goals") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dailyStr,
                        onValueChange = { dailyStr = it },
                        label = { Text("Daily Goal (Hours)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = weeklyStr,
                        onValueChange = { weeklyStr = it },
                        label = { Text("Weekly Goal (Hours)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateStudyGoals(
                            dailyStr.toFloatOrNull() ?: 3f,
                            weeklyStr.toFloatOrNull() ?: 18f
                        )
                        showEditGoalsDialog = false
                    }
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditGoalsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
