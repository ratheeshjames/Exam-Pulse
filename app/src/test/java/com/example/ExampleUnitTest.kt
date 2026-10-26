package com.example

import com.example.data.model.Exam
import com.example.data.model.StudyLogEntity
import com.example.data.model.SyllabusSubject
import com.example.data.model.SyllabusTopic
import com.example.data.presets.ExamPresetsData
import com.example.domain.CountdownCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun presetSyllabusTemplate_parsesCorrectly() {
        val csSubjects = ExamPresetsData.parseTemplate("CS")
        assertTrue(csSubjects.isNotEmpty())
        assertEquals("Engineering Mathematics", csSubjects.first().name)
        assertTrue(csSubjects.first().topics.any { it.name == "Linear Algebra" })
    }

    @Test
    fun spacedRepetition_detects1_3_7_21_dayTopics() {
        val now = 1_760_000_000_000L
        val dayMs = 86_400_000L
        val exam = Exam(
            name = "GATE CS",
            dateTimeIso = "2027-02-07T09:00",
            subjects = listOf(
                SyllabusSubject(
                    name = "Algorithms",
                    topics = listOf(
                        SyllabusTopic(name = "Sorting", isDone = true, doneAtMillis = now - dayMs * 1),
                        SyllabusTopic(name = "DP", isDone = true, doneAtMillis = now - dayMs * 7),
                        SyllabusTopic(name = "Greedy", isDone = true, doneAtMillis = now - dayMs * 4)
                    )
                )
            )
        )
        val due = CountdownCalculator.getRevisionsDueToday(exam, now)
        assertEquals(2, due.size)
        assertTrue(due.any { it.topicName == "Sorting" && it.intervalDay == 1 })
        assertTrue(due.any { it.topicName == "DP" && it.intervalDay == 7 })
    }

    @Test
    fun streakCalculation_countsConsecutiveDays() {
        val now = 1_760_000_000_000L
        val dayMs = 86_400_000L
        val logs = listOf(
            StudyLogEntity(dateIso = CountdownCalculator.todayIso(now), minutes = 60, subjectName = "OS"),
            StudyLogEntity(dateIso = CountdownCalculator.todayIso(now - dayMs), minutes = 45, subjectName = "CN"),
            StudyLogEntity(dateIso = CountdownCalculator.todayIso(now - dayMs * 2), minutes = 90, subjectName = "DBMS")
        )
        val streak = CountdownCalculator.calculateStreakDays(logs, now)
        assertEquals(3, streak)
    }
}
