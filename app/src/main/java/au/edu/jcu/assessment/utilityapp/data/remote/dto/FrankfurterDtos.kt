package au.edu.jcu.assessment.utilityapp.data.remote.dto

import kotlinx.serialization.Serializable

/** Response of Frankfurter's `GET /v1/latest`, e.g. `{"base":"USD","rates":{"SGD":1.28}}`. */
@Serializable
data class FrankfurterRatesDto(
    val base: String,
    val rates: Map<String, Double>,
)
