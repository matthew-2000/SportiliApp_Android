package com.matthew.sportiliapp.newadmin.ui.screens

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class S10AlertDatesTest {
    @Test fun pickerCalendarDayIsStableAcrossPositiveAndNegativeOffsets() {
        val day = LocalDate.of(2026, 10, 25)
        for (zone in listOf("Europe/Rome", "America/Los_Angeles", "Pacific/Kiritimati")) {
            val id = ZoneId.of(zone)
            assertEquals(day, alertPickerDate(alertPickerMillis(day)!!))
            val stored = alertDeadlineMillis(day, null, id)!!
            assertEquals(day, Instant.ofEpochMilli(stored).atZone(id).toLocalDate())
            assertEquals(LocalTime.MIDNIGHT, Instant.ofEpochMilli(stored).atZone(id).toLocalTime())
        }
    }
    @Test fun untouchedTimestampIsPreservedExactlyAndDeadlineCanBeRemoved() {
        val zone = ZoneId.of("Europe/Rome")
        val original = LocalDate.of(2026, 10, 11).atTime(18, 37).atZone(zone).toInstant().toEpochMilli()
        assertEquals(original, alertDeadlineMillis(LocalDate.of(2026, 10, 11), original, zone))
        assertNull(alertDeadlineMillis(null, original, zone))
        assertNull(alertDeadlineMillis(null, null, zone))
        assertNull(alertPickerMillis(null))
        assertEquals(LocalDate.of(2026, 10, 12).atStartOfDay(zone).toInstant().toEpochMilli(),
            alertDeadlineMillis(LocalDate.of(2026, 10, 12), original, zone))
    }
}
