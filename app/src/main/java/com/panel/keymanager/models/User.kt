package com.panel.keymanager.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("user_id")
    val userId: Int,
    val username: String,
    val fullname: String? = null,
    val email: String? = null,
    val level: Int = 3,
    val saldo: Double = 0.0,
    @SerializedName("expiration_date")
    val expirationDate: String? = null,
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("level_text")
    val levelText: String? = null,
    @SerializedName("total_keys")
    val totalKeys: Int? = null,
    @SerializedName("refresh_token")
    val refreshToken: String? = null
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val username: String,
    val fullname: String,
    val password: String,
    val referral: String
)
