package com.gibsonsg.todo.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gibsonsg.todo.core.data.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleAllFutureReminders()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleAllFutureReminders() {
        val now = Instant.now()
        taskRepository.observeActiveTasks().first().forEach { task ->
            val dueAt = task.dueAt
            if (dueAt != null && dueAt.isAfter(now) && !task.isDone) {
                reminderScheduler.schedule(task.id, task.title, dueAt)
            }
        }
    }
}
