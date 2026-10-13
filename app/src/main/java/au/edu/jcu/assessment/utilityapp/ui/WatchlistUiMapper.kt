package au.edu.jcu.assessment.utilityapp.ui

import au.edu.jcu.assessment.utilityapp.data.model.AppSettings
import au.edu.jcu.assessment.utilityapp.data.model.DisplayCurrency
import au.edu.jcu.assessment.utilityapp.data.model.SortOrder
import au.edu.jcu.assessment.utilityapp.data.model.StockQuote
import au.edu.jcu.assessment.utilityapp.data.model.WatchlistSnapshot
import kotlin.math.abs

/**
 * Applies the user's display settings (currency, sort order, alert threshold) to the latest
 * network snapshot. This is pure and fast, so changing these settings updates the main screen
 * instantly without another network request.
 */
object WatchlistUiMapper {

    fun toContent(snapshot: WatchlistSnapshot, settings: AppSettings): WatchlistContent {
        val sgdRate = snapshot.usdToSgdRate
        val currency = if (settings.currency == DisplayCurrency.SGD && sgdRate == null) {
            DisplayCurrency.USD
        } else {
            settings.currency
        }
        val conversionRate = if (currency == DisplayCurrency.SGD) sgdRate ?: 1.0 else 1.0

        // Follow the *current* watchlist so a removed symbol disappears before the next refresh.
        val quotesBySymbol = snapshot.quotes.associateBy { it.symbol }
        val rows = settings.watchlist
            .mapNotNull { quotesBySymbol[it] }
            .map { it.toRow(currency, conversionRate, settings.bigMoveThresholdPercent) }
            .sortedBy(settings.sortOrder)

        return WatchlistContent(
            rows = rows,
            summary = summarise(rows),
            bigMovers = rows.filter { it.isBigMove },
            bigMoveThresholdPercent = settings.bigMoveThresholdPercent,
            marketStatus = snapshot.marketStatus,
            currency = currency,
            isCurrencyFallback = currency != settings.currency,
            usdToSgdRate = sgdRate,
            unknownSymbols = snapshot.unknownSymbols.filter { it in settings.watchlist },
            lastUpdated = snapshot.fetchedAt,
        )
    }

    fun summarise(rows: List<StockRowUiModel>) = WatchlistSummary(
        upCount = rows.count { it.trend == Trend.UP },
        downCount = rows.count { it.trend == Trend.DOWN },
        averageChangePercent = if (rows.isEmpty()) 0.0 else rows.sumOf { it.changePercent } / rows.size,
        bestPerformer = rows.maxByOrNull { it.changePercent },
        worstPerformer = rows.minByOrNull { it.changePercent },
    )

    private fun StockQuote.toRow(
        currency: DisplayCurrency,
        conversionRate: Double,
        bigMoveThresholdPercent: Float,
    ) = StockRowUiModel(
        symbol = symbol,
        companyName = companyName,
        logoUrl = logoUrl,
        price = priceUsd * conversionRate,
        change = changeUsd * conversionRate,
        changePercent = changePercent,
        currency = currency,
        // The sparkline is drawn relative to its own min/max, so it needs no conversion.
        sparkline = sparkline,
        isBigMove = abs(changePercent) >= bigMoveThresholdPercent,
    )

    private fun List<StockRowUiModel>.sortedBy(order: SortOrder) = when (order) {
        SortOrder.WATCHLIST -> this
        SortOrder.TOP_GAINERS -> sortedByDescending { it.changePercent }
        SortOrder.ALPHABETICAL -> sortedBy { it.symbol }
    }
}
