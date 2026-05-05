package com.example.habittracker.domain.use_case

import com.example.habittracker.data.local.dao.RoutineDao
import com.example.habittracker.data.local.entity.HistoryEntity
import com.example.habittracker.data.local.entity.TaskEntity
import com.example.habittracker.data.local.entity.TaskStatus
import javax.inject.Inject

class CompleteTaskAndAdvanceUseCase @Inject constructor(
    private val routineDao: RoutineDao
) {
    suspend operator fun invoke(taskId: String, routineId: String, timeSpent: Long): TaskEntity? {
        // Update TaskEntity status to COMPLETED
        val currentTask = routineDao.getTaskById(taskId)
        if (currentTask != null) {
            routineDao.updateTask(currentTask.copy(status = TaskStatus.COMPLETED))
        }

        // Insert a record into HistoryEntity
        val history = HistoryEntity(
            routineId = routineId,
            completionDate = System.currentTimeMillis(),
            totalTimeSpent = timeSpent
        )
        routineDao.insertHistory(history)

        // Call routineDao.getNextPendingTask(routineId)
        val nextTask = routineDao.getNextPendingTask(routineId)
        
        // If a task is returned, set its status to ACTIVE and return it
        if (nextTask != null) {
            val activeTask = nextTask.copy(status = TaskStatus.ACTIVE)
            routineDao.updateTask(activeTask)
            return activeTask
        }

        // If null, return a "Routine Finished" signal
        return null
    }
}
