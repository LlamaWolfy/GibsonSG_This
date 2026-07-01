package com.gibsonsg.todo.core.domain.usecase

import com.gibsonsg.todo.core.domain.model.Task
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class SmartList {
    TODAY,
    OVERDUE,
    UPCOMING,
    NO_DATE,
    ALL
}

/**
 * Buckets active (non-archived) tasks into the smart-list categories shown as filter tabs.
 * Pure function of [tasks] and the current time so it's cheap to unit test without a database.
 */
class GetTasksForSmartListUseCase @Inject constructor(
    private val clock: Clock
) {
    operator fun invoke(tasks: List<Task>, smartList: SmartList): List<Task> {
        val today = LocalDate.now(clock)
        val zone = clock.zone.takeIf { it != ZoneId.of("Z") } ?: ZoneId.systemDefault()

        // Already-completed tasks don't belong in Today/Overdue/Upcoming/No Date buckets;
        // those are for outstanding work. "All" still shows everything, done or not.
        val candidates = tasks.filter { !it.isDone || smartList == SmartList.ALL }

        return when (smartList) {
            SmartList.ALL -> candidates
            SmartList.NO_DATE -> candidates.filter { it.dueAt == null }
            SmartList.TODAY -> candidates.filter { task ->
                val dueDate = task.dueAt?.atZone(zone)?.toLocalDate() ?: return@filter false
                dueDate.isEqual(today)
            }
            SmartList.OVERDUE -> candidates.filter { task ->
                val dueDate = task.dueAt?.atZone(zone)?.toLocalDate() ?: return@filter false
                dueDate.isBefore(today)
            }
            SmartList.UPCOMING -> candidates.filter { task ->
                val dueDate = task.dueAt?.atZone(zone)?.toLocalDate() ?: return@filter false
                dueDate.isAfter(today)
            }
        }
    }
}
