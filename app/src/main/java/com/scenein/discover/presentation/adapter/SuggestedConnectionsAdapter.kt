package com.scenein.discover.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.scenein.R
import com.scenein.databinding.ItemConnectionProfileBinding
import com.scenein.discover.data.models.SuggestedConnection
class SuggestedConnectionsAdapter(
    private val onConnectClick: (SuggestedConnection) -> Unit,
    private val onProfileClick: (SuggestedConnection) -> Unit
) : ListAdapter<SuggestedConnection, SuggestedConnectionsAdapter.ConnectionViewHolder>(SuggestedConnectionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionViewHolder {
        val binding = ItemConnectionProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConnectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int) {
        val connection = getItem(position)
        holder.bind(connection, onConnectClick, onProfileClick)    }

    inner class ConnectionViewHolder(private val binding: ItemConnectionProfileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            connection: SuggestedConnection,
            onConnectClick: (SuggestedConnection) -> Unit,
            onProfileClick: (SuggestedConnection) -> Unit
        ) {
            // ✅ Force white background for the MaterialCardView
            binding.root.setCardBackgroundColor(
                itemView.context.getColor(R.color.colorSurface)
            )
            binding.tvName.text = connection.name
            binding.tvMutualInterests.text = "${connection.mutualInterestsCount} mutual interests"
            Glide.with(itemView.context)
                .load(connection.profilePic)
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(binding.ivProfilePic)

            when {
                connection.connectionStatus == "pending" && connection.requestSentBy == "me" -> {
                    binding.btnConnect.text = "Requested"
                    binding.btnConnect.isEnabled = true
                }
                connection.connectionStatus == "pending" && connection.requestSentBy == "them" -> {
                    binding.btnConnect.text = "Accept"
                    binding.btnConnect.isEnabled = true
                }
                else -> {
                    binding.btnConnect.text = "Connect"
                    binding.btnConnect.isEnabled = true
                }
            }


            binding.btnConnect.setOnClickListener {
                onConnectClick(connection)
            }

            itemView.setOnClickListener {
                onProfileClick(connection)
            }
        }
    }

    // This class helps ListAdapter figure out what changed in the list
    class SuggestedConnectionDiffCallback : DiffUtil.ItemCallback<SuggestedConnection>() {
        override fun areItemsTheSame(oldItem: SuggestedConnection, newItem: SuggestedConnection): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: SuggestedConnection, newItem: SuggestedConnection): Boolean {
            return oldItem == newItem // Data class checks all fields
        }
    }
}