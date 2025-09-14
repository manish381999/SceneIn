package com.scenein.profile.presentation.screen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.scenein.utils.SP
import com.scenein.createEvent.presentation.screens.CreateEventActivity
import com.scenein.databinding.FragmentEventsBinding
import com.scenein.profile.presentation.adapter.EventAdapter

import com.scenein.profile.presentation.view_model.ProfileViewModel
import com.scenein.utils.NetworkState

class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!
    // We get the ProfileViewModel instance shared from the parent fragment (ProfileFragment)
    private val profileViewModel: ProfileViewModel by viewModels({ requireParentFragment() })

    // The adapter is now a class-level property
    private lateinit var eventAdapter: EventAdapter
    private var userId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // This userId will be from the parent ProfileFragment, determining whose events to show.
        // It's better to get this from arguments, passed by the ProfileViewPagerAdapter.
        userId = SP.getString(requireContext(), SP.USER_ID)

        setupRecyclerView()
        setupClickListeners()
        setupObservers()

        // Initial data fetch
        if (!userId.isNullOrEmpty()) {
            profileViewModel.fetchMyEvents()
        }
    }

    private fun setupRecyclerView() {
        // --- THIS IS THE FIX ---
        // Initialize the adapter ONCE with an empty list.
        eventAdapter = EventAdapter(requireContext())
        binding.rvEvent.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvent.adapter = eventAdapter
        // --- END OF FIX ---
    }

    private fun setupClickListeners() {
        binding.btnCreateEvent.setOnClickListener {
            // Create an Intent to launch the new CreateEventActivity
            val intent = Intent(requireContext(), CreateEventActivity::class.java)

            // Start the activity
            startActivity(intent)
        }
    }

    private fun setupObservers() {
        profileViewModel.eventsState.observe(viewLifecycleOwner) { state ->
            // You can add shimmer or progress bar logic here
            binding.progressBar.isVisible = state is NetworkState.Loading

            when (state) {
                is NetworkState.Success -> {
                    // Filter the events to only show active ones.
                    val activeEvents = state.data.filter { it.status == "1" }

                    // Set visibility of the empty state and the RecyclerView.
                    binding.emptyStateLayout.isVisible = activeEvents.isEmpty()
                    binding.rvEvent.isVisible = activeEvents.isNotEmpty()

                    // --- THIS IS THE FIX ---
                    // Call the updateEvents function to populate the adapter with the new data.
                    eventAdapter.updateEvents(activeEvents)
                    // --- END OF FIX ---
                }
                is NetworkState.Error -> {
                    Log.e("EventsFragment", "Error: ${state.message}")
                    binding.emptyStateLayout.isVisible = true
                    binding.rvEvent.isVisible = false
                    Toast.makeText(requireContext(), "Error loading events.", Toast.LENGTH_SHORT).show()
                }
                is NetworkState.Loading -> {
                    // Handled above by progress bar
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}