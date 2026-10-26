package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.presets.ExamPresetsData
import com.example.domain.CountdownCalculator
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.ExamPulseUiState
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScheduleScreen(
    uiState: ExamPulseUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (String) -> Unit,
    onOpenSettingsSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val year = uiState.calendarYear
    val month = uiState.calendarMonth
    val prefs = uiState.preferences
    val todayIso = remember(uiState.nowMillis) { CountdownCalculator.todayIso(uiState.nowMillis) }

    val monthTitle = remember(year, month) {
        val monthName = DateFormatSymbols(Locale.US).months[month]
        "$monthName $year"
    }

    val weekStartsMod = if (prefs.weekStartsMonday) 1 else 0
    val dayHeaders = remember(weekStartsMod) {
        val base = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        (0..6).map { base[(it + weekStartsMod) % 7] }
    }

    val (leadingEmptyCells, daysInMonth) = remember(year, month, weekStartsMod) {
        val cal = Calendar.getInstance().apply {
            set(year, month, 1)
        }
        val firstDay0To6 = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun..6=Sat
        val offset = (firstDay0To6 - weekStartsMod + 7) % 7
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        offset to maxDay
    }

    val selectedDateIso = uiState.selectedCalendarDateIso
    val selectedDayExams = remember(uiState.exams, selectedDateIso) {
        uiState.exams.filter { it.dateTimeIso.startsWith(selectedDateIso) }
    }
    val selectedDayMilestones = remember(uiState.exams, selectedDateIso) {
        uiState.exams.flatMap { exam ->
            exam.milestones
                .filter { it.dateTimeIso.startsWith(selectedDateIso) }
                .map { exam.name to it.title }
        }
    }
    val selectedDayLogs = remember(uiState.studyLogs, selectedDateIso) {
        uiState.studyLogs.filter { it.dateIso == selectedDateIso }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar_schedule_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Month Switcher Header
        item(key = "calendar_header") {
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrevMonth,
                        modifier = Modifier.testTag("calendar_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Month"
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier.testTag("calendar_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month"
                        )
                    }
                }
            }
        }

        // Calendar Grid
        item(key = "calendar_grid") {
            OutlinedCard(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Weekday Header Row
                    Row(modifier = Modifier.fillMaxWidth()) {
                        dayHeaders.forEach { dName ->
                            Text(
                                text = dName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    val totalCells = leadingEmptyCells + daysInMonth
                    val rows = (totalCells + 6) / 7

                    for (r in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (c in 0 until 7) {
                                val cellIndex = r * 7 + c
                                val dayNum = cellIndex - leadingEmptyCells + 1
                                if (dayNum in 1..daysInMonth) {
                                    val dateIso = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayNum)
                                    val dayExams = uiState.exams.filter { it.dateTimeIso.startsWith(dateIso) }
                                    val dayMilestones = uiState.exams.sumOf { ex ->
                                        ex.milestones.count { it.dateTimeIso.startsWith(dateIso) }
                                    }
                                    val dayMins = uiState.studyLogs.filter { it.dateIso == dateIso }.sumOf { it.minutes }
                                    val isToday = dateIso == todayIso
                                    val isSelected = dateIso == selectedDateIso

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 60.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                    dayExams.isNotEmpty() -> Color(dayExams.first().colorHex).copy(alpha = 0.14f)
                                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
                                                }
                                            )
                                            .border(
                                                width = if (isToday || isSelected) 1.5.dp else 0.dp,
                                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { onSelectDate(dateIso) }
                                            .padding(4.dp)
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            if (dayExams.isNotEmpty()) {
                                                Text(
                                                    text = dayExams.joinToString("") { it.emoji },
                                                    fontSize = 13.sp,
                                                    maxLines = 1
                                                )
                                            }
                                            if (dayMilestones > 0) {
                                                Text(
                                                    text = "◆ $dayMilestones",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                            if (dayMins > 0) {
                                                Text(
                                                    text = "${String.format(Locale.US, "%.1f", dayMins / 60f)}h",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        fontSize = 9.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Date Breakdown Card
        item(key = "selected_date_details") {
            OutlinedCard(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Events & Logs on $selectedDateIso",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (selectedDayExams.isEmpty() && selectedDayMilestones.isEmpty() && selectedDayLogs.isEmpty()) {
                        Text(
                            text = "No exams, milestones, or study sessions recorded on this date.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        selectedDayExams.forEach { ex ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = Color(ex.colorHex).copy(alpha = 0.16f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(text = ex.emoji, fontSize = 22.sp)
                                    Column {
                                        Text(
                                            text = "Exam Day: ${ex.name}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = ex.dateTimeIso.replace("T", " · "),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        selectedDayMilestones.forEach { (examName, msTitle) ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "◆ $examName: $msTitle",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (selectedDayLogs.isNotEmpty()) {
                            val totalMins = selectedDayLogs.sumOf { it.minutes }
                            Text(
                                text = "Study Sessions (${String.format(Locale.US, "%.1f", totalMins / 60f)}h total)",
                                style = MaterialTheme.typography.labelLarge
                            )
                            selectedDayLogs.forEach { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "• ${log.subjectName}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "${log.minutes} mins",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Shortcut to Full Settings & Backup
        item(key = "open_settings_card") {
            FilledTonalButton(
                onClick = onOpenSettingsSheet,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_open_settings_button")
            ) {
                Text("Open Appearance, Time Zone, Pomodoro & Backup Settings")
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsAndBackupSheet(
    uiState: ExamPulseUiState,
    onDismiss: () -> Unit,
    onUpdateThemeMode: (String) -> Unit,
    onUpdateAccentColor: (Long) -> Unit,
    onUpdateDynamicColor: (Boolean) -> Unit,
    onUpdate24Hour: (Boolean) -> Unit,
    onUpdateWeekStartsMonday: (Boolean) -> Unit,
    onUpdateShowSleeps: (Boolean) -> Unit,
    onUpdateAutoArchive: (Boolean) -> Unit,
    onUpdateTimeZoneOffset: (String) -> Unit,
    onToggleRestDay: (Int) -> Unit,
    onUpdatePomodoroDurations: (Int, Int) -> Unit,
    onExportBackupJson: () -> String,
    onExportIcsText: () -> String,
    onImportBackupJson: (String) -> Unit,
    onResetAllData: () -> Unit
) {
    val prefs = uiState.preferences
    val clipboard = LocalClipboardManager.current

    var focusMinsStr by remember(prefs.pomodoroFocusMinutes) { mutableStateOf(prefs.pomodoroFocusMinutes.toString()) }
    var breakMinsStr by remember(prefs.pomodoroBreakMinutes) { mutableStateOf(prefs.pomodoroBreakMinutes.toString()) }
    var exportModalTitle by remember { mutableStateOf<String?>(null) }
    var exportModalContent by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("settings_backup_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Settings, Schedule & Backup",
                style = MaterialTheme.typography.headlineSmall
            )

            // 1. Appearance Section
            OutlinedCard(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Appearance & Display",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("auto" to "System Auto", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                            FilterChip(
                                selected = prefs.themeMode == key,
                                onClick = { onUpdateThemeMode(key) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text(
                        text = "Accent Color Palette",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(ExamPresetsData.COLOR_PALETTE) { swatch ->
                            val selected = !prefs.useDynamicColor && prefs.accentColorHex == swatch
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(swatch))
                                    .clickable { onUpdateAccentColor(swatch) }
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected Accent",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    SettingToggleRow(
                        title = "Material You Dynamic Color (Android 12+)",
                        checked = prefs.useDynamicColor,
                        onCheckedChange = onUpdateDynamicColor
                    )
                    SettingToggleRow(
                        title = "24-hour clock format",
                        checked = prefs.use24HourFormat,
                        onCheckedChange = onUpdate24Hour
                    )
                    SettingToggleRow(
                        title = "Week starts on Monday",
                        checked = prefs.weekStartsMonday,
                        onCheckedChange = onUpdateWeekStartsMonday
                    )
                    SettingToggleRow(
                        title = "Show “sleeps left” instead of days",
                        checked = prefs.showSleepsInsteadOfDays,
                        onCheckedChange = onUpdateShowSleeps
                    )
                    SettingToggleRow(
                        title = "Auto-archive past exams (>2 days completed)",
                        checked = prefs.autoArchivePastExams,
                        onCheckedChange = onUpdateAutoArchive
                    )
                }
            }

            // 2. Time Zone, Rest Days & Pomodoro
            OutlinedCard(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Time Zone, Rest Days & Pomodoro",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        text = "Time Zone for Exam Countdowns",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ExamPresetsData.TIME_ZONES.forEach { (offsetKey, label) ->
                            FilterChip(
                                selected = prefs.timeZoneOffset == offsetKey,
                                onClick = { onUpdateTimeZoneOffset(offsetKey) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Text(
                        text = "Rest Days (excluded from working study days)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayNames.forEachIndexed { idx, dLabel ->
                            FilterChip(
                                selected = prefs.restDays.contains(idx),
                                onClick = { onToggleRestDay(idx) },
                                label = { Text(dLabel) }
                            )
                        }
                    }

                    Text(
                        text = "Pomodoro Durations (Minutes)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = focusMinsStr,
                            onValueChange = {
                                focusMinsStr = it.filter { ch -> ch.isDigit() }
                                val f = focusMinsStr.toIntOrNull() ?: 25
                                val b = breakMinsStr.toIntOrNull() ?: 5
                                onUpdatePomodoroDurations(f, b)
                            },
                            label = { Text("Focus (m)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = breakMinsStr,
                            onValueChange = {
                                breakMinsStr = it.filter { ch -> ch.isDigit() }
                                val f = focusMinsStr.toIntOrNull() ?: 25
                                val b = breakMinsStr.toIntOrNull() ?: 5
                                onUpdatePomodoroDurations(f, b)
                            },
                            label = { Text("Break (m)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Data Backup, .ics Export & Reset
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
                            imageVector = Icons.Outlined.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Local Data & Portability",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = "All data is stored privately on this device using Room SQLite. Export JSON backups or .ics calendar files anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                exportModalTitle = "Export Backup JSON"
                                exportModalContent = onExportBackupJson()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }
                        FilledTonalButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import JSON")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                exportModalTitle = "Calendar (.ics) Export"
                                exportModalContent = onExportIcsText()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Calendar (.ics)")
                        }
                        OutlinedButton(
                            onClick = { showResetConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset All")
                        }
                    }
                }
            }
        }
    }

    if (exportModalTitle != null) {
        AlertDialog(
            onDismissRequest = { exportModalTitle = null },
            title = { Text(exportModalTitle ?: "") },
            text = {
                OutlinedTextField(
                    value = exportModalContent,
                    onValueChange = {},
                    readOnly = true,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(exportModalContent))
                        exportModalTitle = null
                    }
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportModalTitle = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (showImportDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Backup JSON") },
            text = {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    placeholder = { Text("Paste Exam Pulse or web Exam Countdown backup JSON here…") },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jsonInput.isNotBlank()) {
                            onImportBackupJson(jsonInput)
                            showImportDialog = false
                        }
                    },
                    enabled = jsonInput.isNotBlank()
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset All Data?") },
            text = {
                Text("This will permanently delete all exams, syllabus progress, study logs, and mock scores from this device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAllData()
                        showResetConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
