package com.example.habittracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.habittracker.data.local.dao.RoutineDao
import com.example.habittracker.data.local.entity.HistoryEntity
import com.example.habittracker.data.local.entity.RoutineEntity
import com.example.habittracker.data.local.entity.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [RoutineEntity::class, TaskEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routineDao(): RoutineDao

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
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    seedDatabase(database.routineDao())
                }
            }
        }

        suspend fun seedDatabase(dao: RoutineDao) {
            // Template 1: Morning Flow
            val morningId = UUID.randomUUID().toString()
            val morningRoutine = RoutineEntity(id = morningId, title = "Morning Flow", isTemplate = true)
            val morningTasks = listOf(
                TaskEntity(routineId = morningId, name = "Hydrate", seconds = 60, orderIndex = 0),
                TaskEntity(routineId = morningId, name = "Meditate", seconds = 300, orderIndex = 1),
                TaskEntity(routineId = morningId, name = "Stretch", seconds = 300, orderIndex = 2)
            )

            // Template 2: Deep Work
            val workId = UUID.randomUUID().toString()
            val workRoutine = RoutineEntity(id = workId, title = "Deep Work", isTemplate = true)
            val workTasks = listOf(
                TaskEntity(routineId = workId, name = "Clear Desk", seconds = 120, orderIndex = 0),
                TaskEntity(routineId = workId, name = "Focus Block", seconds = 1500, orderIndex = 1),
                TaskEntity(routineId = workId, name = "Review", seconds = 300, orderIndex = 2)
            )

            // Template 3: Wind Down
            val windId = UUID.randomUUID().toString()
            val windRoutine = RoutineEntity(id = windId, title = "Wind Down", isTemplate = true)
            val windTasks = listOf(
                TaskEntity(routineId = windId, name = "Reflect", seconds = 300, orderIndex = 0),
                TaskEntity(routineId = windId, name = "Read", seconds = 900, orderIndex = 1),
                TaskEntity(routineId = windId, name = "Sleep Prep", seconds = 180, orderIndex = 2)
            )

            dao.insertRoutine(morningRoutine)
            dao.insertTasks(morningTasks)

            dao.insertRoutine(workRoutine)
            dao.insertTasks(workTasks)

            dao.insertRoutine(windRoutine)
            dao.insertTasks(windTasks)
        }
    }
}
