package com.panel.keymanager.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.panel.keymanager.api.RetrofitClient
import com.panel.keymanager.databinding.ActivityRegisterBinding
import com.panel.keymanager.models.RegisterRequest
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRegisterBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Register"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
    
    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val fullname = binding.etFullname.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val referral = binding.etReferral.text.toString().trim()
            
            if (validateInput(email, username, fullname, password, confirmPassword, referral)) {
                performRegister(email, username, fullname, password, referral)
            }
        }
        
        binding.tvLogin.setOnClickListener {
            finish()
        }
    }
    
    private fun validateInput(
        email: String,
        username: String,
        fullname: String,
        password: String,
        confirmPassword: String,
        referral: String
    ): Boolean {
        var isValid = true
        
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Valid email is required"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }
        
        if (username.isEmpty() || username.length < 4) {
            binding.tilUsername.error = "Username must be at least 4 characters"
            isValid = false
        } else {
            binding.tilUsername.error = null
        }
        
        if (fullname.isEmpty() || fullname.length < 4) {
            binding.tilFullname.error = "Full name must be at least 4 characters"
            isValid = false
        } else {
            binding.tilFullname.error = null
        }
        
        if (password.isEmpty() || password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }
        
        if (confirmPassword != password) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }
        
        if (referral.isEmpty() || referral.length < 6) {
            binding.tilReferral.error = "Valid referral code is required"
            isValid = false
        } else {
            binding.tilReferral.error = null
        }
        
        return isValid
    }
    
    private fun performRegister(
        email: String,
        username: String,
        fullname: String,
        password: String,
        referral: String
    ) {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.register(
                    RegisterRequest(email, username, fullname, password, referral)
                )
                
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registration successful! Please login.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    val errorMsg = response.body()?.message ?: "Registration failed"
                    Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegisterActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
    }
}
