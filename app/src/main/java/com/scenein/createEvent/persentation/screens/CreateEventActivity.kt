package com.scenein.createEvent.persentation.screens

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.scenein.R
import com.scenein.createEvent.persentation.adapter.CreateEventPagerAdapter
import com.scenein.createEvent.persentation.adapter.TOTAL_STEPS
import com.scenein.createEvent.persentation.view_model.CreateEventViewModel
import com.scenein.databinding.ActivityCreateEventBinding
import com.scenein.utils.EdgeToEdgeUtils
import com.scenein.utils.FileUtils
import com.scenein.utils.NetworkState
import com.scenein.utils.ThemeManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class CreateEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateEventBinding
    private val sharedViewModel: CreateEventViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)

        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.floatingActionContainer.post {
            EdgeToEdgeUtils.setUpInteractiveEdgeToEdge(
                rootView = binding.root,
                contentView = binding.viewPager,
                floatingView = binding.floatingActionContainer
            )
        }

        setupToolbar()
        setupViewPager()
        setupNavigation()
        setupBackButton()
        setupValidationObserver()
        observeCreateEventState()
    }

    private fun observeCreateEventState() {
        sharedViewModel.createEventState.observe(this) { state ->
            when (state) {
                is NetworkState.Loading -> {
                    binding.publishProgressBar.visibility = View.VISIBLE
                    binding.btnNext.text = ""
                    binding.btnNext.isEnabled = false
                }
                is NetworkState.Success -> {
                    binding.publishProgressBar.visibility = View.GONE
                    Toast.makeText(this, "Event published successfully!", Toast.LENGTH_LONG).show()
                    finish() // Close the activity on success
                }
                is NetworkState.Error -> {
                    binding.publishProgressBar.visibility = View.GONE
                    binding.btnNext.text = "Publish Event"
                    binding.btnNext.isEnabled = true
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = CreateEventPagerAdapter(this)
        binding.viewPager.isUserInputEnabled = false

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.progressIndicator.progress = position + 1
                binding.btnBack.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE
                binding.btnNext.text = if (position == TOTAL_STEPS - 1) "Publish Event" else "Next"
                updateNextButtonState(position)
            }
        })
    }

    private fun setupValidationObserver() {
        sharedViewModel.isStep1Valid.observe(this) { updateNextButtonState(0) }
        sharedViewModel.isStep2Valid.observe(this) { updateNextButtonState(1) }
        sharedViewModel.isStep3Valid.observe(this) { updateNextButtonState(2) }
        sharedViewModel.isStep4Valid.observe(this) { updateNextButtonState(3) }
    }

    private fun updateNextButtonState(position: Int) {
        if (binding.viewPager.currentItem == position) {
            binding.btnNext.isEnabled = when (position) {
                0 -> sharedViewModel.isStep1Valid.value ?: false
                1 -> sharedViewModel.isStep2Valid.value ?: false
                2 -> sharedViewModel.isStep3Valid.value ?: false
                3 -> sharedViewModel.isStep4Valid.value ?: false
                else -> false
            }
        }
    }

    private fun setupNavigation() {
        binding.btnNext.setOnClickListener {
            val currentPosition = binding.viewPager.currentItem
            if (currentPosition < TOTAL_STEPS - 1) {
                binding.viewPager.currentItem = currentPosition + 1
            } else {
                publishEvent()
            }
        }
        binding.btnBack.setOnClickListener {
            binding.viewPager.currentItem = binding.viewPager.currentItem - 1
        }
    }

    private fun setupBackButton() {
        onBackPressedDispatcher.addCallback(this) {
            val currentPosition = binding.viewPager.currentItem
            if (currentPosition > 0) {
                binding.viewPager.currentItem = currentPosition - 1
            } else {
                finish()
            }
        }
    }

    private fun publishEvent() {
        val imageUri = sharedViewModel.coverImageUri.value
        if (imageUri == null) {
            Toast.makeText(this, "Please select a cover image.", Toast.LENGTH_SHORT).show()
            binding.viewPager.currentItem = 3
            return
        }

        val venueLocationString = if (!sharedViewModel.venueName.value.isNullOrEmpty() && !sharedViewModel.city.value.isNullOrEmpty()) {
            "${sharedViewModel.venueName.value}, ${sharedViewModel.city.value}"
        } else {
            sharedViewModel.fullAddress.value ?: ""
        }

        val params = mutableMapOf<String, okhttp3.RequestBody>().apply {
            put("event_name", (sharedViewModel.eventName.value ?: "").toRequestBody("text/plain".toMediaType()))
            put("category_id", (sharedViewModel.selectedCategory.value?.first ?: "").toRequestBody("text/plain".toMediaType()))
            put("event_description", (sharedViewModel.eventDescription.value ?: "").toRequestBody("text/plain".toMediaType()))
            put("event_date", (sharedViewModel.eventDate.value ?: "").toRequestBody("text/plain".toMediaType()))
            put("start_time", (sharedViewModel.startTime.value ?: "").toRequestBody("text/plain".toMediaType()))
            put("end_time", (sharedViewModel.endTime.value ?: "").toRequestBody("text/plain".toMediaType()))
            put("eventDeliveryMode", (sharedViewModel.locationMode.value ?: "In-Person").toRequestBody("text/plain".toMediaType()))
            put("venueLocation", venueLocationString.toRequestBody("text/plain".toMediaType()))
            put("full_address", (sharedViewModel.fullAddress.value ?: "").toRequestBody("text/plain".toMediaType()))
            put("city", (sharedViewModel.city.value ?: "").toRequestBody("text/plain".toMediaType()))
            put("pincode", (sharedViewModel.pincode.value ?: "").toRequestBody("text/plain".toMediaType()))
            put("latitude", (sharedViewModel.latitude.value ?: 0.0).toString().toRequestBody("text/plain".toMediaType()))
            put("longitude", (sharedViewModel.longitude.value ?: 0.0).toString().toRequestBody("text/plain".toMediaType()))
            put("meetingLink", (sharedViewModel.meetingLink.value ?: "").toRequestBody("text/plain".toMediaType()))
            put("maximum_participants", (sharedViewModel.maxParticipants.value ?: "").toRequestBody("text/plain".toMediaType()))
            put("selectedAgeId", (sharedViewModel.selectedAge.value?.first ?: "").toRequestBody("text/plain".toMediaType()))
            put("selectedEventType", (sharedViewModel.eventType.value ?: "Free").toRequestBody("text/plain".toMediaType()))
            put("ticketPrice", (sharedViewModel.ticketPrice.value ?: "0").toRequestBody("text/plain".toMediaType()))
        }

        val cover_image = FileUtils.getMultipartBodyPartFromUri(this, imageUri, "cover_image")

        if (cover_image != null) {
            sharedViewModel.createEvent(params, cover_image)
        } else {
            Toast.makeText(this, "Could not process image.", Toast.LENGTH_SHORT).show()
        }
    }
}