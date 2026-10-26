package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Exam
import com.example.domain.LiveCountdown
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.UrgencyCritical
import com.example.ui.theme.UrgencySafe
import com.example.ui.theme.UrgencyWarning
import java.util.Locale

fun urgencyColorForDays(daysCeil: Int): Color = when {
    daysCeil <= 7 -> UrgencyCritical
    daysCeil <= 30 -> UrgencyWarning
    else -> UrgencySafe
}

@Composable
fun UrgencyProgressRing(
    progress: Float,
    daysCeil: Int,
    size: Dp = 64.dp,
    strokeWidth: Dp = 6.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(),
        label = "ring_progress"
    )
    val ringColor = urgencyColorForDays(daysCeil)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroExamSpotlightCard(
    exam: Exam,
    countdown: LiveCountdown,
    isPinned: Boolean,
    showSleeps: Boolean,
    onPinToggle: () -> Unit,
    onOpenFocus: () -> Unit,
    onShareCountdown: () -> Unit,
    onOpenPlanner: () -> Unit,
    onEditExam: () -> Unit,
    onDeleteExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(exam.colorHex)
    val uriHandler = LocalUriHandler.current

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_exam_card"),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.surfaceContainer,
                            accentColor.copy(alpha = 0.06f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = accentColor.copy(alpha = 0.45f),
                    shape = MaterialTheme.shapes.extraLarge
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = accentColor.copy(alpha = 0.18f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = exam.emoji, fontSize = 28.sp)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = accentColor,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = if (isPinned) "PINNED SPOTLIGHT" else "NEXT UPCOMING",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = exam.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            if (exam.attempt > 1) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "Attempt #${exam.attempt}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = exam.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${countdown.formattedTargetDate} · ${countdown.formattedTargetTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    UrgencyProgressRing(
                        progress = countdown.elapsedProgress,
                        daysCeil = countdown.daysCeil,
                        size = 64.dp
                    )
                }

                // Live Tabular Countdown Tiles
                if (countdown.isExamDay) {
                    Surface(
                        color = UrgencySafe.copy(alpha = 0.16f),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎯 Exam Day is Here! Trust your preparation.",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = UrgencySafe
                            )
                        }
                    }
                } else if (countdown.isCompleted) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✅ Exam Completed",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CountdownTile(
                            value = countdown.wholeDays.toString(),
                            label = if (showSleeps) "SLEEPS" else "DAYS",
                            accentColor = accentColor,
                            isPrimary = true,
                            modifier = Modifier.weight(1.3f)
                        )
                        CountdownTile(
                            value = String.format(Locale.US, "%02d", countdown.hours),
                            label = "HRS",
                            accentColor = accentColor,
                            isPrimary = false,
                            modifier = Modifier.weight(1f)
                        )
                        CountdownTile(
                            value = String.format(Locale.US, "%02d", countdown.minutes),
                            label = "MIN",
                            accentColor = accentColor,
                            isPrimary = false,
                            modifier = Modifier.weight(1f)
                        )
                        CountdownTile(
                            value = String.format(Locale.US, "%02d", countdown.seconds),
                            label = "SEC",
                            accentColor = accentColor,
                            isPrimary = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Breakdown Metrics Pills
                if (countdown.totalMillis > 0) {
                    val weeks = String.format(Locale.US, "%.1f", countdown.daysCeil / 7.0)
                    val months = String.format(Locale.US, "%.1f", countdown.daysCeil / 30.4)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetricPill("≈ $weeks wks · $months mos")
                        MetricPill("${countdown.workingDaysLeft} study days")
                        MetricPill("${countdown.weekendDaysLeft} weekend days")
                        if (countdown.nextMilestone != null && countdown.nextMilestoneDays != null) {
                            MetricPill(
                                text = "◆ ${countdown.nextMilestone.title} in ${countdown.nextMilestoneDays}d",
                                highlight = true
                            )
                        }
                    }
                }

                // Syllabus Progress Bar
                if (exam.totalTopics > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Syllabus Mastery",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${exam.completedTopics}/${exam.totalTopics} topics (${(exam.syllabusFraction * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = accentColor
                            )
                        }
                        LinearProgressIndicator(
                            progress = { exam.syllabusFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = accentColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                // Venue, Official Link & Notes
                if (exam.venue.isNotBlank() || exam.officialLink.isNotBlank() || exam.notes.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (exam.venue.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = "Venue",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = exam.venue,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (exam.officialLink.startsWith("http")) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        try {
                                            uriHandler.openUri(exam.officialLink)
                                        } catch (_: Exception) {
                                        }
                                    }
                                    .padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = "Official Portal",
                                    tint = accentColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Official Exam Portal",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accentColor
                                )
                            }
                        }
                        if (exam.notes.isNotBlank()) {
                            Text(
                                text = exam.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Quick Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(
                            onClick = onOpenFocus,
                            label = { Text("Focus") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.CenterFocusStrong,
                                    contentDescription = "Focus Mode",
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("hero_focus_button")
                        )
                        AssistChip(
                            onClick = onOpenPlanner,
                            label = { Text("Syllabus") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                    contentDescription = "Open Syllabus Planner",
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("hero_plan_button")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(onClick = onPinToggle, modifier = Modifier.testTag("hero_pin_button")) {
                            Icon(
                                imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (isPinned) "Unpin Exam" else "Pin Exam",
                                tint = if (isPinned) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onShareCountdown, modifier = Modifier.testTag("hero_share_button")) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share Countdown",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onEditExam, modifier = Modifier.testTag("hero_edit_button")) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit Exam",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDeleteExam, modifier = Modifier.testTag("hero_delete_button")) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete Exam",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownTile(
    value: String,
    label: String,
    accentColor: Color,
    isPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (isPrimary) accentColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = if (isPrimary) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isPrimary) 28.sp else 22.sp
                ),
                color = if (isPrimary) accentColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricPill(
    text: String,
    highlight: Boolean = false
) {
    Surface(
        shape = CircleShape,
        color = if (highlight) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
        }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ExamCardItem(
    exam: Exam,
    countdown: LiveCountdown,
    isPinned: Boolean,
    viewStyle: String,
    showSleeps: Boolean,
    onPinToggle: () -> Unit,
    onOpenFocus: () -> Unit,
    onShareCountdown: () -> Unit,
    onOpenPlanner: () -> Unit,
    onEditExam: () -> Unit,
    onDeleteExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(exam.colorHex)

    if (viewStyle == "compact") {
        OutlinedCard(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onOpenPlanner() }
                .testTag("exam_card_${exam.id}"),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(text = exam.emoji, fontSize = 22.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exam.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${countdown.formattedTargetDate} · ${exam.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (countdown.totalMillis <= 0) "Done" else "${countdown.wholeDays}d",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = accentColor
                    )
                    if (countdown.totalMillis > 0) {
                        Text(
                            text = String.format(
                                Locale.US,
                                "%02d:%02d:%02d",
                                countdown.hours,
                                countdown.minutes,
                                countdown.seconds
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onEditExam) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit ${exam.name}",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        return
    }

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("exam_card_${exam.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = exam.emoji, fontSize = 22.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = exam.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (exam.attempt > 1) {
                            MetricPill("#${exam.attempt}")
                        }
                    }
                    Text(
                        text = "${countdown.formattedTargetDate} · ${exam.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                UrgencyProgressRing(
                    progress = countdown.elapsedProgress,
                    daysCeil = countdown.daysCeil,
                    size = 48.dp,
                    strokeWidth = 5.dp
                )
            }

            // Countdown Strip
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (countdown.isExamDay) {
                        Text(
                            text = "🎯 Exam Day!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = UrgencySafe
                        )
                    } else if (countdown.isCompleted) {
                        Text(
                            text = "✅ Completed",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${countdown.wholeDays}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = accentColor
                            )
                            Text(
                                text = if (showSleeps) "sleeps" else "days",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = String.format(
                                Locale.US,
                                "%02d:%02d:%02d",
                                countdown.hours,
                                countdown.minutes,
                                countdown.seconds
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (exam.totalTopics > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Syllabus ${exam.completedTopics}/${exam.totalTopics}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(exam.syllabusFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = accentColor
                        )
                    }
                    LinearProgressIndicator(
                        progress = { exam.syllabusFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = accentColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (countdown.totalMillis > 0) {
                        "${countdown.workingDaysLeft} study days left"
                    } else {
                        exam.venue.ifBlank { "Past exam" }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onPinToggle, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin ${exam.name}",
                            tint = if (isPinned) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onOpenFocus, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.CenterFocusStrong,
                            contentDescription = "Focus ${exam.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onOpenPlanner, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = "Plan ${exam.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onShareCountdown, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share ${exam.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onEditExam, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit ${exam.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDeleteExam, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete ${exam.name}",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
