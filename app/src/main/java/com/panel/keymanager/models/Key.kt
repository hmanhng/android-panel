package com.panel.keymanager.models

import com.google.gson.annotations.SerializedName

data class Key(
    @SerializedName("id")
    private val _id: String = "0",
    val game: String = "",
    @SerializedName("user_key")
    val userKey: String = "",
    @SerializedName("duration")
    private val _duration: String = "0",
    @SerializedName("duration_text")
    val durationText: String? = null,
    @SerializedName("expired_date")
    val expiredDate: String? = null,
    @SerializedName("max_devices")
    private val _maxDevices: String = "1",
    @SerializedName("devices_count")
    val devicesCount: Int = 0,
    val devices: String? = null,
    @SerializedName("status")
    private val _status: String = "1",
    val registrator: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
) {
    val id: Int
        get() = _id.toIntOrNull() ?: 0

    val duration: Int
        get() = _duration.toIntOrNull() ?: 0

    val maxDevices: Int
        get() = _maxDevices.toIntOrNull() ?: 1

    val status: Int
        get() = _status.toIntOrNull() ?: 1
}

data class CreateKeyRequest(
    @SerializedName("user_id")
    val userId: Int,
    val game: String = "PUBG",
    val duration: Int,
    @SerializedName("max_devices")
    val maxDevices: Int = 1,
    @SerializedName("custom_key")
    val customKey: String? = null,
    val quantity: Int = 1
)

data class UpdateKeyRequest(
    @SerializedName("user_id")
    val userId: Int,
    val game: String? = null,
    @SerializedName("user_key")
    val userKey: String? = null,
    val duration: Int? = null,
    @SerializedName("max_devices")
    val maxDevices: Int? = null,
    val status: Int? = null,
    @SerializedName("expired_date")
    val expiredDate: String? = null,
    val devices: String? = null
)

data class ResetKeyRequest(
    @SerializedName("user_id")
    val userId: Int
)

data class Duration(
    val hours: Int,
    val label: String,
    val price: Double
)

data class Game(
    val code: String,
    val name: String
)

data class GeneratedKey(
    val id: Int,
    @SerializedName("user_key")
    val userKey: String,
    val duration: Int,
    @SerializedName("max_devices")
    val maxDevices: Int
)

data class CreateKeyData(
    val keys: List<GeneratedKey>,
    val quantity: Int,
    @SerializedName("price_charged")
    val priceCharged: Double,
    @SerializedName("new_balance")
    val newBalance: Double
)
