package com.panel.keymanager.ui.keys

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.panel.keymanager.ui.BaseActivity
import androidx.lifecycle.lifecycleScope
import com.panel.keymanager.api.RetrofitClient
import com.panel.keymanager.databinding.ActivityCreateKeyBinding
import com.panel.keymanager.models.CreateKeyRequest
import com.panel.keymanager.models.Duration
import com.panel.keymanager.utils.SessionManager
import kotlinx.coroutines.launch

class CreateKeyActivity : BaseActivity() {

    private lateinit var binding: ActivityCreateKeyBinding
    private lateinit var sessionManager: SessionManager
    private var durations = listOf<Duration>()
    private var selectedDuration: Duration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateKeyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupToolbar()
        loadDurations()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Generate Key"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Show current balance
        val user = sessionManager.getUser()
        val formattedSaldo = String.format("%.2f", user?.saldo ?: 0.0)
        binding.tvBalance.text = "Balance: $$formattedSaldo"
    }

    private fun loadDurations() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getDurations()
                if (response.isSuccessful && response.body()?.status == "success") {
                    durations = response.body()?.data ?: getDefaultDurations()
                    // Assuming API returns data that can be mapped to local Duration class
                    durations = response.body()?.data?.map {
                        Duration(it.hours, it.label, it.price.toDouble())
                    } ?: getDefaultDurations()
                } else {
                    durations = getDefaultDurations()
                }
                setupDurationSpinner()
            } catch (e: Exception) {
                durations = getDefaultDurations()
                setupDurationSpinner()
            }
        }
    }

    private fun getDefaultDurations(): List<Duration> {
        return listOf(
            Duration(1, "1 Hour", 0.5),
            Duration(24, "1 Day", 1.0),
            Duration(168, "7 Days", 7.0),
            Duration(720, "30 Days", 20.0)
        )
    }

    private fun setupDurationSpinner() {
        // Prepare Duration Data
        val durationLabels = durations.map { "${it.label} - $${it.price}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, durationLabels)

        // Setup AutoCompleteTextView
        binding.autoCompleteDuration.setAdapter(adapter)
        binding.autoCompleteDuration.setOnItemClickListener { _, _, position, _ ->
            selectedDuration = durations[position]
            updatePricePreview()
        }

        // Select first item by default if available
        if (durations.isNotEmpty()) {
            binding.autoCompleteDuration.setText(durationLabels[0], false)
            selectedDuration = durations[0]
            updatePricePreview()
        }
    }

    private fun setupListeners() {
        // Quantity spinner
        val quantities = (1..10).toList()
        val quantityLabels = quantities.map { it.toString() }
        val quantityAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, quantityLabels)

        binding.autoCompleteQuantity.setAdapter(quantityAdapter)
        binding.autoCompleteQuantity.setOnItemClickListener { _, _, _, _ ->
            updatePricePreview()
        }
        // Set default
        binding.autoCompleteQuantity.setText(quantities[0].toString(), false)


        // Max devices spinner
        val devices = (1..5).toList()
        val deviceLabels = devices.map { it.toString() }
        val devicesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceLabels)

        binding.autoCompleteMaxDevices.setAdapter(devicesAdapter)
        binding.autoCompleteMaxDevices.setOnItemClickListener { _, _, _, _ ->
            updatePricePreview()
        }
        // Set default
        binding.autoCompleteMaxDevices.setText(devices[0].toString(), false)

        // Custom key checkbox
        binding.cbCustomKey.setOnCheckedChangeListener { _, isChecked ->
            binding.tilCustomKey.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) {
                // When custom key is checked, force quantity to 1 and disable input logic if needed
                binding.autoCompleteQuantity.setText("1", false)
                binding.tilQuantity.isEnabled = false // Disable the layout
            } else {
                binding.tilQuantity.isEnabled = true
            }
            updatePricePreview()
        }

        // Generate button
        binding.btnGenerate.setOnClickListener {
            generateKey()
        }
    }

    private fun updatePricePreview() {
        val duration = selectedDuration ?: return

        // Get values from text fields since getSelectedItemPosition doesn't exist for AutoCompleteTextView
        val quantityText = binding.autoCompleteQuantity.text.toString()
        val quantity = quantityText.toIntOrNull() ?: 1

        val maxDevicesText = binding.autoCompleteMaxDevices.text.toString()
        val maxDevices = maxDevicesText.toIntOrNull() ?: 1

        val totalPrice = duration.price * quantity * maxDevices
        binding.tvPricePreview.text = "Price: $${String.format("%.2f", totalPrice)}"
    }

    private fun generateKey() {
        val duration = selectedDuration
        if (duration == null) {
            Toast.makeText(this, "Please select duration", Toast.LENGTH_SHORT).show()
            return
        }

        // Get values from AutoCompleteTextViews
        val quantityText = binding.autoCompleteQuantity.text.toString()
        val quantity = if (binding.cbCustomKey.isChecked) 1 else (quantityText.toIntOrNull() ?: 1)

        val maxDevicesText = binding.autoCompleteMaxDevices.text.toString()
        val maxDevices = maxDevicesText.toIntOrNull() ?: 1

        val customKey = if (binding.cbCustomKey.isChecked) {
            binding.etCustomKey.text.toString().trim()
        } else null

        if (binding.cbCustomKey.isChecked && customKey.isNullOrEmpty()) {
            binding.tilCustomKey.error = "Please enter custom key"
            return
        } else {
            binding.tilCustomKey.error = null
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnGenerate.isEnabled = false

        val request = CreateKeyRequest(
            userId = sessionManager.getUserId(),
            game = "PUBG", // Default
            duration = duration.hours,
            quantity = quantity,
            maxDevices = maxDevices, // Changed from max_devices
            customKey = customKey // Changed from custom_key
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.createKey(request)
                if (response.isSuccessful && response.body()?.status == "success") {
                    val data = response.body()?.data
                    // Assume api returns price_charged and new_balance
                    // Assigning to _ to suppress unused variable warning, or just removing if not needed.
                    // val priceCharged = data?.priceCharged
                    val newBalance = data?.newBalance // Need to check if api returns this

                    if (newBalance != null) {
                        binding.tvBalance.text = "Balance: $${String.format("%.2f", newBalance)}"
                        sessionManager.updateSaldo(newBalance) // Update session manager with new balance
                    }

                    val keys = data?.keys ?: emptyList()
                    val keyString = keys.joinToString("\n") { k ->
                        val userKey = k.userKey
                        "$userKey"
                    }

                    // Copy to clipboard
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Generated Keys", keyString)
                    clipboard.setPrimaryClip(clip)

                    Toast.makeText(this@CreateKeyActivity, "Keys copied to clipboard!", Toast.LENGTH_LONG).show()
                    finish()

                } else {
                    Toast.makeText(this@CreateKeyActivity, "Error: ${response.body()?.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CreateKeyActivity, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnGenerate.isEnabled = true
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnGenerate.isEnabled = !isLoading
    }
}
