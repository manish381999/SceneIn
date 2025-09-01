package com.scenein.settings.persentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.scenein.databinding.ItemSettingBinding
import com.scenein.databinding.ItemSettingHeaderBinding
import com.scenein.databinding.ItemSettingLogoutBinding
import com.scenein.databinding.ItemSettingSpacerBinding
import com.scenein.databinding.ItemSettingToggleBinding
import com.scenein.settings.data.models.SettingsItem


class SettingsAdapter(private val items: List<SettingsItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_NAVIGABLE = 1
    private val TYPE_TOGGLE = 2
    private val TYPE_SPACER = 3
    private val TYPE_LOGOUT = 4

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SettingsItem.Header -> TYPE_HEADER
            is SettingsItem.NavigableItem -> TYPE_NAVIGABLE
            is SettingsItem.ToggleItem -> TYPE_TOGGLE
            is SettingsItem.Spacer -> TYPE_SPACER
            is SettingsItem.LogoutItem -> TYPE_LOGOUT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(ItemSettingHeaderBinding.inflate(inflater, parent, false))
            TYPE_NAVIGABLE -> NavigableViewHolder(ItemSettingBinding.inflate(inflater, parent, false))
            TYPE_TOGGLE -> ToggleViewHolder(ItemSettingToggleBinding.inflate(inflater, parent, false))
            TYPE_LOGOUT -> LogoutViewHolder(ItemSettingLogoutBinding.inflate(inflater, parent, false)) // Create this layout
            else -> SpacerViewHolder(ItemSettingSpacerBinding.inflate(inflater, parent, false)) // Create this layout
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SettingsItem.Header -> (holder as HeaderViewHolder).bind(item)
            is SettingsItem.NavigableItem -> (holder as NavigableViewHolder).bind(item)
            is SettingsItem.ToggleItem -> (holder as ToggleViewHolder).bind(item)
            is SettingsItem.LogoutItem -> (holder as LogoutViewHolder).bind(item)
            // Spacers need no binding
            else -> {

            }
        }
    }

    override fun getItemCount(): Int = items.size

    // --- ViewHolders ---
    class HeaderViewHolder(private val binding: ItemSettingHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingsItem.Header) { binding.tvHeader.text = item.title }
    }

    class NavigableViewHolder(private val binding: ItemSettingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingsItem.NavigableItem) {
            binding.tvTitle.text = item.title
            binding.tvSubtitle.text = item.subtitle
            binding.ivIcon.setImageResource(item.iconRes)
            binding.root.setOnClickListener { item.action() }
        }
    }

    class ToggleViewHolder(private val binding: ItemSettingToggleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingsItem.ToggleItem) {
            binding.tvTitle.text = item.title
            binding.tvSubtitle.text = item.subtitle
            binding.ivIcon.setImageResource(item.iconRes)
            binding.switchSetting.isChecked = item.isChecked
            binding.switchSetting.setOnCheckedChangeListener { _, isChecked ->
                item.action(isChecked)
            }
        }
    }

    class LogoutViewHolder(private val binding: ItemSettingLogoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingsItem.LogoutItem) {
            binding.btnLogout.text = "Log out ${item.username}"
            binding.btnLogout.setOnClickListener { item.action() }
        }
    }

    class SpacerViewHolder(binding: ItemSettingSpacerBinding) : RecyclerView.ViewHolder(binding.root)
}