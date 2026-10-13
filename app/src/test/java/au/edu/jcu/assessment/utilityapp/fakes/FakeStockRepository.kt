package au.edu.jcu.assessment.utilityapp.fakes

import au.edu.jcu.assessment.utilityapp.data.model.ChartRange
import au.edu.jcu.assessment.utilityapp.data.model.MarketSession
import au.edu.jcu.assessment.utilityapp.data.model.MarketStatus
import au.edu.jcu.assessment.utilityapp.data.model.StockQuote
import au.edu.jcu.assessment.utilityapp.data.model.SymbolMatch
import au.edu.jcu.assessment.utilityapp.data.model.WatchlistSnapshot
import au.edu.jcu.assessment.utilityapp.data.repository.StockRepository
import java.io.IOException
import java.time.Instant

/** In-memory [StockRepository] for tests: no network, configurable results, counts calls. */
class FakeStockRepository : StockRepository {

    val quotes = mutableMapOf(
        "AAPL" to quote("AAPL", price = 100.0, changePercent = 1.0),
        "TSLA" to quote("TSLA", price = 200.0, changePercent = -5.0),
    )
    val searchableSymbols = mutableMapOf("AMZN" to "Amazon.com Inc")
    var usdToSgdRate: Double? = 1.25
    var shouldFail = false
    var snapshotRequests = 0
        private set

    override suspend fun getWatchlistSnapshot(symbols: List<String>, chartRange: ChartRange): WatchlistSnapshot {
        snapshotRequests++
        if (shouldFail) throw IOException("offline")
        return WatchlistSnapshot(
            quotes = symbols.mapNotNull { quotes[it] },
            unknownSymbols = symbols.filter { it !in quotes },
            marketStatus = MarketStatus(MarketSession.REGULAR),
            usdToSgdRate = usdToSgdRate,
            fetchedAt = Instant.parse("2026-10-13T15:00:00Z"),
        )
    }

    override suspend fun findSymbol(symbol: String): SymbolMatch? {
        if (shouldFail) throw IOException("offline")
        return searchableSymbols[symbol]?.let { SymbolMatch(symbol, it) }
    }

    companion object {
        fun quote(symbol: String, price: Double, changePercent: Double) = StockQuote(
            symbol = symbol,
            companyName = "$symbol Inc",
            logoUrl = null,
            priceUsd = price,
            changeUsd = price * changePercent / 100,
            changePercent = changePercent,
            previousCloseUsd = price / (1 + changePercent / 100),
            sparkline = listOf(price - 1, price),
        )
    }
}
