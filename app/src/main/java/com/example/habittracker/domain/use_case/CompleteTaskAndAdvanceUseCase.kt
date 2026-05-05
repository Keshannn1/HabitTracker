package com.example.habittracker.domain.use_case

import com.example.habittracker.data.local.dao.RoutineDao
import com.example.habittracker.data.local.entity.TaskEntity
import com.example.habittracker.data.local.entity.TaskStatus
import javax.inject.Inject

class CompleteTaskAndAdvanceUseCase @Inject constructor(
    private val routineDao: RoutineDao
) {
    /**
     * Completes the current task and returns the next pending task.
     * Does NOT insert history — that's done when the full routine completes.
     */
    suspend operator fun invoke(taskId: String, routineId: String, timeSpent: Long): TaskEntity? {
        // Update TaskEntity status to COMPLETED
        val currentTask = routineDao.getTaskById(taskId)
        if (currentTask != null) {
            routineDao.updateTask(currentTask.copy(status = TaskStatus.COMPLETED))
        }

        // Call routineDao.getNextPendingTask(routineId)
        val nextTask = routineDao.getNextPendingTask(routineId)
        
        // If a task is returned, set its status to ACTIVE and return it
        if (nextTask != null) {
            val activeTask = nextTask.copy(status = TaskStatus.ACTIVE)
            routineDao.updateTask(activeTask)
            return activeTask
        }

        // If null, routine is fully complete
        return null
    }
}