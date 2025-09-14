package com.scenein.chat.presentation.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.scenein.R
import com.scenein.chat.data.model.Message
import com.scenein.chat.data.model.MessageStatus
import com.scenein.databinding.ItemChatImageGridBinding

// MODIFICATION 1: Change the constructor to accept the full Message object
class ImageGridAdapter(
    private val message: Message,
    private val onImageClick: (position: Int) -> Unit
) : RecyclerView.Adapter<ImageGridAdapter.ImageViewHolder>() {

    // The list of images is now derived from the message object
    private val images: List<String> = message.getImageUrls()

    inner class ImageViewHolder(private val binding: ItemChatImageGridBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                // Don't allow clicking while the image is uploading
                if (adapterPosition != RecyclerView.NO_POSITION && message.status != MessageStatus.SENDING) {
                    onImageClick(adapterPosition)
                }
            }
        }

        // MODIFICATION 2: Update the bind function to show/hide the upload UI
        fun bind(imageUrl: String) {
            // Load the image as before
            Glide.with(itemView.context)
                .load(Uri.parse(imageUrl))
                .placeholder(R.drawable.ic_placeholder)
                .into(binding.ivGridImage)

            // Check the message status and update the UI accordingly
            if (message.status == MessageStatus.SENDING) {
                binding.uploadingOverlay.visibility = View.VISIBLE
                binding.uploadingProgress.visibility = View.VISIBLE
            } else {
                binding.uploadingOverlay.visibility = View.GONE
                binding.uploadingProgress.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemChatImageGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size
}