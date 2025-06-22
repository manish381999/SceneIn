package com.tie.vibein.chat.persentation.screens

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
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.R
import com.tie.vibein.chat.data.models.MediaItem
import com.tie.vibein.chat.data.models.Message
import com.tie.vibein.chat.data.models.MessageStatus
import com.tie.vibein.chat.data.models.createImageUrlJson
import com.tie.vibein.chat.persentation.adapter.ChatAdapter
import com.tie.vibein.chat.persentation.adapter.ChatItem
import com.tie.vibein.chat.presentation.viewmodel.ChatViewModel
import com.tie.vibein.databinding.ActivityChatBinding
import com.tie.vibein.utils.NetworkState
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
            val optimisticMessage = Message("-1", currentUserId, receiverId!!, "image", contentJson, "", MessageStatus.SENDING, tempId)

            currentMessages.add(optimisticMessage)
            val processedList = processMessagesWithDates(currentMessages)
            chatAdapter.submitList(processedList)

            // Call the correct ViewModel function for multiple files
            viewModel.uploadMultipleFilesAndSend(this, currentUserId, receiverId!!, uris, tempId)
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
            currentMessages.add(message)
            val processedList = processMessagesWithDates(currentMessages)
            chatAdapter.submitList(processedList)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = SP.getPreferences(this, SP.USER_ID) ?: ""
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
        chatAdapter = ChatAdapter(currentUserId) { clickedMessage, positionInMessage ->

            // 1. Get the specific URL that was clicked by the user.
            val clickedImageUrl = clickedMessage.getImageUrls().getOrNull(positionInMessage)
            if (clickedImageUrl == null) {
                Log.e("MediaViewerDebug", "Error: Clicked image URL was not found.")
                return@ChatAdapter
            }

            // 2. Build the master list of all MediaItems from the entire conversation.
            val allMediaItems = currentMessages
                .filter { it.messageType == "image" }
                .flatMap { msg ->
                    val sender = if (msg.senderId == currentUserId) "You" else (receiverName ?: "Them")
                    msg.getImageUrls().map { url ->
                        MediaItem(url, sender, msg.timestamp)
                    }
                }

            // 3. Find the index of our clicked image within this complete, flat list.
            // This is robust because it matches on primitive values.
            val overallStartIndex = allMediaItems.indexOfFirst { it.url == clickedImageUrl && it.timestamp == clickedMessage.timestamp }

            if (overallStartIndex == -1) {
                Log.e("MediaViewerDebug", "Could not find the clicked media item in the master list.")
                return@ChatAdapter
            }

            Log.d("MediaViewerDebug", "Final calculated start position: $overallStartIndex")

            // 4. Launch the viewer with the correct data.
            val intent = Intent(this, MediaViewerActivity::class.java).apply {
                putExtra("media_items", ArrayList(allMediaItems))
                putExtra("start_position", overallStartIndex)
            }
            startActivity(intent)
        }

        val layoutManager = LinearLayoutManager(this)
        binding.rvChatMessages.adapter = chatAdapter
        binding.rvChatMessages.layoutManager = layoutManager
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
                val optimisticMessage = Message("-1", currentUserId, receiverId!!, "text", messageText, "", MessageStatus.SENDING, tempId)

                currentMessages.add(optimisticMessage)
                val processedList = processMessagesWithDates(currentMessages)
                chatAdapter.submitList(processedList)

                viewModel.sendMessage(currentUserId, receiverId!!, "text", messageText, tempId)
                binding.etMessageInput.text.clear()
            }
        }
        binding.btnAddAttachment.setOnClickListener {
            imagePickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    private fun observeViewModel() {
        viewModel.chatHistoryState.observe(this) { state ->
            if (state is NetworkState.Success) {
                currentMessages = state.data.toMutableList()
                val processedList = processMessagesWithDates(currentMessages)
                chatAdapter.submitList(processedList)
            }
        }

        viewModel.sendMessageState.observe(this) { state ->
            if (state is NetworkState.Success) {
                state.data?.sentMessage?.let { serverMessage ->
                    val index = currentMessages.indexOfFirst { it.tempId == serverMessage.tempId }
                    if (index != -1) {
                        currentMessages[index] = serverMessage
                        val processedList = processMessagesWithDates(currentMessages)
                        chatAdapter.submitList(processedList)
                    }
                }
            }
        }

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
    }

    private fun processMessagesWithDates(messages: List<Message>): List<ChatItem> {
        if (messages.isEmpty()) return emptyList()
        val itemsWithDates = mutableListOf<ChatItem>()
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
        var lastDate: Date? = null
        for (message in messages) {
            val messageTimestamp = message.timestamp
            if (messageTimestamp.isNullOrEmpty()) {
                itemsWithDates.add(ChatItem.MessageItem(message))
                continue
            }
            val currentDate = inputFormat.parse(messageTimestamp) ?: continue
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
            messageDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> SimpleDateFormat("E, d MMM", Locale.getDefault()).format(date)
            else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date)
        }
    }
}