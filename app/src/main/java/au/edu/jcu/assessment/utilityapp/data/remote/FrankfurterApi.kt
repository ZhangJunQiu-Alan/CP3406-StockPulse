package au.edu.jcu.assessment.utilityapp.data.remote

import au.edu.jcu.assessment.utilityapp.data.remote.dto.FrankfurterRatesDto
import retrofit2.http.GET
import retrofit2.http.Query

/** Frankfurter exchange-rate API (European Central Bank data, free, no key required). */
interface FrankfurterApi {

    @GET("v1/latest")
    suspend fun getLatestRates(
        @Query("base") base: String,
        @Query("symbols") symbols: String,
    ): FrankfurterRatesDto

    companion object {
        const val BASE_URL = "https://api.frankfurter.dev/"
    }
}
