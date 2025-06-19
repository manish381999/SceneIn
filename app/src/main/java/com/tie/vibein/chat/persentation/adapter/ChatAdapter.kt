package com.tie.vibein.chat.persentation.adapter

import android.content.Context
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
import com.tie.vibein.databinding.ItemChatMessageReceivedBinding
import com.tie.vibein.databinding.ItemChatMessageSentBinding
import java.text.SimpleDateFormat
import java.util.*


class ChatAdapter(private val currentUserId: String) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback) {


    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    // --- Sent Message ViewHolder ---
    inner class SentViewHolder(private val binding: ItemChatMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            // Updated to use the correct views from the new layout
            handleDisplay(message, binding.mediaCard, binding.textBubbleLayout, binding.ivMedia, binding.tvMessageBody)
            binding.tvTimestamp.text = formatTimestamp(message)

            when (message.status ?: MessageStatus.SENT) {
                MessageStatus.SENDING -> binding.ivMessageStatus.setImageResource(R.drawable.ic_clock)
                MessageStatus.SENT -> binding.ivMessageStatus.setImageResource(R.drawable.ic_check_single)
                MessageStatus.FAILED -> binding.ivMessageStatus.setImageResource(R.drawable.ic_error)
            }
        }
    }

    // --- Received Message ViewHolder ---
    inner class ReceivedViewHolder(private val binding: ItemChatMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            // Updated to use the correct views from the new layout
            handleDisplay(message, binding.mediaCard, binding.textBubbleLayout, binding.ivMedia, binding.tvMessageBody)
            binding.tvTimestamp.text = formatTimestamp(message)
        }
    }

    // --- THIS IS THE UPDATED HELPER FUNCTION ---
    private fun handleDisplay(
        message: Message,
        mediaCard: View, // This is now a generic View (CardView)
        textBubble: View, // This is now a generic View (ConstraintLayout)
        imageView: ImageView,
        textView: TextView
    ) {
        if (message.messageType == "image") {
            // Show the media card, hide the text bubble
            mediaCard.visibility = View.VISIBLE
            textBubble.visibility = View.GONE
            Glide.with(imageView.context)
                .load(message.messageContent)
                .placeholder(R.drawable.ic_placeholder)
                .into(imageView)
        } else {
            // Show the text bubble, hide the media card
            mediaCard.visibility = View.GONE
            textBubble.visibility = View.VISIBLE
            textView.text = message.messageContent
        }
    }

    // This helper now safely handles the message status
    private fun formatTimestamp(message: Message): String {
        if (message.status == MessageStatus.SENDING) return "Sending..."
        if (message.timestamp.isNullOrEmpty()) return ""
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC") // Assume server time is UTC for consistency
            val date = inputFormat.parse(message.timestamp)
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault() // Format to the user's local timezone
            return outputFormat.format(date ?: Date())
        } catch (e: Exception) { return "" }
    }

    // Determines whether to use the 'sent' layout or the 'received' layout for a given item.
    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENT) {
            SentViewHolder(ItemChatMessageSentBinding.inflate(inflater, parent, false))
        } else {
            ReceivedViewHolder(ItemChatMessageReceivedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is SentViewHolder) holder.bind(message)
        else if (holder is ReceivedViewHolder) holder.bind(message)
    }

    // --- Public functions for the Activity to interact with the adapter ---

    fun addMessage(message: Message) {
        submitList(currentList + message)
    }

    fun updateSentMessage(serverMessage: Message) {
        val newList = currentList.map {
            if (it.tempId == serverMessage.tempId) serverMessage else it
        }
        submitList(newList)
    }

    fun markAsFailed(tempId: String) {
        val newList = currentList.map {
            if (it.tempId == tempId) it.copy(status = MessageStatus.FAILED) else it
        }
        submitList(newList)
    }

    object MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem.tempId == newItem.tempId
        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem.messageId == newItem.messageId && oldItem.status == newItem.status
    }
}
