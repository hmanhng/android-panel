package com.panel.keymanager

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.panel.keymanager.ui.BaseActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.panel.keymanager.api.RetrofitClient
import com.panel.keymanager.databinding.ActivityMainBinding
import com.panel.keymanager.models.Key
import com.panel.keymanager.models.RefreshRequest
import com.panel.keymanager.ui.auth.LoginActivity
import com.panel.keymanager.ui.keys.CreateKeyActivity
import com.panel.keymanager.ui.keys.KeyAdapter
import com.panel.keymanager.ui.keys.KeyDetailActivity
import com.panel.keymanager.ui.profile.ProfileActivity
import com.panel.keymanager.utils.SessionManager
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var keyAdapter: KeyAdapter
    private var keysList = mutableListOf<Key>()
    private var masterKeyList = listOf<Key>() // Master list to hold all keys

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(applicationContext)

        if (!sessionManager.isLoggedIn()) {
            navigateToLogin()
            return
        }


        // Set auth token for API requests
        // Initialize RetrofitClient with SessionManager
        RetrofitClient.initialize(sessionManager)

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()

        loadKeys()
    }

    override fun onResume() {
        super.onResume()
        if (sessionManager.isLoggedIn()) {
            loadKeys()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        val username = sessionManager.getUsername()
        supportActionBar?.title = "Welcome, $username"
        supportActionBar?.subtitle = "Total Keys: 0"
    }

    private fun setupRecyclerView() {
        android.util.Log.d("MainActivity", "Setting up RecyclerView")

        keyAdapter = KeyAdapter(
            keys = keysList,
            username = sessionManager.getUsername(),
            onItemClick = { key -> showKeyDetail(key) },
            onResetClick = { key -> confirmResetKey(key) },
            onResetDevicesClick = { key -> confirmResetDevicesKey(key) },
            onDeleteClick = { key -> confirmDeleteKey(key) },
            onCopyClick = { key -> copyKeyToClipboard(key) }
        )

        binding.rvKeys.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = keyAdapter
            setHasFixedSize(true)
        }

        android.util.Log.d("MainActivity", "RecyclerView setup complete with ${keysList.size} items")
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadKeys()
        }
    }

    private fun setupFab() {
        // Initial state for delete fab
        binding.fabDeleteExpired.apply {
            visibility = View.INVISIBLE // Use invisible so we can set valid translation initially if needed, or GONE
            alpha = 0f
            translationY = 100f
            setOnClickListener {
                confirmDeleteExpiredKeys()
            }
        }

        binding.fabAddKey.setOnClickListener {
            // specific behavior: if delete fab is visible, maybe just hide it? 
            // Or just proceed to create key. Let's proceed to create key.
            if (binding.fabDeleteExpired.visibility == View.VISIBLE) {
                hideDeleteFab()
            } else {
                startActivity(Intent(this, CreateKeyActivity::class.java))
            }
        }

        binding.fabAddKey.setOnLongClickListener {
            android.util.Log.d("MainActivity", "Long click detected on Add FAB")
            // Toast.makeText(this, "Debug: Long Click", Toast.LENGTH_SHORT).show()
            showDeleteFab()
            true // Consume the event
        }

        // Hide delete fab when touching list (optional but good UX)
        binding.rvKeys.setOnTouchListener { _, _ ->
            if (binding.fabDeleteExpired.visibility == View.VISIBLE) {
                hideDeleteFab()
            }
            false
        }
    }

    private fun showDeleteFab() {
        if (binding.fabDeleteExpired.visibility != View.VISIBLE) {
            android.util.Log.d("MainActivity", "Showing Delete FAB")
            binding.fabDeleteExpired.apply {
                visibility = View.VISIBLE
                alpha = 0f
                translationY = 100f // Explicitly reset content before animating
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .start()
            }
        }
    }

    private fun hideDeleteFab() {
        if (binding.fabDeleteExpired.visibility == View.VISIBLE) {
            binding.fabDeleteExpired.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(300)
                .withEndAction {
                    binding.fabDeleteExpired.visibility = View.GONE
                }
                .start()
        }
    }

    private fun confirmDeleteExpiredKeys() {
        AlertDialog.Builder(this)
            .setTitle("Delete Expired Keys")
            .setMessage("Are you sure you want to delete ALL expired keys?")
            .setPositiveButton("Delete All") { _, _ -> deleteExpiredKeys() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteExpiredKeys() {
        lifecycleScope.launch {
            try {
                // Call the new API endpoint
                // NOTE: We need to verify if we added this to ApiService. We did.
                // RetofitClient.apiService.deleteExpiredKeys()
                // Wait, deleteKey in ApiService returns Response<ApiResponse<Any>>.
                // We added deleteExpiredKeys returning Response<ApiResponse<Nothing>> which is technically valid but Any is safer.

                // Let's assume user defined it as:
                // @DELETE("keys/expired")
                // suspend fun deleteExpiredKeys(): Response<ApiResponse<Nothing>>

                val response = RetrofitClient.apiService.deleteExpiredKeys()

                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(this@MainActivity, "Expired keys deleted", Toast.LENGTH_SHORT).show()
                    loadKeys()
                    hideDeleteFab()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        response.body()?.message ?: "Failed to delete expired keys",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadKeys() {
        showLoading(true)

        lifecycleScope.launch {
            try {

                android.util.Log.d("MainActivity", "Loading keys")

                val response = RetrofitClient.apiService.getKeys()

                android.util.Log.d("MainActivity", "Response code: ${response.code()}")
                android.util.Log.d("MainActivity", "Response body: ${response.body()}")

                if (response.isSuccessful && response.body()?.status == "success") {
                    val loadedKeys = response.body()?.data ?: emptyList()
                    android.util.Log.d("MainActivity", "Keys loaded: ${loadedKeys.size} keys")
                    supportActionBar?.subtitle = "Total: ${loadedKeys.size} keys"

                    // Update master list with fresh data
                    masterKeyList = loadedKeys

                    // Update adapter (display list) from master list
                    // masterKeyList is a distinct object, so updateKeys logic (clear + addAll) is safe
                    keyAdapter.updateKeys(masterKeyList)

                    // Force RecyclerView to update
                    binding.rvKeys.post {
                        android.util.Log.d("MainActivity", "Adapter data updated")
                    }

                    binding.tvEmpty.visibility = if (loadedKeys.isEmpty()) View.VISIBLE else View.GONE

                } else {
                    val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Failed to load keys"
                    android.util.Log.e("MainActivity", "Error: $errorMsg")

                    // If failed to load new keys, we might want to clear list or keep old one.
                    // For now, let's just show error.
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(
                            this@MainActivity,
                            errorMsg,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Exception: ${e.message}", e)
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(
                        this@MainActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                showLoading(false)
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showKeyDetail(key: Key) {
        val intent = Intent(this, KeyDetailActivity::class.java)
        intent.putExtra("key_id", key.id)
        startActivity(intent)
    }

    private fun confirmResetDevicesKey(key: Key) {
        AlertDialog.Builder(this)
            .setTitle("Reset Devices")
            .setMessage("Are you sure you want to reset devices for key '${key.userKey}'?\nThis will remove all registered devices but keep the expiration date.")
            .setPositiveButton("Reset Devices") { _, _ -> resetDevicesKey(key) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmResetKey(key: Key) {
        AlertDialog.Builder(this)
            .setTitle("Reset Key")
            .setMessage("Are you sure you want to reset key '${key.userKey}'?\nThis will clear all devices.")
            .setPositiveButton("Reset") { _, _ -> resetKey(key) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetDevicesKey(key: Key) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.resetDevices(
                    key.id,
                    com.panel.keymanager.models.ResetKeyRequest(sessionManager.getUserId())
                )

                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(this@MainActivity, "Key devices reset successfully", Toast.LENGTH_SHORT).show()
                    loadKeys()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        response.body()?.message ?: "Failed to reset devices",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetKey(key: Key) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.resetKey(
                    key.id,
                    com.panel.keymanager.models.ResetKeyRequest(sessionManager.getUserId())
                )

                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(this@MainActivity, "Key reset successfully", Toast.LENGTH_SHORT).show()
                    loadKeys()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        response.body()?.message ?: "Failed to reset key",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteKey(key: Key) {
        AlertDialog.Builder(this)
            .setTitle("Delete Key")
            .setMessage("Are you sure you want to delete key '${key.userKey}'?")
            .setPositiveButton("Delete") { _, _ -> deleteKey(key) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteKey(key: Key) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteKey(
                    key.id
                )

                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(this@MainActivity, "Key deleted successfully", Toast.LENGTH_SHORT).show()
                    loadKeys()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        response.body()?.message ?: "Failed to delete key",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as androidx.appcompat.widget.SearchView

        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterKeys(newText)
                return true
            }
        })

        return true
    }

    private fun filterKeys(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            masterKeyList // Return all keys if query is empty
        } else {
            val lowerCaseQuery = query.lowercase()
            masterKeyList.filter { key ->
                key.userKey.lowercase().contains(lowerCaseQuery) ||
                        (key.game.lowercase().contains(lowerCaseQuery)) ||
                        (key.registrator != null && key.registrator.lowercase().contains(lowerCaseQuery))
            }
        }

        // Update the adapter with the filtered list
        keyAdapter.updateKeys(filteredList)

        // Update empty view visibility
        binding.tvEmpty.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }


            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun copyKeyToClipboard(key: Key) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Key", key.userKey)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Key copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                sessionManager.logout()
                navigateToLogin()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}
