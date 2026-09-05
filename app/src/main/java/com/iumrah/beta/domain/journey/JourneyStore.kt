package com.iumrah.beta.domain.journey

import com.iumrah.beta.data.flight.IgnavFlightInventoryProvider
import com.iumrah.beta.domain.pricing.PackageQuote
import com.iumrah.beta.domain.trip.JourneyScope
import com.iumrah.beta.domain.trip.TripDraft
import com.iumrah.beta.models.flight.FlightJourneyDatePair
import com.iumrah.beta.models.flight.FlightJourneySearchRequest
import com.iumrah.beta.models.flight.LiveFlightJourneyCandidate
import com.iumrah.beta.models.hotel.HotelRoom
import com.iumrah.beta.models.hotel.HotelSummary
import com.iumrah.beta.models.hotel.IumrahRoomCategoryOption
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class JourneyState(
    val trip: TripDraft = TripDraft(),
    val makkahHotel: HotelSummary? = null,
    val makkahRoom: HotelRoom? = null,
    val makkahRoomCategory: IumrahRoomCategoryOption? = null,
    val madinahHotel: HotelSummary? = null,
    val madinahRoom: HotelRoom? = null,
    val madinahRoomCategory: IumrahRoomCategoryOption? = null,
    val flightResults: List<LiveFlightJourneyCandidate> = emptyList(),
    val selectedJourneyId: String? = null,
    val quote: PackageQuote? = null,
    val isSearchingFlights: Boolean = false,
    val flightError: String? = null,
    val packageError: String? = null,
) {
    val selectedJourney: LiveFlightJourneyCandidate? get() = flightResults.firstOrNull { it.id == selectedJourneyId }
    val hasMakkahRoomSelection: Boolean get() = makkahRoom != null || makkahRoomCategory != null
    val hasMadinahRoomSelection: Boolean get() = madinahRoom != null || madinahRoomCategory != null
    val hasRequiredHotels: Boolean get() =
        makkahHotel != null && hasMakkahRoomSelection &&
            (trip.scope != JourneyScope.MAKKAH_AND_MADINAH || (madinahHotel != null && hasMadinahRoomSelection))
    val readyForPackage: Boolean get() = hasRequiredHotels && selectedJourney != null
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
            val isMadinah = hotel.city.equals("Madinah", true) || hotel.city.equals("Medina", true) || hotel.city.equals("Al Madinah", true)
            if (isMadinah) {
                if (current.madinahHotel?.id == hotel.id) current
                else current.copy(
                    madinahHotel = hotel,
                    madinahRoom = null,
                    madinahRoomCategory = null,
                    flightResults = emptyList(),
                    selectedJourneyId = null,
                    quote = null,
                    packageError = null,
                )
            } else {
                if (current.makkahHotel?.id == hotel.id) current
                else current.copy(
                    makkahHotel = hotel,
                    makkahRoom = null,
                    makkahRoomCategory = null,
                    flightResults = emptyList(),
                    selectedJourneyId = null,
                    quote = null,
                    packageError = null,
                )
            }
        }
    }

    fun selectRoom(room: HotelRoom?, forMadinah: Boolean) {
        _state.update { current ->
            if (forMadinah) current.copy(madinahRoom = room, madinahRoomCategory = if (room != null) null else current.madinahRoomCategory, quote = null)
            else current.copy(makkahRoom = room, makkahRoomCategory = if (room != null) null else current.makkahRoomCategory, quote = null)
        }
    }

    fun selectRoomCategory(category: IumrahRoomCategoryOption?, forMadinah: Boolean) {
        _state.update { current ->
            if (forMadinah) current.copy(madinahRoomCategory = category, madinahRoom = if (category != null) null else current.madinahRoom, quote = null)
            else current.copy(makkahRoomCategory = category, makkahRoom = if (category != null) null else current.makkahRoom, quote = null)
        }
    }

    fun clearFlights() {
        _state.update { it.copy(flightResults = emptyList(), selectedJourneyId = null, flightError = null, quote = null) }
    }

    suspend fun searchFlights(provider: IgnavFlightInventoryProvider) {
        val trip = _state.value.trip
        if (!trip.canContinue) {
            _state.update { it.copy(flightError = "INVALID_TRIP") }
            return
        }
        if (!_state.value.hasRequiredHotels) {
            _state.update { it.copy(flightError = "HOTEL_ROOM_REQUIRED") }
            return
        }
        _state.update { it.copy(isSearchingFlights = true, flightError = null, flightResults = emptyList(), selectedJourneyId = null, quote = null) }
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
            current.copy(selectedJourneyId = id, trip = current.trip.copy(saudiArrivalDate = arrivalDate), quote = null, packageError = null)
        }
    }

    fun setQuote(quote: PackageQuote) { _state.update { it.copy(quote = quote, packageError = null) } }
    fun setPackageError(message: String?) { _state.update { it.copy(packageError = message, quote = if (message != null) null else it.quote) } }
}
