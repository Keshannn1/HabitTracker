package com.example.habittracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.habittracker.data.local.dao.RoutineDao
import com.example.habittracker.data.local.dao.TodoDao
import com.example.habittracker.data.local.entity.HistoryEntity
import com.example.habittracker.data.local.entity.RoutineEntity
import com.example.habittracker.data.local.entity.TaskEntity
import com.example.habittracker.data.local.entity.TodoEntity
import java.util.UUID

@Database(
    entities = [RoutineEntity::class, TaskEntity::class, HistoryEntity::class, TodoEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routineDao(): RoutineDao
    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habit_tracker_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(SeedingCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedingCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Seed templates using raw SQL since the Room DAO is not available yet
            // (INSTANCE is only set after build() completes)
            seedTemplatesDirect(db)
        }
    }
}

/**
 * Inserts seed template data directly via raw SQL.
 * Called during Room database creation (before INSTANCE is available).
 */
private fun seedTemplatesDirect(db: SupportSQLiteDatabase) {
    // Template 1: Morning Flow
    val morningId = UUID.randomUUID().toString()
    db.execSQL(
        "INSERT INTO routines (id, title, isTemplate) VALUES (?, ?, 1)",
        arrayOf(morningId, "Morning Flow")
    )
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), morningId, "Hydrate", 60, 0))
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), morningId, "Meditate", 300, 1))
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), morningId, "Stretch", 300, 2))

    // Template 2: Deep Work
    val workId = UUID.randomUUID().toString()
    db.execSQL(
        "INSERT INTO routines (id, title, isTemplate) VALUES (?, ?, 1)",
        arrayOf(workId, "Deep Work")
    )
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), workId, "Clear Desk", 120, 0))
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), workId, "Focus Block", 1500, 1))
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), workId, "Review", 300, 2))

    // Template 3: Wind Down
    val windId = UUID.randomUUID().toString()
    db.execSQL(
        "INSERT INTO routines (id, title, isTemplate) VALUES (?, ?, 1)",
        arrayOf(windId, "Wind Down")
    )
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), windId, "Reflect", 300, 0))
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), windId, "Read", 900, 1))
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), windId, "Sleep Prep", 180, 2))

    // Template 4: Quick Workout
    val workoutId = UUID.randomUUID().toString()
    db.execSQL(
        "INSERT INTO routines (id, title, isTemplate) VALUES (?, ?, 1)",
        arrayOf(workoutId, "Quick Workout")
    )
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), workoutId, "Jumping Jacks", 60, 0))
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), workoutId, "Pushups", 60, 1))
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), workoutId, "Plank", 60, 2))
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), workoutId, "Squats", 60, 3))

    // Template 5: Study Session
    val studyId = UUID.randomUUID().toString()
    db.execSQL(
        "INSERT INTO routines (id, title, isTemplate) VALUES (?, ?, 1)",
        arrayOf(studyId, "Study Session")
    )
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), studyId, "Review Notes", 600, 0))
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), studyId, "Practice Problems", 1200, 1))
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), studyId, "Summarize", 300, 2))

    // Template 6: Healthy Cooking
    val cookingId = UUID.randomUUID().toString()
    db.execSQL(
        "INSERT INTO routines (id, title, isTemplate) VALUES (?, ?, 1)",
        arrayOf(cookingId, "Healthy Cooking")
    )
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), cookingId, "Prep Veggies", 600, 0))
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), cookingId, "Cook Protein", 900, 1))
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), cookingId, "Plating", 120, 2))

    // Template 7: Mindfulness Break
    val mindfulId = UUID.randomUUID().toString()
    db.execSQL(
        "INSERT INTO routines (id, title, isTemplate) VALUES (?, ?, 1)",
        arrayOf(mindfulId, "Mindfulness Break")
    )
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), mindfulId, "Deep Breathing", 120, 0))
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), mindfulId, "Body Scan", 300, 1))
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), mindfulId, "Gratitude", 180, 2))

    // Template 8: Digital Cleanup
    val cleanupId = UUID.randomUUID().toString()
    db.execSQL(
        "INSERT INTO routines (id, title, isTemplate) VALUES (?, ?, 1)",
        arrayOf(cleanupId, "Digital Cleanup")
    )
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), cleanupId, "Clear Inbox", 600, 0))
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), cleanupId, "Organize Files", 600, 1))
    db.execSQL("INSERT INTO tasks (id, routineId, name, seconds, orderIndex, status) VALUES (?, ?, ?, ?, ?, 'PENDING')", arrayOf(UUID.randomUUID().toString(), cleanupId, "Update Apps", 300, 2))
}