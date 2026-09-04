package com.iumrah.beta.models.flight

import com.iumrah.beta.core.serialization.BigDecimalJsonSerializer
import com.iumrah.beta.core.serialization.InstantIsoSerializer
import com.iumrah.beta.domain.trip.FlightFareScope
import java.math.BigDecimal
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class FlightDirection { outbound, inbound }

@Serializable
data class FlightAirportSnapshot(
    val code: String,
    val city: String? = null,
    val name: String? = null,
    val terminal: String? = null,
    val timeZoneIdentifier: String? = null,
) {
    val displayCity: String get() = city?.takeIf { it.isNotBlank() } ?: code
    val displayAirport: String get() = name?.takeIf { it.isNotBlank() } ?: code
}

@Serializable
data class FlightSegment(
    val id: String,
    val airline: String,
    val airlineCode: String? = null,
    val flightNumber: String,
    val origin: FlightAirportSnapshot,
    val destination: FlightAirportSnapshot,
    @Serializable(with = InstantIsoSerializer::class) val departureAt: Instant,
    @Serializable(with = InstantIsoSerializer::class) val arrivalAt: Instant,
    val durationMinutes: Int,
    val aircraft: String? = null,
    val operatingCarrier: String? = null,
    val cabin: String? = null,
)

@Serializable
data class FlightBaggageAllowance(val carryOn: Int? = null, val checked: Int? = null)

@Serializable
data class FlightPairedLeg(
    val airline: String,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    @Serializable(with = InstantIsoSerializer::class) val departureAt: Instant,
    @Serializable(with = InstantIsoSerializer::class) val arrivalAt: Instant,
    val stops: Int,
    val durationMinutes: Int,
    val segments: List<FlightSegment>? = null,
)

@Serializable
data class LiveFlightCandidate(
    val id: String,
    val sourceID: String,
    val sourceName: String,
    val direction: FlightDirection,
    val airline: String,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    @Serializable(with = InstantIsoSerializer::class) val departureAt: Instant,
    @Serializable(with = InstantIsoSerializer::class) val arrivalAt: Instant,
    val stops: Int,
    val durationMinutes: Int,
    @Serializable(with = BigDecimalJsonSerializer::class) val observedFare: BigDecimal,
    val observedCurrency: String,
    val fareScope: FlightFareScope,
    @Serializable(with = InstantIsoSerializer::class) val observedAt: Instant,
    val sourceURL: String? = null,
    val rawFingerprint: String? = null,
    val airlineCode: String? = null,
    val segments: List<FlightSegment>? = null,
    val connectionAirports: List<FlightAirportSnapshot>? = null,
    val providerItineraryID: String? = null,
    val cabinClass: String? = null,
    val baggage: FlightBaggageAllowance? = null,
    val requiresSelfTransfer: Boolean? = null,
) {
    fun isDisplayable(now: Instant = Instant.now()): Boolean {
        val age = now.epochSecond - observedAt.epochSecond
        if (sourceID.isBlank() || sourceName.isBlank() || observedFare <= BigDecimal.ZERO || fareScope == FlightFareScope.UNKNOWN) return false
        if (!observedCurrency.matches(Regex("^[A-Z]{3}$")) || age < -300 || age > 12 * 3600 + 300) return false
        if (!origin.matches(Regex("^[A-Z]{3}$")) || !destination.matches(Regex("^[A-Z]{3}$")) || origin == destination) return false
        if (!departureAt.isBefore(arrivalAt) || stops < 0 || durationMinutes <= 0) return false
        val chain = segments ?: return false
        if (chain.isEmpty() || chain.size != stops + 1) return false
        if (chain.first().origin.code != origin || chain.last().destination.code != destination) return false
        for (index in chain.indices) {
            val segment = chain[index]
            if (segment.origin.code == segment.destination.code || !segment.departureAt.isBefore(segment.arrivalAt)) return false
            if (index > 0) {
                val previous = chain[index - 1]
                if (previous.destination.code != segment.origin.code || previous.arrivalAt.isAfter(segment.departureAt)) return false
            }
        }
        val connections = connectionAirports.orEmpty()
        return if (stops == 0) connections.isEmpty() else connections.size == stops && connections.map { it.code } == chain.dropLast(1).map { it.destination.code }
    }
}

@Serializable
data class LiveFlightJourneyCandidate(
    val id: String,
    val sourceID: String,
    val sourceName: String,
    @Serializable(with = BigDecimalJsonSerializer::class) val totalFare: BigDecimal,
    val currency: String,
    val fareScope: FlightFareScope,
    @Serializable(with = InstantIsoSerializer::class) val observedAt: Instant,
    val providerItineraryID: String,
    val outbound: LiveFlightCandidate,
    val inbound: LiveFlightCandidate? = null,
    val baggage: FlightBaggageAllowance? = null,
    val requiresSelfTransfer: Boolean? = null,
) {
    fun isDisplayable(now: Instant = Instant.now()): Boolean {
        if (totalFare <= BigDecimal.ZERO || fareScope == FlightFareScope.UNKNOWN || !outbound.isDisplayable(now) || outbound.direction != FlightDirection.outbound) return false
        if (!outbound.observedCurrency.equals(currency, true)) return false
        val inboundValue = inbound ?: return true
        return inboundValue.direction == FlightDirection.inbound && inboundValue.isDisplayable(now) && inboundValue.observedCurrency.equals(currency, true)
    }
}
