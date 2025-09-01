package com.scenein.credentials.presentation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.scenein.R
import com.scenein.createEvent.data.models.Category
import com.scenein.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val context: Context,
    private val categories: List<Category>,
    // Pre-select items based on the user's saved preference IDs
    initialSelectedIds: MutableList<String>,
    // A listener to report back the new list of selected IDs
    private val onSelectionChanged: (List<String>) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    // A MutableSet is the most efficient way to track selected items.
    private val selectedIds: MutableSet<String> = initialSelectedIds.toMutableSet()

    // The maximum number of items the user is allowed to select.
    private val SELECTION_LIMIT = 4

    inner class CategoryViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun getItemCount(): Int = categories.size

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        val isCurrentlySelected = selectedIds.contains(category.id)

        holder.binding.tvCategoryName.text = category.category_name
        updateSelectionState(holder, isCurrentlySelected) // Set initial state

        holder.itemView.setOnClickListener {
            // --- THIS IS THE DEFINITIVE, CORRECT SELECTION LOGIC ---

            // Check if the item is already selected
            if (selectedIds.contains(category.id)) {
                // If it is, deselect it.
                selectedIds.remove(category.id)
                updateSelectionState(holder, false)
            } else {
                // If it's not selected, check if we have reached the limit.
                if (selectedIds.size < SELECTION_LIMIT) {
                    // If we are under the limit, select it.
                    selectedIds.add(category.id)
                    updateSelectionState(holder, true)
                } else {
                    // If we are at the limit, show a message and do not allow selection.
                    Toast.makeText(context, "You can only select up to $SELECTION_LIMIT interests.", Toast.LENGTH_SHORT).show()
                }
            }

            // After any change, report the new full list of selected IDs back to the activity.
            onSelectionChanged(selectedIds.toList())
        }
    }

    private fun updateSelectionState(holder: CategoryViewHolder, isSelected: Boolean) {
        if (isSelected) {
            holder.binding.tvCategoryName.setBackgroundResource(R.drawable.bg_category_selected)
            holder.binding.tvCategoryName.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            holder.binding.tvCategoryName.setBackgroundResource(R.drawable.bg_category_unselected)
            holder.binding.tvCategoryName.setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
        }
    }
}