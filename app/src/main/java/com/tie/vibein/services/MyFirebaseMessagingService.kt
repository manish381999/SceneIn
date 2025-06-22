package com.tie.vibein.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.BaseActivity
import com.tie.vibein.R
import com.tie.vibein.chat.persentation.screens.ChatActivity
import com.tie.vibein.credentials.data.repository.AuthRepository
import com.tie.vibein.discover.presentation.screens.EventDetailActivity
import com.tie.vibein.profile.persentation.screen.UserProfileActivity
import com.tie.vibein.utils.DeliveryReportWorker // Your import might be com.tie.vibein.workers.DeliveryReportWorker

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCM_FINAL_ROUTER"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.e(TAG, "New FCM Token Generated: $token")
        SP.savePreferences(this, SP.FCM_TEMP_TOKEN, token)
        val userId = SP.getPreferences(this, SP.USER_ID, "")
        if (userId != null) {
            if (userId.isNotEmpty()) {
                val fcmManager = FcmTokenManager(AuthRepository())
                fcmManager.updateFcmToken(userId, token)
            }
        }
    }

    /**
     * Main entry point for ALL FCM messages.
     * This will be called in all app states because the server sends data-only messages.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "--- onMessageReceived CALLED ---")
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Data Payload: ${remoteMessage.data}")
            handleDataPayload(remoteMessage.data)
        }
    }

    /**
     * Central router for all messages containing a data payload.
     */
    private fun handleDataPayload(data: Map<String, String>) {
        when (data["notification_type"]) {
            "new_message" -> handleNewMessage(data)
            "status_update" -> handleStatusUpdate(data)
            else -> {
                // --- THIS IS THE FIX ---
                // This is the fallback for any other notification type
                // like 'connection_request', 'event_join', etc.
                Log.d(TAG, "Handling generic data message type: ${data["notification_type"]}")
                val title = data["title"]
                val body = data["body"]
                // If we have a title and body in the data payload, we can show a notification.
                if (!title.isNullOrBlank() && !body.isNullOrBlank()) {
                    sendSystemNotification(title, body, data)
                } else {
                    Log.w(TAG, "Received data message of type '${data["notification_type"]}' but it has no title or body to display.")
                }
            }
        }
    }

    private fun handleNewMessage(data: Map<String, String>) {
        val messageId = data["message_id"]
        if (!messageId.isNullOrBlank()) {
            scheduleDeliveryReport(listOf(messageId))
        }
        val senderId = data["sender_id"]
        if (ChatActivity.isActivityVisible && ChatActivity.currentChattingWithId == senderId) {
            broadcastNewMessage(data)
        } else {
            val title = data["title"] ?: "New Message"
            val body = data["body"] ?: "You have received a new message."
            sendSystemNotification(title, body, data)
        }
    }

    private fun scheduleDeliveryReport(messageIds: List<String>) {
        Log.d(TAG, "Scheduling delivery report via WorkManager for IDs: $messageIds")
        val workData = workDataOf(DeliveryReportWorker.KEY_MESSAGE_IDS to messageIds.toTypedArray())
        val workRequest = OneTimeWorkRequestBuilder<DeliveryReportWorker>()
            .setInputData(workData)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }

    private fun handleStatusUpdate(data: Map<String, String>) {
        Log.d("STATUS_CHAIN", "1. FCM Service is broadcasting status update: ${data["status"]}") // LOG 1
        val intent = Intent("status_update_broadcast").apply {
            putExtra("status", data["status"])
            putExtra("message_ids_json", data["message_ids"])
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun broadcastNewMessage(data: Map<String, String>) {
        val intent = Intent("new_message_broadcast").apply {
            putExtra("message_data", HashMap(data))
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendSystemNotification(title: String, body: String, data: Map<String, String>) {
        val notificationIntent: Intent
        when (data["notification_type"]) {
            "new_message" -> {
                notificationIntent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("receiver_id", data["sender_id"])
                    putExtra("receiver_name", data["sender_name"])
                    putExtra("receiver_profile_pic", data["sender_profile_pic"])
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            }
            "connection_request", "connection_accepted" -> {
                notificationIntent = Intent(this, UserProfileActivity::class.java).apply {
                    putExtra("user_id", data["sender_id"] ?: data["receiver_id"])
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            }
            "event_join", "event_unjoin" -> {
                notificationIntent = Intent(this, EventDetailActivity::class.java).apply {
                    putExtra("event_id", data["event_id"])
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            }
            else -> {
                notificationIntent = Intent(this, BaseActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            (data["sender_id"]?.hashCode() ?: System.currentTimeMillis()).toInt(),
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "VibeIn Notifications", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = data["sender_id"]?.hashCode() ?: System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.d(TAG, "System Notification was successfully created and displayed for type: ${data["notification_type"]}")
    }
}