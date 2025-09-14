package com.scenein.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.scenein.utils.SP
import com.scenein.BaseActivity
import com.scenein.R
import com.scenein.chat.presentation.screens.ChatActivity
import com.scenein.discover.presentation.screens.EventDetailActivity
import com.scenein.profile.presentation.screen.UserProfileActivity
import com.scenein.utils.DeliveryReportWorker

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCM_SERVICE_FINAL"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token Refreshed: $token")
        // Just save the new token locally. It will be synced with the server on the next login.
        SP.saveString(this, SP.FCM_TEMP_TOKEN, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "--- New FCM Message Received ---")

        val areNotificationsPaused = SP.getBoolean(this, SP.NOTIFICATIONS_PAUSED, false)
        if (areNotificationsPaused) {
            // If notifications are paused, we stop right here.
            Log.i(TAG, "Notifications are paused by the user. Ignoring this push.")
            // Exception: We should still process delivery reports for chat messages,
            // as this is a background task that doesn't disturb the user.
            if (remoteMessage.data["notification_type"] == "new_message") {
                remoteMessage.data["message_id"]?.let { scheduleDeliveryReport(listOf(it)) }
            }
            return // Stop further processing.
        }

        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Data Payload: ${remoteMessage.data}")
            handleDataPayload(remoteMessage.data)
        }
    }

    private fun handleDataPayload(data: Map<String, String>) {
        when (data["notification_type"]) {
            "new_message" -> handleNewMessage(data)
            "status_update" -> handleStatusUpdate(data)
            else -> {
                val title = data["title"]
                val body = data["body"]
                if (!title.isNullOrBlank() && !body.isNullOrBlank()) {
                    sendSystemNotification(title, body, data)
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
        // Check if the user is already on the chat screen with this specific person
        if (ChatActivity.isActivityVisible && ChatActivity.currentChattingWithId == senderId) {
            // Broadcast to the activity to just add the new message to the list, no notification needed.
            broadcastNewMessage(data)
        } else {
            // User is not on the chat screen, so we MUST show a system notification.
            // --- THIS IS THE CORRECTED LOGIC ---
            // The title should be the sender's name. The body is the message content.
            val title = data["sender_name"] ?: "New Message"
            val body = data["body"] ?: "You have received a new message."
            // We pass the full data payload to the notification builder.
            sendSystemNotification(title, body, data)
        }
    }

    // scheduleDeliveryReport, handleStatusUpdate, and broadcastNewMessage are correct as you provided them.
    private fun scheduleDeliveryReport(messageIds: List<String>) {
        Log.d(TAG, "Scheduling delivery report via WorkManager for IDs: $messageIds")
        val workData = workDataOf(DeliveryReportWorker.KEY_MESSAGE_IDS to messageIds.toTypedArray())
        val workRequest = OneTimeWorkRequestBuilder<DeliveryReportWorker>().setInputData(workData).build()
        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }

    private fun handleStatusUpdate(data: Map<String, String>) {
        Log.d("STATUS_CHAIN", "1. FCM Service is broadcasting status update: ${data["status"]}")
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

    /**
     * The definitive, production-ready function for creating and displaying all non-chat notifications.
     */
    private fun sendSystemNotification(title: String, body: String, data: Map<String, String>) {
        val notificationType = data["notification_type"] ?: "default"
        val notificationIntent: Intent

        when (notificationType) {
            "new_message" -> {
                // --- THIS IS THE CRITICAL FIX ---
                // When a chat notification is tapped, it should open the ChatActivity
                // and pass ALL the necessary details for that specific chat.
                notificationIntent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("receiver_id", data["sender_id"]) // The sender becomes the receiver on this screen
                    putExtra("receiver_name", data["sender_name"])
                    putExtra("receiver_profile_pic", data["sender_profile_pic"])
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            }
            "connection_request", "connection_accepted" -> {
                notificationIntent = Intent(this, UserProfileActivity::class.java).apply {
                    putExtra("user_id", data["sender_id"])
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            }
            "event_join", "event_unjoin" -> {
                notificationIntent = Intent(this, EventDetailActivity::class.java).apply {
                    putExtra("event_id", data["event_id"])
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            }
            "ticket_sold", "payout_queued", "payout_processed", "dispute_raised", "resold", "refund_queued", "refund_processed" -> {
                notificationIntent = Intent(this, BaseActivity::class.java).apply {
                    putExtra("NAVIGATE_TO", "TICKETS")
                    putExtra("TICKETS_SUB_TAB", 1)
                }
            }
            else -> {
                notificationIntent = Intent(this, BaseActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            }
        }

        val requestCode = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(this, requestCode, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val channelId = getString(R.string.default_notification_channel_id)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "VibeIn Activity", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val personIconUrl = data["sender_pic"] ?: data["actor_pic"] ?: data["sender_profile_pic"]

        if (!personIconUrl.isNullOrBlank()) {
            Glide.with(applicationContext)
                .asBitmap()
                .load(personIconUrl)
                .circleCrop()
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        notificationBuilder.setLargeIcon(resource)
                        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
                    }
                })
        } else {
            notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        }
    }
}