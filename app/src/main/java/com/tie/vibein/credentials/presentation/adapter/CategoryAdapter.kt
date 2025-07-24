package com.tie.vibein.credentials.presentation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tie.vibein.R
import com.tie.vibein.createEvent.data.models.Category
import com.tie.vibein.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val context: Context,
    private val categories: List<Category>,
    private val onSelectionChanged: (List<String>) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private val selectedIds = mutableSetOf<String>()
    private val maxSelection = 3  // Maximum allowed selections

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val start = position * 3
        val end = (start + 3).coerceAtMost(categories.size)
        holder.bind(categories.subList(start, end))
    }

    override fun getItemCount(): Int = (categories.size + 2) / 3

    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(rowCategories: List<Category>) {
            val chipGroup = binding.chipGroup
            chipGroup.removeAllViews() // Clear any existing views

            rowCategories.forEach { category ->
                val textView = TextView(context).apply {
                    text = category.category_name
                    isClickable = true
                    isFocusable = true

                    // Adjust padding for a larger item
                    setPadding(32, 16, 32, 16) // Increased padding

                    // Set the font size for better visibility
                    textSize = 18f // Adjust font size

                    // Set initial background (unselected)
                    setBackgroundResource(R.drawable.bg_category_chip_unselected)

                    // Set the default text color
                    setTextColor(ContextCompat.getColor(context, R.color.textPrimary))

                    // Handle background change on click (selected/unselected)
                    setOnClickListener {
                        if (selectedIds.contains(category.id)) {
                            // Unselect item
                            selectedIds.remove(category.id)
                            setBackgroundResource(R.drawable.bg_category_chip_unselected)
                            setTextColor(ContextCompat.getColor(context, R.color.textPrimary)) // Unselected text color
                        } else {
                            // Check if less than 3 items are selected
                            if (selectedIds.size < maxSelection) {
                                // Select item
                                selectedIds.add(category.id)
                                setBackgroundResource(R.drawable.bg_category_chip_selected)
                                setTextColor(ContextCompat.getColor(context, R.color.textOnPrimary)) // Selected text color
                            } else {
                                // Show a message that only 3 items can be selected
                                Toast.makeText(context, "You can only select up to $maxSelection items.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        onSelectionChanged(selectedIds.toList())
                    }

                    // Optionally, add margins between the TextViews
                    val layoutParams = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(8, 8, 8, 8) // Adjust margins as needed
                    }
                    layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    this.layoutParams = layoutParams
                }

                chipGroup.addView(textView)
            }
        }
    }
}
