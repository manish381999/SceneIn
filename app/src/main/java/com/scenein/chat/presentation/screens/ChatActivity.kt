package com.scenein.chat.presentation.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.widget.addTextChangedListener
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.scenein.chat.data.model.MediaItem
import com.scenein.chat.data.model.Message
import com.scenein.chat.data.model.MessageStatus
import com.scenein.chat.data.model.createImageUrlJson
import com.scenein.R
import com.scenein.chat.presentation.adapter.ChatAdapter
import com.scenein.chat.presentation.adapter.ChatItem
import com.scenein.databinding.ActivityChatBinding
import com.scenein.utils.EdgeToEdgeUtils
import com.scenein.utils.NetworkState
import com.scenein.utils.SP
import com.scenein.chat.presentation.view_model.ChatViewModel
import com.scenein.profile.presentation.screen.UserProfileActivity

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private var newMessagesCount = 0
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var currentUserId: String
    private var receiverId: String? = null
    private var receiverName: String? = null

    private var currentMessages = mutableListOf<Message>()


    private val adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            val totalItemCount = chatAdapter.itemCount
            val layoutManager = binding.rvChatMessages.layoutManager as LinearLayoutManager
            val lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()

            // The position of the last item before the new messages were added
            val lastPosBeforeInsert = totalItemCount - itemCount

            // This 'if' block is where you check if the user is at the bottom.
            if (lastVisiblePosition == -1 || (lastPosBeforeInsert > 0 && lastVisiblePosition >= lastPosBeforeInsert - 1)) {
                // If they are at the bottom, scroll to the new message...
                binding.rvChatMessages.scrollToPosition(totalItemCount - 1)

                // ...and then add the new line here to mark the message as read.
                binding.rvChatMessages.post { markVisibleMessagesAsRead() }

            } else {
                // Otherwise, the user is scrolled up. Show the indicator.
                newMessagesCount += itemCount
                showNewMessagesButton()
            }
        }
    }

    companion object {
        var isActivityVisible = false
        var currentChattingWithId: String? = null
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(40)) { uris ->
        if (uris.isNotEmpty() && receiverId != null) {
            val tempId = UUID.randomUUID().toString()
            val uriStrings = uris.map { it.toString() }
            val contentJson = createImageUrlJson(uriStrings)
            val optimisticMessage = Message(
                "-1",
                currentUserId,
                receiverId!!,
                "image",
                contentJson,
                "",
                false,
                false,
                MessageStatus.SENDING,
                tempId
            )
            currentMessages.add(optimisticMessage)
            val processedList = processMessagesWithDates(currentMessages)
            chatAdapter.submitList(processedList)
            viewModel.uploadMultipleFilesAndSend(this, receiverId!!, uris, tempId)
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
                timestamp = data["timestamp"] as? String ?: "",
                isDelivered = (data["is_delivered"] as? String)?.toBoolean() ?: false,
                isRead = (data["is_read"] as? String)?.toBoolean() ?: false,
                status = MessageStatus.SENT
            )

            // Add the new message to the UI
            currentMessages.add(message)
            val processedList = processMessagesWithDates(currentMessages)
            chatAdapter.submitList(processedList)

        }
    }

    private fun markVisibleMessagesAsRead() {
        val layoutManager = binding.rvChatMessages.layoutManager as LinearLayoutManager
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()

        // Do nothing if the list is not visible or empty
        if (firstVisiblePosition == -1) return

        for (i in firstVisiblePosition..lastVisiblePosition) {
            val item = chatAdapter.currentList.getOrNull(i)
            if (item is ChatItem.MessageItem) {
                val message = item.message
                // Check if it's an incoming message and if we haven't marked it as read yet
                if (message.senderId != currentUserId && !message.isRead && message.messageId.isNotBlank()) {
                    // Tell the ViewModel to mark it as read
                    viewModel.markMessageAsRead(message.messageId)

                    // IMPORTANT: Also update our local copy of the message.
                    // This prevents us from sending the same "mark as read" request over and over.
                    val indexInSourceList = currentMessages.indexOfFirst { it.messageId == message.messageId }
                    if (indexInSourceList != -1) {
                        currentMessages[indexInSourceList] = currentMessages[indexInSourceList].copy(isRead = true)
                    }
                }
            }
        }
    }

    private val statusUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("STATUS_CHAIN", "2. ChatActivity's statusUpdateReceiver received the broadcast.") // LOG 2
            val statusStr = intent?.getStringExtra("status")
            val messageIdsJson = intent?.getStringExtra("message_ids_json")
            if (statusStr.isNullOrEmpty() || messageIdsJson.isNullOrEmpty()) return

            try {
                val newStatus = MessageStatus.valueOf(statusStr)
                val messageIds: List<String> = Gson().fromJson(messageIdsJson, object : TypeToken<List<String>>() {}.type)

                if (messageIds.isNotEmpty()) {
                    Log.d("STATUS_CHAIN", "3. Calling ViewModel with status: $newStatus for IDs: $messageIds") // LOG 3
                    viewModel.triggerStatusUpdate(messageIds, newStatus)
                }
            } catch (e: Exception) {
                Log.e("STATUS_CHAIN", "Error processing status update broadcast", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Make sure to clean and rebuild your project if you see any "Unresolved reference" errors!

        // --- FINAL, UPDATED CALL ---
        EdgeToEdgeUtils.setUpChatEdgeToEdge(
            rootView = binding.rootChatLayout,
            recyclerView = binding.rvChatMessages,
            fakeStatusBar = binding.fakeStatusBarBackground, // Pass the top bar
            fakeNavBar = binding.fakeNavBarBackground     // Pass the bottom bar
        )

        currentUserId = SP.getString(this, SP.USER_ID) ?: ""
        receiverId = intent.getStringExtra("receiver_id")
        receiverName = intent.getStringExtra("receiver_name")
        val receiverProfilePic = intent.getStringExtra("receiver_profile_pic")

        setupToolbar(receiverName, receiverProfilePic)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        if (receiverId != null) {
            viewModel.fetchChatHistory(currentUserId, receiverId!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatAdapter.unregisterAdapterDataObserver(adapterDataObserver)
    }

    override fun onResume() {
        super.onResume()
        isActivityVisible = true
        currentChattingWithId = receiverId
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, IntentFilter("new_message_broadcast"))
        LocalBroadcastManager.getInstance(this).registerReceiver(statusUpdateReceiver, IntentFilter("status_update_broadcast"))
    }

    override fun onPause() {
        super.onPause()
        isActivityVisible = false
        currentChattingWithId = null
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusUpdateReceiver)

        markVisibleMessagesAsRead()
    }

    private fun setupToolbar(name: String?, profilePicUrl: String?) {
        binding.toolbar.title = ""
        setSupportActionBar(binding.toolbar)
        binding.tvToolbarName.text = name
        Glide.with(this)
            .load(profilePicUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .into(binding.ivToolbarProfilePic)

        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // --- ADD THIS BLOCK ---
        // Make the profile picture and name clickable
        val profileClickListener = View.OnClickListener {
            // Ensure we have a valid receiverId before navigating
            if (!receiverId.isNullOrEmpty()) {
                navigateToUserProfile(receiverId!!)
            }
        }

        binding.ivToolbarProfilePic.setOnClickListener(profileClickListener)
        binding.tvToolbarName.setOnClickListener(profileClickListener)

    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(currentUserId) { clickedMessage, positionInMessage ->
            val clickedImageUrl = clickedMessage.getImageUrls().getOrNull(positionInMessage) ?: return@ChatAdapter
            val allMediaItems = currentMessages.filter { it.messageType == "image" }.flatMap { msg ->
                val sender = if (msg.senderId == currentUserId) "You" else (receiverName ?: "Them")
                msg.getImageUrls().map { url -> MediaItem(url, sender, msg.timestamp) }
            }
            val overallStartIndex = allMediaItems.indexOfFirst { it.url == clickedImageUrl && it.timestamp == clickedMessage.timestamp }
            if (overallStartIndex == -1) return@ChatAdapter
            val intent = Intent(this, MediaViewerActivity::class.java).apply {
                putExtra("media_items", ArrayList(allMediaItems))
                putExtra("start_position", overallStartIndex)
            }
            startActivity(intent)
        }
        binding.rvChatMessages.adapter = chatAdapter

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        binding.rvChatMessages.layoutManager = layoutManager

        binding.rvChatMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition()
                val totalItemCount = chatAdapter.itemCount

                // If user scrolls to the bottom, hide the "new messages" button
                if (totalItemCount > 0 && lastVisibleItemPosition == totalItemCount - 1) {
                    hideNewMessagesButton()
                }
                // ADD THIS LINE to mark messages as read when user scrolls
                markVisibleMessagesAsRead()
            }
        })

        chatAdapter.registerAdapterDataObserver(adapterDataObserver)
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
                val optimisticMessage = Message(
                    "-1",
                    currentUserId,
                    receiverId!!,
                    "text",
                    messageText,
                    "",
                    false,
                    false,
                    MessageStatus.SENDING,
                    tempId
                )
                currentMessages.add(optimisticMessage)
                val processedList = processMessagesWithDates(currentMessages)
                chatAdapter.submitList(processedList)
                viewModel.sendMessage(receiverId!!, "text", messageText, tempId)
                binding.etMessageInput.text.clear()
            }
        }
        binding.btnAddAttachment.setOnClickListener {
            imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnNewMessages.setOnClickListener {
            binding.rvChatMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
            hideNewMessagesButton() // Hide the button after clicking
        }
    }

    private fun observeViewModel() {
        // This observer handles the initial loading of the chat history.
        viewModel.chatHistoryState.observe(this) { state ->
            if (state is NetworkState.Success) {
                currentMessages = state.data.toMutableList()
                val processedList = processMessagesWithDates(currentMessages)
                chatAdapter.submitList(processedList)
            }
        }

        // This observer handles the result of sending a new message.
        viewModel.sendMessageState.observe(this) { state ->
            if (state is NetworkState.Success) {
                state.data?.sentMessage?.let { serverMessage ->
                    // --- FIX: Check that tempId is not null before comparing ---
                    val tempId = serverMessage.tempId
                    if (tempId != null) {
                        val index = currentMessages.indexOfFirst { it.tempId == tempId }
                        if (index != -1) {
                            currentMessages[index] = serverMessage
                            val processedList = processMessagesWithDates(currentMessages)
                            chatAdapter.submitList(processedList)
                        }
                    }
                }
            }
        }

        // This observer handles a failed message send.
        viewModel.failedMessageTempId.observe(this) { tempId ->
            if (tempId != null) {
                val index = currentMessages.indexOfFirst { it.tempId == tempId }
                if (index != -1) {
                    currentMessages[index] = currentMessages[index].copy(status = MessageStatus.FAILED)
                    val processedList = processMessagesWithDates(currentMessages)
                    chatAdapter.submitList(processedList)
                }
                Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show()
                viewModel.onFailedMessageHandled()
            }
        }

        // =====================================================================
        // == THIS IS THE MISSING CODE BLOCK THAT YOU NEED TO ADD ==
        // =====================================================================
        // This new observer listens for real-time status updates (DELIVERED/READ)
        // that are triggered by the silent FCM notifications.
        // In observeViewModel()
        viewModel.messageStatusUpdate.observe(this) { update ->
            if (update != null) {
                Log.d("STATUS_CHAIN", "4. LiveData observer in ChatActivity has fired.") // LOG 4
                val (messageIds, newStatus) = update
                var hasChanged = false

                // Create a NEW list. This is crucial for DiffUtil to detect changes.
                val updatedMessages = currentMessages.map { message ->
                    // Check if this message is one of the ones that needs its status updated.
                    if (message.messageId in messageIds) {
                        // Only upgrade a status (e.g., from SENT to DELIVERED).
                        // Don't downgrade (e.g., from READ back to DELIVERED).
                        if (message.status.ordinal < newStatus.ordinal) {
                            hasChanged = true
                            Log.d("STATUS_CHAIN", "5. Found message ${message.messageId} to update. New status: $newStatus") // LOG 5
                            // Return a *copy* of the message with the new status.
                            message.copy(status = newStatus)
                        } else {
                            message // Status is already the same or higher, no change needed.
                        }
                    } else {
                        message // This message is not in the update list, return it as is.
                    }
                }

                if (hasChanged) {
                    Log.d("STATUS_CHAIN", "6. Changes were made. Submitting new list to adapter.") // LOG 6
                    // Replace the old list with the new one.
                    currentMessages = updatedMessages.toMutableList()
                    // Submit the new list to the adapter.
                    val processedList = processMessagesWithDates(currentMessages)
                    chatAdapter.submitList(processedList)
                }

                // Acknowledge that the event has been handled.
                viewModel.onStatusUpdateHandled()
            }
        }
    }

    private fun processMessagesWithDates(messages: List<Message>): List<ChatItem> {
        if (messages.isEmpty()) return emptyList()

        val itemsWithDates = mutableListOf<ChatItem>()
        var lastDate: LocalDate? = null
        val userZoneId = ZoneId.systemDefault() // Get the user's time zone once

        for (message in messages) {
            val messageTimestamp = message.timestamp
            if (messageTimestamp.isBlank()) {
                itemsWithDates.add(ChatItem.MessageItem(message))
                continue
            }

            try {
                // Convert timestamp to user's local date
                val currentZonedDateTime = Instant.parse(messageTimestamp).atZone(userZoneId)
                val currentDate = currentZonedDateTime.toLocalDate()

                if (lastDate == null || !lastDate.isEqual(currentDate)) {
                    itemsWithDates.add(ChatItem.DateItem(formatDateSeparator(currentZonedDateTime)))
                }
                itemsWithDates.add(ChatItem.MessageItem(message))
                lastDate = currentDate
            } catch (e: Exception) {
                // If timestamp is invalid, just add the message
                itemsWithDates.add(ChatItem.MessageItem(message))
            }
        }
        return itemsWithDates
    }


    private fun formatDateSeparator(messageDate: ZonedDateTime): String {
        val today = ZonedDateTime.now(messageDate.zone)
        val yesterday = today.minusDays(1)

        return when {
            messageDate.toLocalDate().isEqual(today.toLocalDate()) -> "Today"
            messageDate.toLocalDate().isEqual(yesterday.toLocalDate()) -> "Yesterday"
            // Use ChronoUnit to see if the date was within the last 7 days
            ChronoUnit.DAYS.between(messageDate, today) < 7 ->
                messageDate.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
            else ->
                messageDate.format(DateTimeFormatter.ofPattern("E, d MMM yyyy", Locale.getDefault()))
        }
    }

    private fun navigateToUserProfile(userIdToView: String) {
        val intent = Intent(this, UserProfileActivity::class.java).apply {
            putExtra("user_id", userIdToView)
        }
        startActivity(intent)
    }

    private fun showNewMessagesButton() {
        binding.btnNewMessages.text = if (newMessagesCount > 1) "$newMessagesCount New Messages" else "New Message"
        binding.btnNewMessages.visibility = View.VISIBLE
    }

    private fun hideNewMessagesButton() {
        newMessagesCount = 0
        binding.btnNewMessages.visibility = View.GONE
    }
}