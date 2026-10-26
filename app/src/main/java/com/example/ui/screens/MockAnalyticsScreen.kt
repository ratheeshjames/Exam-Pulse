package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.CountdownCalculator
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.ExamPulseUiState
import java.util.Locale

@Composable
fun MockAnalyticsScreen(
    uiState: ExamPulseUiState,
    onSelectExam: (String) -> Unit,
    onAddMockResult: (String, String, Float, Float, Float?, Int?) -> Unit,
    onDeleteMockResult: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeExam = uiState.activeExam
    if (activeExam == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Add an exam on the Exams tab to track mock test analytics.")
        }
        return
    }

    val mocks = activeExam.mocks
    val percentages = remember(mocks) { mocks.map { it.percentage } }
    val avgPercent = remember(percentages) {
        if (percentages.isNotEmpty()) percentages.average().toFloat() else 0f
    }
    val bestPercent = remember(percentages) {
        percentages.maxOrNull() ?: 0f
    }
    val bestRank = remember(mocks) {
        mocks.mapNotNull { it.rank }.minOrNull()
    }

    var showAddMockDialog by remember { mutableStateOf(false) }
    val accentColor = Color(activeExam.colorHex)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("mock_analytics_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Exam Selector + Add Mock Button
        item(key = "mock_exam_selector") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.exams, key = { it.id }) { exam ->
                        FilterChip(
                            selected = exam.id == activeExam.id,
                            onClick = { onSelectExam(exam.id) },
                            label = { Text("${exam.emoji} ${exam.name}") }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showAddMockDialog = true },
                    modifier = Modifier.testTag("add_mock_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Mock")
                }
            }
        }

        // KPI Summary Row
        item(key = "mock_kpi_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MockStatTile(
                    label = "AVERAGE",
                    value = "${String.format(Locale.US, "%.1f", avgPercent)}%",
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                MockStatTile(
                    label = "PEAK SCORE",
                    value = "${String.format(Locale.US, "%.1f", bestPercent)}%",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                MockStatTile(
                    label = "BEST RANK",
                    value = bestRank?.let { "#$it" } ?: "—",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Score Trend Line & Area Chart Card
        item(key = "score_trend_card") {
            ElevatedCard(
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
                            imageVector = Icons.Outlined.Analytics,
                            contentDescription = null,
                            tint = accentColor
                        )
                        Text(
                            text = "Performance Trajectory (${mocks.size} tests)",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (mocks.size < 2) {
                        Text(
                            text = "Log at least 2 mock tests to visualize your score trend curve.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            val padX = 16.dp.toPx()
                            val padY = 14.dp.toPx()
                            val plotW = size.width - padX * 2
                            val plotH = size.height - padY * 2

                            // Horizontal guide lines (25%, 50%, 75%, 100%)
                            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
                                val y = padY + plotH * (1f - frac)
                                drawLine(
                                    color = gridColor,
                                    start = Offset(padX, y),
                                    end = Offset(size.width - padX, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            val points = percentages.mapIndexed { idx, pct ->
                                val x = padX + (idx.toFloat() / (percentages.size - 1).toFloat()) * plotW
                                val y = padY + plotH * (1f - (pct.coerceIn(0f, 100f) / 100f))
                                Offset(x, y)
                            }

                            val linePath = Path().apply {
                                points.forEachIndexed { index, pt ->
                                    if (index == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                                }
                            }

                            val areaPath = Path().apply {
                                addPath(linePath)
                                lineTo(points.last().x, padY + plotH)
                                lineTo(points.first().x, padY + plotH)
                                close()
                            }

                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.35f),
                                        accentColor.copy(alpha = 0.02f)
                                    )
                                )
                            )

                            drawPath(
                                path = linePath,
                                color = accentColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            points.forEach { pt ->
                                drawCircle(
                                    color = Color.White,
                                    radius = 6.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = accentColor,
                                    radius = 4.dp.toPx(),
                                    center = pt
                                )
                            }
                        }
                    }
                }
            }
        }

        // Mock History Header
        item(key = "mock_history_header") {
            Text(
                text = "Mock Test Log (${mocks.size})",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (mocks.isEmpty()) {
            item(key = "empty_mocks") {
                OutlinedCard(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "No mock tests recorded yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tap 'Log Mock' to record your score, accuracy %, and test rank.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(mocks.reversed(), key = { it.id }) { mock ->
                OutlinedCard(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = mock.dateIso,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", mock.score)} / ${String.format(Locale.US, "%.0f", mock.maxScore)}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = accentColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", mock.percentage)}%",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            if (mock.accuracyPercent != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.0f", mock.accuracyPercent)}% acc",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (mock.rank != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = "AIR #${mock.rank}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteMockResult(activeExam.id, mock.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete Mock",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showAddMockDialog) {
        var dateStr by remember { mutableStateOf(CountdownCalculator.todayIso()) }
        var scoreStr by remember { mutableStateOf("") }
        var maxScoreStr by remember { mutableStateOf("100") }
        var accStr by remember { mutableStateOf("") }
        var rankStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddMockDialog = false },
            title = { Text("Log Mock Test Score") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = scoreStr,
                            onValueChange = { scoreStr = it },
                            label = { Text("Score *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxScoreStr,
                            onValueChange = { maxScoreStr = it },
                            label = { Text("Max Score") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = accStr,
                            onValueChange = { accStr = it },
                            label = { Text("Accuracy %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rankStr,
                            onValueChange = { rankStr = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Rank") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sc = scoreStr.toFloatOrNull()
                        if (sc != null) {
                            onAddMockResult(
                                activeExam.id,
                                dateStr,
                                sc,
                                maxScoreStr.toFloatOrNull() ?: 100f,
                                accStr.toFloatOrNull(),
                                rankStr.toIntOrNull()
                            )
                            showAddMockDialog = false
                        }
                    },
                    enabled = scoreStr.toFloatOrNull() != null
                ) {
                    Text("Save Mock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MockStatTile(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
