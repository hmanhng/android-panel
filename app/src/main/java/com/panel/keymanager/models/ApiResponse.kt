package com.panel.keymanager.models

import com.google.gson.annotations.SerializedName

// Generic API Response wrapper
data class ApiResponse<T>(
    val status: String,
    val message: String? = null,
    val data: T? = null,
    val total: Int? = null
)

// Error response
data class ErrorResponse(
    val status: String? = null,
    val messages: ErrorMessages? = null
)

data class ErrorMessages(
    val error: String? = null
)
