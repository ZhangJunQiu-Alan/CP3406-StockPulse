package au.edu.jcu.assessment.utilityapp.ui.format

import au.edu.jcu.assessment.utilityapp.data.model.DisplayCurrency
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Pure text-formatting helpers, kept out of composables so they can be unit tested. */
object StockFormatters {

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val dayAndTimeFormatter = DateTimeFormatter.ofPattern("EEE h:mm a", Locale.US)

    /** e.g. `S$1,234.56` */
    fun price(amount: Double, currency: DisplayCurrency): String =
        currency.prefix + String.format(Locale.US, "%,.2f", amount)

    /** e.g. `+2.31%` or `-0.84%` */
    fun signedPercent(percent: Double): String = String.format(Locale.US, "%+.2f%%", percent)

    /** e.g. `2.31%` – used with words like "Up" / "Down" for screen readers. */
    fun unsignedPercent(percent: Double): String = String.format(Locale.US, "%.2f%%", abs(percent))

    /** Threshold slider label: whole numbers without a trailing `.0`. */
    fun threshold(percent: Float): String = String.format(Locale.US, "%.0f", percent)

    /** USD→SGD rate with four decimals, e.g. `1.2802`. */
    fun exchangeRate(rate: Double): String = String.format(Locale.US, "%.4f", rate)

    /** Short countdown such as `2d 3h`, `2h 15m`, `45m` or `<1m`. */
    fun countdown(duration: Duration): String {
        val totalMinutes = duration.toMinutes().coerceAtLeast(0)
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    /**
     * Time in the user's own zone, adding the weekday when it isn't today –
     * e.g. `9:30 PM` or `Mon 9:30 PM` for a Singapore user watching New York.
     */
    fun localTime(instant: Instant, now: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
        val target = instant.atZone(zone)
        val isToday = target.toLocalDate() == now.atZone(zone).toLocalDate()
        return (if (isToday) timeFormatter else dayAndTimeFormatter).format(target)
    }
}
