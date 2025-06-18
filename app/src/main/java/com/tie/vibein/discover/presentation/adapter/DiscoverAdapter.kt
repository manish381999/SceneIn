package com.tie.vibein.discover.presentation.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.R
import com.tie.vibein.databinding.ItemDiscoverEventBinding
import com.tie.vibein.discover.data.model.EventSummary
import com.tie.vibein.discover.presentation.screens.EventDetailActivity
import com.tie.vibein.discover.presentation.viewmodel.DiscoverViewModel
import com.tie.vibein.utils.EventDiffCallback
import java.text.SimpleDateFormat
import java.util.Locale

class DiscoverAdapter(
    private var events: MutableList<EventSummary>,
    private val viewModel: DiscoverViewModel,
    private val context: Context
) : RecyclerView.Adapter<DiscoverAdapter.DiscoverViewHolder>() {

    inner class DiscoverViewHolder(val binding: ItemDiscoverEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: EventSummary) {
            binding.tvEventName.text = event.event_name
            binding.tvCategory.text = event.category_name
            binding.tvEventVenue.text = event.venueLocation ?: "Unknown venue"
            binding.tvEventDateTime.text = formatDateTime(event.event_date, event.start_time)
            binding.tvEventJoined.text = "${event.joined_participants ?: "0"} Joined"

            Glide.with(binding.imageBanner.context)
                .load(event.cover_image)
                .into(binding.imageBanner)

            val isEventFull = isEventFull(event)
            val isUserJoined = event.has_joined

            // Button Text Logic
            binding.btnJoin.text = when {
                isEventFull && !isUserJoined -> "Full"
                isUserJoined -> "Joined"
                else -> "Join"
            }

            // Button Enable/Disable Logic
            binding.btnJoin.isEnabled = !(isEventFull && !isUserJoined)

            // Background Resource
            val backgroundRes = when {
                isEventFull && !isUserJoined -> R.drawable.bg_button_full
                isUserJoined -> R.drawable.bg_button_unjoin
                else -> R.drawable.bg_button_join
            }

            binding.btnJoin.setBackgroundResource(backgroundRes)

            binding.btnJoin.setOnClickListener {
                val userId = SP.getPreferences(context, SP.USER_ID) ?: ""
                val currentPosition = adapterPosition

                if (currentPosition != RecyclerView.NO_POSITION) {
                    val currentEvent = events[currentPosition]
                    val joinedCount = currentEvent.joined_participants ?: 0
                    val maxParticipants = currentEvent.maximum_participants.toIntOrNull() ?: 0

                    val updatedEvent = if (currentEvent.has_joined) {
                        viewModel.unjoinEvent(userId, currentEvent.id)
                        currentEvent.copy(
                            has_joined = false,
                            joined_participants = joinedCount - 1,
                            is_full = false
                        )
                    } else {
                        viewModel.joinEvent(userId, currentEvent.id)
                        val newJoinedCount = joinedCount + 1
                        currentEvent.copy(
                            has_joined = true,
                            joined_participants = newJoinedCount,
                            is_full = newJoinedCount >= maxParticipants
                        )
                    }

                    events[currentPosition] = updatedEvent
                    notifyItemChanged(currentPosition)
                }
            }


            // Navigate to EventDetailActivity
            binding.cvEvent.setOnClickListener {
                val intent = Intent(context, EventDetailActivity::class.java)
                intent.putExtra("event_id", event.id)
                intent.putExtra("source", "discover")
                context.startActivity(intent)
            }

        }

        private fun formatDateTime(dateStr: String, timeStr: String): String {
            return try {
                val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateStr)
                val outputFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                "${outputFormat.format(date!!)} • $timeStr"
            } catch (e: Exception) {
                "$dateStr • $timeStr"
            }
        }


        private fun isEventFull(event: EventSummary): Boolean {
            val max = event.maximum_participants.toIntOrNull() ?: 0
            val joined = event.joined_participants ?: 0
            return joined >= max
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscoverViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemDiscoverEventBinding.inflate(inflater, parent, false)
        return DiscoverViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiscoverViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    fun updateEvents(newEvents: List<EventSummary>) {
        val diffCallback = EventDiffCallback(events, newEvents)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        events.clear()
        events.addAll(newEvents)
        diffResult.dispatchUpdatesTo(this)
    }
}
