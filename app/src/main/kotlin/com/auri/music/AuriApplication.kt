package com.auri.music

import android.app.Application

class AuriApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @Volatile
        lateinit var instance: AuriApplication
            private set
    }
}
