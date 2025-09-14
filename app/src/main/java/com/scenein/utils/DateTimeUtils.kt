package com.scenein.utils

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

object DateTimeUtils {

    // Default formatter for short display
    private val shortFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE, MMM d • hh:mm a", Locale.getDefault())

    /**
     * ✅ Use this when your backend sends separate eventDate and eventTime fields.
     * Example:
     *   event_date = "2025-12-24T18:30:00.000Z"
     *   event_time = "2025-12-25T18:00:00.000Z"
     */
    fun combineDateAndTime(eventDateUtc: String?, eventTimeUtc: String?): String {
        if (eventDateUtc.isNullOrBlank() || eventTimeUtc.isNullOrBlank()) return "Date not specified"
        return try {
            val dateUtc = ZonedDateTime.parse(eventDateUtc)
            val timeUtc = ZonedDateTime.parse(eventTimeUtc)

            // Extract local date + time separately
            val localDate = dateUtc.withZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
            val localTime = timeUtc.withZoneSameInstant(ZoneId.systemDefault()).toLocalTime()

            // Rebuild proper LocalDateTime in system zone
            val localCombined = LocalDateTime.of(localDate, localTime)
            val zoned = localCombined.atZone(ZoneId.systemDefault())

            zoned.format(shortFormatter)
        } catch (e: Exception) {
            "Invalid date"
        }
    }

    /**
     * Utility: Format just date from UTC
     */
    fun formatEventDate(eventDateUtc: String?, pattern: String = "EEE, dd MMM yyyy"): String {
        return formatUtcToLocal(eventDateUtc, pattern)
    }

    /**
     * Utility: Format just time (or range) from UTC
     */
    fun formatEventTimeRange(startUtc: String?, endUtc: String?): String {
        val start = formatUtcToLocal(startUtc, "hh:mm a")
        val end = formatUtcToLocal(endUtc, "hh:mm a")
        return if (end != "N/A" && end != "Invalid date") "$start - $end" else start
    }

    /**
     * Internal helper: Format UTC datetime with custom pattern
     */
    private fun formatUtcToLocal(utcDateTime: String?, pattern: String): String {
        if (utcDateTime.isNullOrBlank()) return "N/A"
        return try {
            val utcZoned = ZonedDateTime.parse(utcDateTime)
            val localZoned = utcZoned.withZoneSameInstant(ZoneId.systemDefault())
            localZoned.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
        } catch (e: Exception) {
            "Invalid date"
        }
    }
}
