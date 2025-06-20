package com.tie.vibein.chat.persentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tie.vibein.R
import com.tie.vibein.chat.data.models.Conversation
import com.tie.vibein.databinding.ItemConversationBinding
import java.text.SimpleDateFormat
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

    // ======================================================================
    // == NEW, ADVANCED TIMESTAMP FORMATTING LOGIC ==
    // ======================================================================

    private fun formatTimestamp(timestamp: String): String {
        // Return a blank space immediately if the timestamp is invalid
        if (timestamp.isBlank()) return " "

        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val messageDateAsDate = inputFormat.parse(timestamp) ?: return " "

            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val aWeekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
            val messageDate = Calendar.getInstance().apply { time = messageDateAsDate }

            return when {
                // Case 1: If today -> show time (e.g., "10:30 AM")
                isSameDay(messageDate.time, today.time) -> {
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(messageDateAsDate)
                }
                // Case 2: If yesterday -> show "Yesterday"
                isSameDay(messageDate.time, yesterday.time) -> {
                    "Yesterday"
                }
                // Case 3: If within the last week -> show day name (e.g., "Thursday")
                messageDate.after(aWeekAgo) -> {
                    SimpleDateFormat("EEEE", Locale.getDefault()).format(messageDateAsDate)
                }
                // Case 4: If older than a week -> show date (e.g., "15/06/25")
                else -> {
                    SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(messageDateAsDate)
                }
            }
        } catch (e: Exception) {
            return " " // Return empty space on any parsing error
        }
    }

    // NEW: Helper function to compare dates accurately
    private fun isSameDay(date1: Date?, date2: Date?): Boolean {
        if (date1 == null || date2 == null) return false
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    // --- (The rest of the adapter code is unchanged) ---

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