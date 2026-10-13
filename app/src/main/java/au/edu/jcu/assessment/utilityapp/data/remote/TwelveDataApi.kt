package au.edu.jcu.assessment.utilityapp.data.remote

import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Twelve Data REST API, used only for sparkline price history because Finnhub's free tier
 * no longer includes candles.
 *
 * The response is returned as a raw [JsonObject] because its shape changes with the request:
 * one symbol returns `{meta, values}`, while several comma-separated symbols return
 * `{"AAPL": {meta, values}, "NVDA": {...}}`. [TwelveDataParser] handles both.
 */
interface TwelveDataApi {

    @GET("time_series")
    suspend fun getTimeSeries(
        @Query("symbol") commaSeparatedSymbols: String,
        @Query("interval") interval: String,
        @Query("outputsize") outputSize: Int,
    ): JsonObject

    companion object {
        const val BASE_URL = "https://api.twelvedata.com/"
        const val API_KEY_HEADER = "Authorization"
        const val API_KEY_PREFIX = "apikey "
    }
}
