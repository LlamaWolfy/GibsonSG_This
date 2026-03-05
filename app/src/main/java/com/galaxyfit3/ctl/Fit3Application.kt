package com.galaxyfit3.ctl

import android.app.Application

class Fit3Application : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: Fit3Application
            private set
    }
}
