package com.tie.vibein.utils

import androidx.recyclerview.widget.DiffUtil
import com.tie.vibein.discover.data.model.EventSummary

class EventDiffCallback(
    private val oldList: List<EventSummary>,
    private val newList: List<EventSummary>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}