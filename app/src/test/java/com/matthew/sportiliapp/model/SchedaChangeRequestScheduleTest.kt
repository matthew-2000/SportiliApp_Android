package com.matthew.sportiliapp.model

import java.time.Instant
import java.util.Date
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class SchedaChangeRequestScheduleTest {
    @Test fun windowBoundariesInItalianWinterAndSummerTime() {
        for ((instant, expected) in listOf(
            "2026-11-13T18:59:59Z" to false,
            "2026-11-13T19:00:00Z" to true,
            "2026-11-13T23:00:00Z" to true,
            "2026-11-14T08:59:59Z" to true,
            "2026-11-14T09:00:00Z" to false,
            "2026-11-15T08:00:00Z" to false,
            "2026-11-16T08:00:00Z" to false,
            "2026-07-10T17:59:59Z" to false,
            "2026-07-10T18:00:00Z" to true,
            "2026-07-11T07:59:59Z" to true,
            "2026-07-11T08:00:00Z" to false
        )) {
            assertEquals(instant, expected, SchedaChangeRequestSchedule.isWithinWindow(Date.from(Instant.parse(instant))))
        }
    }

    @Test fun deviceTimeZoneDoesNotChangeTheWindow() {
        val original = TimeZone.getDefault()
        try {
            for (zone in listOf("America/Los_Angeles", "Asia/Tokyo", "UTC")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                windowBoundariesInItalianWinterAndSummerTime()
            }
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
