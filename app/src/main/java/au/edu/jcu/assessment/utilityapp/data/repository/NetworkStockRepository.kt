package au.edu.jcu.assessment.utilityapp.data.repository

import au.edu.jcu.assessment.utilityapp.data.model.ChartRange
import au.edu.jcu.assessment.utilityapp.data.model.MarketSession
import au.edu.jcu.assessment.utilityapp.data.model.MarketStatus
import au.edu.jcu.assessment.utilityapp.data.model.StockQuote
import au.edu.jcu.assessment.utilityapp.data.model.SymbolMatch
import au.edu.jcu.assessment.utilityapp.data.model.WatchlistSnapshot
import au.edu.jcu.assessment.utilityapp.data.remote.FinnhubApi
import au.edu.jcu.assessment.utilityapp.data.remote.FrankfurterApi
import au.edu.jcu.assessment.utilityapp.data.remote.TwelveDataApi
import au.edu.jcu.assessment.utilityapp.data.remote.TwelveDataParser
import au.edu.jcu.assessment.utilityapp.data.remote.dto.FinnhubMarketStatusDto
import au.edu.jcu.assessment.utilityapp.data.remote.dto.FinnhubProfileDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

/**
 * Combines three web APIs into one [WatchlistSnapshot]:
 * - Finnhub: live quotes, company names/logos and market status
 * - Twelve Data: recent price history for the sparklines
 * - Frankfurter: the USD→SGD exchange rate
 *
 * All calls run in parallel. Data that rarely changes (profiles, FX rate, sparklines) is cached
 * in memory so frequent auto-refreshes stay well inside the free-tier rate limits.
 */
class NetworkStockRepository(
    private val finnhubApi: FinnhubApi,
    private val twelveDataApi: TwelveDataApi,
    private val frankfurterApi: FrankfurterApi,
    private val clock: Clock = Clock.systemUTC(),
) : StockRepository {

    private data class Cached<T>(val value: T, val fetchedAt: Instant)

    private val profileCache = ConcurrentHashMap<String, FinnhubProfileDto>()
    private val sparklineCache = ConcurrentHashMap<Pair<String, ChartRange>, Cached<List<Double>>>()
    @Volatile private var exchangeRateCache: Cached<Double>? = null

    override suspend fun getWatchlistSnapshot(
        symbols: List<String>,
        chartRange: ChartRange,
    ): WatchlistSnapshot = coroutineScope {
        val quotes = symbols.map { symbol -> async { fetchQuote(symbol) } }
        val marketStatus = async { finnhubApi.getMarketStatus().toMarketStatus() }
        // Sparklines and FX are "nice to have": if they fail, the quotes are still shown.
        val sparklines = async { optional { fetchSparklines(symbols, chartRange) } ?: emptyMap() }
        val usdToSgd = async { optional { fetchUsdToSgdRate() } ?: exchangeRateCache?.value }

        val sparklineBySymbol = sparklines.await()
        val quoteResults = symbols.zip(quotes.awaitAll())

        WatchlistSnapshot(
            quotes = quoteResults.mapNotNull { (symbol, quote) ->
                quote?.copy(sparkline = sparklineBySymbol[symbol].orEmpty())
            },
            unknownSymbols = quoteResults.filter { it.second == null }.map { it.first },
            marketStatus = marketStatus.await(),
            usdToSgdRate = usdToSgd.await(),
            fetchedAt = clock.instant(),
        )
    }

    override suspend fun findSymbol(symbol: String): SymbolMatch? {
        val match = finnhubApi.searchSymbol(symbol).result
            .firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
            ?: return null
        return SymbolMatch(symbol = match.symbol, companyName = match.description)
    }

    /** @return null if Finnhub doesn't recognise the symbol. */
    private suspend fun fetchQuote(symbol: String): StockQuote? = coroutineScope {
        val quoteRequest = async { finnhubApi.getQuote(symbol) }
        val profileRequest = async { fetchProfile(symbol) }

        val quote = quoteRequest.await()
        if (quote.isUnknownSymbol) return@coroutineScope null
        val profile = profileRequest.await()

        StockQuote(
            symbol = symbol,
            companyName = profile.name ?: symbol,
            logoUrl = profile.logo?.takeIf { it.isNotBlank() },
            priceUsd = quote.currentPrice,
            changeUsd = quote.change ?: 0.0,
            changePercent = quote.changePercent ?: 0.0,
        )
    }

    /** Company names and logos never change during a session, so fetch each one only once. */
    private suspend fun fetchProfile(symbol: String): FinnhubProfileDto =
        profileCache[symbol] ?: optional { finnhubApi.getCompanyProfile(symbol) }
            ?.also { profileCache[symbol] = it }
            ?: FinnhubProfileDto()

    /**
     * Only symbols whose cached series has expired are requested, in a single batched call,
     * because every symbol costs one of Twelve Data's 8 free credits per minute.
     */
    private suspend fun fetchSparklines(
        symbols: List<String>,
        chartRange: ChartRange,
    ): Map<String, List<Double>> {
        val now = clock.instant()
        val maxAge = chartRange.cacheDuration()
        val stale = symbols.filter { symbol ->
            val cached = sparklineCache[symbol to chartRange]
            cached == null || Duration.between(cached.fetchedAt, now) > maxAge
        }

        if (stale.isNotEmpty()) {
            val response = twelveDataApi.getTimeSeries(
                commaSeparatedSymbols = stale.joinToString(","),
                interval = chartRange.apiInterval,
                outputSize = chartRange.pointCount,
            )
            TwelveDataParser.parseClosingPrices(response, stale).forEach { (symbol, series) ->
                sparklineCache[symbol to chartRange] = Cached(series, now)
            }
        }

        return symbols.mapNotNull { symbol ->
            sparklineCache[symbol to chartRange]?.let { symbol to it.value }
        }.toMap()
    }

    /** ECB reference rates update once per working day, so an hourly cache is plenty. */
    private suspend fun fetchUsdToSgdRate(): Double? {
        val now = clock.instant()
        exchangeRateCache
            ?.takeIf { Duration.between(it.fetchedAt, now) < EXCHANGE_RATE_MAX_AGE }
            ?.let { return it.value }

        val rate = frankfurterApi.getLatestRates(base = "USD", symbols = "SGD").rates["SGD"]
        if (rate != null) exchangeRateCache = Cached(rate, now)
        return rate
    }

    private fun ChartRange.cacheDuration(): Duration = when (this) {
        ChartRange.ONE_DAY -> Duration.ofMinutes(5)
        ChartRange.ONE_WEEK -> Duration.ofMinutes(30)
        ChartRange.ONE_MONTH -> Duration.ofHours(6)
    }

    private fun FinnhubMarketStatusDto.toMarketStatus() = MarketStatus(
        session = when (session) {
            "pre-market" -> MarketSession.PRE_MARKET
            "regular" -> MarketSession.REGULAR
            "post-market" -> MarketSession.POST_MARKET
            else -> MarketSession.CLOSED
        },
        holiday = holiday?.takeIf { it.isNotBlank() },
    )

    /** Runs a non-essential request, swallowing failures but never coroutine cancellation. */
    private suspend fun <T> optional(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private companion object {
        val EXCHANGE_RATE_MAX_AGE: Duration = Duration.ofHours(1)
    }
}
