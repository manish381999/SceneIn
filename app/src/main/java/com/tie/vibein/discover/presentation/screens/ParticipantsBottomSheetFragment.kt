package com.tie.vibein.discover.presentation.screens

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.chat.persentation.screens.ChatActivity
import com.tie.vibein.databinding.BottomSheetParticipantsBinding
import com.tie.vibein.discover.data.models.Participant
import com.tie.vibein.discover.presentation.adapter.ParticipantsAdapter
import com.tie.vibein.discover.presentation.viewmodel.DiscoverViewModel
import com.tie.vibein.profile.persentation.screen.UserProfileActivity
import com.tie.vibein.profile.persentation.view_model.ProfileViewModel
import com.tie.vibein.utils.NetworkState
import java.util.*

class ParticipantsBottomSheetFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetParticipantsBinding? = null
    private val binding get() = _binding!!
    private val discoverViewModel: DiscoverViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    private lateinit var participantsAdapter: ParticipantsAdapter
    private var eventId: String? = null
    private var totalCount: Int = 0
    private lateinit var currentUserId: String

    // --- State properties for pagination ---
    private var isLoading = false
    private var isLastPage = false
    private var currentPage = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            eventId = it.getString(ARG_EVENT_ID)
            totalCount = it.getInt(ARG_TOTAL_COUNT)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetParticipantsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentUserId = SP.getString(requireContext(), SP.USER_ID) ?: ""

        binding.tvSheetTitle.text = "Participants ($totalCount)"

        setupRecyclerView()
        setupObservers()
        setupSearch()

        fetchParticipants(isInitialLoad = true)
    }

    private fun fetchParticipants(isInitialLoad: Boolean = false) {
        if (!eventId.isNullOrEmpty() && currentUserId.isNotEmpty()) {
            if (isInitialLoad) {
                currentPage = 1
                isLastPage = false
                participantsAdapter.submitList(emptyList())
            }
            discoverViewModel.fetchEventParticipants(currentUserId, eventId!!, currentPage)
        }
    }

    private fun setupRecyclerView() {
        participantsAdapter = ParticipantsAdapter(
            currentUserId = currentUserId,
            onProfileClick = { participant ->
                navigateToUserProfile(participant.userId)
            },
            onActionClick = { participant ->
                handleActionClick(participant)
            }
        )

        binding.rvAttendees.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = participantsAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

            // --- Infinite Scrolling Pagination Listener ---
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                            currentPage++
                            fetchParticipants()
                        }
                    }
                }
            })
        }
    }

    private fun handleActionClick(participant: Participant) {
        val viewerId = currentUserId
        val profileId = participant.userId

        when (participant.connectionStatus) {
            "none" -> profileViewModel.sendConnectionRequest(viewerId, profileId)
            "sent_to_me" -> navigateToUserProfile(profileId)
            "accepted" -> {
                val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                    putExtra("receiver_id", participant.userId)
                    putExtra("receiver_name", participant.name ?: "Chat")
                    putExtra("receiver_profile_pic", participant.profilePic ?: "")
                }
                startActivity(intent)
            }
            "sent_by_me" -> {
                Toast.makeText(requireContext(), "Your connection request is pending.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToUserProfile(userId: String) {
        val intent = Intent(requireContext(), UserProfileActivity::class.java).apply {
            putExtra("user_id", userId)
        }
        startActivity(intent)
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val allParticipants = (discoverViewModel.participantsState.value as? NetworkState.Success)?.data ?: emptyList()
                val query = s.toString().lowercase(Locale.getDefault())
                if (query.isEmpty()) {
                    participantsAdapter.submitList(allParticipants)
                } else {
                    val filteredList = allParticipants.filter {
                        it.name?.lowercase(Locale.getDefault())?.contains(query) == true ||
                                it.userName?.lowercase(Locale.getDefault())?.contains(query) == true
                    }
                    participantsAdapter.submitList(filteredList)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupObservers() {
        discoverViewModel.participantsState.observe(viewLifecycleOwner) { state ->
            isLoading = state is NetworkState.Loading
            binding.progressBar.isVisible = isLoading && currentPage == 1

            when (state) {
                is NetworkState.Success -> {
                    if (currentPage == 1) { // First page load
                        binding.tvEmptyMessage.isVisible = state.data.isEmpty()
                        participantsAdapter.submitList(state.data)
                    } else { // Subsequent page loads for pagination
                        val currentList = participantsAdapter.currentList.toMutableList()
                        currentList.addAll(state.data)
                        participantsAdapter.submitList(currentList)
                    }

                    if (state.data.isEmpty() && currentPage > 1) {
                        isLastPage = true // No more items to load
                    }
                }
                is NetworkState.Error -> {
                    Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                    binding.progressBar.isVisible = false
                }
                is NetworkState.Loading -> { /* Handled above */ }
            }
        }

        profileViewModel.connectionActionState.observe(viewLifecycleOwner) { state ->
            // You can enhance this with item-specific loading
            if (state is NetworkState.Success) {
                Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT).show()
                // A better implementation would be to just update the specific item's
                // connectionStatus and notify the adapter, but a full refresh works for now.
                fetchParticipants(isInitialLoad = true)
            } else if (state is NetworkState.Error) {
                Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_EVENT_ID = "event_id"
        private const val ARG_TOTAL_COUNT = "total_count"
        fun newInstance(eventId: String, totalCount: Int): ParticipantsBottomSheetFragment {
            return ParticipantsBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EVENT_ID, eventId)
                    putInt(ARG_TOTAL_COUNT, totalCount)
                }
            }
        }
    }
}