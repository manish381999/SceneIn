package com.tie.vibein.createEvent.persentation.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.R
import com.tie.vibein.createEvent.persentation.view_model.CreateEventViewModel
import com.tie.vibein.databinding.FragmentCreateEventBinding
import com.tie.vibein.utils.FileUtils // Ensure this import points to your FileUtils
import com.tie.vibein.utils.NetworkState
import com.tie.vibein.utils.PincodeHelper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*

class CreateEventFragment : Fragment() {

    private var _binding: FragmentCreateEventBinding? = null
    private val binding get() = _binding!!

    // --- MODIFICATION 1: We only need to store the URI, not the file path string ---
    private var selectedImageUri: Uri? = null

    private var selectedCategoryId: String? = null
    private var eventDeliveryMode: String? = null
    private var venueLocation: String? = null
    private var meetingLink: String? = null
    private var selectedEventType: String? = null
    private var selectedAgeId: String? = null
    private var ticketPrice: String? = null

    private val viewModel: CreateEventViewModel by viewModels()

    // --- MODIFICATION 2: The image picker now only saves the URI and updates the UI ---
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                binding.imCoverImage.setImageURI(it)
                binding.ivPlaceholder.visibility = View.GONE
                binding.tvUploadCoverImage.visibility = View.GONE
                binding.btnChangeImage.visibility = View.VISIBLE
                Log.d("CreateEventFragment", "Selected Image URI: $selectedImageUri")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initComponents()
        onClickListener()
        setupObservers()
        viewModel.fetchCategories()
        viewModel.fetchAgeRestrictions()

        binding.etEventName.clearErrorOnInput()
        binding.etEventDescription.clearErrorOnInput()
        binding.etDate.clearErrorOnInput()
        binding.etStartTime.clearErrorOnInput()
        binding.etEndTime.clearErrorOnInput()
        binding.etMaxParticipants.clearErrorOnInput()
        binding.etPincode.clearErrorOnInput()
        binding.etCity.clearErrorOnInput()
        binding.etFullAddress.clearErrorOnInput()
        binding.etLink.clearErrorOnInput()
        binding.etTicketPrice.clearErrorOnInput()
    }

    private fun EditText.clearErrorOnInput() {
        this.doAfterTextChanged { this.error = null }
    }

    private fun initComponents() {
        binding.btnChangeImage.visibility = View.GONE
        binding.rgMode.check(R.id.rb_in_person)
        eventDeliveryMode = "In-Person"
        binding.venueContainer.visibility = View.VISIBLE
        binding.onlineContainer.visibility = View.GONE
        binding.rgEventType.check(R.id.rbFree)
        selectedEventType = "Free"
        binding.tvTicketPriceLabel.visibility = View.GONE
        binding.etTicketPrice.visibility = View.GONE
    }

    private fun onClickListener() {
        binding.cvCoverImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnChangeImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.etDate.setOnClickListener { showDatePicker() }

        binding.etStartTime.setOnClickListener {
            showTimePicker { time -> binding.etStartTime.setText(time) }
        }

        binding.etEndTime.setOnClickListener {
            showTimePicker { time -> binding.etEndTime.setText(time) }
        }

        binding.rgMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_in_person -> {
                    binding.venueContainer.visibility = View.VISIBLE
                    binding.onlineContainer.visibility = View.GONE
                    eventDeliveryMode = "In-Person"
                }
                R.id.rb_online -> {
                    binding.venueContainer.visibility = View.GONE
                    binding.onlineContainer.visibility = View.VISIBLE
                    eventDeliveryMode = "Online"
                }
            }
            validateModeInput()
        }

        binding.etPincode.doAfterTextChanged { text ->
            val pincode = text.toString()
            if (pincode.length == 6) {
                PincodeHelper.fetchCityFromPincode(pincode) { city ->
                    binding.etCity.setText(city ?: "")
                }
            }
        }

        binding.rgEventType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbFree -> {
                    binding.tvTicketPriceLabel.visibility = View.GONE
                    binding.etTicketPrice.visibility = View.GONE
                    selectedEventType = "Free"
                }
                R.id.rbPaid -> {
                    binding.tvTicketPriceLabel.visibility = View.VISIBLE
                    binding.etTicketPrice.visibility = View.VISIBLE
                    selectedEventType = "Paid"
                }
            }
            validateEventTypeInput()
        }

        binding.btnPublishEvent.setOnClickListener {
            if (validateModeInput() && validateEventTypeInput()) {
                val eventName = binding.etEventName.text.toString().trim()
                val eventDescription = binding.etEventDescription.text.toString().trim()
                val eventDate = binding.etDate.text.toString().trim()
                val startTime = binding.etStartTime.text.toString().trim()
                val endTime = binding.etEndTime.text.toString().trim()
                val maxParticipants = binding.etMaxParticipants.text.toString().trim()

                val pincode = binding.etPincode.text.toString().trim()
                val city = binding.etCity.text.toString().trim()
                val address = binding.etFullAddress.text.toString().trim()

                if (eventName.isEmpty()) {
                    binding.etEventName.error = "Event name is required"
                    return@setOnClickListener
                }
                if (eventDescription.isEmpty()) {
                    binding.etEventDescription.error = "Event description is required"
                    return@setOnClickListener
                }
                if (eventDate.isEmpty()) {
                    binding.etDate.error = "Event date is required"
                    return@setOnClickListener
                }
                if (startTime.isEmpty()) {
                    binding.etStartTime.error = "Start time is required"
                    return@setOnClickListener
                }
                if (endTime.isEmpty()) {
                    binding.etEndTime.error = "End time is required"
                    return@setOnClickListener
                }
                if (maxParticipants.isEmpty()) {
                    binding.etMaxParticipants.error = "Max participants is required"
                    return@setOnClickListener
                }

                if (selectedCategoryId.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (selectedAgeId.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "Please select an age restriction", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val params = mutableMapOf<String, RequestBody>().apply {
                    val userId = SP.getPreferences(requireContext(), SP.USER_ID) ?: ""
                    put("user_id", userId.toRequestBody())
                    put("event_name", eventName.toRequestBody())
                    put("category_id", selectedCategoryId!!.toRequestBody())
                    put("event_description", eventDescription.toRequestBody())
                    put("event_date", eventDate.toRequestBody())
                    put("start_time", startTime.toRequestBody())
                    put("end_time", endTime.toRequestBody())
                    put("eventDeliveryMode", eventDeliveryMode!!.toRequestBody())
                    put("venueLocation", (venueLocation ?: "").toRequestBody())
                    put("meetingLink", (meetingLink ?: "").toRequestBody())
                    put("maximum_participants", maxParticipants.toRequestBody())
                    put("selectedAgeId", selectedAgeId!!.toRequestBody())
                    put("selectedEventType", selectedEventType!!.toRequestBody())
                    put("ticketPrice", (ticketPrice ?: "0").toRequestBody())
                    put("pincode", pincode.toRequestBody())
                    put("city", city.toRequestBody())
                    put("full_address", address.toRequestBody())
                }

                // --- MODIFICATION 3: Create the MultipartBody.Part directly from the stored URI ---
                val coverImagePart: MultipartBody.Part? = selectedImageUri?.let { uri ->
                    // "cover_image" is the name your server API expects for the file part
                    FileUtils.getMultipartBodyPartFromUri(requireContext(), uri, "cover_image")
                }

                if (coverImagePart == null) {
                    Toast.makeText(requireContext(), "Please select a cover image", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                viewModel.createEvent(params, coverImagePart)
            }
        }
    }

    private fun String.toRequestBody(): RequestBody =
        this.toRequestBody("text/plain".toMediaType())

    private fun setupObservers() {
        viewModel.categoryState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is NetworkState.Loading -> {
                    Log.d("CreateEventFragment", "Loading categories...")
                }

                is NetworkState.Success -> {
                    val categories = state.data.data
                    val activeCategories = categories.filter { it.status == "1" }
                    val categoryNamesWithHint = listOf("Select Category") + activeCategories.map { it.category_name }

                    binding.spinnerCategory.setItems(categoryNamesWithHint)

                    binding.spinnerCategory.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parentView: AdapterView<*>?,
                                selectedView: View?,
                                position: Int,
                                id: Long
                            ) {
                                selectedCategoryId = if (position == 0) null
                                else activeCategories[position - 1].id
                            }

                            override fun onNothingSelected(parentView: AdapterView<*>?) {}
                        }
                }

                is NetworkState.Error -> {
                    Log.e("CreateEventFragment", "Error fetching categories: ${state.message}")
                }
            }
        }

        viewModel.ageRestrictionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is NetworkState.Loading -> {
                    Log.d("CreateEventFragment", "Loading age restrictions...")
                }

                is NetworkState.Success -> {
                    val ageList = state.data.data
                    val activeAges = ageList.filter { it.status == "1" }
                    val ageStringsWithHint = listOf("Select Age Restriction") + activeAges.map { it.age }

                    binding.spinnerAge.setItems(ageStringsWithHint)

                    binding.spinnerAge.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parentView: AdapterView<*>?,
                                selectedView: View?,
                                position: Int,
                                id: Long
                            ) {
                                selectedAgeId = if (position == 0) null else activeAges[position - 1].id
                            }

                            override fun onNothingSelected(parentView: AdapterView<*>?) {}
                        }
                }

                is NetworkState.Error -> {
                    Log.e("CreateEventFragment", "Error fetching age restrictions: ${state.message}")
                }
            }
        }

        viewModel.createEventState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is NetworkState.Loading -> {
                    Log.d("CreateEventFragment", "Creating event...")
                    // You might want to show a progress bar here
                }
                is NetworkState.Success -> {
                    Log.d("CreateEventFragment", "Event created successfully: ${state.data.message}")
                    Toast.makeText(requireContext(), "Event created successfully!", Toast.LENGTH_LONG).show()
                    // Optionally, navigate away from the fragment
                }
                is NetworkState.Error -> {
                    Log.e("CreateEventFragment", "Event creation failed: ${state.message}")
                    Toast.makeText(requireContext(), "Event creation failed: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(requireContext(), { _, y, m, d ->
            val selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d) // Changed to YYYY-MM-DD
            binding.etDate.setText(selectedDate)
        }, year, month, day)

        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }



    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePicker = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, selectedHour)
            cal.set(Calendar.MINUTE, selectedMinute)

            val formatter = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault())
            val formattedTime = formatter.format(cal.time)
            onTimeSelected(formattedTime)
        }, hour, minute, false)

        timePicker.show()
    }

    private fun validateModeInput(): Boolean {
        return when (eventDeliveryMode) {
            "In-Person" -> {
                val pincode = binding.etPincode.text.toString().trim()
                val city = binding.etCity.text.toString().trim()
                val address = binding.etFullAddress.text.toString().trim()
                var isValid = true

                if (pincode.length != 6) {
                    binding.etPincode.error = "Enter a valid 6-digit pincode"
                    isValid = false
                }
                if (city.isEmpty()) {
                    binding.etCity.error = "City is required"
                    isValid = false
                }
                if (address.isEmpty()) {
                    binding.etFullAddress.error = "Full address is required"
                    isValid = false
                }

                if (isValid) {
                    venueLocation = "$address, $city - $pincode"
                    meetingLink = null
                }
                isValid
            }
            "Online" -> {
                val link = binding.etLink.text.toString().trim()
                if (link.isEmpty()) {
                    binding.etLink.error = "Meeting link is required"
                    false
                } else {
                    meetingLink = link
                    venueLocation = null
                    true
                }
            }
            else -> false
        }
    }

    private fun validateEventTypeInput(): Boolean {
        return if (selectedEventType == "Paid") {
            ticketPrice = binding.etTicketPrice.text.toString().trim()
            if (ticketPrice!!.isEmpty()) {
                binding.etTicketPrice.error = "Ticket price is required for paid event"
                false
            } else {
                true
            }
        } else {
            ticketPrice = "0"
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}