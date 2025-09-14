package com.scenein.createEvent.presentation.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.scenein.BuildConfig
import com.scenein.R
import com.scenein.createEvent.presentation.adapter.PlacesAutoCompleteAdapter
import com.scenein.createEvent.presentation.view_model.CreateEventViewModel
import com.scenein.databinding.FragmentStep2TimePlaceBinding
import java.text.SimpleDateFormat
import java.util.*

class Step2TimePlaceFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentStep2TimePlaceBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: CreateEventViewModel by activityViewModels()

    private lateinit var placesClient: PlacesClient
    private lateinit var predictionsAdapter: PlacesAutoCompleteAdapter
    private lateinit var sessionToken: AutocompleteSessionToken

    private var googleMap: GoogleMap? = null
    private var selectedLatLng: LatLng? = null

    private val STATE_ERROR = intArrayOf(R.attr.state_error)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStep2TimePlaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!Places.isInitialized()) {
            Places.initialize(requireContext().applicationContext, BuildConfig.PLACES_API_KEY)
        }
        placesClient = Places.createClient(requireContext())
        sessionToken = AutocompleteSessionToken.newInstance()

        setupDateTimeListeners()
        setupLocationMode()
        setupCustomSearch()
        restoreStateFromViewModel()

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun restoreStateFromViewModel() {
        val uiDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        sharedViewModel.eventDate.value?.let {
            try {
                apiDateFormat.parse(it)?.let { date ->
                    binding.etDate.setText(uiDateFormat.format(date))
                }
            } catch (e: Exception) {
                binding.etDate.setText(it)
            }
        }

        binding.etStartTime.setText(sharedViewModel.startTime.value)
        binding.etEndTime.setText(sharedViewModel.endTime.value)

        if (sharedViewModel.locationMode.value == "Online") {
            binding.rgLocationMode.check(R.id.rbOnline)
        } else {
            binding.rgLocationMode.check(R.id.rbInPerson)
        }

        binding.etVenueSearch.setText(sharedViewModel.venueName.value)
        binding.etMeetingLink.setText(sharedViewModel.meetingLink.value)
    }

    private fun setupDateTimeListeners() {
        binding.etDate.setOnClickListener { showDatePicker() }
        binding.etStartTime.setOnClickListener { showTimePicker(isStartTime = true) }
        binding.etEndTime.setOnClickListener { showTimePicker(isStartTime = false) }
    }

    private fun setupLocationMode() {
        binding.rgLocationMode.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbInPerson) {
                binding.inPersonContainer.visibility = View.VISIBLE
                binding.onlineContainer.visibility = View.GONE
                sharedViewModel.locationMode.value = "In-Person"
            } else {
                binding.inPersonContainer.visibility = View.GONE
                binding.onlineContainer.visibility = View.VISIBLE
                sharedViewModel.locationMode.value = "Online"
            }
            sharedViewModel.checkAllValidations()
        }

        binding.etMeetingLink.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val link = s.toString()
                sharedViewModel.meetingLink.value = link
                validateMeetingLink(link)
                sharedViewModel.checkAllValidations()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun validateMeetingLink(link: String) {
        if (link.isNotEmpty() && !android.util.Patterns.WEB_URL.matcher(link).matches()) {
            showError(binding.etMeetingLink, binding.tvLinkError, "Please enter a valid URL")
        } else {
            clearError(binding.etMeetingLink, binding.tvLinkError)
        }
    }

    private fun setupCustomSearch() {
        predictionsAdapter = PlacesAutoCompleteAdapter { prediction ->
            binding.rvPredictions.visibility = View.GONE
            fetchPlaceDetails(prediction.placeId)
        }
        binding.rvPredictions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = predictionsAdapter
        }

        binding.etVenueSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                if (query.isNotEmpty() && !query.equals(sharedViewModel.venueName.value, ignoreCase = true)) {
                    getAutocompletePredictions(query)
                } else {
                    binding.rvPredictions.visibility = View.GONE
                }
                validateAddress(query)
                sharedViewModel.checkAllValidations()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun validateAddress(address: String) {
        // Validation is handled by checking if fullAddress is empty in the ViewModel
        sharedViewModel.fullAddress.value = address
    }

    private fun getAutocompletePredictions(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setCountry("IN").setSessionToken(sessionToken).setQuery(query).build()
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                predictionsAdapter.setPredictions(response.autocompletePredictions)
                binding.rvPredictions.visibility = View.VISIBLE
            }.addOnFailureListener { exception ->
                Log.e("Step2TimePlace", "Prediction failed", exception)
            }
    }

    private fun fetchPlaceDetails(placeId: String) {
        val placeFields = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.ADDRESS_COMPONENTS, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place

                binding.etVenueSearch.setText(place.name)

                sharedViewModel.venueName.value = place.name
                sharedViewModel.fullAddress.value = place.address

                place.latLng?.let { latLng ->
                    sharedViewModel.latitude.value = latLng.latitude
                    sharedViewModel.longitude.value = latLng.longitude
                    selectedLatLng = latLng
                    binding.mapContainer.visibility = View.VISIBLE
                    updateMapLocation()
                }

                var fetchedCity: String? = null
                var fetchedPincode: String? = null
                place.addressComponents?.asList()?.forEach { c ->
                    if (c.types.contains("locality")) fetchedCity = c.name
                    if (c.types.contains("postal_code")) fetchedPincode = c.name
                }
                sharedViewModel.city.value = fetchedCity
                sharedViewModel.pincode.value = fetchedPincode

                sharedViewModel.checkAllValidations()
            }
            .addOnFailureListener { exception ->
                Log.e("Step2TimePlace", "Place not found.", exception)
            }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isScrollGesturesEnabled = false
        updateMapLocation()
    }

    private fun updateMapLocation() {
        if (googleMap != null && selectedLatLng != null) {
            googleMap?.clear()
            googleMap?.addMarker(MarkerOptions().position(selectedLatLng!!))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng!!, 15f))
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply { set(year, month, dayOfMonth) }

                val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                val dateForApi = apiDateFormat.format(selectedCalendar.time)
                sharedViewModel.eventDate.value = dateForApi

                val uiDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                binding.etDate.setText(uiDateFormat.format(selectedCalendar.time))

                clearError(binding.etDate, binding.tvDateError)
                sharedViewModel.checkAllValidations()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        val timePicker = TimePickerDialog(requireContext(),
            { _, hour, minute ->
                val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute) }
                val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
                if (isStartTime) {
                    binding.etStartTime.setText(formattedTime)
                    sharedViewModel.startTime.value = formattedTime
                    clearError(binding.etStartTime, binding.tvStartTimeError)
                    sharedViewModel.checkAllValidations()
                } else {
                    binding.etEndTime.setText(formattedTime)
                    sharedViewModel.endTime.value = formattedTime
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false
        )
        timePicker.show()
    }

    private fun showError(editText: EditText, errorTextView: TextView, message: String) {
        editText.background.state = STATE_ERROR
        errorTextView.text = message
        errorTextView.visibility = View.VISIBLE
    }

    private fun clearError(editText: EditText, errorTextView: TextView) {
        editText.background.state = intArrayOf()
        errorTextView.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}