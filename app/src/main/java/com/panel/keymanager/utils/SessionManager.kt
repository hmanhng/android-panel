package com.panel.keymanager.utils

import android.content.Context
import android.content.SharedPreferences
import com.panel.keymanager.models.User

class SessionManager(context: Context) {
    private val context = context.applicationContext
    private val prefs: SharedPreferences = this.context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    private val editor: SharedPreferences.Editor = prefs.edit()

    fun saveUser(user: User) {
        saveUserSession(
            userId = user.userId.toString(),
            username = user.username,
            fullname = user.fullname ?: "",
            email = user.email ?: "",
            level = user.level,
            saldo = user.saldo.toString(),
            expirationDate = user.expirationDate,
            token = user.token ?: "",
            refreshToken = user.refreshToken ?: ""
        )
    }

    fun saveUserSession(
        userId: String,
        username: String,
        fullname: String,
        email: String,
        level: Int,
        saldo: String,
        expirationDate: String?,
        token: String,
        refreshToken: String
    ) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_USER_ID, userId)
        editor.putString(KEY_USERNAME, username)
        editor.putString(KEY_FULLNAME, fullname)
        editor.putString(KEY_EMAIL, email)
        editor.putInt(KEY_LEVEL, level)
        editor.putString(KEY_SALDO, saldo)
        editor.putString(KEY_EXPIRATION_DATE, expirationDate)
        editor.putString(KEY_AUTH_TOKEN, token)
        editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        editor.apply()
    }

    fun getUser(): User? {
        if (!isLoggedIn()) return null

        // Note: The types for userId and saldo in getUser are Int and Double respectively,
        // while in saveUserSession they are String. This might lead to runtime errors
        // if not handled carefully. Assuming conversion is needed or types should match.
        // For now, casting String to Int/Float for compatibility with User model.
        return User(
            userId = prefs.getString(KEY_USER_ID, "0")?.toIntOrNull() ?: 0,
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            fullname = prefs.getString(KEY_FULLNAME, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            level = prefs.getInt(KEY_LEVEL, 3),
            saldo = try {
                prefs.getString(KEY_SALDO, "0.0")?.toDoubleOrNull() ?: 0.0
            } catch (e: ClassCastException) {
                // If stored as Float (previous bug), retrieve as Float and convert
                prefs.getFloat(KEY_SALDO, 0f).toDouble()
            },
            token = prefs.getString(KEY_AUTH_TOKEN, "") ?: "",
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "") ?: ""
        )
    }


    fun getUserId(): Int = prefs.getString(KEY_USER_ID, "0")?.toIntOrNull() ?: 0

    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""

    fun getUserLevel(): Int = prefs.getInt(KEY_LEVEL, 3)

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun saveRefreshToken(token: String) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun updateSaldo(newSaldo: Double) {
        prefs.edit().putString(KEY_SALDO, newSaldo.toString()).apply()
    }

    /**
     * Clears all session data without any navigation or event emission.
     * Use this for internal cleanup operations.
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }

    /**
     * Forces logout by clearing session and emitting SessionExpiredEvent.
     * Use this when session expires (e.g., refresh token invalid) from background threads.
     * Activities listening to SessionExpiredEvent will handle navigation.
     */
    fun forceLogout() {
        clearSession()
        SessionExpiredEvent.emit()
    }

    /**
     * Standard logout with direct navigation to LoginActivity.
     * Use this for user-initiated logout actions.
     */
    fun logout() {
        clearSession()

        // Direct navigation to LoginActivity on Main Thread
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val intent = android.content.Intent(context, com.panel.keymanager.ui.auth.LoginActivity::class.java)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        }
    }

    companion object {
        private const val PREF_NAME = "panel_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULLNAME = "fullname"
        private const val KEY_EMAIL = "email"
        private const val KEY_LEVEL = "level"
        private const val KEY_SALDO = "saldo"
        private const val KEY_EXPIRATION_DATE = "expiration_date"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
