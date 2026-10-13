package au.edu.jcu.assessment.utilityapp.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Regular trading hours of the NYSE / NASDAQ (9:30 AM – 4:00 PM New York time, Mon–Fri).
 * Calculating in the exchange's own time zone means daylight-saving changes are handled
 * automatically when the result is shown in the user's local (e.g. Singapore) time.
 */
object UsMarketHours {

    val EXCHANGE_ZONE: ZoneId = ZoneId.of("America/New_York")
    private val OPENING_TIME: LocalTime = LocalTime.of(9, 30)
    private val CLOSING_TIME: LocalTime = LocalTime.of(16, 0)

    /** The closing bell of the New York trading day that contains [now]. */
    fun closingTimeOn(now: Instant): Instant =
        now.atZone(EXCHANGE_ZONE).toLocalDate()
            .atTime(CLOSING_TIME).atZone(EXCHANGE_ZONE).toInstant()

    /**
     * The next regular-session opening bell after [now], skipping weekends.
     * Public holidays are not known in advance on the free API tier, so they are not skipped;
     * on a holiday the live market-status API already tells the user the market is closed.
     */
    fun nextOpeningTime(now: Instant): Instant {
        val exchangeNow = now.atZone(EXCHANGE_ZONE)
        var date = exchangeNow.toLocalDate()
        if (!exchangeNow.toLocalTime().isBefore(OPENING_TIME)) date = date.plusDays(1)
        while (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
            date = date.plusDays(1)
        }
        return date.atTime(OPENING_TIME).atZone(EXCHANGE_ZONE).toInstant()
    }
}
