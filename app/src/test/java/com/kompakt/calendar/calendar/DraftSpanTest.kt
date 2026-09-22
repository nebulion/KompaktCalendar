package com.kompakt.calendar.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class DraftSpanTest {
    private fun at(day: Int, hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 9, day, hour, minute)

    @Test
    fun movingStartKeepsLength() {
        val span = DraftSpan(at(22, 15), at(22, 16, 30))
        assertEquals(DraftSpan(at(23, 9), at(23, 10, 30)), span.moveStart(at(23, 9)))
    }

    @Test
    fun movingStartLateCarriesEndPastMidnight() {
        val span = DraftSpan(at(22, 15), at(22, 17))
        assertEquals(DraftSpan(at(22, 23), at(23, 1)), span.moveStart(at(22, 23)))
    }

    @Test
    fun endOnLaterDayKeepsStartEvenWhenClockTimeIsEarlier() {
        val span = DraftSpan(at(22, 22), at(22, 23))
        assertEquals(DraftSpan(at(22, 22), at(23, 1)), span.setEnd(at(23, 1), allDay = false))
    }

    @Test
    fun endBeforeStartPullsStartBack() {
        val span = DraftSpan(at(22, 15), at(22, 16))
        assertEquals(DraftSpan(at(22, 13), at(22, 14)), span.setEnd(at(22, 14), allDay = false))
    }

    @Test
    fun allDayEndDateBeforeStartMovesStartDate() {
        val span = DraftSpan(at(22, 15), at(24, 16))
        assertEquals(DraftSpan(at(20, 15), at(20, 16)), span.setEnd(at(20, 16), allDay = true))
    }

    @Test
    fun allDaySameDateIgnoresClockTimes() {
        val span = DraftSpan(at(22, 15), at(24, 16))
        assertEquals(DraftSpan(at(22, 15), at(22, 9)), span.setEnd(at(22, 9), allDay = true))
    }
}
