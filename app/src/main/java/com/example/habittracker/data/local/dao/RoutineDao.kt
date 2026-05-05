package com.example.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.habittracker.data.local.entity.HistoryEntity
import com.example.habittracker.data.local.entity.RoutineEntity
import com.example.habittracker.data.local.entity.RoutineWithTasks
import com.example.habittracker.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>): List<Long>

    @Transaction
    @Query("SELECT * FROM routines WHERE isTemplate = 0")
    fun getAllRoutinesWithTasks(): Flow<List<RoutineWithTasks>>

    @Transaction
    @Query("SELECT * FROM routines WHERE isTemplate = 1")
    fun getAllTemplateRoutinesWithTasks(): Flow<List<RoutineWithTasks>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId")
    fun getRoutineWithTasksById(routineId: String): Flow<RoutineWithTasks?>

    @Update
    suspend fun updateRoutine(routine: RoutineEntity): Int

    @Update
    suspend fun updateTask(task: TaskEntity): Int

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity): Int

    @Query("DELETE FROM tasks WHERE routineId = :routineId")
    suspend fun deleteTasksByRoutineId(routineId: String): Int

    // Using orderIndex to fetch the next sequential task
    @Query("SELECT * FROM tasks WHERE routineId = :routineId AND status = 'PENDING' ORDER BY orderIndex ASC LIMIT 1")
    suspend fun getNextPendingTask(routineId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("UPDATE tasks SET status = 'PENDING' WHERE routineId = :routineId")
    suspend fun resetTasksToPending(routineId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long

    @Query("SELECT title FROM routines WHERE id = :routineId")
    suspend fun getRoutineName(routineId: String): String?

    @Query("SELECT * FROM history WHERE routineId = :routineId ORDER BY completionDate DESC")
    fun getHistoryForRoutine(routineId: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY completionDate DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>
}
