package com.scenein.settings.persentation.screen

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.scenein.databinding.ActivityTicketTransactionHistoryBinding
import com.scenein.settings.persentation.adapter.TicketTransactionHistoryAdapter

import com.scenein.settings.persentation.view_model.SettingsViewModel
import com.scenein.utils.EdgeToEdgeUtils
import com.scenein.utils.NetworkState

class TicketTransactionHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTicketTransactionHistoryBinding
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var transactionAdapter: TicketTransactionHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        binding = ActivityTicketTransactionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupObservers()

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadHistory()
        }
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadHistory() {
        viewModel.fetchTicketTransactionHistory()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TicketTransactionHistoryAdapter { clickedTransaction ->
            val intent = Intent(this, TicketTransactionDetailActivity::class.java).apply {
                putExtra("TRANSACTION_ID", clickedTransaction.transactionId)
            }
            startActivity(intent)
        }
        binding.rvTransactions.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(this@TicketTransactionHistoryActivity)
        }
    }

    private fun setupObservers() {
        viewModel.transactionHistoryState.observe(this) { state ->
            val isLoading = state is NetworkState.Loading
            binding.progressBar.isVisible = isLoading && !binding.swipeRefreshLayout.isRefreshing
            binding.swipeRefreshLayout.isRefreshing = isLoading && binding.swipeRefreshLayout.isRefreshing

            if (state is NetworkState.Success) {
                binding.tvEmptyMessage.isVisible = state.data.isEmpty()
                transactionAdapter.submitList(state.data)
            } else if (state is NetworkState.Error) {
                Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}