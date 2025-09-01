package com.scenein.chat.persentation.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import com.scenein.chat.persentation.adapter.ChatAdapter
import com.scenein.chat.persentation.adapter.ChatItem
import com.scenein.databinding.ActivityChatBinding
import com.scenein.utils.EdgeToEdgeUtils
import com.scenein.utils.NetworkState
import com.scenein.utils.SP
import com.scenein.chat.persentation.view_model.ChatViewModel

import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
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
            if (totalItemCount > 0) {
                binding.rvChatMessages.smoothScrollToPosition(totalItemCount - 1)
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

            // --- THIS IS THE CRITICAL FIX ---
            // Now that the message is on screen, tell the server it's been read.
            // This will trigger the blue tick for the sender.
            if (message.messageId.isNotBlank()) {
                viewModel.markMessageAsRead(message.messageId)
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
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.messageInputLayout.post {
            EdgeToEdgeUtils.setUpInteractiveEdgeToEdge(
                rootView = binding.root,
                contentView = binding.rvChatMessages,
                floatingView = binding.messageInputLayout
            )
        }


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
    }

    private fun setupToolbar(name: String?, profilePicUrl: String?) {
        binding.toolbar.title = ""
        setSupportActionBar(binding.toolbar)
        binding.tvToolbarName.text = name
        Glide.with(this).load(profilePicUrl).placeholder(R.drawable.ic_profile_placeholder).into(binding.ivToolbarProfilePic)
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
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
        binding.rvChatMessages.layoutManager = LinearLayoutManager(this)
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
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
        var lastDate: Date? = null
        for (message in messages) {
            val messageTimestamp = message.timestamp
            if (messageTimestamp.isBlank()) {
                itemsWithDates.add(ChatItem.MessageItem(message))
                continue
            }
            val currentDate = try { inputFormat.parse(messageTimestamp) } catch (e: Exception) { null } ?: continue
            if (lastDate == null || !isSameDay(lastDate, currentDate)) {
                itemsWithDates.add(ChatItem.DateItem(formatDateSeparator(currentDate)))
            }
            itemsWithDates.add(ChatItem.MessageItem(message))
            lastDate = currentDate
        }
        return itemsWithDates
    }

    private fun isSameDay(date1: Date?, date2: Date?): Boolean {
        if (date1 == null || date2 == null) return false
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatDateSeparator(date: Date): String {
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val lastWeek = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        val messageDate = Calendar.getInstance().apply { time = date }
        return when {
            isSameDay(messageDate.time, today.time) -> "Today"
            isSameDay(messageDate.time, yesterday.time) -> "Yesterday"
            messageDate.after(lastWeek) -> SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
            else -> SimpleDateFormat("E, d MMM yyyy", Locale.getDefault()).format(date)
        }
    }
}