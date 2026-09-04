package com.iumrah.beta.domain.journey

import com.iumrah.beta.data.flight.IgnavFlightInventoryProvider
import com.iumrah.beta.domain.trip.FlightTripType
import com.iumrah.beta.domain.trip.JourneyScope
import com.iumrah.beta.domain.trip.TripDraft
import com.iumrah.beta.models.flight.FlightJourneyDatePair
import com.iumrah.beta.models.flight.FlightJourneySearchRequest
import com.iumrah.beta.models.flight.LiveFlightJourneyCandidate
import com.iumrah.beta.models.hotel.HotelSummary
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class JourneyState(
    val trip: TripDraft = TripDraft(),
    val makkahHotel: HotelSummary? = null,
    val madinahHotel: HotelSummary? = null,
    val flightResults: List<LiveFlightJourneyCandidate> = emptyList(),
    val selectedJourneyId: String? = null,
    val isSearchingFlights: Boolean = false,
    val flightError: String? = null,
) {
    val selectedJourney: LiveFlightJourneyCandidate? get() = flightResults.firstOrNull { it.id == selectedJourneyId }
    val hasRequiredHotels: Boolean get() = makkahHotel != null && (trip.scope != JourneyScope.MAKKAH_AND_MADINAH || madinahHotel != null)
}

class JourneyStore {
    private val _state = MutableStateFlow(JourneyState())
    val state: StateFlow<JourneyState> = _state

    fun updateTrip(value: TripDraft) {
        _state.update { current ->
            if (current.trip == value) current else JourneyState(trip = value)
        }
    }

    fun selectHotel(hotel: HotelSummary) {
        _state.update { current ->
            if (hotel.city.equals("Madinah", true) || hotel.city.equals("Medina", true)) current.copy(madinahHotel = hotel)
            else current.copy(makkahHotel = hotel)
        }
    }

    fun clearFlights() { _state.update { it.copy(flightResults = emptyList(), selectedJourneyId = null, flightError = null) } }

    suspend fun searchFlights(provider: IgnavFlightInventoryProvider) {
        val trip = _state.value.trip
        if (!trip.canContinue) {
            _state.update { it.copy(flightError = "INVALID_TRIP") }
            return
        }
        _state.update { it.copy(isSearchingFlights = true, flightError = null, flightResults = emptyList(), selectedJourneyId = null) }
        val filters = trip.effectiveFlightFilters
        val request = FlightJourneySearchRequest(
            outboundOrigin = trip.originCode,
            outboundDestination = trip.outboundDestinationCode,
            inboundOrigin = if (trip.isRoundTripFlight) trip.returnOriginCode else null,
            inboundDestination = if (trip.isRoundTripFlight) trip.originCode else null,
            adults = trip.adults,
            children = trip.children,
            infants = trip.infants,
            cabin = filters.cabinClass.wireValue,
            filters = filters,
        )
        val pairs = listOf(FlightJourneyDatePair(trip.departureDate, if (trip.isRoundTripFlight) trip.returnDate else null))
        runCatching {
            provider.searchJourney(request, pairs) { partial ->
                _state.update { it.copy(flightResults = partial) }
            }
        }.onSuccess { values ->
            _state.update { it.copy(flightResults = values, isSearchingFlights = false, flightError = if (values.isEmpty()) "NO_RESULTS" else null) }
        }.onFailure { error ->
            _state.update { it.copy(isSearchingFlights = false, flightError = error.message ?: "SEARCH_FAILED") }
        }
    }

    fun selectJourney(id: String) {
        _state.update { current ->
            val journey = current.flightResults.firstOrNull { it.id == id } ?: return@update current
            val arrivalSegment = journey.outbound.segments?.lastOrNull()
            val zone = arrivalSegment?.destination?.timeZoneIdentifier?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.of("Asia/Riyadh")
            val arrivalDate = journey.outbound.arrivalAt.atZone(zone).toLocalDate()
            current.copy(selectedJourneyId = id, trip = current.trip.copy(saudiArrivalDate = arrivalDate))
        }
    }
}
