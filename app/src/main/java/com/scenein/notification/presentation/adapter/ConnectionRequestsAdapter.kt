package com.scenein.notification.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.scenein.R
import com.scenein.databinding.ItemConnectionRequestBinding
import com.scenein.notification.data.models.ConnectionRequest

class ConnectionRequestsAdapter(
    private val onProfileClick: (ConnectionRequest) -> Unit,
    private val onConfirm: (ConnectionRequest) -> Unit,
    private val onDelete: (ConnectionRequest) -> Unit
) : ListAdapter<ConnectionRequest, ConnectionRequestsAdapter.RequestViewHolder>(DiffCallback) {

    class RequestViewHolder(private val binding: ItemConnectionRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            request: ConnectionRequest,
            onProfileClick: (ConnectionRequest) -> Unit,
            onConfirm: (ConnectionRequest) -> Unit,
            onDelete: (ConnectionRequest) -> Unit
        ) {
            // Populate the views with the request data
            binding.tvFullName.text = request.name
            binding.tvUserName.text = request.userName ?: request.name // Fallback to full name if username is null

            Glide.with(itemView.context)
                .load(request.profilePic)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.ivProfilePic)

            // --- THIS IS THE NEW LOGIC ---
            // Set up individual click listeners for each action
            binding.btnConfirm.setOnClickListener { onConfirm(request) }
            binding.btnDelete.setOnClickListener { onDelete(request) }

            // Make the user info part of the card clickable to view their profile
            // This is the LinearLayout containing the profile pic and names
            binding.userInfoContainer.setOnClickListener { onProfileClick(request) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemConnectionRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(getItem(position), onProfileClick, onConfirm, onDelete)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ConnectionRequest>() {
            override fun areItemsTheSame(oldItem: ConnectionRequest, newItem: ConnectionRequest): Boolean {
                return oldItem.connectionId == newItem.connectionId
            }
            override fun areContentsTheSame(oldItem: ConnectionRequest, newItem: ConnectionRequest): Boolean {
                return oldItem == newItem
            }
        }
    }
}