package com.scenein.discover.presentation.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.load
import coil.request.ImageRequest
import com.scenein.R
import com.scenein.createEvent.data.models.Category
import com.scenein.databinding.ItemCategoryTabBinding

class CategoryAdapter(
    private val onCategoryClicked: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryTabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(private val binding: ItemCategoryTabBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            val context = itemView.context
            binding.tvCategoryName.text = category.category_name

            // --- THIS IS THE FIX ---
            // By using Coil for both cases, we let it manage its own lifecycle
            // and prevent race conditions from recycled views.
            if (category.id == "0") {
                // Use Coil to load the local drawable for "All"
                binding.ivCategoryIcon.load(R.drawable.ic_all_events) {
                    // Optional: Add any specific transformations or settings here
                }
            } else {
                // Use Coil to load the image from the URL for all other categories
                binding.ivCategoryIcon.load(category.category_image) {
                    decoderFactory(SvgDecoder.Factory()) // Ensure SVG support
                    placeholder(R.drawable.ic_placeholder)
                    error(R.drawable.ic_placeholder)
                }
            }

            // This logic to handle highlighting remains the same
            if (adapterPosition == selectedPosition) {
                binding.indicator.visibility = View.VISIBLE
                binding.tvCategoryName.setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
                binding.ivCategoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.textPrimary))
                binding.tvCategoryName.typeface = Typeface.DEFAULT_BOLD
            } else {
                binding.indicator.visibility = View.INVISIBLE
                binding.tvCategoryName.setTextColor(ContextCompat.getColor(context, R.color.textSecondary))
                binding.ivCategoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.textSecondary))
                binding.tvCategoryName.typeface = Typeface.DEFAULT
            }

            binding.root.setOnClickListener {
                if (adapterPosition != selectedPosition) {
                    onCategoryClicked(category)
                    val previouslySelected = selectedPosition
                    selectedPosition = adapterPosition
                    notifyItemChanged(previouslySelected)
                    notifyItemChanged(selectedPosition)
                }
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}
