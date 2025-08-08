package com.tie.vibein.discover.presentation.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.R
import com.tie.vibein.databinding.ActivityEventDetailBinding
import com.tie.vibein.discover.data.models.EventDetail
import com.tie.vibein.discover.data.models.ParticipantPreview
import com.tie.vibein.discover.presentation.viewmodel.DiscoverViewModel
import com.tie.vibein.profile.persentation.screen.UserProfileActivity
import com.tie.vibein.utils.EdgeToEdgeUtils
import com.tie.vibein.utils.NetworkState
import java.text.SimpleDateFormat
import java.util.*

class EventDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailBinding
    private val discoverViewModel: DiscoverViewModel by viewModels()

    private var eventId: String? = null
    private var currentUserId: String? = null
    private var source: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventId = intent.getStringExtra("event_id")
        currentUserId = SP.getString(this, SP.USER_ID)
        source = intent.getStringExtra("source")

        setupObservers()

        // Fetch the event details as soon as the activity is created.
        if (!eventId.isNullOrEmpty() && !currentUserId.isNullOrEmpty()) {
            discoverViewModel.fetchEventDetailsById(currentUserId!!, eventId!!)
        } else {
            Toast.makeText(this, "Invalid event or user session.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupObservers() {
        discoverViewModel.eventDetailsState.observe(this) { state ->
            val isLoading = state is NetworkState.Loading
            binding.shimmerLayout.isVisible = isLoading
            if (isLoading) binding.shimmerLayout.startShimmer() else binding.shimmerLayout.stopShimmer()

            // Assuming your main content container that hides during shimmer is called 'contentScrollView'
            binding.cardContainer.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE

            if (state is NetworkState.Success) {
                val eventDetail = state.data
                if (eventDetail != null) {
                    bindEventDetails(eventDetail)
                    setupClickListeners(eventDetail)
                } else {
                    Toast.makeText(this, "Failed to load event data.", Toast.LENGTH_SHORT).show()
                }
            } else if (state is NetworkState.Error) {
                Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Observers for join/unjoin actions
        discoverViewModel.joinEventState.observe(this) { state ->
            if (state == null) return@observe
            binding.btnJoinEvent.isEnabled = state !is NetworkState.Loading
            if (state is NetworkState.Success) {
                Toast.makeText(this, "Joined event successfully!", Toast.LENGTH_SHORT).show()
                refreshEventData()
            }
            if (state is NetworkState.Error) {
                Toast.makeText(this, "Failed to join: ${state.message}", Toast.LENGTH_SHORT).show()
            }
            discoverViewModel.clearJoinEventState()
        }

        discoverViewModel.unjoinEventState.observe(this) { state ->
            if (state == null) return@observe
            binding.btnJoinEvent.isEnabled = state !is NetworkState.Loading
            if (state is NetworkState.Success) {
                Toast.makeText(this, "Left event successfully", Toast.LENGTH_SHORT).show()
                refreshEventData()
            }
            if (state is NetworkState.Error) {
                Toast.makeText(this, "Failed to leave event: ${state.message}", Toast.LENGTH_SHORT).show()
            }
            discoverViewModel.clearUnjoinEventState()
        }
    }

    private fun refreshEventData() {
        // This function simply re-triggers the API call to get the latest event state
        if (!eventId.isNullOrEmpty() && !currentUserId.isNullOrEmpty()) {
            discoverViewModel.fetchEventDetailsById(currentUserId!!, eventId!!)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindEventDetails(event: EventDetail) {
        // Bind core event data
        binding.tvEventName.text = event.event_name
        binding.tvEventDescription.text = event.event_description
        binding.tvEventVenue.text = event.venueLocation
        binding.tvEventCategory.text = event.category_name
        binding.tvEventTicketPrice.text = if (event.ticketPrice == "0") "Free Entry" else "₹${event.ticketPrice}"
        binding.tvEventMode.text = event.eventDeliveryMode
        binding.tvEventDate.text = formatDate(event.event_date)
        binding.tvEventTime.text = "${event.start_time} - ${event.end_time}"
        binding.tvAgeRestriction.text = event.age_restriction
        Glide.with(this).load(event.cover_image).placeholder(R.drawable.ic_placeholder).into(binding.ivEventCoverImage)

        // Bind participant count and progress bar
        val joined = event.joined_participants
        val max = event.maximum_participants.toIntOrNull() ?: 0
        binding.tvEventSize.text = "$joined/$max participants"
        val percentage = if (max > 0) (joined * 100) / max else 0
        binding.tvEventFillPercentage.text = "$percentage% full"
        binding.progressBar.progress = percentage

        // Bind host details from the nested object
        event.host_details?.let { host ->
            binding.tvEventHostName.text = host.name ?: "VibeIn User"
            binding.tvEventHostID.text = host.user_name?.let { "$it" } ?: ""
            Glide.with(this).load(host.profile_pic).placeholder(R.drawable.ic_profile_placeholder).into(binding.ivEventHostProfilePic)
        }

        // Bind participant preview details from the nested list
        updateParticipantUi(event.participants_preview, event.joined_participants)

        // Update the state of the join button
        updateJoinButtonUI(event)
    }

    private fun updateParticipantUi(previewList: List<ParticipantPreview>, totalCount: Int) {
        val imageViews = listOf(binding.ivParticipant1, binding.ivParticipant2, binding.ivParticipant3)

        binding.tvAttendees.isVisible = totalCount > 0
        val moreCount = totalCount - 3
        binding.tvParticipantCount.text = if (moreCount > 0) "+$moreCount" else ""
        binding.tvParticipantCount.isVisible = moreCount > 0
        binding.tvSeeAll.isVisible = totalCount > 0

        for (i in imageViews.indices) {
            if (i < previewList.size) {
                imageViews[i].visibility = View.VISIBLE
                Glide.with(this).load(previewList[i].profile_pic).placeholder(R.drawable.ic_profile_placeholder).into(imageViews[i])
            } else {
                imageViews[i].visibility = View.GONE
            }
        }
    }

    private fun formatDate(inputDate: String?): String {
        if (inputDate.isNullOrBlank()) return "Date not specified"
        return try {
            val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
            inputFormat.parse(inputDate)?.let { outputFormat.format(it) } ?: inputDate
        } catch (e: Exception) { inputDate }
    }

    private fun setupClickListeners(event: EventDetail) {
        binding.toolbarBackArrow.setOnClickListener { finish() }

        binding.btnJoinEvent.setOnClickListener {
            val uid = currentUserId ?: return@setOnClickListener
            val eid = event.id
            if (!canInteractWithEvent(event.event_date)) {
                Toast.makeText(this, "This event has already passed.", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (event.has_joined) { discoverViewModel.unjoinEvent(uid, eid) }
            else { if (event.is_full) { Toast.makeText(this, "Sorry, this event is full.", Toast.LENGTH_SHORT).show() } else { discoverViewModel.joinEvent(uid, eid) } }
        }

        binding.btnViewHostProfile.setOnClickListener {
            event.host_details?.user_id?.toString()?.let { navigateToUserProfile(it) }
        }

        binding.ivParticipant1.setOnClickListener { event.participants_preview.getOrNull(0)?.user_id?.toString()?.let { navigateToUserProfile(it) } }
        binding.ivParticipant2.setOnClickListener { event.participants_preview.getOrNull(1)?.user_id?.toString()?.let { navigateToUserProfile(it) } }
        binding.ivParticipant3.setOnClickListener { event.participants_preview.getOrNull(2)?.user_id?.toString()?.let { navigateToUserProfile(it) } }

        binding.tvSeeAll.setOnClickListener {
            val eventId = event.id
            val totalCount = event.joined_participants
            ParticipantsBottomSheetFragment.newInstance(eventId, totalCount).show(supportFragmentManager, "ParticipantsBottomSheet")
        }
    }

    private fun navigateToUserProfile(userIdToView: String) {
        val intent = Intent(this, UserProfileActivity::class.java).apply {
            putExtra("user_id", userIdToView)
        }
        startActivity(intent)
    }

    private fun updateJoinButtonUI(event: EventDetail) {
        val isFull = event.is_full && !event.has_joined
        val hasPassed = !canInteractWithEvent(event.event_date)

        if (source == "profile" && event.user_id == currentUserId) {
            binding.btnJoinEvent.text = "You are the host"
            binding.btnJoinEvent.isEnabled = false
            binding.btnJoinEvent.setBackgroundResource(R.drawable.bg_button_full)
            return
        }

        when {
            hasPassed -> {
                binding.btnJoinEvent.text = "Event Has Passed"
                binding.btnJoinEvent.isEnabled = false
                binding.btnJoinEvent.setBackgroundResource(R.drawable.bg_button_full)
            }
            isFull -> {
                binding.btnJoinEvent.text = "Event Full"
                binding.btnJoinEvent.isEnabled = false
                binding.btnJoinEvent.setBackgroundResource(R.drawable.bg_button_full)
            }
            event.has_joined -> {
                binding.btnJoinEvent.text = "Leave Event"
                binding.btnJoinEvent.isEnabled = true
                binding.btnJoinEvent.setBackgroundResource(R.drawable.bg_button_unjoin)
            }
            else -> {
                binding.btnJoinEvent.text = "Join Event"
                binding.btnJoinEvent.isEnabled = true
                binding.btnJoinEvent.setBackgroundResource(R.drawable.bg_button_join)
            }
        }
    }

    private fun canInteractWithEvent(eventDate: String?): Boolean {
        if (eventDate.isNullOrBlank()) return false
        return try {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            sdf.isLenient = false
            val eventDateParsed = sdf.parse(eventDate) ?: return false
            val today = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.time
            !eventDateParsed.before(today)
        } catch (e: Exception) { false }
    }
}