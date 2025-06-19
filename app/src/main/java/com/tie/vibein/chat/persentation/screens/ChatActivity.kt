package com.tie.vibein.chat.persentation.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.R
import com.tie.vibein.chat.data.models.Message
import com.tie.vibein.chat.data.models.MessageStatus
import com.tie.vibein.chat.persentation.adapter.ChatAdapter
import com.tie.vibein.chat.presentation.viewmodel.ChatViewModel
import com.tie.vibein.databinding.ActivityChatBinding
import com.tie.vibein.utils.NetworkState
import java.io.Serializable
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var currentUserId: String
    private var receiverId: String? = null

    companion object {
        var isActivityVisible = false
        var currentChattingWithId: String? = null
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val tempId = UUID.randomUUID().toString()
            val optimisticMessage = Message("-1", currentUserId, receiverId!!, "image", it.toString(), "", MessageStatus.SENDING, tempId)
            chatAdapter.addMessage(optimisticMessage)
            binding.rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)
            viewModel.uploadMediaAndSendMessage(this, currentUserId, receiverId!!, it, "image", tempId)
        }
    }

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val data = intent?.getSerializableExtra("message_data") as? HashMap<*, *> ?: return
            val message = Message(
                messageId = data["message_id"] as? String ?: "",
                senderId = data["sender_id"] as? String ?: "",
                receiverId = data["receiver_id"] as? String ?: "",
                messageType = data["chat_message_type"] as? String ?: "text",
                messageContent = data["message_content"] as? String ?: "",
                timestamp = data["timestamp"] as? String ?: ""
            )
            chatAdapter.addMessage(message)
            binding.rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = SP.getPreferences(this, SP.USER_ID) ?: ""
        receiverId = intent.getStringExtra("receiver_id")
        val receiverName = intent.getStringExtra("receiver_name")
        val receiverProfilePic = intent.getStringExtra("receiver_profile_pic")

        setupToolbar(receiverName, receiverProfilePic)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        if (receiverId != null) {
            viewModel.fetchChatHistory(currentUserId, receiverId!!)
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityVisible = true
        currentChattingWithId = receiverId
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, IntentFilter("new_message_broadcast"))
    }

    override fun onPause() {
        super.onPause()
        isActivityVisible = false
        currentChattingWithId = null
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
    }

    private fun setupToolbar(name: String?, profilePicUrl: String?) {
        binding.toolbar.title = ""
        setSupportActionBar(binding.toolbar)
        binding.tvToolbarName.text = name
        Glide.with(this).load(profilePicUrl).placeholder(R.drawable.ic_profile_placeholder).into(binding.ivToolbarProfilePic)
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(currentUserId)
        val layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvChatMessages.adapter = chatAdapter
        binding.rvChatMessages.layoutManager = layoutManager
    }

    private fun setupClickListeners() {
        binding.btnSendMessage.isEnabled = false
        binding.etMessageInput.addTextChangedListener { text ->
            binding.btnSendMessage.isEnabled = text.toString().trim().isNotEmpty()
        }

        binding.btnSendMessage.setOnClickListener {
            val messageText = binding.etMessageInput.text.toString().trim()
            if (messageText.isNotEmpty() && receiverId != null) {
                val tempId = UUID.randomUUID().toString()
                val optimisticMessage = Message("-1", currentUserId, receiverId!!, "text", messageText, "", MessageStatus.SENDING, tempId)

                chatAdapter.addMessage(optimisticMessage)
                binding.rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)

                viewModel.sendMessage(currentUserId, receiverId!!, "text", messageText, tempId)
                binding.etMessageInput.text.clear()
            }
        }
        binding.btnAddAttachment.setOnClickListener { imagePickerLauncher.launch("image/*") }
    }

    private fun observeViewModel() {
        viewModel.chatHistoryState.observe(this) { state ->
            if (state is NetworkState.Success) {
                chatAdapter.submitList(state.data)
                binding.rvChatMessages.post {
                    binding.rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)
                }
            }
        }

        viewModel.sendMessageState.observe(this) { state ->
            if (state is NetworkState.Success) {
                state.data?.sentMessage?.let { serverMessage ->
                    chatAdapter.updateSentMessage(serverMessage)
                }
            }
        }

        viewModel.failedMessageTempId.observe(this) { tempId ->
            if (tempId != null) {
                chatAdapter.markAsFailed(tempId)
                Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show()
                viewModel.onFailedMessageHandled()
            }
        }
    }
}