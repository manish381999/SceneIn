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

    // --- ViewHolder for messages sent by the current user ---
    inner class SentViewHolder(private val binding: ItemChatMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            handleMediaDisplay(itemView.context, message, binding.ivMedia, binding.tvMessageBody)
            binding.tvTimestamp.text = formatTimestamp(message.timestamp, message.status)

            binding.ivMessageStatus.visibility = View.VISIBLE

            // THE DEFINITIVE FIX: The 'when' statement is now null-safe.
            // It defaults to SENT if message.status is null (which it is for messages from history).
            when (message.status ?: MessageStatus.SENT) {
                MessageStatus.SENDING -> binding.ivMessageStatus.setImageResource(R.drawable.ic_clock)
                MessageStatus.SENT -> binding.ivMessageStatus.setImageResource(R.drawable.ic_check_single)
                MessageStatus.FAILED -> binding.ivMessageStatus.setImageResource(R.drawable.ic_error)
            }
        }
    }

    // --- ViewHolder for messages received from the other user ---
    inner class ReceivedViewHolder(private val binding: ItemChatMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            handleMediaDisplay(itemView.context, message, binding.ivMedia, binding.tvMessageBody)
            // A received message never shows a 'sending' status, so we don't pass the status to the formatter.
            binding.tvTimestamp.text = formatTimestamp(message.timestamp)
        }
    }

    // Helper function to show either an image or text in the chat bubble.
    private fun handleMediaDisplay(context: Context, message: Message, imageView: ImageView, textView: TextView) {
        if (message.messageType == "image" || message.messageType == "video") {
            imageView.visibility = View.VISIBLE
            textView.visibility = View.GONE
            Glide.with(context).load(message.messageContent).placeholder(R.color.gray200).into(imageView)
        } else {
            imageView.visibility = View.GONE
            textView.visibility = View.VISIBLE
            textView.text = message.messageContent
        }
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
        val currentList = currentList.toMutableList()
        currentList.add(message)
        submitList(currentList)
    }

    fun updateMessage(sentMessageFromServer: Message) {
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it.tempId == sentMessageFromServer.tempId }
        if (index != -1) {
            currentList[index] = sentMessageFromServer
            submitList(currentList)
        }
    }

    fun markAsFailed(tempId: String) {
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it.tempId == tempId }
        if (index != -1) {
            currentList[index].status = MessageStatus.FAILED
            notifyItemChanged(index)
        }
    }

    // A robust function to format the timestamp.
    private fun formatTimestamp(timestamp: String?, status: MessageStatus? = null): String {
        // First priority: If the message is currently sending, always show 'Sending...'
        if (status == MessageStatus.SENDING) return "Sending..."

        // If not sending, format the timestamp if it exists.
        if (timestamp.isNullOrEmpty()) return ""

        try {
            // This format matches PHP's `date('c')` (ISO 8601)
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val date = inputFormat.parse(timestamp)
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            return outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            try { // Fallback for standard database 'YYYY-MM-DD HH:MM:SS' format
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(timestamp)
                val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                return outputFormat.format(date ?: Date())
            } catch (e2: Exception) { return "" }
        }
    }

    // --- DiffUtil for efficient list updates ---
    object MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            // Temp ID is the unique source of truth during the sending lifecycle
            return oldItem.tempId == newItem.tempId
        }
        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            // Also check status, so the UI updates when SENDING -> SENT or FAILED
            return oldItem.messageContent == newItem.messageContent && oldItem.status == newItem.status
        }
    }
}