package au.edu.jcu.assessment.utilityapp.data.repository

import au.edu.jcu.assessment.utilityapp.data.model.ChartRange
import au.edu.jcu.assessment.utilityapp.data.model.SymbolMatch
import au.edu.jcu.assessment.utilityapp.data.model.WatchlistSnapshot

/** Single source of market data for the app. The UI layer never talks to an API directly. */
interface StockRepository {

    /** Fetches quotes, sparklines, market status and the USD→SGD rate for [symbols]. */
    suspend fun getWatchlistSnapshot(symbols: List<String>, chartRange: ChartRange): WatchlistSnapshot

    /** Returns the exact US-listed match for [symbol], or null if no such stock exists. */
    suspend fun findSymbol(symbol: String): SymbolMatch?
}
