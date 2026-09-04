package com.iumrah.beta.data.flight

import com.iumrah.beta.core.network.APIClient
import com.iumrah.beta.domain.trip.TripDraft
import com.iumrah.beta.models.flight.FlightInfantSeating
import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FlightFareCalendarEntry(
    @SerialName("outbound_date") val outboundDate: String,
    @SerialName("inbound_date") val inboundDate: String? = null,
    @SerialName("min_total_fare") val minTotalFare: Double,
    @SerialName("min_per_traveler_fare") val minPerTravelerFare: Double,
    val currency: String,
    @SerialName("observed_at") val observedAt: String,
) { val id: String get() = "$outboundDate|${inboundDate.orEmpty()}|$currency" }

@Serializable
private data class FlightFareCalendarEnvelope(
    val ok: Boolean,
    val prices: List<FlightFareCalendarEntry> = emptyList(),
    val observations: List<FlightFareCalendarEntry> = emptyList(),
    val suggestions: List<FlightFareCalendarEntry> = emptyList(),
)

data class FlightFareCalendarSnapshot(
    val prices: List<FlightFareCalendarEntry>,
    val observations: List<FlightFareCalendarEntry>,
    val suggestions: List<FlightFareCalendarEntry>,
)

class FlightFareCalendarService(private val api: APIClient) {
    suspend fun load(trip: TripDraft, from: LocalDate, to: LocalDate, selectedOutbound: LocalDate? = null): FlightFareCalendarSnapshot {
        val filters = trip.effectiveFlightFilters
        val query = linkedMapOf<String, String?>(
            "outbound_origin" to trip.originCode,
            "outbound_destination" to trip.outboundDestinationCode,
            "inbound_origin" to trip.returnOriginCode,
            "inbound_destination" to trip.originCode,
            "adults" to trip.adults.toString(),
            "children" to trip.children.toString(),
            "infants_in_seat" to (if (filters.infantSeating == FlightInfantSeating.SEAT) trip.infants else 0).toString(),
            "infants_on_lap" to (if (filters.infantSeating == FlightInfantSeating.LAP) trip.infants else 0).toString(),
            "cabin_class" to filters.cabinClass.wireValue,
            "from" to from.toString(),
            "to" to to.toString(),
            "selected_outbound" to selectedOutbound?.toString(),
        )
        val response = api.get<FlightFareCalendarEnvelope>("/api/package/flights/calendar", query = query, timeoutSeconds = 10)
        return if (response.ok) FlightFareCalendarSnapshot(response.prices, response.observations, response.suggestions)
        else FlightFareCalendarSnapshot(emptyList(), emptyList(), emptyList())
    }
}
