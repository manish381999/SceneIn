package com.tie.vibein.profile.presentation.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tie.vibein.R
import com.tie.vibein.databinding.ItemEventBinding
import com.tie.vibein.discover.presentation.screens.EventDetailActivity
import com.tie.vibein.profile.data.models.Event
import java.text.SimpleDateFormat
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

    private fun formatEventDateTime(dateStr: String?, timeStr: String?): String {
        if (dateStr.isNullOrBlank() || timeStr.isNullOrBlank()) return "Date not specified"
        return try {
            val inputFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            val dateTime = inputFormat.parse("$dateStr $timeStr") ?: return "$dateStr, $timeStr"
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val eventCalendar = Calendar.getInstance().apply { time = dateTime }
            val eventYear = eventCalendar.get(Calendar.YEAR)
            val outputFormat = if (eventYear == currentYear) {
                SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            } else {
                SimpleDateFormat("MMM dd yyyy, hh:mm a", Locale.getDefault())
            }
            outputFormat.format(dateTime)
        } catch (e: Exception) {
            "$dateStr, $timeStr"
        }
    }
}