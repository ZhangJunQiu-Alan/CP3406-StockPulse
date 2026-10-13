package au.edu.jcu.assessment.utilityapp.ui

import au.edu.jcu.assessment.utilityapp.data.model.AppSettings
import au.edu.jcu.assessment.utilityapp.data.model.ChartRange
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
            .map { it.toRow(currency, conversionRate, settings) }
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
        settings: AppSettings,
    ) = StockRowUiModel(
        symbol = symbol,
        companyName = companyName,
        logoUrl = logoUrl,
        price = priceUsd * conversionRate,
        change = changeUsd * conversionRate,
        changePercent = changePercent,
        currency = currency,
        sparkline = sparklineFor(settings.chartRange),
        isBigMove = abs(changePercent) >= settings.bigMoveThresholdPercent,
    )

    /**
     * The sparkline is drawn relative to its own min/max, so it needs no currency conversion.
     * For 1D the previous close is prepended: the intraday series alone misses the overnight gap,
     * which could make the line fall while the stock is up for the day (and be coloured wrongly).
     */
    private fun StockQuote.sparklineFor(range: ChartRange): List<Double> =
        if (range == ChartRange.ONE_DAY && sparkline.isNotEmpty() && previousCloseUsd > 0) {
            listOf(previousCloseUsd) + sparkline
        } else {
            sparkline
        }

    private fun List<StockRowUiModel>.sortedBy(order: SortOrder) = when (order) {
        SortOrder.WATCHLIST -> this
        SortOrder.TOP_GAINERS -> sortedByDescending { it.changePercent }
        SortOrder.ALPHABETICAL -> sortedBy { it.symbol }
    }
}
