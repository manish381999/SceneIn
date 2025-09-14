package com.scenein.profile.presentation.screen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.scenein.databinding.FragmentUserProfileTicketsBinding
import com.scenein.profile.presentation.view_model.ProfileViewModel
import com.scenein.tickets.presentation.adapter.MyTicketsAdapter
import com.scenein.utils.NetworkState

class UserProfileTicketsFragment : Fragment() {

    private var _binding: FragmentUserProfileTicketsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels({ requireActivity() })
    private lateinit var ticketsAdapter: MyTicketsAdapter
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(ARG_USER_ID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUserProfileTicketsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()

        if (!userId.isNullOrEmpty()) {
            viewModel.fetchMyTicketsActivity()
        }
    }

    private fun setupRecyclerView() {
        ticketsAdapter = MyTicketsAdapter { clickedTicket ->
            Toast.makeText(requireContext(), "Clicked ticket: ${clickedTicket.eventName}", Toast.LENGTH_SHORT).show()
            // TODO: Implement navigation to PurchasedTicketDetailActivity for purchased tickets
        }
        binding.rvProfileTickets.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProfileTickets.adapter = ticketsAdapter
    }

    private fun setupObservers() {
        viewModel.myTicketsState.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state is NetworkState.Loading
            if (state is NetworkState.Success) {
                val allTickets = (state.data.listedTickets + state.data.purchasedTickets)
//                    .sortedByDescending { it.created_at }
                binding.tvEmptyMessage.isVisible = allTickets.isEmpty()
                ticketsAdapter.submitList(allTickets)
            } else if (state is NetworkState.Error) {
                Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        private const val ARG_USER_ID = "user_id"
        fun newInstance(userId: String): UserProfileTicketsFragment {
            return UserProfileTicketsFragment().apply {
                arguments = Bundle().apply { putString(ARG_USER_ID, userId) }
            }
        }
    }
}