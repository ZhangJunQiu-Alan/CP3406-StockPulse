package au.edu.jcu.assessment.utilityapp.data.remote

import au.edu.jcu.assessment.utilityapp.data.remote.dto.FinnhubMarketStatusDto
import au.edu.jcu.assessment.utilityapp.data.remote.dto.FinnhubProfileDto
import au.edu.jcu.assessment.utilityapp.data.remote.dto.FinnhubQuoteDto
import au.edu.jcu.assessment.utilityapp.data.remote.dto.FinnhubSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Finnhub REST API (free tier: US stocks, ~60 calls/minute).
 * The API key is attached as a header by [ApiKeyInterceptor], so it never appears in URLs.
 */
interface FinnhubApi {

    @GET("quote")
    suspend fun getQuote(@Query("symbol") symbol: String): FinnhubQuoteDto

    @GET("stock/profile2")
    suspend fun getCompanyProfile(@Query("symbol") symbol: String): FinnhubProfileDto

    @GET("stock/market-status")
    suspend fun getMarketStatus(@Query("exchange") exchange: String = "US"): FinnhubMarketStatusDto

    @GET("search")
    suspend fun searchSymbol(
        @Query("q") query: String,
        @Query("exchange") exchange: String = "US",
    ): FinnhubSearchResponseDto

    companion object {
        const val BASE_URL = "https://finnhub.io/api/v1/"
        const val API_KEY_HEADER = "X-Finnhub-Token"
    }
}
