package com.scenein.chat.persentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.scenein.R
import com.scenein.chat.data.model.Conversation
import com.scenein.databinding.ItemConversationBinding
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ConversationAdapter(
    private val onClick: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ViewHolder>(ConversationDiffCallback) {

    inner class ViewHolder(private val binding: ItemConversationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(conversation: Conversation) {
            binding.tvUserName.text = conversation.name
            binding.tvLastMessage.text = conversation.lastMessage

            // This will now use our new advanced formatting logic
            binding.tvTimestamp.text = formatTimestamp(conversation.timestamp)

            Glide.with(itemView.context)
                .load(conversation.profilePic)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.ivProfilePic)

            if (conversation.unreadCount > 0) {
                binding.tvUnreadCount.visibility = View.VISIBLE
                binding.tvUnreadCount.text = conversation.unreadCount.toString()
            } else {
                binding.tvUnreadCount.visibility = View.GONE
            }

            itemView.setOnClickListener { onClick(conversation) }
        }
    }

    private fun formatTimestamp(timestamp: String): String {
        if (timestamp.isBlank()) return ""

        return try {
            // 1. Parse the UTC timestamp string into an Instant object.
            val instant = Instant.parse(timestamp)

            // 2. Convert the UTC Instant to the user's local time zone.
            val userZonedDateTime = instant.atZone(ZoneId.systemDefault())

            // 3. Get the current time in the same time zone for comparison.
            val now = ZonedDateTime.now(ZoneId.systemDefault())

            // 4. Compare and format based on the date.
            when {
                // Is the message from today?
                userZonedDateTime.toLocalDate().isEqual(now.toLocalDate()) -> {
                    userZonedDateTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
                }
                // Is the message from yesterday?
                userZonedDateTime.toLocalDate().isEqual(now.toLocalDate().minusDays(1)) -> {
                    "Yesterday"
                }
                // Is the message from within the last week?
                ChronoUnit.DAYS.between(userZonedDateTime, now) < 7 -> {
                    userZonedDateTime.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())) // e.g., "Monday"
                }
                // Otherwise, show the date.
                else -> {
                    userZonedDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yy", Locale.getDefault()))
                }
            }
        } catch (e: Exception) {
            // In case of a parsing error, return a blank space.
            ""
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConversationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object ConversationDiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem.otherUserId == newItem.otherUserId
        }
        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem == newItem
        }
    }
}