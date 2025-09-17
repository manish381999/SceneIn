package com.scenein.notification.presentation.screens

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.scenein.utils.SP
import com.scenein.BaseActivity
import com.scenein.databinding.ActivityNotificationsBinding
import com.scenein.discover.presentation.screens.EventDetailActivity
import com.scenein.notification.data.models.HeaderData
import com.scenein.notification.presentation.view_model.NotificationViewModel
import com.scenein.notification.data.models.Notification
import com.scenein.notification.presentation.adapter.NotificationsAdapter
import com.scenein.profile.presentation.screen.UserProfileActivity
import com.scenein.profile.presentation.view_model.ProfileViewModel

import com.scenein.utils.EdgeToEdgeUtils
import com.scenein.utils.NetworkState

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private val notificationViewModel: NotificationViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels() // Correct, no factory needed
    private lateinit var notificationsAdapter: NotificationsAdapter
    private lateinit var currentUserId: String

    private val connectionRequestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            loadDataAndMarkAsRead()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = SP.getString(this, SP.USER_ID, "") ?: ""
        if (currentUserId.isEmpty()) { finish(); return }

        setupToolbar()
        setupRecyclerView()
        setupObservers()

        binding.swipeRefreshLayout.setOnRefreshListener { notificationViewModel.fetchNotifications() }
    }

    override fun onResume() { super.onResume(); loadDataAndMarkAsRead() }

    private fun loadDataAndMarkAsRead() {
        notificationViewModel.fetchNotifications()
        Handler(Looper.getMainLooper()).postDelayed({
            notificationViewModel.markNotificationsAsRead()
        }, 3000)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        notificationsAdapter = NotificationsAdapter(
            onHeaderClick = { requests ->
                val intent = Intent(this, ConnectionRequestsActivity::class.java).apply {
                    putParcelableArrayListExtra("REQUESTS_LIST", ArrayList(requests))
                }
                connectionRequestLauncher.launch(intent)
            },
            onItemClick = { notification -> handleItemClick(notification) }
        )
        binding.rvNotifications.adapter = notificationsAdapter
    }

    private fun setupObservers() {
        notificationViewModel.notificationState.observe(this) { state ->
            val isLoading = state is NetworkState.Loading
            binding.progressBar.isVisible = isLoading && !binding.swipeRefreshLayout.isRefreshing
            binding.swipeRefreshLayout.isRefreshing = isLoading

            when (state) {
                is NetworkState.Success -> {
                    val response = state.data // ActivityFeedResponse
                    val displayList = mutableListOf<Any>()
                    if (response.connectionRequests.isNotEmpty()) {
                        displayList.add(
                            HeaderData(
                                response.connectionRequests.size,
                                response.connectionRequests
                            )
                        )
                    }
                    displayList.addAll(response.activityFeed)
                    binding.tvEmptyMessage.isVisible = displayList.isEmpty()
                    notificationsAdapter.submitList(displayList)
                }
                is NetworkState.Error -> {
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                is NetworkState.Loading -> { /* Handled above */ }
            }
        }
    }

    private fun handleItemClick(notification: Notification) {
        when (notification.notificationType) {

            // --- Navigation to User Profile ---
            "connection_accepted" -> {
                val userId = notification.relatedUserId
                if (!userId.isNullOrBlank()) {
                    val intent = Intent(this, UserProfileActivity::class.java).apply {
                        putExtra("user_id", userId)
                    }
                    startActivity(intent)
                }
            }

            // --- Navigation to Event Detail Screen ---
            "event_join", "event_unjoin" -> {
                val eventId = notification.relatedId
                if (!eventId.isNullOrBlank()) {
                    val intent = Intent(this, EventDetailActivity::class.java).apply {
                        // Your EventDetailActivity expects an Int or a String? Let's assume String
                        putExtra("event_id", eventId)
                    }
                    startActivity(intent)
                }
            }

            // --- Navigation to the "My Tickets" Tab ---
            "ticket_sold", "payout_queued", "payout_processed",
            "dispute_raised", "dispute_resolved", "refund_queued",
            "refund_processed", "resold" -> {
                val intent = Intent(this, BaseActivity::class.java).apply {
                    // Tell the BaseActivity to navigate to the tickets fragment
                    putExtra("NAVIGATE_TO", "TICKETS")
                    // Tell the tickets fragment to select the "My Tickets" tab (position 1)
                    putExtra("TICKETS_SUB_TAB", 1)
                    // Clear the back stack and create a new task
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish() // Finish the notification activity
            }

            // For a connection request, the main action is the buttons.
            // The item click can navigate to the user's profile.
            "connection_request" -> {
                val userId = notification.relatedUserId
                if (!userId.isNullOrBlank()) {
                    val intent = Intent(this, UserProfileActivity::class.java).apply {
                        putExtra("user_id", userId)
                    }
                    startActivity(intent)
                }
            }

            // Default fallback if a type has no defined navigation
            else -> {
                Toast.makeText(this, "Tapped notification: ${notification.title}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
