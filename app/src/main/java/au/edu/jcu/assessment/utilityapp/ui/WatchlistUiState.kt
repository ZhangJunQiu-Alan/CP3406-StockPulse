package au.edu.jcu.assessment.utilityapp.ui

import au.edu.jcu.assessment.utilityapp.data.model.DisplayCurrency
import au.edu.jcu.assessment.utilityapp.data.model.MarketStatus
import au.edu.jcu.assessment.utilityapp.data.model.SymbolMatch
import java.time.Instant
import kotlin.math.abs

/**
 * State of the main (Utility) screen.
 *
 * A data class is used instead of a Loading/Success/Error sealed type so the last good
 * [content] can stay on screen while a refresh is running or after a refresh fails.
 */
data class WatchlistUiState(
    val content: WatchlistContent? = null,
    val isLoading: Boolean = false,
    /** True only while a refresh the user asked for (pull-to-refresh / button) is running. */
    val isUserRefreshing: Boolean = false,
    val error: LoadError? = null,
)

enum class LoadError { NETWORK, RATE_LIMITED, INVALID_API_KEY, UNKNOWN }

data class WatchlistContent(
    val rows: List<StockRowUiModel>,
    val summary: WatchlistSummary,
    val bigMovers: List<StockRowUiModel>,
    val bigMoveThresholdPercent: Float,
    val marketStatus: MarketStatus,
    val currency: DisplayCurrency,
    /** True when SGD was requested but no exchange rate was available, so USD is shown. */
    val isCurrencyFallback: Boolean,
    val usdToSgdRate: Double?,
    val unknownSymbols: List<String>,
    val lastUpdated: Instant,
)

/** One watchlist row, with prices already converted into [currency]. */
data class StockRowUiModel(
    val symbol: String,
    val companyName: String,
    val logoUrl: String?,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val currency: DisplayCurrency,
    val sparkline: List<Double>,
    val isBigMove: Boolean,
) {
    val trend: Trend get() = Trend.fromChange(changePercent)

    /** Direction of the sparkline itself, which can differ from today's change on 1W / 1M. */
    val sparklineTrend: Trend
        get() = if (sparkline.size < 2) trend else Trend.fromChange(sparkline.last() - sparkline.first())
}

data class WatchlistSummary(
    val upCount: Int,
    val downCount: Int,
    val averageChangePercent: Double,
    val bestPerformer: StockRowUiModel?,
    val worstPerformer: StockRowUiModel?,
)

enum class Trend {
    UP, DOWN, FLAT;

    companion object {
        /** Moves smaller than this are treated as "unchanged" to avoid flickering arrows. */
        private const val FLAT_TOLERANCE = 0.005

        fun fromChange(change: Double): Trend = when {
            abs(change) < FLAT_TOLERANCE -> FLAT
            change > 0 -> UP
            else -> DOWN
        }
    }
}

/** Progress of the "add symbol" field on the Settings screen. */
sealed interface AddSymbolState {
    data object Idle : AddSymbolState
    data object Checking : AddSymbolState
    data class Added(val match: SymbolMatch) : AddSymbolState
    data class Rejected(val reason: AddSymbolError) : AddSymbolState
}

enum class AddSymbolError { INVALID_FORMAT, ALREADY_ADDED, LIST_FULL, NOT_FOUND, NETWORK }
