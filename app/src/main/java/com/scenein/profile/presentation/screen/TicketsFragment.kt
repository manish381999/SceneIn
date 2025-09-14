package com.scenein.profile.presentation.screen

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.scenein.utils.SP
import com.scenein.databinding.FragmentTicketsBinding
import com.scenein.profile.presentation.view_model.ProfileViewModel
import com.scenein.tickets.presentation.adapter.MyTicketsAdapter // We reuse this great adapter
import com.scenein.tickets.presentation.screens.PurchasedTicketDetailActivity
import com.scenein.utils.NetworkState

class TicketsFragment : Fragment() {
    private var _binding: FragmentTicketsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels({ requireParentFragment() })
    private lateinit var myTicketsAdapter: MyTicketsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTicketsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentUserId = SP.getString(requireContext(), SP.USER_ID) ?: ""

        setupRecyclerView()
        setupObservers()

        if (currentUserId.isNotEmpty()) {
            viewModel.fetchMyTicketsActivity()
        }
    }

    private fun setupRecyclerView() {
        myTicketsAdapter = MyTicketsAdapter { clickedTicket ->
            if (clickedTicket.transactionId != null) { // This is a purchased ticket
                val intent = Intent(requireContext(), PurchasedTicketDetailActivity::class.java).apply {
                    putExtra("TICKET_DATA", clickedTicket)
                }
                startActivity(intent)
            } else { // This is a listed ticket
                // TODO: Navigate to ManageListingActivity
                Toast.makeText(requireContext(), "Tapped your ${clickedTicket.listingStatus} ticket", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvTickets.adapter = myTicketsAdapter
    }

    private fun setupObservers() {
        viewModel.myTicketsState.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state is NetworkState.Loading
            when(state) {
                is NetworkState.Success -> {
                    val allMyTickets = (state.data.listedTickets + state.data.purchasedTickets)
                       // .sortedByDescending { it.created_at }
                    binding.tvEmptyMessage.isVisible = allMyTickets.isEmpty()
                    myTicketsAdapter.submitList(allMyTickets)
                }
                is NetworkState.Error -> {
                    Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}