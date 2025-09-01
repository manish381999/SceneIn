package com.scenein.profile.persentation.screen

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.scenein.utils.SP
import com.scenein.chat.persentation.screens.ChatActivity
import com.scenein.databinding.FragmentUserProfileConnectionsBinding
import com.scenein.profile.persentation.adapter.ConnectionsAdapter
import com.scenein.profile.persentation.view_model.ProfileViewModel
import com.scenein.utils.NetworkState

class UserProfileConnectionsFragment : Fragment() {

    private var _binding: FragmentUserProfileConnectionsBinding? = null
    private val binding get() = _binding!!
    // Get a ViewModel instance shared with the parent Activity
    private val viewModel: ProfileViewModel by viewModels({ requireActivity() })
    private lateinit var connectionsAdapter: ConnectionsAdapter

    // The user whose connections we are viewing
    private var profileUserId: String? = null
    // The user who is currently logged in
    private lateinit var currentLoggedInUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            profileUserId = it.getString(ARG_USER_ID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUserProfileConnectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentLoggedInUserId = SP.getString(requireContext(), SP.USER_ID) ?: ""

        setupRecyclerView()
        setupObservers()

        if (!profileUserId.isNullOrEmpty()) {
            // Fetch the connections for the profile being viewed.
            viewModel.fetchPublicUserConnections(profileUserId!!)
        }
    }

    private fun setupRecyclerView() {
        connectionsAdapter = ConnectionsAdapter(
            currentUserId = currentLoggedInUserId, // Use the variable from your fragment
            onProfileClick = { clickedConnection ->
                val intent = Intent(requireContext(), UserProfileActivity::class.java).apply {
                    putExtra("user_id", clickedConnection.userId)
                }
                startActivity(intent)
            },
            onMessageClick = { clickedConnection ->
                val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                    putExtra("receiver_id", clickedConnection.userId)
                    putExtra("receiver_name", clickedConnection.name)
                    putExtra("receiver_profile_pic", clickedConnection.profilePic)
                }
                startActivity(intent)
            },
            onDisconnectClick = { connectionToDisconnect ->
                showDisconnectConfirmationDialog(connectionToDisconnect.name, connectionToDisconnect.userId)
            }
        )
        binding.rvConnections.adapter = connectionsAdapter
        binding.rvConnections.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        binding.rvConnections.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupObservers() {
        viewModel.myConnectionsState.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state is NetworkState.Loading
            when(state) {
                is NetworkState.Success -> {
                    binding.tvEmptyMessage.isVisible = state.data.isEmpty()
                    connectionsAdapter.submitList(state.data)
                }
                is NetworkState.Error -> {
                    binding.tvEmptyMessage.isVisible = true
                 //   binding.tvEmptyMessage.text = "Could not load connections."
                    Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                else -> { /* Loading state handled */ }
            }
        }

        // Observer for the result of the disconnect action
        viewModel.connectionActionState.observe(viewLifecycleOwner) { state ->
            if (state is NetworkState.Success) {
                Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT).show()
                // Refresh the connections list after a successful disconnect.
                if (!profileUserId.isNullOrEmpty()) {
                    viewModel.fetchPublicUserConnections(profileUserId!!)
                }
            } else if (state is NetworkState.Error) {
                Toast.makeText(requireContext(), "Action Failed: ${state.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDisconnectConfirmationDialog(userName: String, userIdToDisconnect: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Disconnect from $userName?")
            .setMessage("They will no longer be in your connections list, and you will no longer be in theirs.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Disconnect") { _, _ ->
                // The current logged in user is the one performing the action
                viewModel.removeConnection(userIdToDisconnect)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_USER_ID = "user_id"
        fun newInstance(userId: String): UserProfileConnectionsFragment {
            return UserProfileConnectionsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)
                }
            }
        }
    }
}