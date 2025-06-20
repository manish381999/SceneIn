package com.tie.vibein.chat.persentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tie.vibein.R
import com.tie.vibein.chat.data.models.Message
import com.tie.vibein.chat.data.models.MessageStatus
import com.tie.vibein.databinding.ItemChatDateSeparatorBinding
import com.tie.vibein.databinding.ItemChatMessageReceivedBinding
import com.tie.vibein.databinding.ItemChatMessageSentBinding
import java.text.SimpleDateFormat
import java.util.*

// UPDATED: The adapter now works with a list of ChatItem
class ChatAdapter(private val currentUserId: String) : ListAdapter<ChatItem, RecyclerView.ViewHolder>(ChatItemDiffCallback) {

    // Define integer constants for our view types
    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2
    private val VIEW_TYPE_DATE_SEPARATOR = 3

    // --- ViewHolder for Sent Messages (Unchanged) ---
    inner class SentViewHolder(private val binding: ItemChatMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            handleDisplay(message, binding.mediaCard, binding.textBubbleLayout, binding.ivMedia, binding.tvMessageBody)
            binding.tvTimestamp.text = formatTimestamp(message)
            when (message.status ?: MessageStatus.SENT) {
                MessageStatus.SENDING -> binding.ivMessageStatus.setImageResource(R.drawable.ic_clock)
                MessageStatus.SENT -> binding.ivMessageStatus.setImageResource(R.drawable.ic_check_single)
                MessageStatus.FAILED -> binding.ivMessageStatus.setImageResource(R.drawable.ic_error)
            }
        }
    }

    // --- ViewHolder for Received Messages (Unchanged) ---
    inner class ReceivedViewHolder(private val binding: ItemChatMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            handleDisplay(message, binding.mediaCard, binding.textBubbleLayout, binding.ivMedia, binding.tvMessageBody)
            binding.tvTimestamp.text = formatTimestamp(message)
        }
    }

    // --- NEW: ViewHolder for the Date Separator ---
    inner class DateSeparatorViewHolder(private val binding: ItemChatDateSeparatorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(dateItem: ChatItem.DateItem) {
            binding.tvDateHeader.text = dateItem.date
        }
    }

    // UPDATED: This now determines the view type based on the ChatItem sealed class
    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ChatItem.MessageItem -> {
                if (item.message.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
            }
            is ChatItem.DateItem -> VIEW_TYPE_DATE_SEPARATOR
        }
    }

    // UPDATED: This creates the correct ViewHolder based on the view type
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT -> SentViewHolder(ItemChatMessageSentBinding.inflate(inflater, parent, false))
            VIEW_TYPE_RECEIVED -> ReceivedViewHolder(ItemChatMessageReceivedBinding.inflate(inflater, parent, false))
            VIEW_TYPE_DATE_SEPARATOR -> DateSeparatorViewHolder(ItemChatDateSeparatorBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    // UPDATED: This binds the data to the correct ViewHolder
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is SentViewHolder -> holder.bind((item as ChatItem.MessageItem).message)
            is ReceivedViewHolder -> holder.bind((item as ChatItem.MessageItem).message)
            is DateSeparatorViewHolder -> holder.bind(item as ChatItem.DateItem)
        }
    }

    // --- Helper functions (Unchanged) ---
    private fun handleDisplay(message: Message, mediaCard: View, textBubble: View, imageView: ImageView, textView: TextView) {
        if (message.messageType == "image") {
            mediaCard.visibility = View.VISIBLE
            textBubble.visibility = View.GONE
            Glide.with(imageView.context).load(message.messageContent).placeholder(R.drawable.ic_placeholder).into(imageView)
        } else {
            mediaCard.visibility = View.GONE
            textBubble.visibility = View.VISIBLE
            textView.text = message.messageContent
        }
    }
    private fun formatTimestamp(message: Message): String {
        if (message.status == MessageStatus.SENDING) return "Sending..."
        if (message.timestamp.isNullOrEmpty()) return ""
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(message.timestamp)
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault()
            return outputFormat.format(date ?: Date())
        } catch (e: Exception) { return "" }
    }
}

// UPDATED: The DiffUtil now compares ChatItem objects
object ChatItemDiffCallback : DiffUtil.ItemCallback<ChatItem>() {
    override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
        return oldItem == newItem
    }
}