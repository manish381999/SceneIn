package com.scenein.discover.presentation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.scenein.R
import com.scenein.databinding.ItemParticipantBinding
import com.scenein.discover.data.models.Participant

class ParticipantsAdapter(
    private val currentUserId: String,
    private val onProfileClick: (Participant) -> Unit,
    private val onActionClick: (Participant) -> Unit
) : ListAdapter<Participant, ParticipantsAdapter.ParticipantViewHolder>(DiffCallback) {

    class ParticipantViewHolder(private val binding: ItemParticipantBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            participant: Participant,
            currentUserId: String,
            onProfileClick: (Participant) -> Unit,
            onActionClick: (Participant) -> Unit
        ) {
            val context = itemView.context
            binding.tvFullName.text = participant.name
            binding.tvUserName.text = participant.userName?.let { "$it" }

            Glide.with(itemView.context)
                .load(participant.profilePic)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.ivProfilePic)

            // Hide the button if the user is viewing their own entry in the list
            if (participant.userId == currentUserId) {
                binding.btnAction.visibility = View.INVISIBLE
            } else {
                binding.btnAction.visibility = View.VISIBLE
                configureButton(context, binding, participant.connectionStatus)
            }

            binding.btnAction.setOnClickListener { onActionClick(participant) }
            // Assuming your item layout's root has the id `attendee_container` from your xml
            binding.attendeeContainer.setOnClickListener { onProfileClick(participant) }
        }

        private fun configureButton(context: Context, binding: ItemParticipantBinding, status: String) {
            when (status) {
                "accepted" -> {
                    binding.btnAction.text = "Message"
                    binding.btnAction.setBackgroundColor(ContextCompat.getColor(context, R.color.gray100))
                    binding.btnAction.setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
                }
                "sent_by_me" -> {
                    binding.btnAction.text = "Requested"
                    binding.btnAction.setBackgroundColor(ContextCompat.getColor(context, R.color.gray100))
                    binding.btnAction.setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
                }
                "sent_to_me" -> {
                    binding.btnAction.text = "Connect Back"
                    binding.btnAction.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    binding.btnAction.setTextColor(ContextCompat.getColor(context, R.color.white))
                }
                else -> { // "none" or any other status
                    binding.btnAction.text = "Connect"
                    binding.btnAction.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    binding.btnAction.setTextColor(ContextCompat.getColor(context, R.color.white))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val binding = ItemParticipantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParticipantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(getItem(position), currentUserId, onProfileClick, onActionClick)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Participant>() {
            override fun areItemsTheSame(oldItem: Participant, newItem: Participant) = oldItem.userId == newItem.userId
            override fun areContentsTheSame(oldItem: Participant, newItem: Participant) = oldItem == newItem
        }
    }
}