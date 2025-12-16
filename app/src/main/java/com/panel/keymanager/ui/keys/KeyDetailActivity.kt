package com.panel.keymanager.ui.keys

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.panel.keymanager.ui.BaseActivity
import androidx.lifecycle.lifecycleScope
import com.panel.keymanager.api.RetrofitClient
import com.panel.keymanager.databinding.ActivityKeyDetailBinding
import com.panel.keymanager.models.Key
import com.panel.keymanager.models.ResetKeyRequest
import com.panel.keymanager.models.UpdateKeyRequest
import com.panel.keymanager.utils.SessionManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.launch

class KeyDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityKeyDetailBinding
    private lateinit var sessionManager: SessionManager
    private var keyId: Int = 0
    private var currentKey: Key? = null
    private var userLevel: Int = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKeyDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        keyId = intent.getIntExtra("key_id", 0)
        userLevel = sessionManager.getUserLevel()

        if (keyId == 0) {
            Toast.makeText(this, "Invalid key", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupSpinners()
        loadKeyDetails()
        setupClickListeners()
        setupFieldAccess()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Key Information"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupSpinners() {
        // Status spinner - like web: Banned/Block, Active
        val statusOptions = listOf("— Select Status —", "Banned/Block", "Active")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, statusOptions)
        binding.autoCompleteStatus.setAdapter(statusAdapter)
        binding.autoCompleteStatus.setOnItemClickListener { _, _, _, _ ->
            // Logic handled on save
        }
    }

    private fun setupFieldAccess() {
        // Only Owner (level 1) can edit all fields
        // Other levels can only edit status
        if (userLevel != 1) {
            binding.etGame.isEnabled = false
            binding.etUserKey.isEnabled = false
            binding.etDuration.isEnabled = false
            binding.etMaxDevices.isEnabled = false
            binding.etRegistrator.isEnabled = false
            binding.etExpiredDate.isEnabled = false
            binding.etDevices.isEnabled = false
        }
    }

    private fun loadKeyDetails() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getKey(keyId)

                if (response.isSuccessful && response.body()?.status == "success") {
                    currentKey = response.body()?.data
                    displayKeyDetails()
                } else {
                    Toast.makeText(
                        this@KeyDetailActivity,
                        response.body()?.message ?: "Failed to load key",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@KeyDetailActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun displayKeyDetails() {
        currentKey?.let { key ->
            binding.apply {
                // Game
                etGame.setText(key.game)

                // User Key
                etUserKey.setText(key.userKey)

                // Duration
                etDuration.setText(key.duration.toString())

                // Max Devices
                etMaxDevices.setText(key.maxDevices.toString())

                // Status: 0 = Banned, 1 = Active
                val statusOptions = listOf("— Select Status —", "Banned/Block", "Active")
                val statusIndex = key.status + 1
                if (statusIndex in statusOptions.indices) {
                    autoCompleteStatus.setText(statusOptions[statusIndex], false)
                }

                // Registrator
                etRegistrator.setText(key.registrator ?: "")

                // Expired Date
                etExpiredDate.setText(key.expiredDate)

                // Devices info
                tvDevicesInfo.text = "Devices: ${key.devicesCount}/${key.maxDevices}"

                // Devices list
                if (!key.devices.isNullOrEmpty()) {
                    // Convert comma-separated to newline-separated
                    val devicesList = key.devices.split(",").joinToString("\n")
                    etDevices.setText(devicesList)
                } else {
                    etDevices.setText("")
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveChanges()
        }

        binding.btnReset.setOnClickListener {
            confirmReset()
        }

        binding.btnDelete.setOnClickListener {
            confirmDelete()
        }



        binding.tilUserKey.setEndIconOnClickListener {
            copyKeyToClipboard()
        }
    }

    private fun saveChanges() {
        val userKey = binding.etUserKey.text.toString().trim()
        val game = binding.etGame.text.toString().trim()
        val duration = binding.etDuration.text.toString().toIntOrNull()
        val maxDevices = binding.etMaxDevices.text.toString().toIntOrNull() ?: 1
        val statusText = binding.autoCompleteStatus.text.toString()
        val statusOptions = listOf("— Select Status —", "Banned/Block", "Active")
        val statusSelection = statusOptions.indexOf(statusText)
        val status = if (statusSelection > 0) statusSelection - 1 else (currentKey?.status ?: 1)
        val expiredDate = binding.etExpiredDate.text.toString().trim()
        val devices = binding.etDevices.text.toString().trim()

        if (userKey.isEmpty()) {
            binding.tilUserKey.error = "Key is required"
            return
        }

        if (duration == null || duration < 1) {
            binding.tilDuration.error = "Valid duration is required"
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val request = UpdateKeyRequest(
                    userId = sessionManager.getUserId(),
                    userKey = userKey,
                    game = game,
                    duration = duration,
                    status = status,
                    maxDevices = maxDevices,
                    expiredDate = if (expiredDate.isNotEmpty()) expiredDate else null,
                    devices = if (devices.isNotEmpty()) devices.replace("\n", ",") else null
                )

                val response = RetrofitClient.apiService.updateKey(keyId, request)

                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(
                        this@KeyDetailActivity,
                        "Key updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadKeyDetails()
                } else {
                    Toast.makeText(
                        this@KeyDetailActivity,
                        response.body()?.message ?: "Failed to update key",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@KeyDetailActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("Reset Key")
            .setMessage("Are you sure you want to reset this key?\nThis will clear all devices and expiration date.")
            .setPositiveButton("Reset") { _, _ -> resetKey() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetKey() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.resetKey(
                    keyId,
                    ResetKeyRequest(sessionManager.getUserId())
                )

                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(
                        this@KeyDetailActivity,
                        "Key reset successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadKeyDetails()
                } else {
                    Toast.makeText(
                        this@KeyDetailActivity,
                        response.body()?.message ?: "Failed to reset key",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@KeyDetailActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete Key")
            .setMessage("Are you sure you want to delete this key?\nThis action cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> deleteKey() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteKey() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteKey(
                    keyId
                )

                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(
                        this@KeyDetailActivity,
                        "Key deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@KeyDetailActivity,
                        response.body()?.message ?: "Failed to delete key",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@KeyDetailActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun copyKeyToClipboard() {
        currentKey?.let { key ->
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Key", key.userKey)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Key copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !isLoading
        binding.btnReset.isEnabled = !isLoading
        binding.btnDelete.isEnabled = !isLoading
    }
}
