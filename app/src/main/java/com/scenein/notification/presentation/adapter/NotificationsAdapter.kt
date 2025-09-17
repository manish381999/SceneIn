package com.scenein.notification.presentation.adapter

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.scenein.R
import com.scenein.databinding.ItemNotificationCardBinding
import com.scenein.databinding.ItemNotificationHeaderBinding
import com.scenein.notification.data.models.ConnectionRequest
import com.scenein.notification.data.models.HeaderData
import com.scenein.notification.data.models.Notification
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// Define constants for the different view types our RecyclerView will display
private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_ITEM = 1

class NotificationsAdapter(
    private val onHeaderClick: (List<ConnectionRequest>) -> Unit,
    private val onItemClick: (Notification) -> Unit
) : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback) {

    // --- ViewHolder for the "Connection Requests" Header Item ---
    class HeaderViewHolder(private val binding: ItemNotificationHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(headerData: HeaderData, onHeaderClick: (List<ConnectionRequest>) -> Unit) {
            binding.tvSubtitle.text = "${headerData.count} new requests to review"
            val first = headerData.requests.firstOrNull()
            if (first != null) {
                Glide.with(itemView.context).load(first.profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.ivProfilePicFront)
            }
            val second = if (headerData.requests.size > 1) headerData.requests[1] else null
            binding.ivProfilePicBack.isVisible = second != null
            if (second != null) {
                Glide.with(itemView.context).load(second.profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.ivProfilePicBack)
            }
            itemView.setOnClickListener { onHeaderClick(headerData.requests) }
        }
    }

    // --- ViewHolder for a Standard Notification Item ---
    class ItemViewHolder(private val binding: ItemNotificationCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: Notification, onItemClick: (Notification) -> Unit) {
            val context = itemView.context

            // --- THIS IS THE FINAL LOGIC for the notification body ---
            binding.tvBody.text = formatNotificationBody(notification)
            binding.tvTimestamp.text = toRelativeTime(notification.createdAt)

            val bgColorRes = if (notification.isRead) R.color.colorSurface else R.color.unread_background_color
            binding.rootLayout.setBackgroundColor(ContextCompat.getColor(context, bgColorRes))

            var imageUrl = notification.relatedUserProfilePic
            var placeholder = R.drawable.ic_profile_placeholder

            if (imageUrl.isNullOrBlank() && !notification.eventImageUrl.isNullOrBlank()) {
                imageUrl = notification.eventImageUrl
                placeholder = R.drawable.ic_event_placeholder
            }
            Glide.with(context).load(imageUrl).placeholder(placeholder).error(R.drawable.ic_notification_icon).into(binding.ivIcon)

            binding.ivContentImage.isVisible = !notification.eventImageUrl.isNullOrBlank() && !notification.relatedUserProfilePic.isNullOrBlank()
            if(binding.ivContentImage.isVisible){
                Glide.with(context).load(notification.eventImageUrl).centerCrop().into(binding.ivContentImage)
            }

            itemView.setOnClickListener { onItemClick(notification) }
        }

        private fun toRelativeTime(mysqlTimestamp: String?): String {
            if (mysqlTimestamp.isNullOrBlank()) return ""
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC") // important
                val time = sdf.parse(mysqlTimestamp)?.time ?: return ""
                val now = System.currentTimeMillis()
                val diff = now - time
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                when {
                    days > 6 -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(time))
                    days > 0 -> "${days}d"
                    else -> {
                        val hours = TimeUnit.MILLISECONDS.toHours(diff)
                        if (hours > 0) "${hours}h"
                        else {
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                            if (minutes > 0) "${minutes}m"
                            else "Just now"
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }


        // --- THIS IS THE NEW, SMARTER FORMATTING FUNCTION ---
        private fun formatNotificationBody(notification: Notification): SpannableStringBuilder {
            val builder = SpannableStringBuilder()

            val actorName = notification.relatedUserName ?: "Someone"
            val actorUsername = notification.relatedUserUsername?.let { "@$it" } ?: ""
            val bodyText = notification.body

            // Build the string: "Manish Kumar Singh (@manish_singh) sent you a connection request."
            // We find the actor's name in the body and replace it with "Name (@username)"

            if (bodyText.contains(actorName)) {
                // If the username exists, create the combined string, otherwise just use the name.
                val combinedName = if (actorUsername.isNotEmpty()) "$actorName ($actorUsername)" else actorName
                val finalBody = bodyText.replace(actorName, combinedName)

                builder.append(finalBody)
                // Make the combined name bold
                val startIndex = finalBody.indexOf(combinedName)
                if (startIndex != -1) {
                    builder.setSpan(StyleSpan(Typeface.BOLD), startIndex, startIndex + combinedName.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                }
            } else {
                // If the name isn't in the body, just append the body as-is.
                builder.append(bodyText)
            }

            return builder
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) { is HeaderData -> VIEW_TYPE_HEADER; is Notification -> VIEW_TYPE_ITEM; else -> throw IllegalArgumentException() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(ItemNotificationHeaderBinding.inflate(inflater, parent, false))
            VIEW_TYPE_ITEM -> ItemViewHolder(ItemNotificationCardBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HeaderData -> (holder as HeaderViewHolder).bind(item, onHeaderClick)
            is Notification -> (holder as ItemViewHolder).bind(item, onItemClick)
        }
    }

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<Any>() {
            override fun areItemsTheSame(old: Any, new: Any): Boolean {
                return if (old is Notification && new is Notification) old.notificationId == new.notificationId
                else old is HeaderData && new is HeaderData
            }
            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(old: Any, new: Any): Boolean { return old == new }
        }
    }
}