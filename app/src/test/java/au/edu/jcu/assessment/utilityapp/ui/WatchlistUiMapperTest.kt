package au.edu.jcu.assessment.utilityapp.ui

import au.edu.jcu.assessment.utilityapp.data.model.AppSettings
import au.edu.jcu.assessment.utilityapp.data.model.ChartRange
import au.edu.jcu.assessment.utilityapp.data.model.DisplayCurrency
import au.edu.jcu.assessment.utilityapp.data.model.MarketSession
import au.edu.jcu.assessment.utilityapp.data.model.MarketStatus
import au.edu.jcu.assessment.utilityapp.data.model.SortOrder
import au.edu.jcu.assessment.utilityapp.data.model.WatchlistSnapshot
import au.edu.jcu.assessment.utilityapp.fakes.FakeStockRepository.Companion.quote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class WatchlistUiMapperTest {

    private val snapshot = WatchlistSnapshot(
        quotes = listOf(
            quote("AAPL", price = 100.0, changePercent = 1.0),
            quote("NVDA", price = 50.0, changePercent = 4.0),
            quote("TSLA", price = 200.0, changePercent = -5.0),
        ),
        unknownSymbols = emptyList(),
        marketStatus = MarketStatus(MarketSession.REGULAR),
        usdToSgdRate = 1.3,
        fetchedAt = Instant.EPOCH,
    )
    private val baseSettings = AppSettings(
        watchlist = listOf("AAPL", "NVDA", "TSLA"),
        currency = DisplayCurrency.USD,
        chartRange = ChartRange.ONE_WEEK,
    )

    @Test
    fun sgdCurrency_convertsPricesWithExchangeRate() {
        val content = WatchlistUiMapper.toContent(snapshot, baseSettings.copy(currency = DisplayCurrency.SGD))

        assertEquals(DisplayCurrency.SGD, content.currency)
        assertEquals(130.0, content.rows.first { it.symbol == "AAPL" }.price, 1e-9)
        assertFalse(content.isCurrencyFallback)
    }

    @Test
    fun sgdCurrency_withoutRate_fallsBackToUsd() {
        val content = WatchlistUiMapper.toContent(
            snapshot.copy(usdToSgdRate = null),
            baseSettings.copy(currency = DisplayCurrency.SGD),
        )

        assertEquals(DisplayCurrency.USD, content.currency)
        assertEquals(100.0, content.rows.first { it.symbol == "AAPL" }.price, 1e-9)
        assertTrue(content.isCurrencyFallback)
    }

    @Test
    fun sortOrders_orderRowsCorrectly() {
        fun symbolsFor(order: SortOrder) =
            WatchlistUiMapper.toContent(snapshot, baseSettings.copy(sortOrder = order)).rows.map { it.symbol }

        assertEquals(listOf("AAPL", "NVDA", "TSLA"), symbolsFor(SortOrder.WATCHLIST))
        assertEquals(listOf("NVDA", "AAPL", "TSLA"), symbolsFor(SortOrder.TOP_GAINERS))
        assertEquals(listOf("AAPL", "NVDA", "TSLA"), symbolsFor(SortOrder.ALPHABETICAL))
    }

    @Test
    fun bigMovers_useAbsoluteChangeAgainstThreshold() {
        val content = WatchlistUiMapper.toContent(snapshot, baseSettings.copy(bigMoveThresholdPercent = 4f))

        assertEquals(listOf("NVDA", "TSLA"), content.bigMovers.map { it.symbol })
    }

    @Test
    fun removedSymbol_disappearsWithoutRefetch() {
        val content = WatchlistUiMapper.toContent(snapshot, baseSettings.copy(watchlist = listOf("AAPL")))

        assertEquals(listOf("AAPL"), content.rows.map { it.symbol })
    }

    @Test
    fun summary_countsDirectionAndFindsExtremes() {
        val summary = WatchlistUiMapper.toContent(snapshot, baseSettings).summary

        assertEquals(2, summary.upCount)
        assertEquals(1, summary.downCount)
        assertEquals(0.0, summary.averageChangePercent, 1e-9)
        assertEquals("NVDA", summary.bestPerformer?.symbol)
        assertEquals("TSLA", summary.worstPerformer?.symbol)
    }

    @Test
    fun oneDaySparkline_startsAtPreviousClose() {
        val content = WatchlistUiMapper.toContent(snapshot, baseSettings.copy(chartRange = ChartRange.ONE_DAY))
        val aapl = content.rows.first { it.symbol == "AAPL" }

        assertEquals(100.0 / 1.01, aapl.sparkline.first(), 1e-9)
        assertEquals(Trend.UP, aapl.sparklineTrend)
    }
}
