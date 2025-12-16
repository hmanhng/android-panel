package com.panel.keymanager.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.panel.keymanager.ui.auth.LoginActivity
import com.panel.keymanager.utils.SessionExpiredEvent

/**
 * Base Activity that handles session expiration events.
 * All activities that require authentication should extend this class.
 */
abstract class BaseActivity : AppCompatActivity() {

    private val sessionExpiredListener: () -> Unit = {
        runOnUiThread {
            onSessionExpired()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionExpiredEvent.addListener(sessionExpiredListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        SessionExpiredEvent.removeListener(sessionExpiredListener)
    }

    /**
     * Called when session expires. Override to customize behavior.
     * Default implementation shows a toast and navigates to login.
     */
    protected open fun onSessionExpired() {
        Toast.makeText(
            this,
            "Session expired. Please login again.",
            Toast.LENGTH_LONG
        ).show()
        navigateToLogin()
    }

    /**
     * Navigates to LoginActivity and clears the back stack.
     */
    protected fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
