package com.panel.keymanager.api

import com.panel.keymanager.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var sessionManager: com.panel.keymanager.utils.SessionManager? = null
    private var authToken: String? = null

    fun initialize(manager: com.panel.keymanager.utils.SessionManager) {
        sessionManager = manager
        authToken = manager.getAuthToken()
    }

    fun updateToken(token: String?) {
        authToken = token
    }

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()

        // Check for the "No-Authentication" header
        if (original.header("No-Authentication") == null) {
            authToken?.let {
                builder.header("Authorization", "Bearer $it")
            }
        } else {
            // Remove the header so it doesn't get sent to the server
            builder.removeHeader("No-Authentication")
        }

        val request = builder.build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttpClient WITHOUT authenticator - used for refresh token requests
    // This prevents infinite loop when refresh token fails
    private val noAuthOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ApiService without authenticator - for refresh token only
    private val noAuthRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .client(noAuthOkHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val refreshApiService: ApiService = noAuthRetrofit.create(ApiService::class.java)

    private val authenticator = okhttp3.Authenticator { _, response ->
        sessionManager?.let { manager ->
            // Use refreshApiService (without authenticator) to prevent infinite loop
            com.panel.keymanager.api.TokenAuthenticator(manager) { refreshApiService }.authenticate(null, response)
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .authenticator(authenticator)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
