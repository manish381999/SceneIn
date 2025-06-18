package com.tie.vibein.profile.persentation.screen

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.tie.vibein.R
import com.tie.vibein.createEvent.persentation.screens.CreateEventFragment
import com.tie.vibein.databinding.FragmentEventsBinding
import com.tie.vibein.databinding.FragmentUserProfileEventsBinding
import com.tie.vibein.profile.presentation.adapter.EventAdapter
import com.tie.vibein.profile.presentation.viewmodel.ProfileViewModel
import com.tie.vibein.utils.NetworkState

class UserProfileEventsFragment : Fragment() {

    private var _binding: FragmentUserProfileEventsBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel: ProfileViewModel by viewModels()

    private var userId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Step 2: Get the user_id from arguments
        userId = arguments?.getString("user_id")


        initComponents()
    }

    private fun initComponents() {
        // Step 3: Use userId here
        if (userId != null) {
          getEvent(userId!!)
        } else {
            // Handle missing userId (fallback behavior or error)
        }
    }

    private fun getEvent(userId: String) {
        binding.rvEvent.layoutManager = LinearLayoutManager(requireContext())

        profileViewModel.fetchEventsByUser(userId)

        profileViewModel.eventsState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is NetworkState.Loading -> {
                    // Show loading UI
                }

                is NetworkState.Success -> {
                    val allEvents = state.data
                    val filteredEvents = allEvents.filter { it.status == "1" }

                    if (filteredEvents.isEmpty()) {
                        binding.rvEvent.visibility = View.GONE
                        binding.emptyStateLayout.visibility = View.VISIBLE
                    } else {
                        binding.rvEvent.visibility = View.VISIBLE
                        binding.emptyStateLayout.visibility = View.GONE
                        val adapter = EventAdapter(requireContext(), filteredEvents)
                        binding.rvEvent.adapter = adapter
                    }
//                    binding.btnCreateEvent.setOnClickListener {
//                        // Create an instance of CreateEventFragment
//                        val createEventFragment = CreateEventFragment()
//
//                        // Begin a FragmentTransaction and replace the current fragment with CreateEventFragment
//                        requireActivity().supportFragmentManager.beginTransaction()
//                            .replace(R.id.fragment_profile, createEventFragment)  // Use the ID of your container
//                            .addToBackStack(null)  // Optional: To add to back stack
//                            .commit()
//                    }
                }

                is NetworkState.Error -> {
                    Log.e("EventsFragment", "Error: ${state.message}")
                }
            }
        })

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



}