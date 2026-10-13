package au.edu.jcu.assessment.utilityapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of `GET /quote`. Finnhub returns all zeros for an unknown symbol. */
@Serializable
data class FinnhubQuoteDto(
    @SerialName("c") val currentPrice: Double = 0.0,
    @SerialName("d") val change: Double? = null,
    @SerialName("dp") val changePercent: Double? = null,
    @SerialName("pc") val previousClose: Double = 0.0,
) {
    val isUnknownSymbol: Boolean get() = currentPrice == 0.0 && previousClose == 0.0
}

/** Response of `GET /stock/profile2`. Every field is optional because unknown symbols return `{}`. */
@Serializable
data class FinnhubProfileDto(
    val name: String? = null,
    val logo: String? = null,
)

/** Response of `GET /stock/market-status`. */
@Serializable
data class FinnhubMarketStatusDto(
    val isOpen: Boolean = false,
    /** "pre-market", "regular", "post-market" or null when closed. */
    val session: String? = null,
    val holiday: String? = null,
)

/** Response of `GET /search`. */
@Serializable
data class FinnhubSearchResponseDto(
    val result: List<FinnhubSearchResultDto> = emptyList(),
)

@Serializable
data class FinnhubSearchResultDto(
    val description: String = "",
    val symbol: String = "",
)
