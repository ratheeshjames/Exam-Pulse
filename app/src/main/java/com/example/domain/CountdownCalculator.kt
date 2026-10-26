package com.example.domain

import com.example.data.model.Exam
import com.example.data.model.ExamMilestone
import com.example.data.model.StudyLogEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class LiveCountdown(
    val totalMillis: Long,
    val daysCeil: Int,
    val wholeDays: Long,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val isExamDay: Boolean,
    val isCompleted: Boolean,
    val isArchived: Boolean,
    val elapsedProgress: Float,
    val workingDaysLeft: Int,
    val weekendDaysLeft: Int,
    val formattedTargetDate: String,
    val formattedTargetTime: String,
    val nextMilestone: ExamMilestone? = null,
    val nextMilestoneDays: Int? = null
)

data class RevisionItem(
    val subjectName: String,
    val topicId: String,
    val topicName: String,
    val intervalDay: Int // 1, 3, 7, or 21
)

data class WeakTopicItem(
    val subjectId: String,
    val subjectName: String,
    val topicId: String,
    val topicName: String
)

data class HeatmapDay(
    val dateIso: String,
    val minutes: Int,
    val intensityFraction: Float
)

data class EarnedBadge(
    val emoji: String,
    val title: String,
    val subtitle: String
)

object CountdownCalculator {
    private const val DAY_MS = 86_400_000L

    fun todayIso(nowMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(nowMillis))
    }

    fun parseTargetMillis(dateTimeIso: String, timeZoneOffset: String = ""): Long {
        val clean = dateTimeIso.trim()
        return try {
            val normalized = if (clean.length == 10) "${clean}T09:00" else clean.take(16)
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
            if (timeZoneOffset.isNotBlank()) {
                sdf.timeZone = TimeZone.getTimeZone("GMT$timeZoneOffset")
            }
            sdf.parse(normalized)?.time ?: (System.currentTimeMillis() + DAY_MS * 30)
        } catch (_: Exception) {
            System.currentTimeMillis() + DAY_MS * 30
        }
    }

    fun computeLiveCountdown(
        exam: Exam,
        nowMillis: Long,
        timeZoneOffset: String,
        use24Hour: Boolean,
        restDays: Set<Int>,
        autoArchive: Boolean
    ): LiveCountdown {
        val targetMillis = parseTargetMillis(exam.dateTimeIso, timeZoneOffset)
        val diff = targetMillis - nowMillis
        val isExamDay = diff <= 0L && diff > -DAY_MS
        val isCompleted = diff <= -DAY_MS
        val isArchived = autoArchive && diff < -DAY_MS * 2

        val totalSecs = max(0L, diff / 1000L)
        val wholeDays = totalSecs / 86400L
        val remSecs = (totalSecs % 86400L).toInt()
        val hours = remSecs / 3600
        val minutes = (remSecs % 3600) / 60
        val seconds = remSecs % 60
        val daysCeil = ceil(diff.toDouble() / DAY_MS.toDouble()).toInt()

        val totalSpan = max(1L, targetMillis - exam.startMillis)
        val elapsed = (nowMillis - exam.startMillis).toFloat() / totalSpan.toFloat()
        val elapsedProgress = min(1f, max(0f, elapsed))

        val (workingDays, weekendDays) = countWorkingAndWeekendDays(nowMillis, targetMillis, restDays)

        val dateFormat = SimpleDateFormat("EEE, d MMM yyyy", Locale.US)
        val timePattern = if (use24Hour) "HH:mm" else "hh:mm a"
        val timeFormat = SimpleDateFormat(timePattern, Locale.US)
        val targetDateObj = Date(targetMillis)

        val upcomingMilestone = exam.milestones
            .map { ms -> ms to parseTargetMillis(ms.dateTimeIso, timeZoneOffset) }
            .filter { it.second > nowMillis }
            .minByOrNull { it.second }

        val nextMilestoneDays = upcomingMilestone?.let {
            ceil((it.second - nowMillis).toDouble() / DAY_MS.toDouble()).toInt()
        }

        return LiveCountdown(
            totalMillis = diff,
            daysCeil = daysCeil,
            wholeDays = wholeDays,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            isExamDay = isExamDay,
            isCompleted = isCompleted,
            isArchived = isArchived,
            elapsedProgress = elapsedProgress,
            workingDaysLeft = workingDays,
            weekendDaysLeft = weekendDays,
            formattedTargetDate = dateFormat.format(targetDateObj),
            formattedTargetTime = timeFormat.format(targetDateObj),
            nextMilestone = upcomingMilestone?.first,
            nextMilestoneDays = nextMilestoneDays
        )
    }

    fun countWorkingAndWeekendDays(
        nowMillis: Long,
        targetMillis: Long,
        restDays: Set<Int>
    ): Pair<Int, Int> {
        if (targetMillis <= nowMillis) return 0 to 0
        var working = 0
        var weekend = 0
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        var steps = 0
        while (cal.timeInMillis < targetMillis && steps < 3700) {
            // Calendar.SUNDAY = 1 .. Calendar.SATURDAY = 7 -> convert to 0..6 (0=Sun..6=Sat)
            val dayOfWeek0To6 = cal.get(Calendar.DAY_OF_WEEK) - 1
            val isWeekend = (dayOfWeek0To6 == 0 || dayOfWeek0To6 == 6)
            if (isWeekend) {
                weekend++
            }
            if (!isWeekend && !restDays.contains(dayOfWeek0To6)) {
                working++
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
            steps++
        }
        return working to weekend
    }

    fun getRevisionsDueToday(exam: Exam, nowMillis: Long = System.currentTimeMillis()): List<RevisionItem> {
        val intervals = setOf(1, 3, 7, 21)
        val result = mutableListOf<RevisionItem>()
        for (subject in exam.subjects) {
            for (topic in subject.topics) {
                if (topic.isDone && topic.doneAtMillis > 0L) {
                    val daysAgo = ((nowMillis - topic.doneAtMillis) / DAY_MS).toInt()
                    if (daysAgo in intervals) {
                        result.add(
                            RevisionItem(
                                subjectName = subject.name,
                                topicId = topic.id,
                                topicName = topic.name,
                                intervalDay = daysAgo
                            )
                        )
                    }
                }
            }
        }
        return result
    }

    fun getWeakTopics(exam: Exam): List<WeakTopicItem> {
        val result = mutableListOf<WeakTopicItem>()
        for (subject in exam.subjects) {
            for (topic in subject.topics) {
                if (topic.isWeak) {
                    result.add(
                        WeakTopicItem(
                            subjectId = subject.id,
                            subjectName = subject.name,
                            topicId = topic.id,
                            topicName = topic.name
                        )
                    )
                }
            }
        }
        return result
    }

    fun calculateStreakDays(logs: List<StudyLogEntity>, nowMillis: Long = System.currentTimeMillis()): Int {
        val minutesByDate = logs.groupBy { it.dateIso }.mapValues { entry -> entry.value.sumOf { it.minutes } }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        var checkMillis = nowMillis
        val todayKey = sdf.format(Date(checkMillis))
        if ((minutesByDate[todayKey] ?: 0) <= 0) {
            checkMillis -= DAY_MS
        }
        var streak = 0
        while (streak < 3650) {
            val key = sdf.format(Date(checkMillis))
            if ((minutesByDate[key] ?: 0) > 0) {
                streak++
                checkMillis -= DAY_MS
            } else {
                break
            }
        }
        return streak
    }

    fun build84DayHeatmap(logs: List<StudyLogEntity>, nowMillis: Long = System.currentTimeMillis()): List<HeatmapDay> {
        val minutesByDate = logs.groupBy { it.dateIso }.mapValues { entry -> entry.value.sumOf { it.minutes } }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val rawDays = (0 until 84).map { idx ->
            val dayMs = nowMillis - (83 - idx) * DAY_MS
            val iso = sdf.format(Date(dayMs))
            iso to (minutesByDate[iso] ?: 0)
        }
        val maxMins = max(60, rawDays.maxOfOrNull { it.second } ?: 60)
        return rawDays.map { (iso, mins) ->
            HeatmapDay(
                dateIso = iso,
                minutes = mins,
                intensityFraction = min(1f, mins.toFloat() / maxMins.toFloat())
            )
        }
    }

    fun buildLast7DaysMinutes(logs: List<StudyLogEntity>, nowMillis: Long = System.currentTimeMillis()): List<Pair<String, Int>> {
        val minutesByDate = logs.groupBy { it.dateIso }.mapValues { entry -> entry.value.sumOf { it.minutes } }
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFormat = SimpleDateFormat("EEE", Locale.US)
        return (0 until 7).map { idx ->
            val date = Date(nowMillis - (6 - idx) * DAY_MS)
            val iso = isoFormat.format(date)
            val label = labelFormat.format(date)
            label to (minutesByDate[iso] ?: 0)
        }
    }

    fun computeEarnedBadges(
        exams: List<Exam>,
        logs: List<StudyLogEntity>,
        nowMillis: Long,
        timeZoneOffset: String
    ): List<EarnedBadge> {
        val badges = mutableListOf<EarnedBadge>()
        val minDaysLeft = exams
            .map { ceil((parseTargetMillis(it.dateTimeIso, timeZoneOffset) - nowMillis).toDouble() / DAY_MS.toDouble()).toInt() }
            .filter { it >= 0 }
            .minOrNull() ?: Int.MAX_VALUE

        listOf(100, 50, 30, 7).forEach { threshold ->
            if (minDaysLeft <= threshold) {
                badges.add(EarnedBadge("⏳", "$threshold Days Window", "Entered final $threshold-day sprint"))
            }
        }

        val currentStreak = calculateStreakDays(logs, nowMillis)
        listOf(3, 7, 30).forEach { threshold ->
            if (currentStreak >= threshold) {
                badges.add(EarnedBadge("🔥", "$threshold-Day Streak", "Consistent daily study momentum"))
            }
        }

        val totalHours = logs.sumOf { it.minutes } / 60f
        listOf(10, 50, 100).forEach { threshold ->
            if (totalHours >= threshold) {
                badges.add(EarnedBadge("📚", "${threshold}h Studied", "Logged over $threshold hours of deep work"))
            }
        }

        if (exams.any { it.syllabusFraction >= 0.25f }) {
            badges.add(EarnedBadge("🎯", "Syllabus Pacer", "25%+ syllabus topics mastered"))
        }
        if (exams.any { it.mocks.size >= 3 }) {
            badges.add(EarnedBadge("📈", "Mock Contender", "Completed 3+ full mock tests"))
        }
        return badges
    }
}
