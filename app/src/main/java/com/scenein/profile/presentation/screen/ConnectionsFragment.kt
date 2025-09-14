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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.scenein.utils.SP
import com.scenein.chat.presentation.screens.ChatActivity // Import ChatActivity
import com.scenein.databinding.FragmentConnectionsBinding
import com.scenein.profile.presentation.adapter.ConnectionsAdapter
import com.scenein.profile.presentation.view_model.ProfileViewModel
import com.scenein.utils.NetworkState

class ConnectionsFragment : Fragment() {

    private var _binding: FragmentConnectionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels({ requireParentFragment() })

    private lateinit var connectionsAdapter: ConnectionsAdapter
    private lateinit var currentLoggedInUserId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentLoggedInUserId = SP.getString(requireContext(), SP.USER_ID) ?: ""
        if (currentLoggedInUserId.isEmpty()) { return }

        setupRecyclerView()
        setupObservers()

        viewModel.fetchMyConnections()
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

        binding.rvConnections.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = connectionsAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
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
                    Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }

        viewModel.connectionActionState.observe(viewLifecycleOwner) { state ->
            if (state is NetworkState.Success) {
                Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT).show()
                // Refresh the connections list after a successful disconnect.
                viewModel.fetchMyConnections()
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
                viewModel.removeConnection( userIdToDisconnect)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}