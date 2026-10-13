package au.edu.jcu.assessment.utilityapp.ui.format

import au.edu.jcu.assessment.utilityapp.data.model.DisplayCurrency
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class StockFormattersTest {

    @Test
    fun price_usesCurrencyPrefixAndThousandsSeparator() {
        assertEquals("S$1,234.50", StockFormatters.price(1234.5, DisplayCurrency.SGD))
        assertEquals("US$0.99", StockFormatters.price(0.989, DisplayCurrency.USD))
    }

    @Test
    fun percent_isSignedOrUnsignedAsRequested() {
        assertEquals("+2.31%", StockFormatters.signedPercent(2.3149))
        assertEquals("-0.84%", StockFormatters.signedPercent(-0.844))
        assertEquals("0.84%", StockFormatters.unsignedPercent(-0.844))
    }

    @Test
    fun countdown_picksTheTwoMostUsefulUnits() {
        assertEquals("2d 3h", StockFormatters.countdown(Duration.ofHours(51)))
        assertEquals("2h 15m", StockFormatters.countdown(Duration.ofMinutes(135)))
        assertEquals("45m", StockFormatters.countdown(Duration.ofMinutes(45)))
        assertEquals("<1m", StockFormatters.countdown(Duration.ofSeconds(30)))
        assertEquals("<1m", StockFormatters.countdown(Duration.ofMinutes(-5)))
    }

    @Test
    fun localTime_addsWeekdayOnlyWhenNotToday() {
        val singapore = ZoneId.of("Asia/Singapore")
        val now = Instant.parse("2026-10-13T10:00:00Z") // Tue 6:00 PM in Singapore

        assertEquals("9:30 PM", StockFormatters.localTime(Instant.parse("2026-10-13T13:30:00Z"), now, singapore))
        assertEquals("Wed 4:00 AM", StockFormatters.localTime(Instant.parse("2026-10-13T20:00:00Z"), now, singapore))
    }
}
