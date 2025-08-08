package com.tie.vibein.profile.persentation.screen

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.R
import com.tie.vibein.databinding.FragmentUserProfileEventsBinding
import com.tie.vibein.profile.presentation.adapter.EventAdapter
import com.tie.vibein.profile.persentation.view_model.ProfileViewModel
import com.tie.vibein.utils.NetworkState

class UserProfileEventsFragment : Fragment() {

    private var _binding: FragmentUserProfileEventsBinding? = null
    private val binding get() = _binding!!
    // Use viewModels() with a factory that references the parent Activity to share the ViewModel
    private val profileViewModel: ProfileViewModel by viewModels({ requireActivity() })
    private lateinit var eventAdapter: EventAdapter
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(USER_ID_ARG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()

        if (userId.isNullOrEmpty()) {
            // Handle case where user ID is missing
            binding.emptyStateLayout.isVisible = true
            binding.rvEvent.isVisible = false
        } else {
            profileViewModel.fetchEventsByUser(userId!!)
        }
    }

    private fun setupRecyclerView() {
        // --- THIS IS THE FIX: Initialize the adapter without data ---
        eventAdapter = EventAdapter(requireContext())
        binding.rvEvent.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvent.adapter = eventAdapter
    }

    private fun setupObservers() {
        profileViewModel.eventsState.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state is NetworkState.Loading

            when (state) {
                is NetworkState.Success -> {
                    val activeEvents = state.data.filter { it.status == "1" }

                    binding.emptyStateLayout.isVisible = activeEvents.isEmpty()
                    binding.rvEvent.isVisible = activeEvents.isNotEmpty()

                    // --- THIS IS THE FIX: Call the new updateEvents function ---
                    eventAdapter.updateEvents(activeEvents)
                }
                is NetworkState.Error -> {
                    Log.e("EventsFragment", "Error: ${state.message}")
                    binding.emptyStateLayout.isVisible = true
                    binding.rvEvent.isVisible = false
                }
                is NetworkState.Loading -> {
                    // Handled by progress bar visibility
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val USER_ID_ARG = "user_id"
        fun newInstance(userId: String): UserProfileEventsFragment {
            return UserProfileEventsFragment().apply {
                arguments = Bundle().apply {
                    putString(USER_ID_ARG, userId)
                }
            }
        }
    }
}