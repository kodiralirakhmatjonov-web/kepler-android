package com.iumrah.beta.data.flight

import com.iumrah.beta.core.network.APIClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FlightDiscoveryOffer(
    val id: String,
    val origin: String,
    val destination: String,
    @SerialName("originAirport") val originAirport: String = "",
    @SerialName("destinationAirport") val destinationAirport: String = "",
    val price: Double,
    @SerialName("airlineCode") val airlineCode: String = "",
    @SerialName("flightNumber") val flightNumber: String = "",
    @SerialName("departureAt") val departureAt: String,
    @SerialName("returnAt") val returnAt: String? = null,
    val transfers: Int = 0,
    @SerialName("returnTransfers") val returnTransfers: Int? = null,
    @SerialName("durationMinutes") val durationMinutes: Int = 0,
    @SerialName("returnDurationMinutes") val returnDurationMinutes: Int? = null,
    @SerialName("bookingUrl") val bookingUrl: String? = null,
) {
    val isRoundTrip: Boolean get() = !returnAt.isNullOrBlank()
    val isDirect: Boolean get() = transfers == 0 && (returnTransfers ?: 0) == 0
    val routeTitle: String get() = "${originAirport.ifBlank { origin }} → ${destinationAirport.ifBlank { destination }}"
    val monitorKey: String get() = listOf(
        origin.uppercase(), destination.uppercase(), airlineCode.uppercase(), flightNumber.uppercase(),
        departureAt.take(16), returnAt.orEmpty().take(16),
    ).joinToString("|")
}

@Serializable
data class FlightDiscoveryCalendarDay(
    val date: String,
    val id: String,
    val origin: String,
    val destination: String,
    @SerialName("originAirport") val originAirport: String = "",
    @SerialName("destinationAirport") val destinationAirport: String = "",
    val price: Double,
    @SerialName("airlineCode") val airlineCode: String = "",
    @SerialName("flightNumber") val flightNumber: String = "",
    @SerialName("departureAt") val departureAt: String,
    @SerialName("returnAt") val returnAt: String? = null,
    val transfers: Int = 0,
    @SerialName("returnTransfers") val returnTransfers: Int? = null,
    @SerialName("durationMinutes") val durationMinutes: Int = 0,
    @SerialName("returnDurationMinutes") val returnDurationMinutes: Int? = null,
    @SerialName("bookingUrl") val bookingUrl: String? = null,
) {
    val offer: FlightDiscoveryOffer get() = FlightDiscoveryOffer(
        id, origin, destination, originAirport, destinationAirport, price, airlineCode, flightNumber,
        departureAt, returnAt, transfers, returnTransfers, durationMinutes, returnDurationMinutes, bookingUrl,
    )
}

@Serializable
private data class FlightDiscoveryOffersEnvelope(
    val ok: Boolean,
    val currency: String? = null,
    @SerialName("generatedAt") val generatedAt: String? = null,
    val offers: List<FlightDiscoveryOffer> = emptyList(),
)

@Serializable
private data class FlightDiscoveryCalendarEnvelope(
    val ok: Boolean,
    val currency: String? = null,
    @SerialName("generatedAt") val generatedAt: String? = null,
    val days: List<FlightDiscoveryCalendarDay> = emptyList(),
)

data class FlightDiscoveryOffersResult(
    val offers: List<FlightDiscoveryOffer>,
    val currency: String,
    val generatedAt: String?,
)

data class FlightDiscoveryCalendarResult(
    val days: List<FlightDiscoveryCalendarDay>,
    val currency: String,
    val generatedAt: String?,
)

class AviasalesFlightDiscoveryService(private val api: APIClient) {
    suspend fun offers(
        origin: String,
        destination: String,
        departure: String,
        returnAt: String? = null,
        direct: Boolean = false,
        limit: Int = 100,
        currency: String = "usd",
    ): FlightDiscoveryOffersResult {
        val response = api.get<FlightDiscoveryOffersEnvelope>(
            "/api/package/flights/data",
            query = linkedMapOf(
                "view" to if (direct) "direct" else "offers",
                "origin" to origin.uppercase(),
                "destination" to destination.uppercase(),
                "departure" to departure,
                "return" to returnAt?.takeIf { it.isNotBlank() },
                "currency" to currency.lowercase(),
                "limit" to limit.coerceIn(1, 100).toString(),
            ),
            timeoutSeconds = 15,
        )
        return if (response.ok) FlightDiscoveryOffersResult(response.offers, response.currency ?: currency, response.generatedAt)
        else FlightDiscoveryOffersResult(emptyList(), response.currency ?: currency, response.generatedAt)
    }

    suspend fun calendar(
        origin: String,
        destination: String,
        month: String,
        direct: Boolean = false,
        currency: String = "usd",
    ): FlightDiscoveryCalendarResult {
        val response = api.get<FlightDiscoveryCalendarEnvelope>(
            "/api/package/flights/data",
            query = mapOf(
                "view" to "calendar",
                "origin" to origin.uppercase(),
                "destination" to destination.uppercase(),
                "departure" to month,
                "currency" to currency.lowercase(),
                "direct" to direct.toString(),
            ),
            timeoutSeconds = 15,
        )
        return if (response.ok) FlightDiscoveryCalendarResult(response.days, response.currency ?: currency, response.generatedAt)
        else FlightDiscoveryCalendarResult(emptyList(), response.currency ?: currency, response.generatedAt)
    }
}
