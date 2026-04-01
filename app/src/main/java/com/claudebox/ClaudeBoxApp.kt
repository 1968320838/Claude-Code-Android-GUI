package com.claudebox

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ClaudeBoxApp : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}
