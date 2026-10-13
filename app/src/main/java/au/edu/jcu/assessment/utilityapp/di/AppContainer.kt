package au.edu.jcu.assessment.utilityapp.di

import au.edu.jcu.assessment.utilityapp.BuildConfig
import au.edu.jcu.assessment.utilityapp.data.remote.ApiKeyInterceptor
import au.edu.jcu.assessment.utilityapp.data.remote.FinnhubApi
import au.edu.jcu.assessment.utilityapp.data.remote.FrankfurterApi
import au.edu.jcu.assessment.utilityapp.data.remote.TwelveDataApi
import au.edu.jcu.assessment.utilityapp.data.repository.InMemorySettingsRepository
import au.edu.jcu.assessment.utilityapp.data.repository.NetworkStockRepository
import au.edu.jcu.assessment.utilityapp.data.repository.SettingsRepository
import au.edu.jcu.assessment.utilityapp.data.repository.StockRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit

/** Manual dependency-injection container: builds and owns the app-wide singletons. */
interface AppContainer {
    val stockRepository: StockRepository
    val settingsRepository: SettingsRepository
}

class DefaultAppContainer : AppContainer {

    private val json = Json { ignoreUnknownKeys = true }

    /** One shared client so all three APIs reuse the same connection pool and thread pool. */
    private val baseHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private fun clientWithApiKey(headerName: String, headerValue: String): OkHttpClient =
        baseHttpClient.newBuilder()
            .addInterceptor(ApiKeyInterceptor(headerName, headerValue))
            .build()

    private val finnhubApi: FinnhubApi by lazy {
        retrofit(
            FinnhubApi.BASE_URL,
            clientWithApiKey(FinnhubApi.API_KEY_HEADER, BuildConfig.FINNHUB_API_KEY),
        ).create()
    }

    private val twelveDataApi: TwelveDataApi by lazy {
        retrofit(
            TwelveDataApi.BASE_URL,
            clientWithApiKey(
                TwelveDataApi.API_KEY_HEADER,
                TwelveDataApi.API_KEY_PREFIX + BuildConfig.TWELVE_DATA_API_KEY,
            ),
        ).create()
    }

    private val frankfurterApi: FrankfurterApi by lazy {
        retrofit(FrankfurterApi.BASE_URL, baseHttpClient).create()
    }

    override val stockRepository: StockRepository by lazy {
        NetworkStockRepository(finnhubApi, twelveDataApi, frankfurterApi)
    }

    override val settingsRepository: SettingsRepository by lazy {
        InMemorySettingsRepository()
    }
}
