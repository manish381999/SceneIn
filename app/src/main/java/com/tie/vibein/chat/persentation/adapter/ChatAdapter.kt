package com.tie.vibein.chat.persentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tie.vibein.R
import com.tie.vibein.chat.data.models.Message
import com.tie.vibein.chat.data.models.MessageStatus
import com.tie.vibein.databinding.ItemChatDateSeparatorBinding
import com.tie.vibein.databinding.ItemChatMessageReceivedBinding
import com.tie.vibein.databinding.ItemChatMessageSentBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val currentUserId: String,
    private val onImageClick: (message: Message, positionInMessage: Int) -> Unit
) : ListAdapter<ChatItem, RecyclerView.ViewHolder>(ChatItemDiffCallback) {

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2
    private val VIEW_TYPE_DATE_SEPARATOR = 3

    inner class SentViewHolder(private val binding: ItemChatMessageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.rvMediaGrid.layoutManager = GridLayoutManager(binding.root.context, 2)
            val spacing = binding.root.context.resources.getDimensionPixelSize(R.dimen.dp_5)
            binding.rvMediaGrid.addItemDecoration(GridSpacingItemDecoration(2, spacing, false))
        }

        fun bind(message: Message) {
            handleDisplay(message, binding)
        }
    }

    inner class ReceivedViewHolder(private val binding: ItemChatMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.rvMediaGrid.layoutManager = GridLayoutManager(binding.root.context, 2)
            val spacing = binding.root.context.resources.getDimensionPixelSize(R.dimen.dp_5)
            binding.rvMediaGrid.addItemDecoration(GridSpacingItemDecoration(2, spacing, false))
        }

        fun bind(message: Message) {
            handleDisplay(message, binding)
        }
    }

    inner class DateSeparatorViewHolder(private val binding: ItemChatDateSeparatorBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(dateItem: ChatItem.DateItem) {
            binding.tvDateHeader.text = dateItem.date
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ChatItem.MessageItem -> if (item.message.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
            is ChatItem.DateItem -> VIEW_TYPE_DATE_SEPARATOR
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT -> SentViewHolder(
                ItemChatMessageSentBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            VIEW_TYPE_RECEIVED -> ReceivedViewHolder(
                ItemChatMessageReceivedBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            VIEW_TYPE_DATE_SEPARATOR -> DateSeparatorViewHolder(
                ItemChatDateSeparatorBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is SentViewHolder -> holder.bind((item as ChatItem.MessageItem).message)
            is ReceivedViewHolder -> holder.bind((item as ChatItem.MessageItem).message)
            is DateSeparatorViewHolder -> holder.bind(item as ChatItem.DateItem)
        }
    }

    private fun handleDisplay(message: Message, binding: ItemChatMessageSentBinding) {
        if (message.messageType == "image") {
            binding.mediaBubbleLayout.visibility = View.VISIBLE
            binding.textBubbleLayout.visibility = View.GONE
            binding.tvMediaTimestamp.text = formatTimestamp(message)
            binding.ivMediaMessageStatus.visibility = View.VISIBLE
            when (message.status) {
                MessageStatus.SENDING -> binding.ivMediaMessageStatus.setImageResource(R.drawable.ic_clock)
                MessageStatus.SENT -> binding.ivMediaMessageStatus.setImageResource(R.drawable.ic_check_single)
                MessageStatus.DELIVERED -> binding.ivMediaMessageStatus.setImageResource(R.drawable.ic_check_double)
                MessageStatus.READ -> binding.ivMediaMessageStatus.setImageResource(R.drawable.ic_check_double_blue)
                MessageStatus.FAILED -> binding.ivMediaMessageStatus.setImageResource(R.drawable.ic_error)
            }
            setupImageGrid(binding.rvMediaGrid, message)
        } else {
            binding.mediaBubbleLayout.visibility = View.GONE
            binding.textBubbleLayout.visibility = View.VISIBLE
            binding.tvMessageBody.text = message.messageContent
            binding.tvTimestamp.text = formatTimestamp(message)
            binding.ivMessageStatus.visibility = View.VISIBLE // Ensure it's visible for text
            when (message.status) {
                MessageStatus.SENDING -> binding.ivMessageStatus.setImageResource(R.drawable.ic_clock)
                MessageStatus.SENT -> binding.ivMessageStatus.setImageResource(R.drawable.ic_check_single)
                MessageStatus.DELIVERED -> binding.ivMessageStatus.setImageResource(R.drawable.ic_check_double)
                MessageStatus.READ -> binding.ivMessageStatus.setImageResource(R.drawable.ic_check_double_blue)
                MessageStatus.FAILED -> binding.ivMessageStatus.setImageResource(R.drawable.ic_error)
            }
        }
    }

    private fun handleDisplay(message: Message, binding: ItemChatMessageReceivedBinding) {
        if (message.messageType == "image") {
            binding.mediaBubbleLayout.visibility = View.VISIBLE
            binding.textBubbleLayout.visibility = View.GONE
            binding.tvMediaTimestamp.text = formatTimestamp(message)
            setupImageGrid(binding.rvMediaGrid, message)
        } else {
            binding.mediaBubbleLayout.visibility = View.GONE
            binding.textBubbleLayout.visibility = View.VISIBLE
            binding.tvMessageBody.text = message.messageContent
            binding.tvTimestamp.text = formatTimestamp(message)
        }
    }

    private fun setupImageGrid(imageGrid: RecyclerView, message: Message) {
        val imageUrls = message.getImageUrls()
        (imageGrid.layoutManager as? GridLayoutManager)?.spanCount =
            if (imageUrls.size == 1) 1 else 2
        val gridAdapter = ImageGridAdapter(message) { position ->
            onImageClick(message, position)
        }
        imageGrid.adapter = gridAdapter
    }

    private fun formatTimestamp(message: Message): String {
        if (message.status == MessageStatus.SENDING) return "Sending..."
        if (message.timestamp.isNullOrEmpty()) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(message.timestamp)
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            ""
        }
    }
}


    object ChatItemDiffCallback : DiffUtil.ItemCallback<ChatItem>() {
    override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean = oldItem == newItem
}