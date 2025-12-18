package com.panel.keymanager.ui.keys

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.panel.keymanager.R
import com.panel.keymanager.databinding.ItemKeyBinding
import com.panel.keymanager.models.Key

class KeyAdapter(
    private var keys: MutableList<Key>,
    private var username: String,
    private val onItemClick: (Key) -> Unit,
    private val onResetClick: (Key) -> Unit,
    private val onResetDevicesClick: (Key) -> Unit,
    private val onDeleteClick: (Key) -> Unit,
    private val onCopyClick: (Key) -> Unit
) : RecyclerView.Adapter<KeyAdapter.KeyViewHolder>() {

    inner class KeyViewHolder(private val binding: ItemKeyBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(key: Key) {
            try {
                binding.apply {
                    // Key ID
                    tvKeyId.text = "#${key.id}"

                    // Key Value
                    tvKeyValue.text = key.userKey

                    // Game tag
                    tvGame.text = key.game

                    // Devices
                    tvDevices.text = "${key.devicesCount}/${key.maxDevices}"

                    // Duration
                    tvDuration.text = key.durationText ?: "${key.duration} Hours"

                    // Status indicator
                    val statusInt = key.status
                    val statusColor = if (statusInt == 1) {
                        ContextCompat.getColor(root.context, R.color.status_active)
                    } else {
                        ContextCompat.getColor(root.context, R.color.status_inactive)
                    }
                    viewStatus.setBackgroundColor(statusColor)
                    tvStatus.text = if (statusInt == 1) "Active" else "Banned"
                    tvStatus.setTextColor(statusColor)

                    // Expired date
                    if (!key.expiredDate.isNullOrEmpty()) {
                        tvExpired.text = key.expiredDate
                    } else {
                        tvExpired.text = "(not started)"
                    }

                    // Registrator
                    tvRegistrator.text = "Registrator: ${key.registrator ?: "-"}"

                    // Click listeners
                    root.setOnClickListener { onItemClick(key) }
                    btnResetDevices.setOnClickListener { onResetDevicesClick(key) }
                    btnReset.setOnClickListener { onResetClick(key) }
                    btnDelete.setOnClickListener { onDeleteClick(key) }
                    btnCopy.setOnClickListener { onCopyClick(key) }
                }
            } catch (e: Exception) {
                android.util.Log.e("KeyAdapter", "Error binding key: ${e.message}", e)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeyViewHolder {
        val binding = ItemKeyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return KeyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KeyViewHolder, position: Int) {
        holder.bind(keys[position])
    }

    override fun getItemCount(): Int {
        return keys.size
    }

    fun updateKeys(newKeys: List<Key>) {
        keys.clear()
        keys.addAll(newKeys)
        notifyDataSetChanged()
    }

    // updateUsername is no longer needed but keeping method signature if called elsewhere might be safe, 
    // but better to remove it. If MainActivity calls it, I should check.
    // MainActivity doesn't call it in the code I saw, it only initializes adapter with username.
}
