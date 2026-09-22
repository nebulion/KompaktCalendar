package com.kompakt.calendar.calendar

import java.time.LocalDate
import java.time.LocalDateTime

data class CalendarAccount(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val ownerAccount: String?,
    val color: Int,
    val isPrimary: Boolean,
    val isVisible: Boolean,
    val isWritable: Boolean
) {
    val isDavx5: Boolean
        get() = accountType.equals("bitfire.at.davdroid", ignoreCase = true) ||
                accountType.contains("davx", ignoreCase = true) ||
                accountType.contains("davdroid", ignoreCase = true)

    /** DecSync CC registers calendars under `org.decsync.calendars`. */
    val isDecsync: Boolean
        get() = accountType.startsWith("org.decsync", ignoreCase = true)

    /** Short name of the app that syncs this calendar, shown next to the account name. */
    val syncSourceLabel: String?
        get() = when {
            isDavx5 -> "DAVx5"
            isDecsync -> "DecSync"
            isLocal -> "phone only"
            else -> null
        }

    /** A calendar stored only on this phone: no sync adapter copies it anywhere. */
    val isLocal: Boolean
        get() = accountType.equals(android.provider.CalendarContract.ACCOUNT_TYPE_LOCAL, ignoreCase = true)

    /** Phone-only calendars have no sync adapter to ask. */
    val canRequestSync: Boolean
        get() = accountType.isNotEmpty() &&
                !accountType.equals(android.provider.CalendarContract.ACCOUNT_TYPE_LOCAL, ignoreCase = true)
}

/**
 * The calendar that new events go to: the saved choice if it is still writable, else a
 * synced calendar (so the event leaves the phone), else any writable calendar.
 */
fun pickDefaultCalendar(calendars: List<CalendarAccount>, savedId: Long?): CalendarAccount? {
    val writable = calendars.filter { it.isWritable }
    return writable.firstOrNull { it.id == savedId }
        ?: writable.firstOrNull { !it.isLocal && it.isPrimary }
        ?: writable.firstOrNull { !it.isLocal }
        ?: writable.firstOrNull()
}

data class CalendarEvent(
    val id: Long,
    val calendarId: Long,
    val calendarDisplayName: String,
    val calendarColor: Int,
    val title: String,
    val description: String?,
    val location: String?,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val allDay: Boolean,
    val rrule: String?,
    val timezone: String?,
    val hasReminder: Boolean,
    val reminders: List<Int> = emptyList()
) {
    fun occursOn(date: LocalDate): Boolean {
        val s = start.toLocalDate()
        val e = end.toLocalDate()
        return !date.isBefore(s) && !date.isAfter(if (allDay) e.minusDays(1) else e)
    }
}
