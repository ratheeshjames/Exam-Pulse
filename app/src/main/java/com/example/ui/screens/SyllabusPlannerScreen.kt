package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.Exam
import com.example.data.presets.ExamPresetsData
import com.example.domain.CountdownCalculator
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.UrgencyCritical
import com.example.ui.viewmodel.ExamPulseUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SyllabusPlannerScreen(
    uiState: ExamPulseUiState,
    onSelectExam: (String) -> Unit,
    onUpdateHoursPerTopic: (String, Float) -> Unit,
    onApplyTemplate: (String, String) -> Unit,
    onAddSubject: (String, String, Int) -> Unit,
    onUpdateSubjectWeightage: (String, String, Int) -> Unit,
    onDeleteSubject: (String, String) -> Unit,
    onAddTopic: (String, String, String) -> Unit,
    onToggleTopicDone: (String, String, String) -> Unit,
    onToggleTopicWeak: (String, String, String) -> Unit,
    onDeleteTopic: (String, String, String) -> Unit,
    onAddMilestone: (String, String, String) -> Unit,
    onDeleteMilestone: (String, String) -> Unit,
    onOpenNewExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeExam = uiState.activeExam
    if (activeExam == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            OutlinedCard(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "No Exam Selected",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Add an exam first to unlock the hours planner, 1-3-7-21 spaced repetition, and syllabus breakdown.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onOpenNewExam) {
                        Text("Create Exam")
                    }
                }
            }
        }
        return
    }

    val prefs = uiState.preferences
    val now = uiState.nowMillis
    val countdown = remember(activeExam, now, prefs.timeZoneOffset, prefs.restDaysCsv) {
        CountdownCalculator.computeLiveCountdown(
            exam = activeExam,
            nowMillis = now,
            timeZoneOffset = prefs.timeZoneOffset,
            use24Hour = prefs.use24HourFormat,
            restDays = prefs.restDays,
            autoArchive = prefs.autoArchivePastExams
        )
    }

    val revisionsDue = remember(activeExam, now) {
        CountdownCalculator.getRevisionsDueToday(activeExam, now)
    }
    val weakTopics = remember(activeExam) {
        CountdownCalculator.getWeakTopics(activeExam)
    }

    val remainingTopics = max(0, activeExam.totalTopics - activeExam.completedTopics)
    val neededHours = remainingTopics * activeExam.hoursPerTopic
    val studyDaysLeft = max(1, countdown.workingDaysLeft)
    val requiredHoursPerDay = neededHours / studyDaysLeft.toFloat()

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showAddMilestoneDialog by remember { mutableStateOf(false) }
    var addTopicForSubjectId by remember { mutableStateOf<String?>(null) }
    var editWeightForSubject by remember { mutableStateOf<Pair<String, Int>?>(null) }

    val accentColor = Color(activeExam.colorHex)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("syllabus_planner_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Exam Selector Bar + Actions
        item(key = "exam_selector_row") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.exams, key = { it.id }) { exam ->
                        FilterChip(
                            selected = exam.id == activeExam.id,
                            onClick = { onSelectExam(exam.id) },
                            label = { Text("${exam.emoji} ${exam.name}") },
                            modifier = Modifier.testTag("planner_exam_chip_${exam.id}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showAddSubjectDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_subject_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Subject")
                    }

                    FilledTonalButton(
                        onClick = { showTemplateDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("load_template_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LibraryAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Templates")
                    }
                }
            }
        }

        // Smart Hours Velocity Planner Card
        item(key = "hours_velocity_card") {
            ElevatedCard(
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
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = accentColor
                        )
                        Text(
                            text = "Study Velocity & Hours Planner",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$remainingTopics topics left × ${String.format(Locale.US, "%.1f", activeExam.hoursPerTopic)}h",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.0f", neededHours)}h total study needed",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Across $studyDaysLeft working study days (rest days excluded)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (requiredHoursPerDay > 8f) {
                                UrgencyCritical.copy(alpha = 0.15f)
                            } else {
                                accentColor.copy(alpha = 0.15f)
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", requiredHoursPerDay),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (requiredHoursPerDay > 8f) UrgencyCritical else accentColor
                                )
                                Text(
                                    text = "h / day",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Estimated Hours per Topic",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", activeExam.hoursPerTopic)} hrs",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                            )
                        }
                        Slider(
                            value = activeExam.hoursPerTopic,
                            onValueChange = { onUpdateHoursPerTopic(activeExam.id, it) },
                            valueRange = 1f..12f,
                            steps = 21
                        )
                    }
                }
            }
        }

        // 1-3-7-21 Spaced Repetition & Weak Topics Row
        item(key = "spaced_repetition_and_weak_topics") {
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
                                imageVector = Icons.Outlined.HistoryEdu,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Revisions Due Today (1-3-7-21 Rule)",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (revisionsDue.isEmpty()) {
                            Text(
                                text = "No spaced-repetition topics due today. Completed topics automatically resurface 1, 3, 7, and 21 days after mastery.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            revisionsDue.forEach { rev ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${rev.subjectName} · ${rev.topicName}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondary
                                        ) {
                                            Text(
                                                text = "Day ${rev.intervalDay}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Weak Topics Card
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
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Flagged Weak Topics (${weakTopics.size})",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (weakTopics.isEmpty()) {
                            Text(
                                text = "Tap the flag icon on any syllabus topic to bookmark weak concepts for targeted revision.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                weakTopics.forEach { wt ->
                                    AssistChip(
                                        onClick = {
                                            onToggleTopicWeak(activeExam.id, wt.subjectId, wt.topicId)
                                        },
                                        label = { Text("${wt.subjectName}: ${wt.topicName}") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Flag,
                                                contentDescription = "Unflag weak topic",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Milestones Card
        item(key = "milestones_card") {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EventAvailable,
                                contentDescription = null,
                                tint = accentColor
                            )
                            Text(
                                text = "Exam Milestones",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        TextButton(
                            onClick = { showAddMilestoneDialog = true },
                            modifier = Modifier.testTag("add_milestone_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Milestone")
                        }
                    }

                    if (activeExam.milestones.isEmpty()) {
                        Text(
                            text = "Track registration deadlines, admit card release, and mock test windows.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        activeExam.milestones.forEach { ms ->
                            val targetMs = CountdownCalculator.parseTargetMillis(ms.dateTimeIso, prefs.timeZoneOffset)
                            val daysLeft = ceil((targetMs - now).toDouble() / 86_400_000.0).toInt()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ms.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                    Text(
                                        text = ms.dateTimeIso.replace("T", " "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = if (daysLeft >= 0) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = if (daysLeft >= 0) "${daysLeft}d left" else "Past",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteMilestone(activeExam.id, ms.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Delete Milestone",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Syllabus Subjects Header
        item(key = "subjects_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Syllabus Subjects (${activeExam.subjects.size})",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${activeExam.completedTopics}/${activeExam.totalTopics} topics done",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = accentColor
                )
            }
        }

        // Subject Cards
        items(activeExam.subjects, key = { it.id }) { subject ->
            val doneCount = subject.topics.count { it.isDone }
            val totalCount = subject.topics.size
            val subjectProgress = if (totalCount > 0) doneCount.toFloat() / totalCount.toFloat() else 0f

            OutlinedCard(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subject_card_${subject.name}")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subject.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$doneCount/$totalCount topics",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            editWeightForSubject = subject.id to subject.weightagePercent
                                        }
                                ) {
                                    Text(
                                        text = "${subject.weightagePercent}% weight",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { addTopicForSubjectId = subject.id }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Topic",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Topic")
                            }
                            IconButton(
                                onClick = { onDeleteSubject(activeExam.id, subject.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete Subject",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { subjectProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = accentColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    if (subject.topics.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            subject.topics.forEach { topic ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = topic.isDone,
                                        onCheckedChange = {
                                            onToggleTopicDone(activeExam.id, subject.id, topic.id)
                                        }
                                    )
                                    Text(
                                        text = topic.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = if (topic.isDone) {
                                                TextDecoration.LineThrough
                                            } else {
                                                TextDecoration.None
                                            }
                                        ),
                                        color = if (topic.isDone) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            onToggleTopicWeak(activeExam.id, subject.id, topic.id)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (topic.isWeak) Icons.Filled.Flag else Icons.Outlined.Flag,
                                            contentDescription = "Flag Weak Topic",
                                            tint = if (topic.isWeak) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            onDeleteTopic(activeExam.id, subject.id, topic.id)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Delete Topic",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
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

    // Dialogs
    if (showAddSubjectDialog) {
        var subjectName by remember { mutableStateOf("") }
        var weightStr by remember { mutableStateOf("10") }
        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            title = { Text("Add Syllabus Subject") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = { Text("Subject Name") },
                        placeholder = { Text("e.g., Operating Systems") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Marks Weightage %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddSubject(
                            activeExam.id,
                            subjectName,
                            weightStr.toIntOrNull() ?: 10
                        )
                        showAddSubjectDialog = false
                    },
                    enabled = subjectName.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubjectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("Load Engineering Syllabus Template") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Select a branch to append its core subjects and topics to ${activeExam.name}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExamPresetsData.SYLLABUS_TEMPLATES.keys.forEach { key ->
                        OutlinedCard(
                            onClick = {
                                onApplyTemplate(activeExam.id, key)
                                showTemplateDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "GATE / Engineering ($key)",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Load →",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplateDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    addTopicForSubjectId?.let { targetSubId ->
        var topicName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addTopicForSubjectId = null },
            title = { Text("Add Topic") },
            text = {
                OutlinedTextField(
                    value = topicName,
                    onValueChange = { topicName = it },
                    label = { Text("Topic Name") },
                    placeholder = { Text("e.g., Virtual Memory & Paging") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddTopic(activeExam.id, targetSubId, topicName)
                        addTopicForSubjectId = null
                    },
                    enabled = topicName.isNotBlank()
                ) {
                    Text("Add Topic")
                }
            },
            dismissButton = {
                TextButton(onClick = { addTopicForSubjectId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    editWeightForSubject?.let { (subId, currentWeight) ->
        var weightStr by remember { mutableStateOf(currentWeight.toString()) }
        AlertDialog(
            onDismissRequest = { editWeightForSubject = null },
            title = { Text("Update Marks Weightage %") },
            text = {
                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Weightage %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateSubjectWeightage(activeExam.id, subId, weightStr.toIntOrNull() ?: 0)
                        editWeightForSubject = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editWeightForSubject = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddMilestoneDialog) {
        var title by remember { mutableStateOf("") }
        var dateIso by remember {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            mutableStateOf(sdf.format(Date(System.currentTimeMillis() + 86_400_000L * 14)))
        }
        var showDateModal by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddMilestoneDialog = false },
            title = { Text("New Exam Milestone") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Milestone Title") },
                        placeholder = { Text("e.g., Admit Card Release") },
                        singleLine = true
                    )
                    FilledTonalButton(
                        onClick = { showDateModal = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Date: $dateIso")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddMilestone(activeExam.id, title, "${dateIso}T09:00")
                        showAddMilestoneDialog = false
                    },
                    enabled = title.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMilestoneDialog = false }) {
                    Text("Cancel")
                }
            }
        )

        if (showDateModal) {
            val dpState = rememberDatePickerState(
                initialSelectedDateMillis = CountdownCalculator.parseTargetMillis("${dateIso}T09:00")
            )
            DatePickerDialog(
                onDismissRequest = { showDateModal = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dpState.selectedDateMillis?.let { ms ->
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                    timeZone = TimeZone.getTimeZone("UTC")
                                }
                                dateIso = sdf.format(Date(ms))
                            }
                            showDateModal = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDateModal = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = dpState)
            }
        }
    }
}
