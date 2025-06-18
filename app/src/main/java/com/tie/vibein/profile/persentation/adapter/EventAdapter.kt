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

class EventAdapter(private val context: Context, private val eventList: List<Event>) :
    RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    inner class EventViewHolder(val binding: ItemEventBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventList[position]

        // Format date and time based on year
        val formattedDateTime = formatEventDateTime(event.event_date, event.start_time)
        holder.binding.tvDateTime.text = formattedDateTime

        holder.binding.tvEventName.text = event.event_name
        holder.binding.tvEventFullAddress.text = event.venueLocation
        holder.binding.tvEventJoined.text = "${event.joined_participants ?: "0"} joined"
        holder.binding.tvRole.text=event.role

        Glide.with(context)
            .load(event.cover_image)
            .placeholder(R.drawable.ic_placeholder)
            .into(holder.binding.ivCoverImage)

        holder.binding.btnViewDetails.setOnClickListener {
            val intent = Intent(context, EventDetailActivity::class.java)
            intent.putExtra("event_id", event.id)
            intent.putExtra("source", "profile")
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = eventList.size

    private fun formatEventDateTime(dateStr: String, timeStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            val dateTime = inputFormat.parse("$dateStr $timeStr") ?: return "$dateStr, $timeStr"

            val currentYear = Calendar.getInstance().get(Calendar.YEAR)

            val eventCalendar = Calendar.getInstance()
            eventCalendar.time = dateTime
            val eventYear = eventCalendar.get(Calendar.YEAR)

            val outputFormat = if (eventYear == currentYear) {
                SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())  // e.g., May 03 15:26
            } else {
                SimpleDateFormat("MMM dd yyyy, HH:mm", Locale.getDefault())  // e.g., May 03, 2024 15:26
            }

            outputFormat.format(dateTime)
        } catch (e: Exception) {
            "$dateStr, $timeStr" // fallback in case of error
        }
    }
}
