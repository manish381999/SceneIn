package com.scenein.tickets.presentation.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.scenein.utils.SP
import com.scenein.databinding.FragmentMyTicketsBinding
import com.scenein.tickets.presentation.adapter.MyTicketsAdapter
import com.scenein.tickets.presentation.view_model.TicketViewModel
import com.scenein.utils.NetworkState

class MyTicketsFragment : Fragment() {

    private var _binding: FragmentMyTicketsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TicketViewModel by viewModels()
    private lateinit var ticketsAdapter: MyTicketsAdapter

    // --- NEW: An ActivityResultLauncher to handle refreshing the list ---
    // This listens for a result from ManageListingActivity or PurchasedTicketDetailActivity.
    private val ticketActionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // An action was successfully taken on the detail screen (e.g., ticket was resold or delisted).
            // We should refresh the list to show the change.
            binding.swipeRefreshLayout.isRefreshing = true
            loadUserTickets()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentMyTicketsBinding.inflate(inflater, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        binding.swipeRefreshLayout.setOnRefreshListener { loadUserTickets() }
    }

    override fun onResume() {
        super.onResume()
        loadUserTickets()
    }

    private fun loadUserTickets() {
        val currentUserId = SP.getString(requireContext(), SP.USER_ID)
        if (currentUserId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please log in to see your tickets.", Toast.LENGTH_SHORT).show()
            binding.swipeRefreshLayout.isRefreshing = false // Stop animation if not logged in
        } else {
            viewModel.fetchMyActivity()
        }
    }

    private fun setupRecyclerView() {
        ticketsAdapter = MyTicketsAdapter { clickedTicket ->
            // --- THIS IS THE DEFINITIVE, COMPLETE LOGIC ---

            // Check if the ticket has a transaction status, which means it was purchased.
            if (clickedTicket.transactionStatus != null) {
                // This is a ticket the user BOUGHT.
                // Open the PurchasedTicketDetailActivity where they can take action.
                val intent = Intent(requireContext(), PurchasedTicketDetailActivity::class.java).apply {
                    putExtra("TICKET_DATA", clickedTicket)
                }
                ticketActionLauncher.launch(intent)

            } else {
                // This is a ticket the user LISTED FOR SALE.
                if (clickedTicket.listingStatus.lowercase() == "live") {
                    // If the ticket is 'live', open the screen where they can manage it.
                    val intent = Intent(requireContext(), ManageListingActivity::class.java).apply {
                        putExtra("TICKET_DATA", clickedTicket)
                    }
                    ticketActionLauncher.launch(intent)
                } else {
                    // For any other status ('sold', 'expired'), the listing is no longer active,
                    // so just show an informative message.
                    Toast.makeText(
                        requireContext(),
                        "This listing is currently ${clickedTicket.listingStatus} and cannot be modified.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        binding.rvMyTickets.apply {
            adapter = ticketsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupObservers() {
        viewModel.myActivityState.observe(viewLifecycleOwner) { state ->
            val isLoading = state is NetworkState.Loading
            binding.progressBar.isVisible = isLoading && !binding.swipeRefreshLayout.isRefreshing
            binding.swipeRefreshLayout.isRefreshing = isLoading && binding.swipeRefreshLayout.isRefreshing

            if (state is NetworkState.Success) {
                // Combine both purchased and listed tickets into a single list
                val allTickets = (state.data.purchasedTickets + state.data.listedTickets)
                    // Sort them logically: future live tickets first, then purchased, then past/sold.
                    .sortedWith(
                        compareBy(
                            { it.eventDate }, // Sort by date first
                            { it.listingStatus != "live" } // Then put "live" tickets at the top
                        )
                    ).reversed()

                binding.tvEmptyMessage.isVisible = allTickets.isEmpty()
                ticketsAdapter.submitList(allTickets)
            } else if (state is NetworkState.Error) {
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}