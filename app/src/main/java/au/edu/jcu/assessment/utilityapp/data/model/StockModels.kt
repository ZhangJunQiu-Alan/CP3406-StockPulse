package au.edu.jcu.assessment.utilityapp.data.model

import java.time.Instant

/** A single stock's latest quote, always stored in USD (the currency the APIs report in). */
data class StockQuote(
    val symbol: String,
    val companyName: String,
    val logoUrl: String?,
    val priceUsd: Double,
    val changeUsd: Double,
    val changePercent: Double,
    val previousCloseUsd: Double,
    /** Closing prices in chronological order, used to draw the sparkline. Empty if unavailable. */
    val sparkline: List<Double> = emptyList(),
)

enum class MarketSession { PRE_MARKET, REGULAR, POST_MARKET, CLOSED }

data class MarketStatus(
    val session: MarketSession,
    /** Name of today's US market holiday, if any (e.g. "Thanksgiving Day"). */
    val holiday: String? = null,
)

/** Everything the main screen needs from one network refresh. */
data class WatchlistSnapshot(
    val quotes: List<StockQuote>,
    /** Symbols the APIs did not recognise, so the UI can tell the user. */
    val unknownSymbols: List<String>,
    val marketStatus: MarketStatus,
    /** Null when the exchange-rate service is unreachable; the UI then falls back to USD. */
    val usdToSgdRate: Double?,
    val fetchedAt: Instant,
)

/** A confirmed match from the symbol search, used when adding to the watchlist. */
data class SymbolMatch(val symbol: String, val companyName: String)
