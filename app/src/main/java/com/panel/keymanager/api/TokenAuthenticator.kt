package com.panel.keymanager.api

import com.panel.keymanager.models.RefreshRequest
import com.panel.keymanager.utils.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

class TokenAuthenticator(
    private val sessionManager: SessionManager,
    private val apiServiceProvider: () -> ApiService
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 3) {
            return null // Failed 3 times, give up to prevent infinite loop
        }

        // If the failed request was a refresh request, don't try to refresh again
        if (response.request.header("No-Authentication") != null) {
            return null
        }

        val refreshToken = sessionManager.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            return null
        }

        return try {
            // Synchronous call to refresh token
            val refreshResponse = apiServiceProvider().refreshToken(RefreshRequest(refreshToken)).execute()

            if (refreshResponse.isSuccessful) {
                val data = refreshResponse.body()?.data
                val newAccessToken = data?.token
                val newRefreshToken = data?.refreshToken

                if (!newAccessToken.isNullOrEmpty()) {
                    sessionManager.saveAuthToken(newAccessToken)

                    if (!newRefreshToken.isNullOrEmpty()) {
                        sessionManager.saveRefreshToken(newRefreshToken)
                    }

                    // CRITICAL: Update the token in RetrofitClient singleton to prevent next request failing
                    com.panel.keymanager.api.RetrofitClient.updateToken(newAccessToken)

                    // Retry the request with the new token
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                } else {
                    sessionManager.forceLogout() // Refresh success but no token? Force logout via event.
                    null
                }
            } else {
                sessionManager.forceLogout() // Refresh failed (e.g. 401/403). Force logout via event.
                null
            }
        } catch (e: Exception) {
            // Check for other errors (runtime, parsing, etc)
            e.printStackTrace()
            // If we can't refresh, we should probably logout or at least give up
            // Ideally, we only logout if we are sure it's an auth error, 
            // but for "spin and exit" protection, ensuring we don't crash is key.
            // If it's a parsing error from 401, we should logout.
            sessionManager.forceLogout()
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            result++
            priorResponse = priorResponse.priorResponse
        }
        return result
    }
}
