package com.tie.vibein.chat.persentation.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tie.vibein.R
import com.tie.vibein.databinding.ItemChatImageGridBinding

class ImageGridAdapter(
    private val images: List<String>,
    private val onImageClick: (position: Int) -> Unit
) : RecyclerView.Adapter<ImageGridAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(private val binding: ItemChatImageGridBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onImageClick(adapterPosition)
                }
            }
        }

        fun bind(imageUrl: String) {
            Glide.with(itemView.context)
                .load(Uri.parse(imageUrl))
                .placeholder(R.drawable.ic_placeholder)// Use Uri.parse to handle both local and network URIs
                .into(binding.ivGridImage)
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