package com.shadowcalc.app

import android.app.Application
import android.os.Process

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val securityManager = SecurityManager(this)
        AutoLockManager.getInstance().init(this, securityManager)
    }
}
