package com.panel.keymanager.api

import com.panel.keymanager.models.ApiResponse
import com.panel.keymanager.models.RefreshRequest
import com.panel.keymanager.models.RefreshResponse
import com.panel.keymanager.models.User
import com.panel.keymanager.models.LoginRequest
import com.panel.keymanager.models.RegisterRequest
import com.panel.keymanager.models.Key
import com.panel.keymanager.models.CreateKeyRequest
import com.panel.keymanager.models.CreateKeyData
import com.panel.keymanager.models.UpdateKeyRequest
import com.panel.keymanager.models.ResetKeyRequest
import com.panel.keymanager.models.Duration
import com.panel.keymanager.models.Game
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<User>>

    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<Map<String, Int>>>

    @Headers("No-Authentication: true")
    @POST("api/refresh")
    fun refreshToken(@Body request: RefreshRequest): retrofit2.Call<ApiResponse<RefreshResponse>>

    @Headers("No-Authentication: true")
    @POST("api/logout")
    suspend fun logout(@Body request: RefreshRequest): Response<ApiResponse<Any>>

    // Profile
    @GET("api/profile")
    suspend fun getProfile(): Response<ApiResponse<User>>

    // Keys
    @GET("api/keys")
    suspend fun getKeys(): Response<ApiResponse<List<Key>>>

    @GET("api/keys/{id}")
    suspend fun getKey(@Path("id") keyId: Int): Response<ApiResponse<Key>>

    @POST("api/keys")
    suspend fun createKey(@Body request: CreateKeyRequest): Response<ApiResponse<CreateKeyData>>

    @PUT("api/keys/{id}")
    suspend fun updateKey(
        @Path("id") keyId: Int,
        @Body request: UpdateKeyRequest
    ): Response<ApiResponse<Any>>

    @DELETE("api/keys/{id}")
    suspend fun deleteKey(
        @Path("id") keyId: Int
    ): Response<ApiResponse<Any>>

    @DELETE("api/keys/expired")
    suspend fun deleteExpiredKeys(): Response<ApiResponse<Any>>

    @POST("api/keys/{id}/reset")
    suspend fun resetKey(
        @Path("id") keyId: Int,
        @Body request: ResetKeyRequest
    ): Response<ApiResponse<Any>>

    @POST("api/keys/{id}/resetDevices")
    suspend fun resetDevices(
        @Path("id") keyId: Int,
        @Body request: ResetKeyRequest
    ): Response<ApiResponse<Any>>

    // Config
    @GET("api/durations")
    suspend fun getDurations(): Response<ApiResponse<List<Duration>>>

    @GET("api/games")
    suspend fun getGames(): Response<ApiResponse<List<Game>>>
}
