package au.edu.jcu.assessment.utilityapp.ui

import au.edu.jcu.assessment.utilityapp.data.model.AppSettings
import au.edu.jcu.assessment.utilityapp.data.model.MarketSession
import au.edu.jcu.assessment.utilityapp.data.model.MarketStatus
import au.edu.jcu.assessment.utilityapp.data.model.StockQuote
import au.edu.jcu.assessment.utilityapp.data.model.WatchlistSnapshot
import java.time.Instant

/** Realistic sample data for Compose previews (no network needed). */
object PreviewData {

    val now: Instant = Instant.parse("2026-10-13T16:45:00Z")

    val snapshot = WatchlistSnapshot(
        quotes = listOf(
            StockQuote("AAPL", "Apple Inc", null, 340.97, 4.33, 1.29, 336.64, listOf(336.6, 338.1, 337.6, 339.5, 340.3, 341.0)),
            StockQuote("NVDA", "NVIDIA Corp", null, 182.40, 6.21, 3.52, 176.19, listOf(176.2, 177.0, 179.8, 179.1, 181.5, 182.4)),
            StockQuote("MSFT", "Microsoft Corp", null, 512.30, -2.10, -0.41, 514.40, listOf(514.4, 515.0, 513.2, 512.8, 511.9, 512.3)),
            StockQuote("TSLA", "Tesla Inc", null, 401.12, -17.80, -4.25, 418.92, listOf(418.9, 414.0, 409.5, 405.2, 403.0, 401.1)),
        ),
        unknownSymbols = emptyList(),
        marketStatus = MarketStatus(MarketSession.REGULAR),
        usdToSgdRate = 1.2802,
        fetchedAt = now,
    )

    val content: WatchlistContent = WatchlistUiMapper.toContent(
        snapshot,
        AppSettings(watchlist = listOf("AAPL", "NVDA", "MSFT", "TSLA")),
    )
}
