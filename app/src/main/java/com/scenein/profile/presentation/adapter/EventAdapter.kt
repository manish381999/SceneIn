package com.scenein.profile.presentation.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.scenein.R
import com.scenein.databinding.ItemEventBinding
import com.scenein.discover.presentation.screens.EventDetailActivity
import com.scenein.profile.data.models.Event
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class EventAdapter(private val context: Context) : // The list is no longer in the constructor
    RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    // --- THIS IS THE FIX 1/2: The internal list is now a mutable var ---
    private var eventList: List<Event> = emptyList()

    inner class EventViewHolder(val binding: ItemEventBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventList[position]

        val formattedDateTime = formatEventDateTime(event.event_date, event.start_time)
        holder.binding.tvDateTime.text = formattedDateTime
        holder.binding.tvEventName.text = event.event_name
        holder.binding.tvEventFullAddress.text = event.venueLocation
        holder.binding.tvEventJoined.text = "${event.joined_participants ?: "0"} joined"
        holder.binding.tvRole.text = event.role

        Glide.with(context)
            .load(event.cover_image)
            .placeholder(R.drawable.ic_placeholder)
            .into(holder.binding.ivCoverImage)

        holder.binding.btnViewDetails.setOnClickListener {
            val intent = Intent(context, EventDetailActivity::class.java).apply {
                putExtra("event_id", event.id)
                putExtra("source", "profile")
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = eventList.size

    // --- THIS IS THE FIX 2/2: The missing updateEvents function ---
    /**
     * Updates the list of events in the adapter and refreshes the RecyclerView.
     */
    fun updateEvents(newEvents: List<Event>) {
        this.eventList = newEvents
        notifyDataSetChanged() // Tell the RecyclerView to redraw itself
    }

    private fun formatEventDateTime(dateUtc: String?, startTimeUtc: String?): String {
        if (dateUtc.isNullOrBlank() || startTimeUtc.isNullOrBlank()) return "Date not specified"

        return try {
            // 1. Parse the UTC timestamp strings into Instant objects
            val eventDateInstant = Instant.parse(dateUtc)
            val startTimeInstant = Instant.parse(startTimeUtc)

            // 2. Convert the UTC Instants to the user's local time zone
            val userZoneId = ZoneId.systemDefault()
            val localEventDate = eventDateInstant.atZone(userZoneId)
            val localStartTime = startTimeInstant.atZone(userZoneId)

            // 3. Determine the correct date format (with or without year)
            val currentYear = ZonedDateTime.now(userZoneId).year
            val datePattern = if (localEventDate.year == currentYear) {
                "MMM dd" // e.g., "Sep 14"
            } else {
                "MMM dd, yyyy" // e.g., "Sep 14, 2026"
            }

            // 4. Format the date and time parts separately
            val dateFormatter = DateTimeFormatter.ofPattern(datePattern, Locale.getDefault())
            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

            val formattedDate = localEventDate.format(dateFormatter)
            val formattedTime = localStartTime.format(timeFormatter)

            // 5. Combine them for the final output
            "$formattedDate, $formattedTime"

        } catch (e: Exception) {
            // Fallback if parsing fails
            "Invalid date"
        }
    }
}