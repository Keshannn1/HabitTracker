package com.example.habittracker.domain.use_case

import com.example.habittracker.data.local.dao.RoutineDao
import com.example.habittracker.data.local.entity.RoutineEntity
import com.example.habittracker.data.local.entity.TaskEntity
import com.example.habittracker.data.local.entity.TaskStatus
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import javax.inject.Inject

class CloneTemplateUseCase @Inject constructor(
    private val routineDao: RoutineDao
) {
    suspend operator fun invoke(templateId: String): String? {
        val templateWithTasks = routineDao.getRoutineWithTasksById(templateId).firstOrNull() ?: return null
        
        val newRoutineId = UUID.randomUUID().toString()
        val newRoutine = RoutineEntity(
            id = newRoutineId,
            title = templateWithTasks.routine.title,
            isTemplate = false
        )
        
        val newTasks = templateWithTasks.tasks.map { oldTask ->
            TaskEntity(
                id = UUID.randomUUID().toString(),
                routineId = newRoutineId,
                name = oldTask.name,
                seconds = oldTask.seconds,
                orderIndex = oldTask.orderIndex,
                status = TaskStatus.PENDING
            )
        }
        
        routineDao.insertRoutine(newRoutine)
        routineDao.insertTasks(newTasks)
        
        return newRoutineId
    }
}
