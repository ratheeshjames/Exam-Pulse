package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SyllabusTopic(
    val id: String = UUID.randomUUID().toString().take(8),
    val name: String,
    val isDone: Boolean = false,
    val doneAtMillis: Long = 0L,
    val isWeak: Boolean = false
)

data class SyllabusSubject(
    val id: String = UUID.randomUUID().toString().take(8),
    val name: String,
    val weightagePercent: Int = 0,
    val topics: List<SyllabusTopic> = emptyList()
)

data class ExamMilestone(
    val id: String = UUID.randomUUID().toString().take(8),
    val title: String,
    val dateTimeIso: String
)

data class MockTestResult(
    val id: String = UUID.randomUUID().toString().take(8),
    val dateIso: String,
    val score: Float,
    val maxScore: Float = 100f,
    val accuracyPercent: Float? = null,
    val rank: Int? = null
) {
    val percentage: Float
        get() = if (maxScore > 0f) (score / maxScore) * 100f else 0f
}

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString().take(8),
    val name: String,
    val dateTimeIso: String,
    val startMillis: Long = System.currentTimeMillis(),
    val emoji: String = "🎯",
    val colorHex: Long = 0xFF6366F1,
    val category: String = "Other",
    val venue: String = "",
    val officialLink: String = "",
    val attempt: Int = 1,
    val notes: String = "",
    val hoursPerTopic: Float = 4f,
    val subjectsJson: String = "[]",
    val milestonesJson: String = "[]",
    val mocksJson: String = "[]"
)

data class Exam(
    val id: String = UUID.randomUUID().toString().take(8),
    val name: String,
    val dateTimeIso: String,
    val startMillis: Long = System.currentTimeMillis(),
    val emoji: String = "🎯",
    val colorHex: Long = 0xFF6366F1,
    val category: String = "Other",
    val venue: String = "",
    val officialLink: String = "",
    val attempt: Int = 1,
    val notes: String = "",
    val hoursPerTopic: Float = 4f,
    val subjects: List<SyllabusSubject> = emptyList(),
    val milestones: List<ExamMilestone> = emptyList(),
    val mocks: List<MockTestResult> = emptyList()
) {
    val totalTopics: Int
        get() = subjects.sumOf { it.topics.size }

    val completedTopics: Int
        get() = subjects.sumOf { sub -> sub.topics.count { it.isDone } }

    val syllabusFraction: Float
        get() = if (totalTopics > 0) completedTopics.toFloat() / totalTopics.toFloat() else 0f
}

@Entity(tableName = "study_logs")
data class StudyLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString().take(8),
    val dateIso: String,
    val minutes: Int,
    val subjectName: String,
    val examId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_todos")
data class StudyTodoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString().take(8),
    val text: String,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_preferences")
data class AppPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val pinnedExamId: String? = null,
    val motivationWhy: String = "Today's consistent focus builds tomorrow's top rank.",
    val dailyGoalHours: Float = 3f,
    val weeklyGoalHours: Float = 18f,
    val themeMode: String = "auto", // "auto", "light", "dark"
    val accentColorHex: Long = 0xFF4F46E5,
    val useDynamicColor: Boolean = false,
    val use24HourFormat: Boolean = false,
    val weekStartsMonday: Boolean = true,
    val viewStyle: String = "expressive", // "expressive", "compact", "grid"
    val showSleepsInsteadOfDays: Boolean = false,
    val timeZoneOffset: String = "",
    val restDaysCsv: String = "0", // Sunday by default
    val pomodoroFocusMinutes: Int = 25,
    val pomodoroBreakMinutes: Int = 5,
    val autoArchivePastExams: Boolean = true,
    val hasSeenWelcome: Boolean = false
) {
    val restDays: Set<Int>
        get() = restDaysCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
}

object ExamJsonMapper {
    fun toEntity(exam: Exam): ExamEntity = ExamEntity(
        id = exam.id,
        name = exam.name,
        dateTimeIso = exam.dateTimeIso,
        startMillis = exam.startMillis,
        emoji = exam.emoji,
        colorHex = exam.colorHex,
        category = exam.category,
        venue = exam.venue,
        officialLink = exam.officialLink,
        attempt = exam.attempt,
        notes = exam.notes,
        hoursPerTopic = exam.hoursPerTopic,
        subjectsJson = encodeSubjects(exam.subjects),
        milestonesJson = encodeMilestones(exam.milestones),
        mocksJson = encodeMocks(exam.mocks)
    )

    fun toDomain(entity: ExamEntity): Exam = Exam(
        id = entity.id,
        name = entity.name,
        dateTimeIso = entity.dateTimeIso,
        startMillis = entity.startMillis,
        emoji = entity.emoji,
        colorHex = entity.colorHex,
        category = entity.category,
        venue = entity.venue,
        officialLink = entity.officialLink,
        attempt = entity.attempt,
        notes = entity.notes,
        hoursPerTopic = entity.hoursPerTopic,
        subjects = decodeSubjects(entity.subjectsJson),
        milestones = decodeMilestones(entity.milestonesJson),
        mocks = decodeMocks(entity.mocksJson)
    )

    fun encodeSubjects(subjects: List<SyllabusSubject>): String {
        val arr = JSONArray()
        for (sub in subjects) {
            val obj = JSONObject()
            obj.put("id", sub.id)
            obj.put("n", sub.name)
            obj.put("w", sub.weightagePercent)
            val tArr = JSONArray()
            for (t in sub.topics) {
                val tObj = JSONObject()
                tObj.put("id", t.id)
                tObj.put("n", t.name)
                tObj.put("dn", if (t.isDone) 1 else 0)
                tObj.put("dd", t.doneAtMillis)
                tObj.put("wk", t.isWeak)
                tArr.put(tObj)
            }
            obj.put("t", tArr)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun decodeSubjects(json: String): List<SyllabusSubject> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val tArr = obj.optJSONArray("t") ?: JSONArray()
                    val topics = buildList {
                        for (j in 0 until tArr.length()) {
                            val tObj = tArr.getJSONObject(j)
                            add(
                                SyllabusTopic(
                                    id = tObj.optString("id").ifBlank { UUID.randomUUID().toString().take(8) },
                                    name = tObj.optString("n"),
                                    isDone = tObj.optInt("dn", 0) == 1 || tObj.optBoolean("dn", false),
                                    doneAtMillis = tObj.optLong("dd", 0L),
                                    isWeak = tObj.optBoolean("wk", false)
                                )
                            )
                        }
                    }
                    add(
                        SyllabusSubject(
                            id = obj.optString("id").ifBlank { UUID.randomUUID().toString().take(8) },
                            name = obj.optString("n"),
                            weightagePercent = obj.optInt("w", 0),
                            topics = topics
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeMilestones(milestones: List<ExamMilestone>): String {
        val arr = JSONArray()
        for (m in milestones) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("n", m.title)
            obj.put("d", m.dateTimeIso)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun decodeMilestones(json: String): List<ExamMilestone> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        ExamMilestone(
                            id = obj.optString("id").ifBlank { UUID.randomUUID().toString().take(8) },
                            title = obj.optString("n"),
                            dateTimeIso = obj.optString("d")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeMocks(mocks: List<MockTestResult>): String {
        val arr = JSONArray()
        for (m in mocks) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("d", m.dateIso)
            obj.put("score", m.score.toDouble())
            obj.put("max", m.maxScore.toDouble())
            if (m.accuracyPercent != null) obj.put("acc", m.accuracyPercent.toDouble())
            if (m.rank != null) obj.put("rank", m.rank)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun decodeMocks(json: String): List<MockTestResult> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val accStr = obj.optString("acc", "")
                    val rankStr = obj.optString("rank", "")
                    add(
                        MockTestResult(
                            id = obj.optString("id").ifBlank { UUID.randomUUID().toString().take(8) },
                            dateIso = obj.optString("d"),
                            score = obj.optDouble("score", 0.0).toFloat(),
                            maxScore = obj.optDouble("max", 100.0).toFloat(),
                            accuracyPercent = accStr.toFloatOrNull(),
                            rank = rankStr.toIntOrNull()
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
