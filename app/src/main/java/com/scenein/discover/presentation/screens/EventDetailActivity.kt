package com.scenein.discover.presentation.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.scenein.R
import com.scenein.databinding.ActivityEventDetailBinding
import com.scenein.discover.data.models.EventDetail
import com.scenein.discover.data.models.ParticipantPreview
import com.scenein.discover.presentation.view_model.DiscoverViewModel
import com.scenein.profile.persentation.screen.UserProfileActivity
import com.scenein.utils.DateTimeUtils
import com.scenein.utils.EdgeToEdgeUtils
import com.scenein.utils.NetworkState
import com.scenein.utils.SP
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.*

class EventDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityEventDetailBinding
    private val discoverViewModel: DiscoverViewModel by viewModels()

    private var eventId: String? = null
    private var currentUserId: String? = null
    private var source: String? = null

    private var googleMap: GoogleMap? = null
    private var eventLatLng: LatLng? = null
    private var eventAddressForNav: String? = null
    private var hasJoinStatusChanged = false
    private var currentEvent: EventDetail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventId = intent.getStringExtra("event_id")
        currentUserId = SP.getString(this, SP.USER_ID)
        source = intent.getStringExtra("source")

        setupObservers()

        if (!eventId.isNullOrEmpty()) {
            discoverViewModel.fetchEventDetailsById(eventId!!)
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
            binding.cardContainer.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE

            if (state is NetworkState.Success) {
                state.data?.let {
                    currentEvent = it // Keep track of the latest event details
                    bindEventDetails(it)
                    setupClickListeners(it)
                } ?: Toast.makeText(this, "Failed to load event data.", Toast.LENGTH_SHORT).show()
            } else if (state is NetworkState.Error) {
                Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
            }
        }

        discoverViewModel.joinEventState.observe(this) { state ->
            if (state == null) return@observe
            binding.btnJoinEvent.isEnabled = state !is NetworkState.Loading
            if (state is NetworkState.Success) {
                hasJoinStatusChanged = true
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
                hasJoinStatusChanged = true
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
        if (!eventId.isNullOrEmpty()) {
            discoverViewModel.fetchEventDetailsById(eventId!!)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindEventDetails(event: EventDetail) {
        binding.tvEventName.text = event.eventName
        binding.tvEventDescription.text = event.eventDescription
        binding.tvEventVenue.text = event.venueLocation
        binding.tvEventCategory.text = event.categoryName
        binding.tvEventType.text = event.selectedEventType
        binding.tvEventTicketPrice.text = if (event.ticketPrice == "0") "Free Entry" else "₹${event.ticketPrice}"
        binding.tvEventMode.text = event.eventDeliveryMode

        binding.tvEventDate.text = DateTimeUtils.formatEventDate(event.eventDate)
        binding.tvEventTime.text = DateTimeUtils.formatEventTimeRange(event.startTime, event.endTime)
        binding.tvAgeRestriction.text = event.ageRestriction

        Glide.with(this).load(event.coverImage).placeholder(R.drawable.ic_placeholder).into(binding.ivEventCoverImage)

        val lat = event.latitude?.toDoubleOrNull()
        val lon = event.longitude?.toDoubleOrNull()
        if (lat != null && lon != null) {
            this.eventLatLng = LatLng(lat, lon)
            this.eventAddressForNav = event.fullAddress ?: event.venueLocation
            val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }

        val joined = event.joinedParticipants
        val max = event.maximumParticipants.toIntOrNull() ?: 0
        binding.tvEventSize.text = "$joined/$max participants"
        val percentage = if (max > 0) (joined * 100) / max else 0
        binding.tvEventFillPercentage.text = "$percentage% full"
        binding.progressBar.progress = percentage

        event.hostDetails?.let { host ->
            binding.tvEventHostName.text = host.name ?: "VibeIn User"
            binding.tvEventHostID.text = host.userName?.let { "@$it" } ?: ""
            Glide.with(this).load(host.profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.ivEventHostProfilePic)
        }

        updateParticipantUi(event.participantsPreview, event.joinedParticipants)
        updateJoinButtonUI(event)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isMapToolbarEnabled = false
        googleMap?.uiSettings?.isScrollGesturesEnabled = false
        googleMap?.uiSettings?.isZoomGesturesEnabled = false

        eventLatLng?.let { latLng ->
            binding.mapCardContainer.visibility = View.VISIBLE
            googleMap?.clear()
            googleMap?.addMarker(MarkerOptions().position(latLng))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
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
                Glide.with(this).load(previewList[i].profilePic).placeholder(R.drawable.ic_profile_placeholder).into(imageViews[i])
            } else {
                imageViews[i].visibility = View.GONE
            }
        }
    }

    private fun formatUtcDateTime(dateTimeStr: String?, pattern: String): String {
        if (dateTimeStr.isNullOrBlank()) return "N/A"
        return try {
            val instant = Instant.parse(dateTimeStr)
            val userZoneId = ZoneId.systemDefault()
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            instant.atZone(userZoneId).format(formatter)
        } catch (e: Exception) {
            dateTimeStr // Fallback to the raw string on error
        }
    }


    private fun setupClickListeners(event: EventDetail) {
        binding.toolbarBackArrow.setOnClickListener {  finishWithResult() }

        binding.btnNavigate.setOnClickListener {
            eventLatLng?.let { latLng ->
                val gmmIntentUri = Uri.parse("geo:${latLng.latitude},${latLng.longitude}?q=${Uri.encode(eventAddressForNav)}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    Toast.makeText(this, "Google Maps is not installed.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnJoinEvent.setOnClickListener {
            val eid = event.id
            // Use the new function with the START time here as well.
            if (!isEventJoinable(event.startTime)) {
                Toast.makeText(this, "This event has already started.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (event.hasJoined) {
                discoverViewModel.unjoinEvent(eid)
            } else {
                if (event.isFull) {
                    Toast.makeText(this, "Sorry, this event is full.", Toast.LENGTH_SHORT).show()
                } else {
                    discoverViewModel.joinEvent(eid)
                }
            }
        }

        binding.btnViewHostProfile.setOnClickListener {
            event.hostDetails?.userId?.toString()?.let { navigateToUserProfile(it) }
        }

        binding.ivParticipant1.setOnClickListener {
            event.participantsPreview.getOrNull(0)?.userId?.toString()?.let { navigateToUserProfile(it) }
        }
        binding.ivParticipant2.setOnClickListener {
            event.participantsPreview.getOrNull(1)?.userId?.toString()?.let { navigateToUserProfile(it) }
        }
        binding.ivParticipant3.setOnClickListener {
            event.participantsPreview.getOrNull(2)?.userId?.toString()?.let { navigateToUserProfile(it) }
        }

        binding.tvSeeAll.setOnClickListener {
            val eventId = event.id
            val totalCount = event.joinedParticipants
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
        val isFull = event.isFull && !event.hasJoined
        // Use the new function with the event's START time.
        // `hasStarted` is true if the event is no longer joinable.
        val hasStarted = !isEventJoinable(event.startTime)

        if (source == "profile" && event.userId == currentUserId) {
            binding.btnJoinEvent.text = "You are the host"
            binding.btnJoinEvent.isEnabled = false
            binding.btnJoinEvent.setBackgroundResource(R.drawable.bg_button_full)
            return
        }

        if ((source == "profile" || source == "discover") && event.userId == currentUserId) {
            binding.btnJoinEvent.text = "You are the host"
            binding.btnJoinEvent.isEnabled = false
            binding.btnJoinEvent.setBackgroundResource(R.drawable.bg_button_full)
            return
        }


        when {
            // The condition is now based on whether the event has started.
            hasStarted -> {
                binding.btnJoinEvent.text = "Event Has Started" // Or "Event In Progress"
                binding.btnJoinEvent.isEnabled = false
                binding.btnJoinEvent.setBackgroundResource(R.drawable.bg_button_full)
            }
            isFull -> {
                binding.btnJoinEvent.text = "Event Full"
                binding.btnJoinEvent.isEnabled = false
                binding.btnJoinEvent.setBackgroundResource(R.drawable.bg_button_full)
            }
            event.hasJoined -> {
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

    private fun isEventJoinable(startTimeStr: String?): Boolean {
        if (startTimeStr.isNullOrBlank()) return false
        return try {
            val eventStartInstant = Instant.parse(startTimeStr)
            val now = Instant.now()
            // The event is joinable only if the current moment is BEFORE the start time.
            now.isBefore(eventStartInstant)
        } catch (e: Exception) {
            false
        }
    }


    private fun finishWithResult() {
        if (hasJoinStatusChanged && currentEvent != null) {
            val resultIntent = Intent().apply {
                putExtra("event_id_result", currentEvent!!.id)

            }
            setResult(Activity.RESULT_OK, resultIntent)
        } else {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }

    // And also override the default back press behavior
    @Deprecated("Deprecated in ComponentActivity")
    override fun onBackPressed() {
        finishWithResult()
        // Note: We don't call super.onBackPressed() because finishWithResult() calls finish()
    }
}