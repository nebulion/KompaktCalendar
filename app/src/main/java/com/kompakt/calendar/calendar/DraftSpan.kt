package com.kompakt.calendar.calendar

import java.time.Duration
import java.time.LocalDateTime

/** The start and end of an event that is being edited. */
data class DraftSpan(val start: LocalDateTime, val end: LocalDateTime) {

    /** Moves the start and keeps the event length, so the end moves with it. */
    fun moveStart(newStart: LocalDateTime): DraftSpan {
        val length = Duration.between(start, end).coerceAtLeast(Duration.ZERO)
        return DraftSpan(newStart, newStart.plus(length))
    }

    /**
     * Sets the end. For a timed event, an end at or before the start pulls the start
     * back to one hour before the end. For an all-day event, only the dates count.
     */
    fun setEnd(newEnd: LocalDateTime, allDay: Boolean): DraftSpan = when {
        allDay && newEnd.toLocalDate().isBefore(start.toLocalDate()) ->
            DraftSpan(start.with(newEnd.toLocalDate()), newEnd)
        allDay -> DraftSpan(start, newEnd)
        !newEnd.isAfter(start) -> DraftSpan(newEnd.minusHours(1), newEnd)
        else -> DraftSpan(start, newEnd)
    }
}
