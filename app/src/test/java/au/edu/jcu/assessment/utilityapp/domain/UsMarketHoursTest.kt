package au.edu.jcu.assessment.utilityapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class UsMarketHoursTest {

    @Test
    fun nextOpen_beforeOpeningOnWeekday_isSameDay() {
        // Tue 8:00 AM New York (EDT, UTC-4)
        val now = Instant.parse("2026-10-13T12:00:00Z")
        assertEquals(Instant.parse("2026-10-13T13:30:00Z"), UsMarketHours.nextOpeningTime(now))
    }

    @Test
    fun nextOpen_afterFridayClose_skipsWeekend() {
        // Fri 5:00 PM New York
        val now = Instant.parse("2026-10-16T21:00:00Z")
        assertEquals(Instant.parse("2026-10-19T13:30:00Z"), UsMarketHours.nextOpeningTime(now))
    }

    @Test
    fun nextOpen_followsDaylightSavingChange() {
        // Sat 31 Oct; US clocks go back on Sun 1 Nov, so Monday opens at 14:30 UTC (EST, UTC-5)
        val now = Instant.parse("2026-10-31T15:00:00Z")
        assertEquals(Instant.parse("2026-11-02T14:30:00Z"), UsMarketHours.nextOpeningTime(now))
    }

    @Test
    fun closingTime_isFourPmNewYork() {
        val now = Instant.parse("2026-10-13T15:00:00Z")
        assertEquals(Instant.parse("2026-10-13T20:00:00Z"), UsMarketHours.closingTimeOn(now))
    }
}
