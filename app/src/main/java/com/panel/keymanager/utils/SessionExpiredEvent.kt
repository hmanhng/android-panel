package com.panel.keymanager.utils

import android.os.Handler
import android.os.Looper

/**
 * Event object for session expiration notifications.
 * Activities can register listeners to be notified when the session expires
 * (e.g., when refresh token is invalid/expired).
 */
object SessionExpiredEvent {
    private val listeners = mutableSetOf<() -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())

    // Flag to prevent multiple emissions when multiple requests fail simultaneously
    @Volatile
    private var hasEmitted = false

    fun addListener(listener: () -> Unit) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: () -> Unit) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    fun emit() {
        // Only emit once per session expiration
        if (hasEmitted) return
        hasEmitted = true

        mainHandler.post {
            synchronized(listeners) {
                listeners.toList().forEach { it.invoke() }
            }
        }
    }

    /**
     * Reset the emission flag. Call this when user logs in again.
     */
    fun reset() {
        hasEmitted = false
    }
}
