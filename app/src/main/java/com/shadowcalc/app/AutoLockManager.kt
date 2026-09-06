package com.shadowcalc.app

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class AutoLockManager private constructor() : DefaultLifecycleObserver {
    private val handler = Handler(Looper.getMainLooper())
    private var lockRunnable: Runnable? = null
    private var autoLockMinutes = 5
    private var isVaultOpen = false

    companion object {
        @Volatile
        private var instance: AutoLockManager? = null
        const val ACTION_LOCK_VAULT = "com.shadowcalc.app.LOCK_VAULT"

        fun getInstance(): AutoLockManager {
            return instance ?: synchronized(this) {
                instance ?: AutoLockManager().also { instance = it }
            }
        }
    }

    fun init(application: Application, securityManager: SecurityManager) {
        autoLockMinutes = securityManager.getAutoLockMinutes()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun setVaultOpen(open: Boolean) {
        isVaultOpen = open
        if (open) resetTimer() else cancelTimer()
    }

    fun resetTimer() {
        cancelTimer()
        if (!isVaultOpen) return
        lockRunnable = Runnable {
            broadcastLock()
        }.also { handler.postDelayed(it, autoLockMinutes * 60 * 1000L) }
    }

    fun cancelTimer() {
        lockRunnable?.let { handler.removeCallbacks(it) }
        lockRunnable = null
    }

    private fun broadcastLock() {
        val intent = Intent(ACTION_LOCK_VAULT)
        intent.setPackage("com.shadowcalc.app")
        // Broadcast will be received by activities
    }

    override fun onStop(owner: LifecycleOwner) {
        // App went to background
        if (isVaultOpen) {
            handler.postDelayed({ broadcastLock() }, 500)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        // App came to foreground
    }

    fun updateAutoLockMinutes(minutes: Int) {
        autoLockMinutes = minutes
        if (isVaultOpen) resetTimer()
    }
}
