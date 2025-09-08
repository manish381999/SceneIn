package com.scenein.discover.presentation.adapter

import android.content.Context
import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.scenein.R
import com.scenein.databinding.ItemDiscoverEventBinding
import com.scenein.databinding.ItemSuggestedConnectionsBinding
import com.scenein.discover.data.models.EventSummary
import com.scenein.discover.data.models.FeedItem
import com.scenein.discover.data.models.SuggestedConnection
import java.text.SimpleDateFormat
import java.util.*

private const val VIEW_TYPE_EVENT = 1
private const val VIEW_TYPE_CONNECTIONS = 2

class FeedAdapter(
    private val currentUserId: String,
    private val onEventClicked: (String) -> Unit,
    private val onJoinClicked: (String, Int) -> Unit,
    private val onUnjoinClicked: (String, Int) -> Unit,
    private val onAddBookmarkClicked: (String) -> Unit,
    private val onRemoveBookmarkClicked: (String) -> Unit,
    private val onConnectClicked: (SuggestedConnection) -> Unit,
    private val onConnectionProfileClicked: (SuggestedConnection) -> Unit
) : ListAdapter<FeedItem, RecyclerView.ViewHolder>(FeedDiffCallback()) {

    private var userLocation: Location? = null

    fun setUserLocation(location: Location) {
        this.userLocation = location
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is FeedItem.Event -> VIEW_TYPE_EVENT
            is FeedItem.Connections -> VIEW_TYPE_CONNECTIONS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_EVENT -> EventViewHolder(
                ItemDiscoverEventBinding.inflate(inflater, parent, false),
                currentUserId,
                onEventClicked,
                onJoinClicked,
                onUnjoinClicked,
                onAddBookmarkClicked,    // <-- Pass new listener
                onRemoveBookmarkClicked  // <-- Pass new listener
            )
            VIEW_TYPE_CONNECTIONS -> {
                val binding = ItemSuggestedConnectionsBinding.inflate(inflater, parent, false)
                ConnectionsViewHolder(binding, onConnectClicked, onConnectionProfileClicked)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is FeedItem.Event -> (holder as EventViewHolder).bind(item.eventSummary, userLocation)
            is FeedItem.Connections -> (holder as ConnectionsViewHolder).bind(item)
        }
    }

    class EventViewHolder(
        private val binding: ItemDiscoverEventBinding,
        private val currentUserId: String,
        private val onEventClicked: (String) -> Unit,
        private val onJoinClicked: (String, Int) -> Unit,
        private val onUnjoinClicked: (String, Int) -> Unit,
        private val onAddBookmarkClicked: (String) -> Unit,
        private val onRemoveBookmarkClicked: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val context: Context = itemView.context

        fun bind(event: EventSummary, userLocation: Location?) {
            // --- Bind Standard Info ---
            binding.tvEventName.text = event.eventName
            binding.tvCategory.text = event.categoryName
            binding.tvEventDateTime.text = formatDateTime(event.eventDate, event.startTime)
            binding.tvEventVenue.text = event.venueLocation
            Glide.with(context)
                .load(event.coverImage)
                .placeholder(R.drawable.ic_placeholder)
                .into(binding.imageBanner)

            // --- Bind Distance ---
            binding.tvEventDistance.visibility = View.GONE
            val eventLat = event.latitude?.toDoubleOrNull()
            val eventLon = event.longitude?.toDoubleOrNull()
            if (userLocation != null && eventLat != null && eventLon != null) {
                val eventLocation = Location("Event").apply {
                    latitude = eventLat
                    longitude = eventLon
                }
                val distanceInMeters = userLocation.distanceTo(eventLocation)
                val distanceInKm = distanceInMeters / 1000.0
                binding.tvEventDistance.text = "• %.1f km".format(distanceInKm)
                binding.tvEventDistance.visibility = View.VISIBLE
            }

            // --- Bind Host Details ---
            if (event.hostDetails != null) {
                binding.ivHostProfilePic.visibility = View.VISIBLE
                binding.tvHostUseName.visibility = View.VISIBLE
                binding.tvHostUseName.text = event.hostDetails.userName
                Glide.with(context)
                    .load(event.hostDetails.profilePic)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(binding.ivHostProfilePic)
            } else {
                binding.ivHostProfilePic.visibility = View.GONE
                binding.tvHostUseName.visibility = View.GONE
            }

            // --- Bind Participant Preview ---
            if (event.participantsPreview.isNotEmpty()) {
                binding.participantsContainer.visibility = View.VISIBLE
                val participantImageViews = listOf(binding.ivParticipant1, binding.ivParticipant2, binding.ivParticipant3)

                event.participantsPreview.forEachIndexed { index, participant ->
                    if (index < participantImageViews.size) {
                        participantImageViews[index].visibility = View.VISIBLE
                        Glide.with(context)
                            .load(participant.profilePic)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .into(participantImageViews[index])
                    }
                }
                for (i in event.participantsPreview.size until participantImageViews.size) {
                    participantImageViews[i].visibility = View.GONE
                }

                // Set summary correctly
                binding.tvParticipantsSummary.text = when {
                    event.joinedParticipants == 1 -> "1 person has joined"
                    event.joinedParticipants > 1 -> "${event.joinedParticipants} have joined"
                    else -> ""
                }

            } else {
                // Reset properly when no participants
                binding.participantsContainer.visibility = View.GONE
                binding.tvParticipantsSummary.text = ""   // <-- Important line
            }

            // --- THIS IS THE CORRECTED LOGIC ---
            val isHost = event.hostId == currentUserId
            if (isHost) {
                // If user is the host, show the "Host" chip and hide the action bar
                binding.tvHostChip.visibility = View.VISIBLE
                binding.actionBarContainer.visibility = View.GONE
            } else {
                // If user is a guest, hide the "Host" chip and show the action bar
                binding.tvHostChip.visibility = View.GONE
                binding.actionBarContainer.visibility = View.VISIBLE

                // Configure the Join button state
                val isEventFull = event.isFull && !event.hasJoined
                binding.btnJoin.text = when {
                    isEventFull -> "Full"
                    event.hasJoined -> "Joined"
                    else -> "Join"
                }
                binding.btnJoin.isEnabled = !isEventFull
                binding.btnJoin.isSelected = event.hasJoined

                if(isEventFull) {
                    binding.btnJoin.background.setTint(ContextCompat.getColor(context, R.color.fullButtonColor))
                    binding.btnJoin.setTextColor(ContextCompat.getColor(context, R.color.gray500))
                }

                // Set action listeners
                binding.btnJoin.setOnClickListener {
                    if (event.hasJoined) {
                        onUnjoinClicked(event.id, adapterPosition)
                    } else {
                        onJoinClicked(event.id, adapterPosition)
                    }
                }
                binding.ivActionShare.setOnClickListener {
                    // Your share logic here
                }
                binding.ivActionBookmark.setOnClickListener {
                    if (event.hasBookmarked) {
                        onRemoveBookmarkClicked(event.id)
                    } else {
                        onAddBookmarkClicked(event.id)
                    }
                }
            }

            binding.contentContainer.setOnClickListener {
                onEventClicked(event.id)
            }
        }

        private fun formatDateTime(dateStr: String?, timeStr: String?): String {
            if (dateStr == null || timeStr == null) return "Date not specified"
            return try {
                // Combine date + time into one string
                val input = "$dateStr $timeStr"

                // Parse with both date and time
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val dateTime = inputFormat.parse(input)

                // Output format with weekday, month, day, and 12-hour time
                val outputFormat = SimpleDateFormat("EEE, MMM d • hh:mm a", Locale.getDefault())
                outputFormat.format(dateTime!!)
            } catch (e: Exception) {
                "$dateStr • $timeStr"
            }
        }

    }

    class ConnectionsViewHolder(
        private val binding: ItemSuggestedConnectionsBinding,
        onConnectClicked: (SuggestedConnection) -> Unit,
        onProfileClicked: (SuggestedConnection) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val connectionsAdapter = SuggestedConnectionsAdapter(onConnectClicked, onProfileClicked)
        init {
            binding.rvConnections.layoutManager =
                LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            binding.rvConnections.adapter = connectionsAdapter
        }
        fun bind(connectionsItem: FeedItem.Connections) {
            binding.tvTitle.text = connectionsItem.title
            connectionsAdapter.submitList(connectionsItem.connections)
        }
    }

    class FeedDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
        override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
            return if (oldItem is FeedItem.Event && newItem is FeedItem.Event) {
                oldItem.eventSummary.id == newItem.eventSummary.id
            } else if (oldItem is FeedItem.Connections && newItem is FeedItem.Connections) {
                oldItem.title == newItem.title
            } else {
                oldItem.javaClass == newItem.javaClass
            }
        }
        override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
            return oldItem == newItem
        }
    }
}