package com.example.data.presets

import com.example.data.model.Exam
import com.example.data.model.ExamMilestone
import com.example.data.model.MockTestResult
import com.example.data.model.StudyLogEntity
import com.example.data.model.StudyTodoEntity
import com.example.data.model.SyllabusSubject
import com.example.data.model.SyllabusTopic
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

data class ExamPreset(
    val name: String,
    val emoji: String,
    val category: String,
    val monthDay: String, // MM-DD
    val colorHex: Long,
    val syllabusTemplateKey: String? = null
)

object ExamPresetsData {
    val CATEGORIES = listOf("ESE", "GATE", "JEE", "NEET", "UPSC", "SSC", "Bank", "MBA", "Other")

    val COLOR_PALETTE = listOf(
        0xFF4F46E5L, // Indigo
        0xFF0EA5E9L, // Sky
        0xFF10B981L, // Emerald
        0xFFF59E0BL, // Amber
        0xFFF43F5EL, // Rose
        0xFF8B5CF6L, // Violet
        0xFFEC4899L, // Pink
        0xFF14B8A6L  // Teal
    )

    val EMOJI_OPTIONS = listOf(
        "🎯", "💻", "📡", "⚡", "⚙️", "🏗️", "🛠️", "📝", "🧪", "🚀",
        "🩺", "🏛️", "📋", "📈", "⚖️", "🎖️", "🏦", "🚆", "🎓", "📚"
    )

    val QUOTES = listOf(
        "Small steps every day beat big bursts once a month.",
        "Discipline is choosing what you want most over what you want now.",
        "You don't need more time, you need more focus.",
        "Today's effort is tomorrow's rank.",
        "Consistency turns average into excellent.",
        "Done is better than perfect. Revise, then revise again.",
        "The exam rewards the one who kept showing up."
    )

    val TIME_ZONES = listOf(
        "" to "Device time zone",
        "+05:30" to "India (IST, +05:30)",
        "+00:00" to "UTC (+00:00)",
        "-05:00" to "US Eastern (-05:00)",
        "+01:00" to "Central Europe (+01:00)",
        "+08:00" to "Singapore / China (+08:00)",
        "+04:00" to "Gulf (+04:00)"
    )

    val PRESETS = listOf(
        ExamPreset("GATE CS", "💻", "GATE", "02-07", 0xFF4F46E5L, "CS"),
        ExamPreset("GATE EC", "📡", "GATE", "02-08", 0xFF0EA5E9L, "EC"),
        ExamPreset("GATE EE", "⚡", "GATE", "02-14", 0xFFF59E0BL, "EE"),
        ExamPreset("GATE ME", "⚙️", "GATE", "02-15", 0xFF10B981L, "ME"),
        ExamPreset("GATE CE", "🏗️", "GATE", "02-01", 0xFF8B5CF6L, "CE"),
        ExamPreset("ESE/IES Prelims", "🛠️", "ESE", "02-09", 0xFF14B8A6L, "ME"),
        ExamPreset("ESE/IES Mains", "📝", "ESE", "06-21", 0xFF0EA5E9L, null),
        ExamPreset("JEE Main", "🧪", "JEE", "01-24", 0xFF4F46E5L, null),
        ExamPreset("JEE Advanced", "🚀", "JEE", "05-18", 0xFFF43F5EL, null),
        ExamPreset("NEET UG", "🩺", "NEET", "05-03", 0xFF10B981L, null),
        ExamPreset("UPSC CSE Prelims", "🏛️", "UPSC", "05-24", 0xFFF59E0BL, null),
        ExamPreset("SSC CGL", "📋", "SSC", "09-09", 0xFF8B5CF6L, null),
        ExamPreset("CAT", "📈", "MBA", "11-30", 0xFFEC4899L, null),
        ExamPreset("CLAT", "⚖️", "Other", "12-07", 0xFF4F46E5L, null),
        ExamPreset("NDA", "🎖️", "Other", "04-13", 0xFF10B981L, null),
        ExamPreset("IBPS PO", "🏦", "Bank", "08-17", 0xFF0EA5E9L, null),
        ExamPreset("SBI PO", "🏦", "Bank", "03-08", 0xFF14B8A6L, null),
        ExamPreset("RRB NTPC", "🚆", "Other", "06-15", 0xFFF59E0BL, null),
        ExamPreset("CUET UG", "🎓", "Other", "05-15", 0xFF8B5CF6L, null),
        ExamPreset("UGC NET", "📚", "Other", "06-25", 0xFFF43F5EL, null)
    )

    val SYLLABUS_TEMPLATES: Map<String, String> = mapOf(
        "CS" to "Engineering Mathematics:Linear Algebra,Probability,Calculus;Discrete Maths:Logic,Graph Theory,Combinatorics;Data Structures:Arrays & Lists,Trees,Hashing;Algorithms:Sorting,DP,Greedy;TOC:Automata,Decidability;Compiler Design:Parsing,Optimization;OS:Scheduling,Memory,Deadlocks;DBMS:SQL,Normalization,Transactions;CN:TCP/IP,Routing;COA:Pipelining,Cache",
        "EC" to "Maths:Linear Algebra,Calculus;Networks:Theorems,Transients;Signals & Systems:Fourier,Laplace;Analog Circuits:Diodes,BJT/FET,Op-amps;Digital Circuits:Logic,Sequential;Control Systems:Stability,Root Locus;Communications:AM/FM,Digital Comm;EM Theory:Maxwell,Transmission Lines;Electronic Devices:Semiconductors",
        "EE" to "Maths:Linear Algebra,Calculus;Circuits:Theorems,Transients;EM Fields:Electrostatics,Magnetics;Machines:Transformers,Induction,Synchronous;Power Systems:Faults,Load Flow;Control:Stability,Compensators;Power Electronics:Converters,Inverters;Measurements:Bridges,Instruments",
        "ME" to "Maths:Linear Algebra,Calculus;Strength of Materials:Stress-Strain,Beams;TOM:Mechanisms,Vibrations;Thermodynamics:Laws,Cycles;Fluid Mechanics:Flow,Boundary Layer;Heat Transfer:Conduction,Convection;Manufacturing:Casting,Machining;Industrial Eng:Queuing,Inventory",
        "CE" to "Maths:Linear Algebra,Calculus;Structural Analysis:Trusses,Indeterminate;RCC & Steel:Limit State,Steel Design;Geotechnical:Soil Properties,Foundations;Fluid Mechanics:Hydraulics,Hydrology;Environmental:Water Treatment,Waste Water;Transportation:Highway,Traffic;Surveying:Levelling,Theodolite"
    )

    fun parseTemplate(templateKey: String): List<SyllabusSubject> {
        val raw = SYLLABUS_TEMPLATES[templateKey] ?: return emptyList()
        return raw.split(";").mapNotNull { block ->
            val parts = block.split(":")
            if (parts.size < 2) return@mapNotNull null
            val subjectName = parts[0].trim()
            val topics = parts[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { tName ->
                SyllabusTopic(name = tName)
            }
            SyllabusSubject(
                name = subjectName,
                weightagePercent = 10,
                topics = topics
            )
        }
    }

    fun buildExamFromPreset(preset: ExamPreset, nowMillis: Long = System.currentTimeMillis()): Exam {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentYear = cal.get(Calendar.YEAR)
        val parts = preset.monthDay.split("-")
        val month = (parts.getOrNull(0)?.toIntOrNull() ?: 1) - 1
        val day = parts.getOrNull(1)?.toIntOrNull() ?: 1

        val targetCal = Calendar.getInstance().apply {
            set(currentYear, month, day, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (targetCal.timeInMillis <= nowMillis) {
            targetCal.set(Calendar.YEAR, currentYear + 1)
        }
        val targetYear = targetCal.get(Calendar.YEAR)
        val dateIso = String.format(Locale.US, "%04d-%02d-%02dT09:00", targetYear, month + 1, day)
        val subjects = preset.syllabusTemplateKey?.let { parseTemplate(it) } ?: emptyList()

        return Exam(
            id = UUID.randomUUID().toString().take(8),
            name = preset.name,
            dateTimeIso = dateIso,
            startMillis = nowMillis,
            emoji = preset.emoji,
            colorHex = preset.colorHex,
            category = preset.category,
            venue = "",
            officialLink = "",
            attempt = 1,
            notes = "",
            hoursPerTopic = 4f,
            subjects = subjects,
            milestones = emptyList(),
            mocks = emptyList()
        )
    }

    fun createStarterSeedData(nowMillis: Long = System.currentTimeMillis()): Triple<List<Exam>, List<StudyLogEntity>, List<StudyTodoEntity>> {
        val dayMs = 86_400_000L
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        // Starter Exam 1: GATE CS (Pinned Hero)
        val gatePreset = PRESETS.first()
        val gateExamBase = buildExamFromPreset(gatePreset, nowMillis)
        val populatedSubjects = gateExamBase.subjects.mapIndexed { sIdx, subject ->
            subject.copy(
                weightagePercent = when (sIdx) {
                    0 -> 13
                    1 -> 9
                    2 -> 10
                    3 -> 12
                    else -> 8
                },
                topics = subject.topics.mapIndexed { tIdx, topic ->
                    when {
                        sIdx == 0 && tIdx == 0 -> topic.copy(
                            isDone = true,
                            doneAtMillis = nowMillis - dayMs * 1 // Due for 1-day revision today!
                        )
                        sIdx == 0 && tIdx == 1 -> topic.copy(
                            isDone = true,
                            doneAtMillis = nowMillis - dayMs * 3 // Due for 3-day revision today!
                        )
                        sIdx == 1 && tIdx == 0 -> topic.copy(
                            isDone = true,
                            doneAtMillis = nowMillis - dayMs * 7 // Due for 7-day revision today!
                        )
                        sIdx == 2 && tIdx == 0 -> topic.copy(
                            isDone = true,
                            doneAtMillis = nowMillis - dayMs * 5
                        )
                        sIdx == 3 && tIdx == 1 -> topic.copy(
                            isWeak = true // Flagged weak topic: DP
                        )
                        sIdx == 6 && tIdx == 2 -> topic.copy(
                            isWeak = true // Flagged weak topic: Deadlocks
                        )
                        else -> topic
                    }
                }
            )
        }

        val admitCardDateIso = dateFormat.format(nowMillis + dayMs * 21) + "T10:00"
        val mockSeriesDateIso = dateFormat.format(nowMillis + dayMs * 7) + "T09:00"

        val gateExam = gateExamBase.copy(
            id = "gate_cs_hero",
            startMillis = nowMillis - dayMs * 45,
            venue = "IIT Autonomous Test Center · Hall B",
            officialLink = "https://gate2026.iitg.ac.in",
            notes = "Target 75+ marks. Prioritize Engineering Maths, DSA, Algorithms & OS.",
            subjects = populatedSubjects,
            milestones = listOf(
                ExamMilestone(id = "ms_1", title = "Full-Length Mock Window", dateTimeIso = mockSeriesDateIso),
                ExamMilestone(id = "ms_2", title = "Admit Card Download", dateTimeIso = admitCardDateIso)
            ),
            mocks = listOf(
                MockTestResult(
                    id = "mk_1",
                    dateIso = dateFormat.format(nowMillis - dayMs * 21),
                    score = 54f,
                    maxScore = 100f,
                    accuracyPercent = 78f,
                    rank = 420
                ),
                MockTestResult(
                    id = "mk_2",
                    dateIso = dateFormat.format(nowMillis - dayMs * 14),
                    score = 61.5f,
                    maxScore = 100f,
                    accuracyPercent = 82f,
                    rank = 265
                ),
                MockTestResult(
                    id = "mk_3",
                    dateIso = dateFormat.format(nowMillis - dayMs * 7),
                    score = 68f,
                    maxScore = 100f,
                    accuracyPercent = 86f,
                    rank = 142
                ),
                MockTestResult(
                    id = "mk_4",
                    dateIso = dateFormat.format(nowMillis - dayMs * 2),
                    score = 73.5f,
                    maxScore = 100f,
                    accuracyPercent = 89f,
                    rank = 88
                )
            )
        )

        // Starter Exam 2: CAT or UPSC CSE Prelims
        val catPreset = PRESETS.first { it.name == "CAT" }
        val catExam = buildExamFromPreset(catPreset, nowMillis).copy(
            id = "cat_mba",
            startMillis = nowMillis - dayMs * 20,
            venue = "Metro Digital Zone 4",
            notes = "Focus on DILR sets & VARC reading comprehension speed.",
            subjects = listOf(
                SyllabusSubject(
                    name = "Quantitative Aptitude",
                    weightagePercent = 34,
                    topics = listOf(
                        SyllabusTopic(name = "Arithmetic & Percentages", isDone = true, doneAtMillis = nowMillis - dayMs * 4),
                        SyllabusTopic(name = "Algebra & Functions", isDone = true, doneAtMillis = nowMillis - dayMs * 1),
                        SyllabusTopic(name = "Geometry & Mensuration", isWeak = true),
                        SyllabusTopic(name = "Number System")
                    )
                ),
                SyllabusSubject(
                    name = "DILR",
                    weightagePercent = 32,
                    topics = listOf(
                        SyllabusTopic(name = "Seating Arrangements", isDone = true, doneAtMillis = nowMillis - dayMs * 6),
                        SyllabusTopic(name = "Games & Tournaments", isWeak = true),
                        SyllabusTopic(name = "Caselets & Networks")
                    )
                ),
                SyllabusSubject(
                    name = "VARC",
                    weightagePercent = 34,
                    topics = listOf(
                        SyllabusTopic(name = "Reading Comprehension", isDone = true, doneAtMillis = nowMillis - dayMs * 2),
                        SyllabusTopic(name = "Para Jumbles & Odd Sentence")
                    )
                )
            )
        )

        // Starter Study Logs across the last 14 days for a vibrant streak & heatmap
        val studySubjects = listOf("Engineering Mathematics", "Data Structures", "Algorithms", "OS", "DBMS")
        val logs = buildList {
            for (daysAgo in 0..18) {
                if (daysAgo == 8 || daysAgo == 15) continue // Occasional rest days in the past
                val dateStr = dateFormat.format(nowMillis - daysAgo * dayMs)
                val mins1 = 50 + (daysAgo * 15) % 75
                val subj1 = studySubjects[daysAgo % studySubjects.size]
                add(
                    StudyLogEntity(
                        id = "seed_log_${daysAgo}_a",
                        dateIso = dateStr,
                        minutes = mins1,
                        subjectName = subj1,
                        examId = gateExam.id,
                        timestamp = nowMillis - daysAgo * dayMs
                    )
                )
                if (daysAgo % 2 == 0) {
                    val subj2 = studySubjects[(daysAgo + 2) % studySubjects.size]
                    add(
                        StudyLogEntity(
                            id = "seed_log_${daysAgo}_b",
                            dateIso = dateStr,
                            minutes = 45 + (daysAgo * 10) % 45,
                            subjectName = subj2,
                            examId = gateExam.id,
                            timestamp = nowMillis - daysAgo * dayMs + 3600_000L
                        )
                    )
                }
            }
        }

        val todos = listOf(
            StudyTodoEntity(id = "todo_1", text = "Complete 1-3-7-21 spaced revision for Linear Algebra & Logic", isDone = false),
            StudyTodoEntity(id = "todo_2", text = "Solve 25 previous year questions on Dynamic Programming", isDone = false),
            StudyTodoEntity(id = "todo_3", text = "Review Mock Test #4 error log", isDone = true)
        )

        return Triple(listOf(gateExam, catExam), logs, todos)
    }
}
