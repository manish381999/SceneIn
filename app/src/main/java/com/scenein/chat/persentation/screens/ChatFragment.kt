package com.scenein.chat.persentation.screens

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.scenein.utils.SP
import com.scenein.chat.persentation.adapter.ConversationAdapter
import com.scenein.chat.persentation.view_model.ChatFilter // Make sure ChatFilter is imported
import com.scenein.chat.persentation.view_model.ChatViewModel
import com.scenein.databinding.FragmentChatBinding
import com.scenein.utils.NetworkState


class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var currentUserId: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentUserId = SP.getString(requireContext(), SP.USER_ID) ?: ""
        if (currentUserId.isEmpty()) {
            binding.tvNoChats.text = "Please log in to see your messages."
            binding.tvNoChats.visibility = View.VISIBLE
            return
        }
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        if (::currentUserId.isInitialized && currentUserId.isNotEmpty()) {
            // 1. Reset the filter in the ViewModel to the default state.
            viewModel.resetFilterToDefault()

            // 2. Visually reset the ChipGroup to show "All" as checked.
            binding.chipGroupCategories.check(binding.chipAll.id)

            // 3. Fetch the latest conversation data. The ViewModel will automatically apply the "ALL" filter.
            viewModel.fetchConversations()
        }
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter { conversation ->
            val intent = Intent(requireActivity(), ChatActivity::class.java).apply {
                putExtra("receiver_id", conversation.otherUserId)
                putExtra("receiver_name", conversation.name)
                putExtra("receiver_profile_pic", conversation.profilePic)
            }
            startActivity(intent)
        }
        binding.rvConversations.adapter = conversationAdapter
    }

    private fun setupClickListeners() {
//        binding.fabNewMessage.setOnClickListener {
//            Toast.makeText(requireContext(), "New Message Clicked", Toast.LENGTH_SHORT).show()
//        }

        binding.chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                binding.chipAll.isChecked = true
                viewModel.setFilter(ChatFilter.ALL)
                return@setOnCheckedStateChangeListener
            }

            // --- The "Groups" case has been removed from here ---
            when (checkedIds[0]) {
                binding.chipAll.id -> viewModel.setFilter(ChatFilter.ALL)
                binding.chipRequests.id -> viewModel.setFilter(ChatFilter.REQUESTS)
                binding.chipUnread.id -> viewModel.setFilter(ChatFilter.UNREAD)
            }
        }

        binding.searchBar.setOnClickListener {
            Toast.makeText(requireContext(), "Search Clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.conversationsState.observe(viewLifecycleOwner) { state ->
            binding.progressBar.visibility = if (state is NetworkState.Loading) View.VISIBLE else View.GONE

            when (state) {
                is NetworkState.Success -> {
                    binding.rvConversations.visibility = View.VISIBLE
                    conversationAdapter.submitList(state.data)

                    if (state.data.isEmpty()) {
                        binding.tvNoChats.visibility = View.VISIBLE
                        // --- The "Groups" case has been removed from here ---
                        binding.tvNoChats.text = when (viewModel.currentFilter.value) {
                            ChatFilter.REQUESTS -> "No new message requests"
                            ChatFilter.UNREAD -> "All caught up!"
                            else -> "No conversations yet"
                        }
                    } else {
                        binding.tvNoChats.visibility = View.GONE
                    }
                }
                is NetworkState.Error -> {
                    binding.rvConversations.visibility = View.GONE
                    binding.tvNoChats.visibility = View.VISIBLE
                    binding.tvNoChats.text = state.message
                }
                is NetworkState.Loading -> {
                    binding.rvConversations.visibility = View.GONE
                    binding.tvNoChats.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}