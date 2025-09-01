package com.scenein.discover.presentation.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import com.scenein.R
import com.scenein.createEvent.data.models.Category
import com.scenein.databinding.FragmentDiscoverBinding
import com.scenein.discover.data.models.SuggestedConnection
import com.scenein.discover.presentation.adapter.CategoryAdapter
import com.scenein.discover.presentation.adapter.FeedAdapter
import com.scenein.discover.presentation.view_model.DiscoverViewModel
import com.scenein.notification.presentation.screens.NotificationsActivity
import com.scenein.profile.persentation.screen.UserProfileActivity
import com.scenein.profile.persentation.view_model.ProfileViewModel
import com.scenein.utils.*
import java.util.*

class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    private lateinit var locationHelper: LocationHelper
    private lateinit var locationServiceManager: LocationServiceManager

    private val viewModel: DiscoverViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    private lateinit var feedAdapter: FeedAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    // Removed dateFilterAdapter as it's no longer used

    private lateinit var currentUserId: String
    private var lastInteractedConnection: SuggestedConnection? = null
    private var lastFetchTimestamp: Long = 0L // For smart refresh

    private val locationSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        fetchAndDisplayLocation(forceRefresh = true)
    }

    private val locationPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchAndDisplayLocation(forceRefresh = true)
        }
    }

    private val eventDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val eventId = data?.getStringExtra("event_id_result")

            // --- THIS IS THE NEW LOGIC ---
            if (eventId != null) {
                // Tell the ViewModel to get fresh data for this one event
                viewModel.refreshSingleEventInFeed(eventId)
            }
        }
    }
    private val userProfileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val userId = data?.getStringExtra("user_id_result")
            val status = data?.getStringExtra("connection_status_result")
            val sentBy = data?.getStringExtra("request_sent_by_result")
            if (userId != null) {
                viewModel.updateConnectionStatusInFeed(userId, status, sentBy, null)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserId = SP.getString(requireContext(), SP.USER_ID) ?: ""
        locationHelper = LocationHelper(requireContext())
        locationServiceManager = LocationServiceManager(requireContext())

        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()

        viewModel.currentCity?.let {
            binding.tvLocation.text = it
        }

        if (locationHelper.hasLocationPermission()) {
            fetchAndDisplayLocation(forceRefresh = false)
        } else {
            locationHelper.requestLocationPermission(this, 1001)
        }
    }

    override fun onResume() {
        super.onResume()
        val refreshThresholdMinutes = 5
        val thresholdInMillis = refreshThresholdMinutes * 60 * 1000
        val shouldRefresh = System.currentTimeMillis() - lastFetchTimestamp > thresholdInMillis

        if (feedAdapter.itemCount > 0 && shouldRefresh) {
            fetchAndDisplayLocation(forceRefresh = true)
        }
    }

    private fun setupRecyclerViews() {
        feedAdapter = FeedAdapter(
            currentUserId = currentUserId,
            onEventClicked = { eventId ->
                val intent = Intent(requireContext(), EventDetailActivity::class.java).apply {
                    putExtra("event_id", eventId)
                    putExtra("source", "discover")
                }
                eventDetailLauncher.launch(intent)
            },
            onJoinClicked = { eventId, position -> viewModel.joinEvent(eventId) },
            onUnjoinClicked = { eventId, position -> viewModel.unjoinEvent(eventId) },
            onAddBookmarkClicked = { eventId -> viewModel.addBookmark(eventId) },
            onRemoveBookmarkClicked = { eventId -> viewModel.removeBookmark(eventId) },
            onConnectClicked = { connection ->
                lastInteractedConnection = connection
                when {
                    connection.connectionStatus == "pending" && connection.requestSentBy == "me" ->
                        profileViewModel.removeConnection(connection.userId)
                    connection.connectionStatus == "pending" && connection.requestSentBy == "them" ->
                        profileViewModel.respondToConnectionRequest(connection.connectionId ?: "", "accepted", "", connection.userId)
                    else ->
                        profileViewModel.sendConnectionRequest(connection.userId)
                }
            },
            onConnectionProfileClicked = { connection ->
                val intent = Intent(requireContext(), UserProfileActivity::class.java).apply {
                    putExtra("user_id", connection.userId)
                }
                userProfileLauncher.launch(intent)
            }
        )
        val layoutManager = LinearLayoutManager(requireContext())
        binding.rvFeed.layoutManager = layoutManager
        binding.rvFeed.adapter = feedAdapter

        // Add the scroll listener for infinite scrolling (pagination)
        binding.rvFeed.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount

                // When the user is 5 items away from the end of the list, fetch more.
                if (lastVisibleItemPosition > totalItemCount - 5 && totalItemCount > 0) {
                    viewModel.fetchMoreFeedItems()
                }
            }
        })

        categoryAdapter = CategoryAdapter { category ->
            val city = viewModel.currentCity ?: return@CategoryAdapter // CORRECT: Access the variable directly
            viewModel.filterEventsByCategory(category.id, city)
        }

        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvCategories.adapter = categoryAdapter
    }

    private fun setupClickListeners() {
        binding.locationContainer.setOnClickListener {
            locationPickerLauncher.launch(Intent(requireContext(), LocationPickerActivity::class.java))
        }
        binding.searchBarContainer.setOnClickListener {
            Toast.makeText(requireContext(), "Search coming soon!", Toast.LENGTH_SHORT).show()
        }
        binding.ivNotifications.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }
        binding.appBarLayout.addOnOffsetChangedListener { _, verticalOffset ->
            binding.swipeRefreshLayout.isEnabled = verticalOffset == 0
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchAndDisplayLocation(forceRefresh = true)
        }
    }

    private fun fetchAndDisplayLocation(forceRefresh: Boolean) {
        if (!forceRefresh && viewModel.feedState.value is NetworkState.Success) {
            binding.swipeRefreshLayout.isRefreshing = false
            return
        }

        if (!locationServiceManager.isLocationEnabled()) {
            binding.swipeRefreshLayout.isRefreshing = false
            CustomAlertDialog.show(
                context = requireContext(),
                title = "Enable Location",
                message = "To discover events near you, please turn on your device's location services.",
                positiveButtonText = "Enable",
                onPositiveClick = {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    locationSettingsLauncher.launch(intent)
                },
                negativeButtonText = "Not Now"
            )
            return
        }

        binding.swipeRefreshLayout.isRefreshing = true
        locationHelper.fetchLocation { result ->
            if (_binding == null) return@fetchLocation
            binding.swipeRefreshLayout.isRefreshing = false

            if (result is LocationHelper.LocationResult.Success) {
                lastFetchTimestamp = System.currentTimeMillis()
                feedAdapter.setUserLocation(result.location)
                val city = result.city ?: "Unknown"
                binding.tvLocation.text = city // Set the text in the UI
                viewModel.currentCity = city   // Save the city to the ViewModel
                viewModel.fetchInitialFeed(city)
            } else if (result is LocationHelper.LocationResult.Error){
                Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.feedState.observe(viewLifecycleOwner) { state ->
            // --- THIS IS THE UPDATED LOGIC ---
            // Show shimmer only when loading the very first page
            binding.shimmerLayout.visibility = if (state is NetworkState.Loading && viewModel.currentPage == 1) View.VISIBLE else View.GONE

            // Show the list only on success
            binding.rvFeed.visibility = if (state is NetworkState.Success) View.VISIBLE else View.GONE

            // Show empty state text only on success with an empty list
            binding.tvEmptyState.visibility = if (state is NetworkState.Success && state.data.isEmpty()) View.VISIBLE else View.GONE

            when (state) {
                is NetworkState.Success -> {
                    binding.shimmerLayout.stopShimmer()
                    feedAdapter.submitList(state.data)
                }
                is NetworkState.Error -> {
                    binding.shimmerLayout.stopShimmer()
                    Snackbar.make(binding.root, "Error: ${state.message}", Snackbar.LENGTH_LONG).show()
                }
                is NetworkState.Loading -> {
                    if (viewModel.currentPage == 1) {
                        binding.shimmerLayout.startShimmer()
                    }
                }
            }
        }

        viewModel.categoryState.observe(viewLifecycleOwner) { state ->
            if (state is NetworkState.Success) {
                val categoriesWithAll = mutableListOf(Category("0", "All", "", "1", ""))
                categoriesWithAll.addAll(state.data.data)
                categoryAdapter.submitList(categoriesWithAll)
            }
        }

        viewModel.joinEventState.observe(viewLifecycleOwner) { state ->
            if (state == null) return@observe
            if (state is NetworkState.Success) {
                Snackbar.make(binding.root, "Joined event successfully!", Snackbar.LENGTH_SHORT).show()
            } else if (state is NetworkState.Error) {
                Snackbar.make(binding.root, "Failed to join: ${state.message}", Snackbar.LENGTH_LONG).show()
            }
            viewModel.clearJoinEventState()
        }

        viewModel.unjoinEventState.observe(viewLifecycleOwner) { state ->
            if (state == null) return@observe
            if (state is NetworkState.Success) {
                Snackbar.make(binding.root, "Left event successfully", Snackbar.LENGTH_SHORT).show()
            } else if (state is NetworkState.Error) {
                Snackbar.make(binding.root, "Failed to leave event: ${state.message}", Snackbar.LENGTH_LONG).show()
            }
            viewModel.clearUnjoinEventState()
        }

        profileViewModel.connectionActionState.observe(viewLifecycleOwner) { state ->
            if (state is NetworkState.Success) {
                Snackbar.make(binding.root, state.data, Snackbar.LENGTH_SHORT).show()
                lastInteractedConnection?.let { connection ->
                    val newStatus: String?
                    val newSentBy: String?
                    when {
                        connection.connectionStatus == "pending" && connection.requestSentBy == "me" -> {
                            newStatus = null
                            newSentBy = null
                        }
                        connection.connectionStatus == "pending" && connection.requestSentBy == "them" -> {
                            newStatus = "accepted"
                            newSentBy = "them"
                        }
                        else -> {
                            newStatus = "pending"
                            newSentBy = "me"
                        }
                    }
                    viewModel.updateConnectionStatusInFeed(connection.userId, newStatus, newSentBy, connection.connectionId)
                }
                lastInteractedConnection = null
            } else if (state is NetworkState.Error) {
                Snackbar.make(binding.root, "Action Failed: ${state.message}", Snackbar.LENGTH_LONG).show()
                lastInteractedConnection = null
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchAndDisplayLocation(forceRefresh = true)
            } else {
                Snackbar.make(binding.root, "Location permission is required.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvFeed.adapter = null
        binding.rvCategories.adapter = null
        _binding = null
    }
}