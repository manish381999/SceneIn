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
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.BaseActivity
import com.tie.vibein.R
import com.tie.vibein.chat.persentation.screens.ChatActivity
import com.tie.vibein.credentials.data.repository.AuthRepository
import com.tie.vibein.discover.presentation.screens.EventDetailActivity
import com.tie.vibein.profile.persentation.screen.UserProfileActivity
import java.io.Serializable

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCM_FINAL_DEBUG"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.e(TAG, "New FCM Token Generated: $token")
        SP.savePreferences(this, SP.FCM_TEMP_TOKEN, token)
        val userId = SP.getPreferences(this, SP.USER_ID, "")
        if (!userId.isNullOrEmpty()) {
            // In a production app, you would likely use a WorkManager or a Coroutine
            // to safely send this token from a background thread.
             val fcmManager = FcmTokenManager(AuthRepository())
             fcmManager.updateFcmToken(userId, token)
        }
    }

    /**
     * This method is called for all incoming FCM messages and routes them correctly.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.e(TAG, "--- FCM MESSAGE RECEIVED ---")
        Log.d(TAG, "Data Payload: ${remoteMessage.data}")

        val data = remoteMessage.data
        val notificationType = data["notification_type"]

        // =====================================================================
        // == THE KEY FIX: Check if we should broadcast INTERNALLY first. ==
        // =====================================================================
        if (notificationType == "new_message") {
            val senderId = data["sender_id"]
            if (ChatActivity.isActivityVisible && ChatActivity.currentChattingWithId == senderId) {
                // The user is in the active chat screen. Broadcast the message internally
                // AND stop further execution so a system notification is NOT shown.
                Log.d(TAG, "User in active chat. Broadcasting message and stopping.")
                broadcastNewMessage(remoteMessage.notification, data)
                return
            }
        }

        // --- STANDARD NOTIFICATION LOGIC ---
        // If the code reaches here, it means one of two things:
        // 1. It was NOT a 'new_message' type (e.g., it was 'connection_request').
        // 2. It WAS a 'new_message', but the user was NOT in the correct chat screen.
        // In both these cases, we should show a system notification.

        val title = remoteMessage.notification?.title
        val body = remoteMessage.notification?.body

        if (title.isNullOrBlank() || body.isNullOrBlank()) {
            Log.e(TAG, "Cannot show notification because title or body is missing.")
            return
        }

        Log.d(TAG, "Proceeding to show a system notification for type: $notificationType")
        sendSystemNotification(title, body, data)
    }

    private fun broadcastNewMessage(notification: RemoteMessage.Notification?, data: Map<String, String>) {
        val messageDataForBroadcast = HashMap<String, String>()
        messageDataForBroadcast.putAll(data)
        // Ensure title and body from the notification object are included for the receiver
        messageDataForBroadcast["title"] = notification?.title ?: ""
        messageDataForBroadcast["body"] = notification?.body ?: ""

        val intent = Intent("new_message_broadcast").apply {
            putExtra("message_data", messageDataForBroadcast)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    /**
     * Creates and displays a standard system notification that appears in the notification drawer.
     */
    private fun sendSystemNotification(title: String, body: String, data: Map<String, String>) {
        val notificationIntent: Intent

        // Decide which Activity to open when the user taps the notification.
        when (data["notification_type"]) {
            "connection_request", "connection_accepted" -> {
                notificationIntent = Intent(this, UserProfileActivity::class.java).apply {
                    putExtra("user_id", data["sender_id"] ?: data["receiver_id"])
                }
            }
            "new_message" -> {
                notificationIntent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("receiver_id", data["sender_id"])
                    putExtra("receiver_name", title) // Title is the sender's name for chat notifications
                    // If you send profile pic in data payload, you'd add it here.
                }
            }
            "event_join", "event_unjoin" -> {
                notificationIntent = Intent(this, EventDetailActivity::class.java).apply {
                    putExtra("event_id", data["event_id"])
                }
            }
            else -> {
                notificationIntent = Intent(this, BaseActivity::class.java)
            }
        }

        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), notificationIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
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

        // Group chat notifications by sender ID to stack them cleanly.
        val notificationId = data["sender_id"]?.hashCode() ?: System.currentTimeMillis().toInt()

        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.e(TAG, "System Notification was successfully created and displayed.")
    }
}