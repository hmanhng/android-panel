package com.panel.keymanager.utils

object Constants {
    // BASE_URL is read from local.properties via BuildConfig
    val BASE_URL: String = com.panel.keymanager.BuildConfig.PANEL_URL

    // SharedPreferences keys
    const val PREF_NAME = "panel_prefs"
    const val KEY_USER_ID = "user_id"
    const val KEY_USERNAME = "username"
    const val KEY_FULLNAME = "fullname"
    const val KEY_EMAIL = "email"
    const val KEY_LEVEL = "level"
    const val KEY_SALDO = "saldo"
    const val KEY_TOKEN = "token"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
}
