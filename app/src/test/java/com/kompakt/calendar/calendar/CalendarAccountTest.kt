package com.kompakt.calendar.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarAccountTest {

    private fun account(type: String) = CalendarAccount(
        id = 1, displayName = "Calendar", accountName = "acc", accountType = type,
        ownerAccount = null, color = 0, isPrimary = false, isVisible = true, isWritable = true
    )

    @Test
    fun decsyncCalendarsAccountIsRecognised() {
        val cal = account("org.decsync.calendars")
        assertTrue(cal.isDecsync)
        assertFalse(cal.isDavx5)
        assertEquals("DecSync", cal.syncSourceLabel)
    }

    @Test
    fun davx5AccountKeepsItsLabel() {
        assertEquals("DAVx5", account("bitfire.at.davdroid").syncSourceLabel)
    }

    @Test
    fun localAndOtherAccountsHaveNoLabelAndAreNotSyncable() {
        assertEquals("phone only", account("LOCAL").syncSourceLabel)
        assertFalse(account("LOCAL").canRequestSync)
        assertEquals(null, account("com.google").syncSourceLabel)
        assertTrue(account("com.google").canRequestSync)
        assertTrue(account("org.decsync.calendars").canRequestSync)
    }

    private fun cal(id: Long, type: String, primary: Boolean = false, writable: Boolean = true) =
        account(type).copy(id = id, isPrimary = primary, isWritable = writable)

    @Test
    fun defaultCalendarPrefersTheSavedChoice() {
        val cals = listOf(cal(1, "LOCAL"), cal(2, "org.decsync.calendars"))
        assertEquals(1L, pickDefaultCalendar(cals, savedId = 1)?.id)
    }

    @Test
    fun defaultCalendarSkipsPhoneOnlyCalendarsWhenNothingIsSaved() {
        val cals = listOf(cal(1, "LOCAL", primary = true), cal(2, "org.decsync.calendars"), cal(3, "org.decsync.calendars"))
        assertEquals(2L, pickDefaultCalendar(cals, savedId = null)?.id)
    }

    @Test
    fun defaultCalendarIgnoresASavedCalendarThatIsGoneOrReadOnly() {
        val cals = listOf(cal(1, "LOCAL"), cal(2, "org.decsync.calendars", writable = false))
        assertEquals(1L, pickDefaultCalendar(cals, savedId = 2)?.id)
        assertEquals(null, pickDefaultCalendar(emptyList(), savedId = 2))
    }
}
