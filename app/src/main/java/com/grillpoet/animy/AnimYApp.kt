package com.grillpoet.animy

import android.app.Application
import timber.log.Timber

class AnimYApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
