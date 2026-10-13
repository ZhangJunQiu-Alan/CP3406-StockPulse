package au.edu.jcu.assessment.utilityapp.data.model

/**
 * User preferences edited on the Settings screen. Held in memory only – the A1 spec does not
 * require persistence, so these reset to the defaults below whenever the app process restarts.
 */
data class AppSettings(
    val watchlist: List<String> = listOf("AAPL", "NVDA", "MSFT", "TSLA", "GOOGL"),
    val currency: DisplayCurrency = DisplayCurrency.SGD,
    val chartRange: ChartRange = ChartRange.ONE_DAY,
    val sortOrder: SortOrder = SortOrder.WATCHLIST,
    val bigMoveThresholdPercent: Float = 3f,
    val refreshInterval: RefreshInterval = RefreshInterval.ONE_MINUTE,
) {
    companion object {
        /** Twelve Data's free tier allows 8 credits per minute and each sparkline costs one. */
        const val MAX_WATCHLIST_SIZE = 8
        const val MIN_BIG_MOVE_PERCENT = 1f
        const val MAX_BIG_MOVE_PERCENT = 10f
    }
}

enum class DisplayCurrency(val code: String, val prefix: String) {
    USD("USD", "US$"),
    SGD("SGD", "S$"),
}

/** Each range maps to a Twelve Data interval and the number of points needed to cover it. */
enum class ChartRange(val label: String, val apiInterval: String, val pointCount: Int) {
    ONE_DAY("1D", "15min", 26),
    ONE_WEEK("1W", "1h", 35),
    ONE_MONTH("1M", "1day", 22),
}

enum class SortOrder(val label: String) {
    WATCHLIST("My order"),
    TOP_GAINERS("Top gainers"),
    ALPHABETICAL("A–Z"),
}

enum class RefreshInterval(val label: String, val periodMillis: Long?) {
    MANUAL("Off", null),
    THIRTY_SECONDS("30s", 30_000),
    ONE_MINUTE("1m", 60_000),
    FIVE_MINUTES("5m", 300_000),
}
