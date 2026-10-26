package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Exam
import com.example.data.presets.ExamPreset
import com.example.data.presets.ExamPresetsData
import com.example.domain.CountdownCalculator
import com.example.domain.LiveCountdown
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExamEditorSheet(
    initialExam: Exam?,
    onDismiss: () -> Unit,
    onSave: (Exam) -> Unit
) {
    val defaultDateIso = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.format(Date(System.currentTimeMillis() + 86_400_000L * 30))
    }
    val initialDatePart = initialExam?.dateTimeIso?.substringBefore("T") ?: defaultDateIso
    val initialTimePart = initialExam?.dateTimeIso?.substringAfter("T", "09:00")?.take(5) ?: "09:00"

    var name by remember { mutableStateOf(initialExam?.name ?: "") }
    var dateIso by remember { mutableStateOf(initialDatePart) }
    var timeIso by remember { mutableStateOf(initialTimePart) }
    var emoji by remember { mutableStateOf(initialExam?.emoji ?: "🎯") }
    var colorHex by remember { mutableLongStateOf(initialExam?.colorHex ?: 0xFF4F46E5L) }
    var category by remember { mutableStateOf(initialExam?.category ?: "GATE") }
    var venue by remember { mutableStateOf(initialExam?.venue ?: "") }
    var officialLink by remember { mutableStateOf(initialExam?.officialLink ?: "") }
    var attemptStr by remember { mutableStateOf((initialExam?.attempt ?: 1).toString()) }
    var notes by remember { mutableStateOf(initialExam?.notes ?: "") }
    var templateChoice by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("exam_editor_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (initialExam == null) "Create Exam Countdown" else "Edit Exam Countdown",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Exam Name *") },
                placeholder = { Text("e.g., GATE CS 2027, JEE Advanced") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exam_name_input")
            )

            // Date & Time Picker Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = "Pick Date",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = dateIso)
                }

                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = "Pick Time",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = timeIso)
                }
            }

            // Emoji Selector
            Text(
                text = "Icon & Accent Color",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ExamPresetsData.EMOJI_OPTIONS) { itemEmoji ->
                    val selected = emoji == itemEmoji
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { emoji = itemEmoji }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = itemEmoji, fontSize = 20.sp)
                        }
                    }
                }
            }

            // Color Swatches
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ExamPresetsData.COLOR_PALETTE) { swatch ->
                    val selected = colorHex == swatch
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(swatch))
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { colorHex = swatch }
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected Color",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Category Chips
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ExamPresetsData.CATEGORIES.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }

            // Optional Syllabus Template on Creation
            if (initialExam == null) {
                Text(
                    text = "Pre-load Syllabus Template (Optional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = templateChoice.isEmpty(),
                        onClick = { templateChoice = "" },
                        label = { Text("Blank") }
                    )
                    ExamPresetsData.SYLLABUS_TEMPLATES.keys.forEach { key ->
                        FilterChip(
                            selected = templateChoice == key,
                            onClick = { templateChoice = key },
                            label = { Text("GATE/Eng $key") }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue / Center") },
                    singleLine = true,
                    modifier = Modifier.weight(1.6f)
                )
                OutlinedTextField(
                    value = attemptStr,
                    onValueChange = { attemptStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Attempt #") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = officialLink,
                onValueChange = { officialLink = it },
                label = { Text("Official Website URL (https://...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Preparation Notes / Target Score") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (name.isBlank()) return@Button
                        val initialSubjects = initialExam?.subjects
                            ?: if (templateChoice.isNotEmpty()) {
                                ExamPresetsData.parseTemplate(templateChoice)
                            } else emptyList()

                        val result = Exam(
                            id = initialExam?.id ?: UUID.randomUUID().toString().take(8),
                            name = name.trim(),
                            dateTimeIso = "${dateIso}T${timeIso}",
                            startMillis = initialExam?.startMillis ?: System.currentTimeMillis(),
                            emoji = emoji,
                            colorHex = colorHex,
                            category = category,
                            venue = venue.trim(),
                            officialLink = officialLink.trim(),
                            attempt = attemptStr.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            notes = notes.trim(),
                            hoursPerTopic = initialExam?.hoursPerTopic ?: 4f,
                            subjects = initialSubjects,
                            milestones = initialExam?.milestones ?: emptyList(),
                            mocks = initialExam?.mocks ?: emptyList()
                        )
                        onSave(result)
                        onDismiss()
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.testTag("save_exam_button")
                ) {
                    Text(if (initialExam == null) "Create Exam" else "Save Changes")
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(dateIso) {
            CountdownCalculator.parseTargetMillis("${dateIso}T09:00")
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            dateIso = sdf.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val parts = timeIso.split(":")
        val initHour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val initMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val timePickerState = rememberTimePickerState(
            initialHour = initHour,
            initialMinute = initMin,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Exam Time") },
            text = {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        timeIso = String.format(
                            Locale.US,
                            "%02d:%02d",
                            timePickerState.hour,
                            timePickerState.minute
                        )
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsBrowserSheet(
    onDismiss: () -> Unit,
    onAddPreset: (ExamPreset) -> Unit
) {
    var filterCat by remember { mutableStateOf("All") }
    val categories = remember { listOf("All") + ExamPresetsData.PRESETS.map { it.category }.distinct() }
    val filtered = remember(filterCat) {
        if (filterCat == "All") ExamPresetsData.PRESETS
        else ExamPresetsData.PRESETS.filter { it.category == filterCat }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("presets_browser_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Competitive Exam Presets",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Dates automatically adjust to the next upcoming exam cycle. GATE presets also include full subject & topic syllabi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = filterCat == cat,
                        onClick = { filterCat = cat },
                        label = { Text(cat) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.name }) { preset ->
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(preset.colorHex).copy(alpha = 0.16f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = preset.emoji, fontSize = 20.sp)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${preset.category} · Annual window (${preset.monthDay})" +
                                        if (preset.syllabusTemplateKey != null) " · Includes Syllabus" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    onAddPreset(preset)
                                    onDismiss()
                                },
                                modifier = Modifier.testTag("add_preset_${preset.name}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImmersiveFocusOverlay(
    exam: Exam,
    countdown: LiveCountdown,
    motivationWhy: String,
    showSleeps: Boolean,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val accentColor = Color(exam.colorHex)
    val infiniteTransition = rememberInfiniteTransition(label = "focus_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A12))
            .clickable { onDismiss() }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("immersive_focus_overlay"),
        contentAlignment = Alignment.Center
    ) {
        // Ambient radial glow + Exam Day Confetti Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = pulseAlpha),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height * 0.42f),
                    radius = size.minDimension * 0.72f
                )
            )
            if (countdown.isExamDay) {
                val confettiColors = listOf(
                    Color(0xFFF43F5E),
                    Color(0xFFFBBF24),
                    Color(0xFF10B981),
                    Color(0xFF38BDF8),
                    Color(0xFFA855F7)
                )
                for (i in 0 until 45) {
                    val x = ((i * 97) % 100) / 100f * size.width
                    val y = ((i * 53 + (pulseAlpha * 400).toInt()) % 100) / 100f * size.height
                    drawRect(
                        color = confettiColors[i % confettiColors.size],
                        topLeft = Offset(x, y),
                        size = Size(14f, 22f)
                    )
                }
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Exit Focus Mode",
                tint = Color.White.copy(alpha = 0.8f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.22f),
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = exam.emoji, fontSize = 44.sp)
                }
            }

            Text(
                text = exam.name,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "${countdown.formattedTargetDate} · ${countdown.formattedTargetTime}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (countdown.isExamDay) {
                Text(
                    text = "EXAM DAY!",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF10B981)
                )
            } else {
                Text(
                    text = "${countdown.wholeDays}",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 88.sp,
                        lineHeight = 92.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = if (showSleeps) "SLEEPS REMAINING" else "DAYS REMAINING",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 2.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = accentColor
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = String.format(
                            Locale.US,
                            "%02d : %02d : %02d",
                            countdown.hours,
                            countdown.minutes,
                            countdown.seconds
                        ),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "“${motivationWhy.ifBlank { ExamPresetsData.QUOTES.first() }}”",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tap anywhere to exit immersive mode",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f)
            )
        }
    }
}
