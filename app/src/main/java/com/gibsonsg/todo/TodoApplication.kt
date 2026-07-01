package com.gibsonsg.todo

import android.app.Application
import com.gibsonsg.todo.core.reminder.ReminderNotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TodoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderNotificationHelper.createChannel(this)
    }
}
