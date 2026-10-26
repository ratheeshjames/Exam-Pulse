package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Exam
import com.example.data.presets.ExamPresetsData
import com.example.domain.CountdownCalculator
import com.example.ui.components.ExamCardItem
import com.example.ui.components.HeroExamSpotlightCard
import com.example.ui.viewmodel.ExamPulseUiState
import com.example.ui.viewmodel.SortOption
import java.util.Calendar

@Composable
fun ExamsDashboardScreen(
    uiState: ExamPulseUiState,
    onDismissWelcome: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectSortOption: (SortOption) -> Unit,
    onChangeViewStyle: (String) -> Unit,
    onPinToggle: (String) -> Unit,
    onOpenFocus: (String) -> Unit,
    onShareExam: (Exam) -> Unit,
    onOpenPlanner: (String) -> Unit,
    onEditExam: (Exam) -> Unit,
    onDeleteExam: (Exam) -> Unit,
    onOpenNewExam: () -> Unit,
    onOpenPresets: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.preferences
    val now = uiState.nowMillis

    val countdownMap = remember(uiState.exams, now, prefs.timeZoneOffset, prefs.use24HourFormat, prefs.restDaysCsv, prefs.autoArchivePastExams) {
        uiState.exams.associate { exam ->
            exam.id to CountdownCalculator.computeLiveCountdown(
                exam = exam,
                nowMillis = now,
                timeZoneOffset = prefs.timeZoneOffset,
                use24Hour = prefs.use24HourFormat,
                restDays = prefs.restDays,
                autoArchive = prefs.autoArchivePastExams
            )
        }
    }

    val categories = remember(uiState.exams) {
        val dynamicCats = uiState.exams.map { it.category }.distinct().sorted()
        listOf("All") + dynamicCats + listOf("Archived")
    }

    val filteredExams = remember(
        uiState.exams,
        uiState.selectedCategory,
        uiState.searchQuery,
        uiState.sortOption,
        countdownMap
    ) {
        val q = uiState.searchQuery.trim().lowercase()
        val list = uiState.exams.filter { exam ->
            val cd = countdownMap[exam.id]
            val isArch = cd?.isArchived == true
            val matchesArchive = if (uiState.selectedCategory == "Archived") isArch else !isArch
            val matchesCat = uiState.selectedCategory == "All" ||
                uiState.selectedCategory == "Archived" ||
                exam.category.equals(uiState.selectedCategory, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                exam.name.lowercase().contains(q) ||
                exam.category.lowercase().contains(q) ||
                exam.venue.lowercase().contains(q)
            matchesArchive && matchesCat && matchesQuery
        }

        when (uiState.sortOption) {
            SortOption.NAME -> list.sortedBy { it.name.lowercase() }
            SortOption.PROGRESS -> list.sortedByDescending { countdownMap[it.id]?.elapsedProgress ?: 0f }
            SortOption.SOONEST -> list.sortedBy {
                CountdownCalculator.parseTargetMillis(it.dateTimeIso, prefs.timeZoneOffset)
            }
        }
    }

    val heroExam = uiState.heroExam
    val dailyQuote = remember {
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        ExamPresetsData.QUOTES[dayOfMonth % ExamPresetsData.QUOTES.size]
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("exams_dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Welcome Banner
        item(key = "welcome_banner") {
            AnimatedVisibility(visible = !prefs.hasSeenWelcome) {
                ElevatedCard(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("welcome_banner_card")
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
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.welcome_banner_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stringResource(R.string.welcome_banner_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onOpenPresets,
                                modifier = Modifier.testTag("welcome_presets_button")
                            ) {
                                Text("Browse 20+ Presets")
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = onDismissWelcome,
                                modifier = Modifier.testTag("dismiss_welcome_button")
                            ) {
                                Text(stringResource(R.string.dismiss_welcome))
                            }
                        }
                    }
                }
            }
        }

        // Hero Spotlight Countdown
        if (heroExam != null) {
            val heroCd = countdownMap[heroExam.id]
            if (heroCd != null) {
                item(key = "hero_${heroExam.id}") {
                    HeroExamSpotlightCard(
                        exam = heroExam,
                        countdown = heroCd,
                        isPinned = prefs.pinnedExamId == heroExam.id,
                        showSleeps = prefs.showSleepsInsteadOfDays,
                        onPinToggle = { onPinToggle(heroExam.id) },
                        onOpenFocus = { onOpenFocus(heroExam.id) },
                        onShareCountdown = { onShareExam(heroExam) },
                        onOpenPlanner = { onOpenPlanner(heroExam.id) },
                        onEditExam = { onEditExam(heroExam) },
                        onDeleteExam = { onDeleteExam(heroExam) }
                    )
                }
            }
        }

        // Daily Quote Pill
        item(key = "quote_banner") {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "“$dailyQuote”",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        // Search & Filter Controls
        item(key = "search_and_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.search_exams_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search Exams"
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_exams_input")
                )

                // Category Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = { onSelectCategory(category) },
                            label = { Text(category) },
                            modifier = Modifier.testTag("category_chip_$category")
                        )
                    }
                }

                // Header + Sort & View Mode Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Exams (${filteredExams.size})",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SortOption.entries.forEach { option ->
                            FilterChip(
                                selected = uiState.sortOption == option,
                                onClick = { onSelectSortOption(option) },
                                label = { Text(option.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }

                        IconButton(
                            onClick = {
                                val nextStyle = when (prefs.viewStyle) {
                                    "expressive" -> "compact"
                                    "compact" -> "grid"
                                    else -> "expressive"
                                }
                                onChangeViewStyle(nextStyle)
                            },
                            modifier = Modifier.testTag("toggle_view_style_button")
                        ) {
                            Icon(
                                imageVector = when (prefs.viewStyle) {
                                    "compact" -> Icons.AutoMirrored.Outlined.FormatListBulleted
                                    "grid" -> Icons.Outlined.GridView
                                    else -> Icons.Outlined.ViewAgenda
                                },
                                contentDescription = "Toggle View Style"
                            )
                        }
                    }
                }
            }
        }

        // Empty State or Exam Cards
        if (filteredExams.isEmpty()) {
            item(key = "empty_exams_state") {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_exams_card"),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.EventNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = stringResource(R.string.empty_exams_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.empty_exams_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            FilledTonalButton(onClick = onOpenPresets) {
                                Text("Browse Presets")
                            }
                            Button(onClick = onOpenNewExam) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Exam")
                            }
                        }
                    }
                }
            }
        } else if (prefs.viewStyle == "grid" && filteredExams.size > 1) {
            val rows = filteredExams.chunked(2)
            items(rows) { rowExams ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowExams.forEach { exam ->
                        val cd = countdownMap[exam.id] ?: return@forEach
                        Box(modifier = Modifier.weight(1f)) {
                            ExamCardItem(
                                exam = exam,
                                countdown = cd,
                                isPinned = prefs.pinnedExamId == exam.id,
                                viewStyle = "expressive",
                                showSleeps = prefs.showSleepsInsteadOfDays,
                                onPinToggle = { onPinToggle(exam.id) },
                                onOpenFocus = { onOpenFocus(exam.id) },
                                onShareCountdown = { onShareExam(exam) },
                                onOpenPlanner = { onOpenPlanner(exam.id) },
                                onEditExam = { onEditExam(exam) },
                                onDeleteExam = { onDeleteExam(exam) }
                            )
                        }
                    }
                    if (rowExams.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            items(filteredExams, key = { it.id }) { exam ->
                val cd = countdownMap[exam.id] ?: return@items
                ExamCardItem(
                    exam = exam,
                    countdown = cd,
                    isPinned = prefs.pinnedExamId == exam.id,
                    viewStyle = prefs.viewStyle,
                    showSleeps = prefs.showSleepsInsteadOfDays,
                    onPinToggle = { onPinToggle(exam.id) },
                    onOpenFocus = { onOpenFocus(exam.id) },
                    onShareCountdown = { onShareExam(exam) },
                    onOpenPlanner = { onOpenPlanner(exam.id) },
                    onEditExam = { onEditExam(exam) },
                    onDeleteExam = { onDeleteExam(exam) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}
