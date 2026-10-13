package au.edu.jcu.assessment.utilityapp.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TwelveDataParserTest {

    private fun parse(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    @Test
    fun singleSymbol_isNotWrappedAndIsReturnedOldestFirst() {
        val response = parse(
            """{"meta":{"symbol":"AAPL"},"values":[{"close":"3.0"},{"close":"2.0"},{"close":"1.0"}],"status":"ok"}"""
        )

        val result = TwelveDataParser.parseClosingPrices(response, listOf("AAPL"))

        assertEquals(mapOf("AAPL" to listOf(1.0, 2.0, 3.0)), result)
    }

    @Test
    fun batch_skipsSymbolsThatFailedIndividually() {
        val response = parse(
            """
            {
              "AAPL": {"values":[{"close":"2.0"},{"close":"1.0"}],"status":"ok"},
              "ZZZZQ": {"code":404,"message":"symbol not found","status":"error"}
            }
            """
        )

        val result = TwelveDataParser.parseClosingPrices(response, listOf("AAPL", "ZZZZQ"))

        assertEquals(mapOf("AAPL" to listOf(1.0, 2.0)), result)
    }

    @Test
    fun requestLevelError_throwsWithCode() {
        val response = parse("""{"code":429,"message":"You have run out of API credits","status":"error"}""")

        val error = assertThrows(TwelveDataException::class.java) {
            TwelveDataParser.parseClosingPrices(response, listOf("AAPL", "NVDA"))
        }
        assertEquals(429, error.code)
    }
}
