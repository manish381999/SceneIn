package com.tie.vibein.discover.presentation.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.mikhaellopez.circularimageview.CircularImageView
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.R
import com.tie.vibein.databinding.ActivityEventDetailBinding
import com.tie.vibein.discover.data.models.GetUsersResponse
import com.tie.vibein.discover.data.models.EventDetail
import com.tie.vibein.discover.presentation.viewmodel.DiscoverViewModel
import com.tie.vibein.profile.persentation.screen.UserProfileActivity
import com.tie.vibein.utils.NetworkState
import java.text.SimpleDateFormat
import java.util.Locale

class EventDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailBinding
    private val viewModel: DiscoverViewModel by viewModels()

    private val participantUsers = mutableListOf<GetUsersResponse.User>()
    private var participantTotalCount = 0
    private var currentEvent: EventDetail? = null

    private var eventId: String? = null
    private var userId: String? = null

    // NEW: Track source
    private var source: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventId = intent.getStringExtra("event_id")
        userId = SP.getString(this, SP.USER_ID) ?: ""
        source = intent.getStringExtra("source") // "discover" or "profile"

        setupObservers()

        if (!eventId.isNullOrEmpty() && userId!!.isNotEmpty()) {
            viewModel.fetchEventDetailsById(userId!!, eventId!!)
        } else {
            Toast.makeText(this, "Invalid user or event ID", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.eventDetailsState.observe(this) { state ->
            when (state) {
                is NetworkState.Loading -> {
                    Log.d("EventDetailsState", "Loading...")
                }
                is NetworkState.Success -> {
                    val eventDetail = state.data
                    if (eventDetail != null) {
                        currentEvent = eventDetail
                        initComponents(eventDetail)
                        onClickListener(eventDetail)

                        // Fetch event host user info
                        viewModel.fetchUserDetailsById(eventDetail.user_id, 1, 1)

                        val participantsRaw = eventDetail.participants_user_id
                        if (!participantsRaw.isNullOrEmpty()) {
                            val list = participantsRaw.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            participantTotalCount = list.size
                            updateParticipantCount(participantTotalCount)
                            participantUsers.clear()
                            // Fetch details for first 3 participants only
                            list.take(3).forEach { id -> viewModel.fetchUserDetailsById(id, 1, 3) }
                            binding.tvAttendees.visibility = View.VISIBLE
                            binding.tvSeeAll.visibility = View.VISIBLE
                        } else {
                            participantTotalCount = 0
                            participantUsers.clear()
                            updateParticipantImages()
                            updateParticipantCount(0)
                            binding.tvAttendees.visibility = View.GONE
                            binding.tvSeeAll.visibility = View.GONE
                        }
                    }
                }
                is NetworkState.Error -> {
                    Log.e("EventDetailsError", "Error: ${state.message}")
                    Toast.makeText(this, "Failed to load event details: ${state.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.userDetailsState.observe(this) { state ->
            when (state) {
                is NetworkState.Success -> {
                    val users = state.data
                    if (users.isEmpty()) return@observe

                    val user = users.first()
                    if (user.user_id.toString() == currentEvent?.user_id) {
                        // This user is the host of the event
                        binding.tvEventHostName.text = user.name ?: "No Name"
                        binding.tvEventHostID.text = user.user_name ?: "No Username"
                        Glide.with(this)
                            .load(user.profile_pic)
                            .placeholder(R.drawable.ic_placeholder)
                            .into(binding.ivEventHostProfilePic)
                    } else {
                        // This user is a participant
                        if (!participantUsers.any { it.user_id == user.user_id }) {
                            participantUsers.add(user)
                            updateParticipantImages()
                        }
                    }
                }
                is NetworkState.Error -> {
                    Toast.makeText(this, "Error loading user details: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }

        viewModel.joinEventState.observe(this) { state ->
            when (state) {
                is NetworkState.Loading -> {
                    binding.btnJoinEvent.isEnabled = false
                }
                is NetworkState.Success -> {
                    binding.btnJoinEvent.isEnabled = true
                    refreshEventAndUserDetails()
                    Toast.makeText(this, "Joined event successfully", Toast.LENGTH_SHORT).show()
                }
                is NetworkState.Error -> {
                    Toast.makeText(this, "Failed to join event: ${state.message}", Toast.LENGTH_SHORT).show()
                    binding.btnJoinEvent.isEnabled = true
                }
                else -> Unit
            }
        }

        viewModel.unjoinEventState.observe(this) { state ->
            when (state) {
                is NetworkState.Loading -> {
                    binding.btnJoinEvent.isEnabled = false
                }
                is NetworkState.Success -> {
                    binding.btnJoinEvent.isEnabled = true
                    refreshEventAndUserDetails()
                    Toast.makeText(this, "Left event successfully", Toast.LENGTH_SHORT).show()
                }
                is NetworkState.Error -> {
                    Toast.makeText(this, "Failed to leave event: ${state.message}", Toast.LENGTH_SHORT).show()
                    binding.btnJoinEvent.isEnabled = true
                }
                else -> Unit
            }
        }
    }

    private fun refreshEventAndUserDetails() {
        userId?.let { uid ->
            eventId?.let { eid ->
                viewModel.fetchEventDetailsById(uid, eid)
                currentEvent?.user_id?.let { hostId ->
                    viewModel.fetchUserDetailsById(hostId, 1, 1)
                }
            }
        }
    }

    private fun updateParticipantImages() {
        val imageViews = listOf(
            findViewById<CircularImageView>(R.id.ivParticipant1),
            findViewById<CircularImageView>(R.id.ivParticipant2),
            findViewById<CircularImageView>(R.id.ivParticipant3)
        )

        for (i in imageViews.indices) {
            if (i < participantUsers.size) {
                imageViews[i].visibility = View.VISIBLE
                Glide.with(this)
                    .load(participantUsers[i].profile_pic)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(imageViews[i])
            } else {
                imageViews[i].visibility = View.GONE
            }
        }
    }

    private fun updateParticipantCount(total: Int) {
        val moreCount = total - 3
        findViewById<TextView>(R.id.tvParticipantCount).text = if (moreCount > 0) "+$moreCount" else ""
    }

    @SuppressLint("SetTextI18n")
    private fun initComponents(event: EventDetail) {
        binding.tvEventName.text = event.event_name ?: "No event name"
        binding.tvEventDescription.text = event.event_description ?: "No description"
        binding.tvEventVenue.text = event.venueLocation ?: "Venue not available"
        binding.tvEventCategory.text = event.category_name ?: "Category not specified"
        binding.tvEventTicketPrice.text =
            if (event.ticketPrice == "0" || event.ticketPrice.isNullOrEmpty()) "Free Entry" else "₹${event.ticketPrice}"
        binding.tvEventMode.text = event.eventDeliveryMode ?: "Mode N/A"
        binding.tvEventDate.text = formatDate(event.event_date ?: "")
        binding.tvEventType.text = event.eventDeliveryMode ?: ""
        binding.tvEventTime.text = "${event.start_time ?: ""}-${event.end_time ?: ""}"
        binding.tvAgeRestriction.text = event.age_restriction ?: "No age restriction"

        Glide.with(this)
            .load(event.cover_image)
            .placeholder(R.drawable.ic_placeholder)
            .into(binding.ivEventCoverImage)

        val joined = event.joined_participants ?: 0
        val max = event.maximum_participants.toIntOrNull() ?: 0
        binding.tvEventSize.text = "$joined/$max participants"
        val percentage = if (max > 0) (joined * 100) / max else 0
        binding.tvEventFillPercentage.text = "$percentage% full"
        binding.progressBar.progress = percentage

        // NEW: Use source to determine button behavior
        if (source == "profile") {
            updateJoinButtonForProfile(event)
        } else {
            updateJoinButtonUI(event)
        }
    }

    private fun formatDate(inputDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(inputDate)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            inputDate
        }
    }

    private fun onClickListener(event: EventDetail) {
        binding.toolbarBackArrow.setOnClickListener { finish() }

        binding.btnJoinEvent.setOnClickListener {
            val current = currentEvent ?: return@setOnClickListener
            val userId = SP.getString(this, SP.USER_ID) ?: return@setOnClickListener

            // Restrict join/unjoin after event date for both sources
            if (!canUnjoinEvent(current.event_date)) {
                Toast.makeText(this, "You cannot join or unjoin after the event date.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (current.has_joined) {
                viewModel.unjoinEvent(userId, current.id)
            } else {
                val joined = current.joined_participants ?: 0
                val max = current.maximum_participants.toIntOrNull() ?: 0
                if (joined >= max) {
                    Toast.makeText(this, "Event is full", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.joinEvent(userId, current.id)
            }
        }

        binding.btnViewHostProfile.setOnClickListener {
            val hostUserId = currentEvent?.user_id
            if (!hostUserId.isNullOrEmpty()) {
                val intent = Intent(this, UserProfileActivity::class.java)
                intent.putExtra("user_id", hostUserId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Host user ID not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateJoinButtonUI(event: EventDetail) {
        val isFull = event.is_full && !event.has_joined

        binding.btnJoinEvent.text = when {
            isFull -> "Full"
            event.has_joined -> "Joined"
            else -> "Join"
        }

        binding.btnJoinEvent.isEnabled = !isFull

        val bgRes = when {
            isFull -> R.drawable.bg_button_full
            event.has_joined -> R.drawable.bg_button_unjoin
            else -> R.drawable.bg_button_join
        }
        binding.btnJoinEvent.setBackgroundResource(bgRes)

    }

    // NEW: For profile source, show role and disable button
    private fun updateJoinButtonForProfile(event: EventDetail) {
        val canJoinOrUnjoin = canUnjoinEvent(event.event_date)
        val isFull = event.is_full && !event.has_joined

        binding.btnJoinEvent.text = when {
            isFull -> "Full"
            event.has_joined -> "Joined"
            else -> "Join"
        }

        // Only enable if not full and event date is not past
        binding.btnJoinEvent.isEnabled = !isFull && canJoinOrUnjoin

        val bgRes = when {
            isFull -> R.drawable.bg_button_full
            event.has_joined -> R.drawable.bg_button_unjoin
            else -> R.drawable.bg_button_join
        }
        binding.btnJoinEvent.setBackgroundResource(bgRes)
    }

    private fun canUnjoinEvent(eventDate: String?): Boolean {
        if (eventDate.isNullOrEmpty()) return false
        return try {
            val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val event = inputFormat.parse(eventDate)
            val today = inputFormat.parse(inputFormat.format(System.currentTimeMillis()))
            // Allow unjoin if today is before or equal to event date
            today <= event
        } catch (e: Exception) {
            false
        }
    }
}