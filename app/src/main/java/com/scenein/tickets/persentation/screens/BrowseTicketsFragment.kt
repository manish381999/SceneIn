package com.scenein.tickets.persentation.screens

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.scenein.databinding.FragmentBrowseTicketsBinding
import com.scenein.tickets.persentation.adapter.BrowseTicketsAdapter
import com.scenein.tickets.persentation.view_model.TicketViewModel
import com.scenein.utils.NetworkState

class BrowseTicketsFragment : Fragment() {
    private var _binding: FragmentBrowseTicketsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TicketViewModel by viewModels()
    private lateinit var ticketsAdapter: BrowseTicketsAdapter

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentBrowseTicketsBinding.inflate(inflater, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchBrowseTickets()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchBrowseTickets()
    }

    private fun setupRecyclerView() {
        ticketsAdapter = BrowseTicketsAdapter { clickedTicket ->
            val intent = Intent(requireContext(), TicketDetailActivity::class.java).apply {
                putExtra("TICKET_DATA", clickedTicket)
            }
            startActivity(intent)
        }
        binding.rvBrowseTickets.apply {
            adapter = ticketsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupObservers() {
        viewModel.browseTicketsState.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state is NetworkState.Loading && !binding.swipeRefreshLayout.isRefreshing
            binding.swipeRefreshLayout.isRefreshing = state is NetworkState.Loading && binding.swipeRefreshLayout.isRefreshing
            when (state) {
                is NetworkState.Success -> {
                    binding.tvEmptyMessage.isVisible = state.data.isEmpty()
                    ticketsAdapter.submitList(state.data)
                }
                is NetworkState.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                else -> {}
            }
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}