package com.example.data.local

import com.example.data.model.AppPreferencesEntity
import com.example.data.model.Exam
import com.example.data.model.ExamJsonMapper
import com.example.data.model.StudyLogEntity
import com.example.data.model.StudyTodoEntity
import com.example.data.presets.ExamPresetsData
import com.example.domain.CountdownCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class ExamPulseRepository(private val dao: ExamPulseDao) {

    val examsFlow: Flow<List<Exam>> = dao.observeExams().map { entities ->
        entities.map { ExamJsonMapper.toDomain(it) }
    }

    val studyLogsFlow: Flow<List<StudyLogEntity>> = dao.observeStudyLogs()

    val studyTodosFlow: Flow<List<StudyTodoEntity>> = dao.observeStudyTodos()

    val preferencesFlow: Flow<AppPreferencesEntity> = dao.observePreferences().map {
        it ?: AppPreferencesEntity()
    }

    suspend fun ensureSeededIfFirstLaunch() {
        val currentPrefs = dao.getPreferencesOnce()
        if (currentPrefs == null && dao.getExamCount() == 0) {
            val (seedExams, seedLogs, seedTodos) = ExamPresetsData.createStarterSeedData()
            dao.upsertExams(seedExams.map { ExamJsonMapper.toEntity(it) })
            dao.insertStudyLogs(seedLogs)
            dao.upsertStudyTodos(seedTodos)
            dao.upsertPreferences(
                AppPreferencesEntity(
                    id = 1,
                    pinnedExamId = seedExams.firstOrNull()?.id
                )
            )
        }
    }

    suspend fun upsertExam(exam: Exam) {
        dao.upsertExam(ExamJsonMapper.toEntity(exam))
    }

    suspend fun deleteExam(examId: String) {
        dao.deleteExamById(examId)
        val prefs = dao.getPreferencesOnce()
        if (prefs?.pinnedExamId == examId) {
            dao.upsertPreferences(prefs.copy(pinnedExamId = null))
        }
    }

    suspend fun logStudySession(dateIso: String, minutes: Int, subjectName: String, examId: String? = null) {
        if (minutes <= 0) return
        dao.insertStudyLog(
            StudyLogEntity(
                id = UUID.randomUUID().toString().take(8),
                dateIso = dateIso,
                minutes = minutes,
                subjectName = subjectName.ifBlank { "General" },
                examId = examId
            )
        )
    }

    suspend fun deleteStudyLog(logId: String) {
        dao.deleteStudyLogById(logId)
    }

    suspend fun addTodo(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        dao.upsertStudyTodo(
            StudyTodoEntity(
                id = UUID.randomUUID().toString().take(8),
                text = trimmed,
                isDone = false
            )
        )
    }

    suspend fun toggleTodo(todo: StudyTodoEntity) {
        dao.upsertStudyTodo(todo.copy(isDone = !todo.isDone))
    }

    suspend fun deleteTodo(todoId: String) {
        dao.deleteStudyTodoById(todoId)
    }

    suspend fun updatePreferences(transform: (AppPreferencesEntity) -> AppPreferencesEntity) {
        val current = dao.getPreferencesOnce() ?: AppPreferencesEntity()
        dao.upsertPreferences(transform(current))
    }

    suspend fun resetAllData() {
        dao.deleteAllExams()
        dao.deleteAllStudyLogs()
        dao.deleteAllStudyTodos()
        dao.deleteAllPreferences()
        dao.upsertPreferences(AppPreferencesEntity(id = 1, hasSeenWelcome = true))
    }

    fun exportBackupJson(
        exams: List<Exam>,
        logs: List<StudyLogEntity>,
        todos: List<StudyTodoEntity>,
        prefs: AppPreferencesEntity
    ): String {
        val root = JSONObject()
        root.put("pin", prefs.pinnedExamId ?: JSONObject.NULL)
        root.put("why", prefs.motivationWhy)
        root.put("gd", prefs.dailyGoalHours.toDouble())
        root.put("gw", prefs.weeklyGoalHours.toDouble())
        root.put("seen", if (prefs.hasSeenWelcome) 1 else 0)

        // Settings compatible with original HTML backup format + extended native fields
        val sObj = JSONObject()
        sObj.put("th", prefs.themeMode)
        sObj.put("ac", String.format("#%06X", (0xFFFFFF and prefs.accentColorHex.toInt())))
        sObj.put("h24", if (prefs.use24HourFormat) 1 else 0)
        sObj.put("wk", if (prefs.weekStartsMonday) 1 else 0)
        sObj.put("v", prefs.viewStyle)
        sObj.put("sl", if (prefs.showSleepsInsteadOfDays) 1 else 0)
        sObj.put("tz", prefs.timeZoneOffset)
        val restArr = JSONArray()
        prefs.restDays.forEach { restArr.put(it) }
        sObj.put("rest", restArr)
        sObj.put("pw", prefs.pomodoroFocusMinutes)
        sObj.put("pb", prefs.pomodoroBreakMinutes)
        sObj.put("arch", if (prefs.autoArchivePastExams) 1 else 0)
        root.put("s", sObj)

        val exArr = JSONArray()
        for (e in exams) {
            val eObj = JSONObject()
            eObj.put("id", e.id)
            eObj.put("name", e.name)
            eObj.put("date", e.dateTimeIso)
            eObj.put("start", e.startMillis)
            eObj.put("emoji", e.emoji)
            eObj.put("color", String.format("#%06X", (0xFFFFFF and e.colorHex.toInt())))
            eObj.put("cat", e.category)
            eObj.put("venue", e.venue)
            eObj.put("link", e.officialLink)
            eObj.put("attempt", e.attempt)
            eObj.put("notes", e.notes)
            eObj.put("hpt", e.hoursPerTopic.toDouble())
            eObj.put("sub", JSONArray(ExamJsonMapper.encodeSubjects(e.subjects)))
            eObj.put("ms", JSONArray(ExamJsonMapper.encodeMilestones(e.milestones)))
            eObj.put("mocks", JSONArray(ExamJsonMapper.encodeMocks(e.mocks)))
            exArr.put(eObj)
        }
        root.put("ex", exArr)

        val logArr = JSONArray()
        for (l in logs) {
            val lObj = JSONObject()
            lObj.put("d", l.dateIso)
            lObj.put("m", l.minutes)
            lObj.put("s", l.subjectName)
            logArr.put(lObj)
        }
        root.put("log", logArr)

        val tdArr = JSONArray()
        for (td in todos) {
            val tdObj = JSONObject()
            tdObj.put("t", td.text)
            tdObj.put("d", if (td.isDone) 1 else 0)
            tdArr.put(tdObj)
        }
        root.put("td", tdArr)

        return root.toString(2)
    }

    suspend fun importBackupJson(jsonText: String): Boolean {
        return try {
            val root = JSONObject(jsonText)
            val exArr = root.optJSONArray("ex") ?: JSONArray()
            val importedExams = buildList {
                for (i in 0 until exArr.length()) {
                    val obj = exArr.getJSONObject(i)
                    val colorStr = obj.optString("color", "#4F46E5").removePrefix("#")
                    val colorLong = colorStr.toLongOrNull(16)?.let { 0xFF000000L or it } ?: 0xFF4F46E5L
                    val subArr = obj.optJSONArray("sub")?.toString() ?: "[]"
                    val msArr = obj.optJSONArray("ms")?.toString() ?: "[]"
                    val mocksArr = obj.optJSONArray("mocks")?.toString() ?: "[]"
                    add(
                        Exam(
                            id = obj.optString("id").ifBlank { UUID.randomUUID().toString().take(8) },
                            name = obj.optString("name", "Exam"),
                            dateTimeIso = obj.optString("date", "2027-02-07T09:00"),
                            startMillis = obj.optLong("start", System.currentTimeMillis()),
                            emoji = obj.optString("emoji", "🎯"),
                            colorHex = colorLong,
                            category = obj.optString("cat", "Other"),
                            venue = obj.optString("venue", ""),
                            officialLink = obj.optString("link", ""),
                            attempt = obj.optInt("attempt", 1),
                            notes = obj.optString("notes", ""),
                            hoursPerTopic = obj.optDouble("hpt", 4.0).toFloat(),
                            subjects = ExamJsonMapper.decodeSubjects(subArr),
                            milestones = ExamJsonMapper.decodeMilestones(msArr),
                            mocks = ExamJsonMapper.decodeMocks(mocksArr)
                        )
                    )
                }
            }

            val logArr = root.optJSONArray("log") ?: JSONArray()
            val importedLogs = buildList {
                for (i in 0 until logArr.length()) {
                    val obj = logArr.getJSONObject(i)
                    add(
                        StudyLogEntity(
                            id = UUID.randomUUID().toString().take(8),
                            dateIso = obj.optString("d"),
                            minutes = obj.optInt("m", 0),
                            subjectName = obj.optString("s", "General")
                        )
                    )
                }
            }

            val tdArr = root.optJSONArray("td") ?: JSONArray()
            val importedTodos = buildList {
                for (i in 0 until tdArr.length()) {
                    val obj = tdArr.getJSONObject(i)
                    add(
                        StudyTodoEntity(
                            id = UUID.randomUUID().toString().take(8),
                            text = obj.optString("t"),
                            isDone = obj.optInt("d", 0) == 1 || obj.optBoolean("d", false)
                        )
                    )
                }
            }

            val sObj = root.optJSONObject("s")
            val currentPrefs = dao.getPreferencesOnce() ?: AppPreferencesEntity()
            val restCsv = sObj?.optJSONArray("rest")?.let { arr ->
                (0 until arr.length()).map { arr.optInt(it) }.joinToString(",")
            } ?: currentPrefs.restDaysCsv

            val acHex = sObj?.optString("ac", "#4F46E5")?.removePrefix("#")?.toLongOrNull(16)
                ?.let { 0xFF000000L or it } ?: currentPrefs.accentColorHex

            val updatedPrefs = currentPrefs.copy(
                pinnedExamId = root.optString("pin").takeIf { it.isNotBlank() && it != "null" },
                motivationWhy = root.optString("why", currentPrefs.motivationWhy),
                dailyGoalHours = root.optDouble("gd", currentPrefs.dailyGoalHours.toDouble()).toFloat(),
                weeklyGoalHours = root.optDouble("gw", currentPrefs.weeklyGoalHours.toDouble()).toFloat(),
                themeMode = sObj?.optString("th", currentPrefs.themeMode) ?: currentPrefs.themeMode,
                accentColorHex = acHex,
                use24HourFormat = (sObj?.optInt("h24", 0) ?: 0) == 1,
                weekStartsMonday = (sObj?.optInt("wk", 1) ?: 1) == 1,
                showSleepsInsteadOfDays = (sObj?.optInt("sl", 0) ?: 0) == 1,
                timeZoneOffset = sObj?.optString("tz", "") ?: "",
                restDaysCsv = restCsv,
                pomodoroFocusMinutes = sObj?.optInt("pw", 25) ?: 25,
                pomodoroBreakMinutes = sObj?.optInt("pb", 5) ?: 5,
                autoArchivePastExams = (sObj?.optInt("arch", 1) ?: 1) == 1
            )

            dao.deleteAllExams()
            dao.deleteAllStudyLogs()
            dao.deleteAllStudyTodos()
            dao.upsertExams(importedExams.map { ExamJsonMapper.toEntity(it) })
            dao.insertStudyLogs(importedLogs)
            dao.upsertStudyTodos(importedTodos)
            dao.upsertPreferences(updatedPrefs)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun generateIcsCalendar(exams: List<Exam>, timeZoneOffset: String): String {
        val utcFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val stamp = utcFormat.format(Date())
        val events = exams.joinToString("\n") { exam ->
            val startMs = CountdownCalculator.parseTargetMillis(exam.dateTimeIso, timeZoneOffset)
            val endMs = startMs + 3 * 3600_000L
            buildString {
                appendLine("BEGIN:VEVENT")
                appendLine("UID:${exam.id}@exampulse")
                appendLine("DTSTAMP:$stamp")
                appendLine("DTSTART:${utcFormat.format(Date(startMs))}")
                appendLine("DTEND:${utcFormat.format(Date(endMs))}")
                appendLine("SUMMARY:${exam.emoji} ${exam.name}")
                if (exam.venue.isNotBlank()) appendLine("LOCATION:${exam.venue}")
                if (exam.notes.isNotBlank()) appendLine("DESCRIPTION:${exam.notes.replace("\n", " ")}")
                append("END:VEVENT")
            }
        }
        return "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//ExamPulse//EN\n$events\nEND:VCALENDAR"
    }
}
