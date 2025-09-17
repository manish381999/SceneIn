package com.scenein.tickets.presentation.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.scenein.databinding.FragmentMyTicketsBinding
import com.scenein.tickets.data.models.Ticket
import com.scenein.tickets.presentation.adapter.MyTicketsAdapter
import com.scenein.tickets.presentation.view_model.TicketViewModel
import com.scenein.utils.NetworkState
import com.scenein.utils.SP

class MyTicketsFragment : Fragment() {

    private var _binding: FragmentMyTicketsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TicketViewModel by viewModels()
    private lateinit var ticketsAdapter: MyTicketsAdapter

    private val ticketActionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            binding.swipeRefreshLayout.isRefreshing = true
            loadUserTickets()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyTicketsBinding.inflate(inflater, container, false)
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
            binding.swipeRefreshLayout.isRefreshing = false
        } else {
            viewModel.fetchMyActivity()
        }
    }

    private fun setupRecyclerView() {
        ticketsAdapter = MyTicketsAdapter { clickedTicket ->
            if (clickedTicket.transactionId != null) {
                // Purchased ticket
                val intent = Intent(requireContext(), PurchasedTicketDetailActivity::class.java)
                    .apply { putExtra("TICKET_DATA", clickedTicket) }
                ticketActionLauncher.launch(intent)
            } else {
                // Listed ticket
                if (clickedTicket.listingStatus.lowercase() == "live") {
                    val intent = Intent(requireContext(), ManageListingActivity::class.java)
                        .apply { putExtra("TICKET_DATA", clickedTicket) }
                    ticketActionLauncher.launch(intent)
                } else {
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

            when (state) {
                is NetworkState.Success -> {
                    // Combine purchased + listed tickets
                    val allTickets: List<Ticket> = state.data.purchasedTickets + state.data.listedTickets

                    // --- Sort tickets: live first, then purchased, then expired/delisted by event date descending
                    val sortedTickets = allTickets.sortedWith(
                        compareBy<Ticket> {
                            it.listingStatus.lowercase() != "live" && it.transactionId == null
                        }.thenByDescending { it.eventDate }
                    )

                    Log.d("MyTicketsFragment", "Tickets received: ${sortedTickets.size}")
                    sortedTickets.forEach { Log.d("MyTicketsFragment", it.toString()) }

                    binding.tvEmptyMessage.isVisible = sortedTickets.isEmpty()
                    binding.rvMyTickets.isVisible = sortedTickets.isNotEmpty()

                    ticketsAdapter.submitList(sortedTickets)
                }
                is NetworkState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
