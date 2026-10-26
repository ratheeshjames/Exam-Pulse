package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AppPreferencesEntity
import com.example.data.model.ExamEntity
import com.example.data.model.StudyLogEntity
import com.example.data.model.StudyTodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamPulseDao {
    // Exams
    @Query("SELECT * FROM exams ORDER BY dateTimeIso ASC")
    fun observeExams(): Flow<List<ExamEntity>>

    @Query("SELECT COUNT(*) FROM exams")
    suspend fun getExamCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExam(exam: ExamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExams(exams: List<ExamEntity>)

    @Query("DELETE FROM exams WHERE id = :examId")
    suspend fun deleteExamById(examId: String)

    @Query("DELETE FROM exams")
    suspend fun deleteAllExams()

    // Study Logs
    @Query("SELECT * FROM study_logs ORDER BY dateIso DESC, timestamp DESC")
    fun observeStudyLogs(): Flow<List<StudyLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyLog(log: StudyLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyLogs(logs: List<StudyLogEntity>)

    @Query("DELETE FROM study_logs WHERE id = :logId")
    suspend fun deleteStudyLogById(logId: String)

    @Query("DELETE FROM study_logs")
    suspend fun deleteAllStudyLogs()

    // Study Todos
    @Query("SELECT * FROM study_todos ORDER BY isDone ASC, createdAt DESC")
    fun observeStudyTodos(): Flow<List<StudyTodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudyTodo(todo: StudyTodoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudyTodos(todos: List<StudyTodoEntity>)

    @Query("DELETE FROM study_todos WHERE id = :todoId")
    suspend fun deleteStudyTodoById(todoId: String)

    @Query("DELETE FROM study_todos")
    suspend fun deleteAllStudyTodos()

    // Preferences
    @Query("SELECT * FROM app_preferences WHERE id = 1 LIMIT 1")
    fun observePreferences(): Flow<AppPreferencesEntity?>

    @Query("SELECT * FROM app_preferences WHERE id = 1 LIMIT 1")
    suspend fun getPreferencesOnce(): AppPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreferences(prefs: AppPreferencesEntity)

    @Query("DELETE FROM app_preferences")
    suspend fun deleteAllPreferences()
}
