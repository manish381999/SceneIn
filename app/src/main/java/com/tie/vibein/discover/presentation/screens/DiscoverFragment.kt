package com.tie.vibein.discover.presentation.screens

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.R
import com.tie.vibein.databinding.FragmentDiscoverBinding
import com.tie.vibein.discover.presentation.adapter.DiscoverAdapter
import com.tie.vibein.discover.presentation.viewmodel.DiscoverViewModel
import com.tie.vibein.utils.LocationHelper
import com.tie.vibein.utils.NetworkState
import java.text.SimpleDateFormat
import java.util.*

class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    private lateinit var locationHelper: LocationHelper
    private lateinit var viewModel: DiscoverViewModel
    private val LOCATION_PERMISSION_REQUEST = 1001

    private var city: String? = null
    private var country: String? = null
    private var pincode: String? = null
    private var latitude: Double? = null
    private var longitude: Double? = null

    private lateinit var adapter: DiscoverAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationHelper = LocationHelper(requireContext())
        viewModel = ViewModelProvider(this)[DiscoverViewModel::class.java]

        adapter = DiscoverAdapter(mutableListOf(), viewModel, requireContext())
        binding.rvEvent.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvent.adapter = adapter

        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorError,
            R.color.colorSecondary
        )

        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchAndDisplayLocation()
        }

        observeViewModel()
        initComponents()
    }

    override fun onResume() {
        super.onResume()
        // Call the API when the fragment becomes visible
        if (locationHelper.hasLocationPermission()) {
            fetchAndDisplayLocation()
        } else {
            locationHelper.requestLocationPermission(requireActivity(), LOCATION_PERMISSION_REQUEST)
        }
    }

    private fun initComponents() {
        // Removed the initial fetchAndDisplayLocation() call here.  It's now in onResume()
    }

    private fun fetchAndDisplayLocation() {
        locationHelper.fetchLocation { result ->
            if (_binding == null) return@fetchLocation // view is destroyed

            when (result) {
                is LocationHelper.LocationResult.Success -> {
                    city = result.city ?: "Unknown City"
                    country = result.country ?: "Unknown Country"
                    pincode = result.pincode ?: "Unknown Pincode"
                    latitude = result.latitude
                    longitude = result.longitude

                    binding.tvLocation.text = "$city, $country"

                    val userId = SP.getString(requireContext(), SP.USER_ID) ?: ""
                    val currentDate = getCurrentDate()
                    viewModel.fetchCityEvents(userId, city!!, currentDate)
                }

                is LocationHelper.LocationResult.Error -> {
                    binding.tvLocation.text = "Location unavailable"
                    Log.d("DiscoverFragment", "Error: ${result.message}")
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.eventState.observe(viewLifecycleOwner) { state ->
            if (_binding == null) return@observe

            binding.swipeRefreshLayout.isRefreshing = false

            when (state) {
                is NetworkState.Loading -> {
                    binding.shimmerLayout.visibility = View.VISIBLE
                    binding.shimmerLayout.startShimmer()
                    binding.rvEvent.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.GONE
                }
                is NetworkState.Success -> {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.visibility = View.GONE

                    if (state.data.isNotEmpty()) {
                        binding.rvEvent.visibility = View.VISIBLE
                        binding.tvEmptyState.visibility = View.GONE
                        adapter.updateEvents(state.data)
                    } else {
                        binding.rvEvent.visibility = View.GONE
                        binding.tvEmptyState.visibility = View.VISIBLE
                    }
                }
                is NetworkState.Error -> {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.visibility = View.GONE
                    binding.rvEvent.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.GONE

                    Snackbar.make(binding.root, "Error: ${state.message}", Snackbar.LENGTH_LONG).show()
                    Log.d("DiscoverFragment", "ApiError: ${state.message}")
                }
            }
        }

        viewModel.joinEventState.observe(viewLifecycleOwner) { state ->
            if (state == null) return@observe

            when (state) {
                is NetworkState.Loading -> {
                    // Optional: Show loading indicator
                }
                is NetworkState.Success -> {
                    Snackbar.make(binding.root, "Joined Successfully", Snackbar.LENGTH_SHORT).show()
                }
                is NetworkState.Error -> {
                    Snackbar.make(binding.root, "Error: ${state.message}", Snackbar.LENGTH_SHORT).show()
                }
            }

            viewModel.clearJoinEventState() // 👈 Reset state after observing
        }

        viewModel.unjoinEventState.observe(viewLifecycleOwner) { state ->
            if (state == null) return@observe

            when (state) {
                is NetworkState.Loading -> {
                    // Optional: Show loading indicator
                }
                is NetworkState.Success -> {
                    Snackbar.make(binding.root, "Unjoined Successfully", Snackbar.LENGTH_SHORT).show()
                }
                is NetworkState.Error -> {
                    Snackbar.make(binding.root, "Error: ${state.message}", Snackbar.LENGTH_SHORT).show()
                }
            }

            viewModel.clearUnjoinEventState() // 👈 Reset state after observing
        }
    }


    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchAndDisplayLocation()
            } else {
                binding.tvLocation.text = "Permission denied"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}