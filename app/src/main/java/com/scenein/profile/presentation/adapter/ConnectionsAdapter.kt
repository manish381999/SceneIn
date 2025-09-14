package com.scenein.profile.presentation.adapter

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.scenein.R
import com.scenein.databinding.ItemConnectionBinding
import com.scenein.profile.data.models.ConnectionProfile


class ConnectionsAdapter(
    // --- THIS IS THE CRITICAL NEW PARAMETER ---
    private val currentUserId: String,

    private val onProfileClick: (ConnectionProfile) -> Unit,
    private val onMessageClick: (ConnectionProfile) -> Unit,
    private val onDisconnectClick: (ConnectionProfile) -> Unit

) : ListAdapter<ConnectionProfile, ConnectionsAdapter.ConnectionViewHolder>(DiffCallback) {

    class ConnectionViewHolder(private val binding: ItemConnectionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            connection: ConnectionProfile,
            currentUserId: String, // Pass the ID down to the bind function
            onProfileClick: (ConnectionProfile) -> Unit,
            onMessageClick: (ConnectionProfile) -> Unit,
            onDisconnectClick: (ConnectionProfile) -> Unit
        ) {
            val context = itemView.context

            binding.tvFullName.text = connection.name
            binding.tvUserName.text = connection.userName
            Glide.with(context)
                .load(connection.profilePic)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.ivProfilePic)

            // --- THIS IS THE DEFINITIVE VISIBILITY FIX ---
            // Check if the profile in this list item belongs to the logged-in user.
            if (connection.userId == currentUserId) {
                // If it's my own profile, hide all action buttons.
                binding.btnMainAction.isVisible = false
                binding.ivOptions.isVisible = false
            } else {
                // If it's someone else, show the buttons.
                binding.btnMainAction.isVisible = true
                binding.ivOptions.isVisible = true
                binding.btnMainAction.text = "Message"
            }
            // --- END OF FIX ---

            binding.connectionItemContainer.setOnClickListener { onProfileClick(connection) }
            binding.btnMainAction.setOnClickListener { onMessageClick(connection) }
            binding.ivOptions.setOnClickListener { view ->
                // The three-dot menu logic is unchanged and correct
                val popup = PopupMenu(context, view)
                popup.menuInflater.inflate(R.menu.connection_options_menu, popup.menu)
                val disconnectItem = popup.menu.findItem(R.id.action_disconnect)
                val spannable = SpannableString(disconnectItem.title)
                val redColor = ContextCompat.getColor(context, R.color.colorError)
                spannable.setSpan(ForegroundColorSpan(redColor), 0, spannable.length, 0)
                disconnectItem.title = spannable

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_disconnect -> { onDisconnectClick(connection); true }
                        else -> { Toast.makeText(context, "${menuItem.title} clicked (TBD)", Toast.LENGTH_SHORT).show(); true }
                    }
                }
                popup.show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionViewHolder {
        val binding = ItemConnectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConnectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int) {
        holder.bind(getItem(position), currentUserId, onProfileClick, onMessageClick, onDisconnectClick)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ConnectionProfile>() {
            override fun areItemsTheSame(oldItem: ConnectionProfile, newItem: ConnectionProfile) = oldItem.userId == newItem.userId
            override fun areContentsTheSame(oldItem: ConnectionProfile, newItem: ConnectionProfile) = oldItem == newItem
        }
    }
}