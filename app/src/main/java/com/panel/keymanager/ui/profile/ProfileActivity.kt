package com.panel.keymanager.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.panel.keymanager.ui.BaseActivity
import androidx.lifecycle.lifecycleScope
import com.panel.keymanager.api.RetrofitClient
import com.panel.keymanager.databinding.ActivityProfileBinding
import com.panel.keymanager.ui.auth.LoginActivity
import com.panel.keymanager.utils.SessionManager
import kotlinx.coroutines.launch

class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Set auth token for API requests
        RetrofitClient.updateToken(sessionManager.getAuthToken())

        setupToolbar()
        loadProfile()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun loadProfile() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getProfile()

                if (response.isSuccessful && response.body()?.status == "success") {
                    val user = response.body()?.data
                    user?.let {
                        binding.apply {
                            // Preserve existing tokens before saving
                            val existingToken = sessionManager.getAuthToken()
                            val existingRefreshToken = sessionManager.getRefreshToken()

                            val userToSave = it.copy(
                                token = if (it.token.isNullOrEmpty()) existingToken else it.token,
                                refreshToken = if (it.refreshToken.isNullOrEmpty()) existingRefreshToken else it.refreshToken
                            )

                            sessionManager.saveUser(userToSave) // Save fresh data with preserved tokens

                            tvUsername.text = it.username
                            tvFullname.text = it.fullname ?: "-"
                            tvEmail.text = it.email ?: "-"
                            tvLevel.text = it.levelText ?: getLevelText(it.level)
                            tvSaldo.text = "$${String.format("%.2f", it.saldo)}"
                            tvTotalKeys.text = "${it.totalKeys ?: 0} keys"

                            if (it.expirationDate != null) {
                                tvExpiration.text = "${it.expirationDate}"
                            } else {
                                tvExpiration.text = "No expiration"
                            }
                        }
                    }
                } else {
                    // Fallback to local data
                    displayLocalData()
                }
            } catch (e: Exception) {
                displayLocalData()
                Toast.makeText(
                    this@ProfileActivity,
                    "Could not refresh profile: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun displayLocalData() {
        val user = sessionManager.getUser()
        user?.let {
            binding.apply {
                tvUsername.text = it.username
                tvFullname.text = it.fullname ?: "-"
                tvEmail.text = it.email ?: "-"
                tvLevel.text = getLevelText(it.level)
                tvSaldo.text = "$${String.format("%.2f", it.saldo)}"
                tvTotalKeys.text = "- keys"
                tvExpiration.text = if (it.expirationDate != null) "${it.expirationDate}" else "No expiration"
            }
        }
    }

    private fun getLevelText(level: Int): String {
        return when (level) {
            1 -> "Owner"
            2 -> "Admin"
            3 -> "Reseller"
            else -> "User"
        }
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            confirmLogout()
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadProfile()
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val refreshToken = sessionManager.getRefreshToken()
                        if (refreshToken != null) {
                            // Use refreshApiService (without authenticator) to ensure logout works
                            // even when access token is expired
                            RetrofitClient.refreshApiService.logout(
                                com.panel.keymanager.models.RefreshRequest(refreshToken)
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        sessionManager.logout()
                        // Note: sessionManager.logout() handles navigation to LoginActivity
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
