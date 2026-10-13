package au.edu.jcu.assessment.utilityapp.data.remote

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Thrown when Twelve Data rejects the whole request (bad key, rate limit, etc.). */
class TwelveDataException(val code: Int?, message: String) : Exception(message)

/** Turns Twelve Data `time_series` responses into closing-price series keyed by symbol. */
object TwelveDataParser {

    /**
     * @return chronological closing prices for each symbol that succeeded. Symbols that failed
     * individually (e.g. not found) are simply left out so one bad symbol can't break the rest.
     * @throws TwelveDataException if the request as a whole failed.
     */
    fun parseClosingPrices(response: JsonObject, requestedSymbols: List<String>): Map<String, List<Double>> {
        if (response.isError()) {
            throw TwelveDataException(
                code = response["code"]?.jsonPrimitive?.intOrNull,
                message = response["message"]?.jsonPrimitive?.content ?: "Twelve Data request failed",
            )
        }

        // A single-symbol request is not wrapped in a {"SYMBOL": ...} object.
        if (requestedSymbols.size == 1) {
            val series = response.closingPrices() ?: return emptyMap()
            return mapOf(requestedSymbols.single() to series)
        }

        return requestedSymbols.mapNotNull { symbol ->
            val entry = response[symbol] as? JsonObject ?: return@mapNotNull null
            if (entry.isError()) return@mapNotNull null
            entry.closingPrices()?.let { symbol to it }
        }.toMap()
    }

    private fun JsonObject.isError(): Boolean =
        this["status"]?.jsonPrimitive?.content == "error"

    /** Twelve Data lists newest first; charts read left-to-right, so reverse into time order. */
    private fun JsonObject.closingPrices(): List<Double>? {
        val values = this["values"] as? JsonArray ?: return null
        return values
            .mapNotNull { (it as? JsonObject)?.get("close")?.jsonPrimitive?.doubleOrNull }
            .reversed()
    }
}
