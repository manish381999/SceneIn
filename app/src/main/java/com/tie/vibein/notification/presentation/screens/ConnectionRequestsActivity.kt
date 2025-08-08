package com.tie.vibein.notifications.presentation.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.databinding.ActivityConnectionRequestsBinding
import com.tie.vibein.notification.presentation.view_model.NotificationViewModel
import com.tie.vibein.notifications.data.models.ConnectionRequest
import com.tie.vibein.notifications.presentation.adapter.ConnectionRequestsAdapter
import com.tie.vibein.profile.persentation.screen.UserProfileActivity
import com.tie.vibein.profile.persentation.view_model.ProfileViewModel
import com.tie.vibein.utils.EdgeToEdgeUtils
import com.tie.vibein.utils.NetworkState

class ConnectionRequestsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConnectionRequestsBinding
    private val notificationViewModel: NotificationViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private lateinit var requestsAdapter: ConnectionRequestsAdapter
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        binding = ActivityConnectionRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = SP.getString(this, SP.USER_ID, "") ?: ""

        val requestsList: ArrayList<ConnectionRequest>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("REQUESTS_LIST", ConnectionRequest::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("REQUESTS_LIST")
        }

        if (currentUserId.isEmpty() || requestsList == null) {
            Toast.makeText(this, "Could not load requests.", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        setupToolbar()
        setupRecyclerView()
        setupObservers()

        binding.tvEmptyMessage.isVisible = requestsList.isEmpty()
        requestsAdapter.submitList(requestsList)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        requestsAdapter = ConnectionRequestsAdapter(
            // --- NEW: Handle clicks on the user's profile ---
            onProfileClick = { request ->
                // Navigate to the UserProfileActivity of the person who sent the request
                val intent = Intent(this, UserProfileActivity::class.java).apply {
                    putExtra("user_id", request.userId)
                }
                startActivity(intent)
            },
            onConfirm = { request ->
                profileViewModel.respondToConnectionRequest(
                    connectionId = request.connectionId.toString(),
                    response = "accepted",
                    viewerId = currentUserId,
                    profileId = request.userId
                )
            },
            onDelete = { request ->
                profileViewModel.respondToConnectionRequest(
                    connectionId = request.connectionId.toString(),
                    response = "declined",
                    viewerId = currentUserId,
                    profileId = request.userId
                )
            }
        )
        binding.rvRequests.layoutManager = LinearLayoutManager(this)
        binding.rvRequests.adapter = requestsAdapter
    }

    private fun setupObservers() {
        profileViewModel.connectionActionState.observe(this) { state ->
            // You could show a loading shimmer/progress on the specific list item here
            when(state) {
                is NetworkState.Success -> {
                    Toast.makeText(this, state.data, Toast.LENGTH_SHORT).show()
                    // Signal the previous activity to refresh its data and close this screen
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                is NetworkState.Error -> {
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                is NetworkState.Loading -> {
                    // You could disable the buttons in the adapter here
                }
            }
        }

        // We can listen to the full notification state to refresh the list live
        // after an action, which provides a great UX.
        notificationViewModel.notificationState.observe(this) { state ->
            if (state is NetworkState.Success) {
                binding.tvEmptyMessage.isVisible = state.data.connectionRequests.isEmpty()
                requestsAdapter.submitList(state.data.connectionRequests)
            }
        }
    }
}