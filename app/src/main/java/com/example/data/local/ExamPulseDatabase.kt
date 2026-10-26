package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AppPreferencesEntity
import com.example.data.model.ExamEntity
import com.example.data.model.StudyLogEntity
import com.example.data.model.StudyTodoEntity

@Database(
    entities = [
        ExamEntity::class,
        StudyLogEntity::class,
        StudyTodoEntity::class,
        AppPreferencesEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ExamPulseDatabase : RoomDatabase() {
    abstract fun examPulseDao(): ExamPulseDao

    companion object {
        @Volatile
        private var INSTANCE: ExamPulseDatabase? = null

        fun getInstance(context: Context): ExamPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExamPulseDatabase::class.java,
                    "exam_pulse_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
