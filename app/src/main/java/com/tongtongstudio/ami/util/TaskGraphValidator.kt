package com.tongtongstudio.ami.util

import com.tongtongstudio.ami.data.Repository

class TaskGraphValidator(private val repository: Repository) {

    suspend fun validateGraph(taskId: Long, newParentId: Long?): ValidationResult {
        return when {
            taskId == newParentId ->
                ValidationResult.Invalid("Self-reference not allowed")

            newParentId != null -> {
                if (hasCircularDependency(taskId, newParentId)) {
                    ValidationResult.Invalid("Circular dependency detected")
                } else {
                    ValidationResult.Valid
                }
            }

            else -> ValidationResult.Valid
        }
    }

    private suspend fun hasCircularDependency(sourceId: Long, targetId: Long): Boolean {
        val visited = mutableSetOf<Long>()
        val queue = ArrayDeque<Long>().apply { add(targetId) }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            if (current == sourceId) return true
            if (visited.contains(current)) continue

            visited.add(current)
            //repository.getTaskDependencies(current).forEach { queue.add(it) }
        }

        return false
    }

    private suspend fun hasCircularDependency(parentId: Long, currentTaskId: Long?): Boolean {
        if (currentTaskId == null) return false

        val visited = mutableSetOf<Long>()
        var currentId: Long? = parentId

        while (currentId != null) {
            if (currentId == currentTaskId) return true
            if (visited.contains(currentId)) return true // Boucle infinie

            visited.add(currentId)
            currentId = repository.getTask(currentId)?.parentTaskId
        }
        return false
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }
}