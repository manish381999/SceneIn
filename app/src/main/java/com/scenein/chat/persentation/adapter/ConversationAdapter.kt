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
        if (timestamp.isBlank()) return " "

        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val messageDate = inputFormat.parse(timestamp) ?: return " "

            val calendar = Calendar.getInstance().apply { time = messageDate }
            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val aWeekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }

            when {
                isSameDay(calendar.time, today.time) -> {
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)
                }
                isSameDay(calendar.time, yesterday.time) -> {
                    "Yesterday"
                }
                calendar.after(aWeekAgo) -> {
                    SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time) // like "Monday"
                }
                else -> {
                    SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(calendar.time)
                }
            }
        } catch (e: Exception) {
            " "
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