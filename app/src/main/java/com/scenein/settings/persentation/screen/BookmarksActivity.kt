package com.scenein.settings.persentation.screen

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.scenein.databinding.ActivityBookmarksBinding
import com.scenein.discover.presentation.adapter.FeedAdapter
import com.scenein.discover.presentation.screens.EventDetailActivity
import com.scenein.profile.persentation.screen.UserProfileActivity
import com.scenein.settings.persentation.view_model.SettingsViewModel
import com.scenein.utils.EdgeToEdgeUtils
import com.scenein.utils.NetworkState
import com.scenein.utils.SP

class BookmarksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookmarksBinding
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var currentUserId: String

    private val eventDetailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // When user returns, refresh the list to reflect any changes.
        // This handles cases where a user might un-bookmark from the detail screen.
        viewModel.fetchBookmarkedEvents()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        binding = ActivityBookmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = SP.getString(this, SP.USER_ID) ?: ""

        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        viewModel.fetchBookmarkedEvents()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        feedAdapter = FeedAdapter(
            currentUserId = currentUserId,
            onEventClicked = { eventId ->
                val intent = Intent(this, EventDetailActivity::class.java).apply {
                    putExtra("event_id", eventId)
                }
                eventDetailLauncher.launch(intent)
            },
            onJoinClicked = { eventId, _ -> viewModel.joinEvent(eventId) },
            onUnjoinClicked = { eventId, _ -> viewModel.unjoinEvent(eventId) },
            onAddBookmarkClicked = { eventId -> /* Already bookmarked, so this won't be called */ },
            onRemoveBookmarkClicked = { eventId -> viewModel.removeBookmark(eventId) },
            onConnectClicked = {},
            onConnectionProfileClicked = { connection ->
                val intent = Intent(this, UserProfileActivity::class.java).apply {
                    putExtra("user_id", connection.userId)
                }
                startActivity(intent)
            }
        )
        val layoutManager = LinearLayoutManager(this)


        binding.rvBookmarkedEvents.layoutManager = layoutManager
        binding.rvBookmarkedEvents.adapter = feedAdapter

        // 3. NOW it's safe to add the scroll listener that uses the layoutManager.
        binding.rvBookmarkedEvents.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount

                // Load more when the user is 5 items away from the end
                if (lastVisibleItemPosition > totalItemCount - 5) {
                    viewModel.fetchMoreBookmarkedEvents()
                }
            }
        })
    }

    private fun observeViewModel() {
        viewModel.bookmarkedEventsState.observe(this) { state ->
            binding.tvEmptyState.visibility = if (state is NetworkState.Success && state.data.isEmpty()) View.VISIBLE else View.GONE
            binding.rvBookmarkedEvents.visibility = if (state is NetworkState.Success) View.VISIBLE else View.GONE

            if (state is NetworkState.Success) {
                feedAdapter.submitList(state.data)
            } else if (state is NetworkState.Error) {
                Snackbar.make(binding.root, state.message ?: "Failed to load bookmarks", Snackbar.LENGTH_LONG).show()
            }
        }

        // Observers for showing feedback Snackbars
        viewModel.joinEventState.observe(this) { state ->
            if (state == null) return@observe
            val message = if (state is NetworkState.Success) state.data else (state as NetworkState.Error).message
            Snackbar.make(binding.root, message ?: "An error occurred", Snackbar.LENGTH_SHORT).show()
            viewModel.clearJoinState()
        }

        viewModel.unjoinEventState.observe(this) { state ->
            if (state == null) return@observe
            val message = if (state is NetworkState.Success) state.data else (state as NetworkState.Error).message
            Snackbar.make(binding.root, message ?: "An error occurred", Snackbar.LENGTH_SHORT).show()
            viewModel.clearUnjoinState()
        }

        viewModel.bookmarkState.observe(this) { state ->
            if (state == null) return@observe
            val message = if (state is NetworkState.Success) state.data else (state as NetworkState.Error).message
            Snackbar.make(binding.root, message ?: "An error occurred", Snackbar.LENGTH_SHORT).show()
            viewModel.clearBookmarkState()
        }
    }
}