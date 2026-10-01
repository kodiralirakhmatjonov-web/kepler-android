package com.iumrah.beta.domain.journey

import com.iumrah.beta.data.flight.CuratedFlightRecommendationService
import com.iumrah.beta.data.flight.IgnavFlightInventoryProvider
import com.iumrah.beta.data.hotel.RemotePackageEngineClient
import com.iumrah.beta.domain.pricing.PackageQuote
import com.iumrah.beta.domain.trip.HaramainFareClass
import com.iumrah.beta.domain.trip.JourneyScope
import com.iumrah.beta.domain.trip.PackageFlightPath
import com.iumrah.beta.domain.trip.PackageMealSelection
import com.iumrah.beta.domain.trip.PackageTier
import com.iumrah.beta.domain.trip.TransferVehicleKind
import com.iumrah.beta.domain.trip.TripDraft
import com.iumrah.beta.models.flight.FlightJourneyDatePair
import com.iumrah.beta.models.flight.FlightJourneySearchRequest
import com.iumrah.beta.models.flight.CuratedPublishedFlightSelection
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
    val selectedOutboundJourneyId: String? = null,
    val packageFlightPath: PackageFlightPath = PackageFlightPath.PUBLISHED_DIRECT,
    val selectedPublishedCompleteID: String? = null,
    val selectedPublishedOutboundID: String? = null,
    val selectedPublishedReturnID: String? = null,
    val selectedTransferVehicle: TransferVehicleKind? = null,
    val haramainTrainSelected: Boolean = false,
    val haramainFareClass: HaramainFareClass = HaramainFareClass.ECONOMY,
    val haramainAdultTickets: Int = 0,
    val haramainChildTickets: Int = 0,
    val transferSelectionConfirmed: Boolean = false,
    val quote: PackageQuote? = null,
    val isSearchingFlights: Boolean = false,
    val flightError: String? = null,
    val packageError: String? = null,
) {
    val selectedJourney: LiveFlightJourneyCandidate? get() = flightResults.firstOrNull { it.id == selectedJourneyId }
    val selectedOutboundJourney: LiveFlightJourneyCandidate? get() = flightResults.firstOrNull { it.id == selectedOutboundJourneyId }
    val haramainTicketCount: Int get() = maxOf(0, haramainAdultTickets) + maxOf(0, haramainChildTickets)
    val resolvedTransferVehicle: TransferVehicleKind get() = selectedTransferVehicle ?: TransferVehicleKind.CARNIVAL
    val hasMakkahRoomSelection: Boolean get() = makkahRoom != null || makkahRoomCategory != null
    val hasMadinahRoomSelection: Boolean get() = madinahRoom != null || madinahRoomCategory != null
    val hasRequiredHotels: Boolean get() =
        makkahHotel != null && hasMakkahRoomSelection &&
            (trip.scope != JourneyScope.MAKKAH_AND_MADINAH || (madinahHotel != null && hasMadinahRoomSelection))
    val readyForPackage: Boolean get() = hasRequiredHotels && selectedJourney != null
    val hasCompletePublishedFlightSelection: Boolean get() =
        !selectedPublishedCompleteID.isNullOrBlank() ||
            (!selectedPublishedOutboundID.isNullOrBlank() && !selectedPublishedReturnID.isNullOrBlank())
    val hasSelectableHotelMeals: Boolean get() = trip.packageTier == PackageTier.COMFORT || trip.packageTier == PackageTier.LUXURY
    val hasFinalGeneratorQuote: Boolean get() =
        quote?.quoteId?.startsWith("server-") == true &&
            !quote?.quoteProof.isNullOrBlank() &&
            (quote?.totalPackagePrice?.signum() ?: 0) > 0 &&
            (quote?.pricePerPerson?.signum() ?: 0) > 0
}

class JourneyStore {
    private val _state = MutableStateFlow(JourneyState())
    val state: StateFlow<JourneyState> = _state

    fun updateTrip(value: TripDraft) {
        _state.update { current ->
            if (current.trip == value) current else JourneyState(
                trip = value,
                packageFlightPath = if (value.isWeekendUmrah) PackageFlightPath.WEEKEND else PackageFlightPath.PUBLISHED_DIRECT,
            )
        }
    }

    /** Commits the first configurator step while preserving the iOS published-flight path/selection. */
    fun commitTripBuilder(
        value: TripDraft,
        path: PackageFlightPath,
        completeID: String? = null,
        outboundID: String? = null,
        returnID: String? = null,
    ) {
        _state.update { current ->
            val routeChanged = current.trip.originCode != value.originCode ||
                current.trip.outboundDestinationCode != value.outboundDestinationCode ||
                current.trip.returnOriginCode != value.returnOriginCode ||
                current.trip.scope != value.scope
            val keepPublished = !routeChanged && current.packageFlightPath == path
            current.copy(
                trip = value,
                packageFlightPath = path,
                selectedPublishedCompleteID = completeID ?: if (keepPublished) current.selectedPublishedCompleteID else null,
                selectedPublishedOutboundID = outboundID ?: if (keepPublished) current.selectedPublishedOutboundID else null,
                selectedPublishedReturnID = returnID ?: if (keepPublished) current.selectedPublishedReturnID else null,
                flightResults = emptyList(),
                selectedJourneyId = null,
                selectedOutboundJourneyId = null,
                quote = null,
                packageError = null,
                transferSelectionConfirmed = false,
            )
        }
    }

    fun setPackageFlightPath(path: PackageFlightPath) {
        _state.update { current ->
            if (current.packageFlightPath == path) current else current.copy(
                packageFlightPath = path,
                selectedPublishedCompleteID = null,
                selectedPublishedOutboundID = null,
                selectedPublishedReturnID = null,
                flightResults = emptyList(),
                selectedJourneyId = null,
                selectedOutboundJourneyId = null,
                quote = null,
                packageError = null,
                transferSelectionConfirmed = false,
            )
        }
    }

    fun setPublishedSelection(completeID: String? = null, outboundID: String? = null, returnID: String? = null) {
        _state.update { current -> current.copy(
            selectedPublishedCompleteID = completeID,
            selectedPublishedOutboundID = outboundID,
            selectedPublishedReturnID = returnID,
            flightResults = emptyList(),
            selectedJourneyId = null,
            selectedOutboundJourneyId = null,
            quote = null,
            packageError = null,
            transferSelectionConfirmed = false,
        ) }
    }

    fun clearPublishedFlightSelection() {
        _state.update { current -> current.copy(
            selectedPublishedCompleteID = null,
            selectedPublishedOutboundID = null,
            selectedPublishedReturnID = null,
            quote = null,
            packageError = null,
        ) }
    }

    suspend fun preparePublishedDirectPackage(
        curated: CuratedFlightRecommendationService,
        packageEngine: RemotePackageEngineClient,
    ): Result<PackageQuote> {
        val snapshot = _state.value
        if (snapshot.packageFlightPath != PackageFlightPath.PUBLISHED_DIRECT || !snapshot.hasCompletePublishedFlightSelection) {
            val error = IllegalStateException("Select published outbound and return flights first.")
            _state.update { it.copy(packageError = error.message) }
            return Result.failure(error)
        }
        return runCatching {
            val resolved = curated.resolvePublishedSelection(
                snapshot.trip,
                CuratedPublishedFlightSelection(
                    completeID = snapshot.selectedPublishedCompleteID,
                    outboundID = snapshot.selectedPublishedOutboundID,
                    returnID = snapshot.selectedPublishedReturnID,
                ),
            )
            val arrivalSegment = resolved.outbound.segments?.lastOrNull()
            val zone = arrivalSegment?.destination?.timeZoneIdentifier
                ?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.of("Asia/Riyadh")
            val arrivalDate = resolved.outbound.arrivalAt.atZone(zone).toLocalDate()
            val resolvedState = _state.value.copy(
                trip = _state.value.trip.copy(saudiArrivalDate = arrivalDate),
                flightResults = listOf(resolved),
                selectedJourneyId = resolved.id,
                selectedOutboundJourneyId = resolved.id,
                packageError = null,
                quote = null,
            )
            _state.value = resolvedState
            val quote = packageEngine.packageQuote(resolvedState)
            _state.update { it.copy(quote = quote, packageError = null) }
            quote
        }.onFailure { error ->
            _state.update { it.copy(packageError = error.message ?: "PACKAGE_QUOTE_FAILED", quote = null) }
        }
    }

    fun setMealSelection(value: PackageMealSelection) {
        _state.update { current ->
            if (!current.hasSelectableHotelMeals || current.trip.mealSelection == value) current
            else current.copy(trip = current.trip.copy(mealSelection = value), quote = null, packageError = null, transferSelectionConfirmed = false)
        }
    }

    fun selectHotel(hotel: HotelSummary) {
        _state.update { current ->
            val normalizedCity = hotel.city.trim().lowercase()
            val isMadinah = normalizedCity in setOf(
                "madinah", "medina", "madina", "medinah",
                "al madinah", "al medina", "madinah al munawwarah", "al madinah al munawwarah",
            ) || normalizedCity.contains("madinah") || normalizedCity.contains("medina")
            if (isMadinah) {
                if (current.madinahHotel?.id == hotel.id) current
                else current.copy(
                    madinahHotel = hotel,
                    madinahRoom = null,
                    madinahRoomCategory = null,
                    flightResults = emptyList(),
                    selectedJourneyId = null,
                    selectedOutboundJourneyId = null,
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
                    selectedOutboundJourneyId = null,
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
        _state.update { it.copy(flightResults = emptyList(), selectedJourneyId = null, selectedOutboundJourneyId = null, flightError = null, quote = null, transferSelectionConfirmed = false) }
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
        _state.update { it.copy(isSearchingFlights = true, flightError = null, flightResults = emptyList(), selectedJourneyId = null, selectedOutboundJourneyId = null, quote = null, transferSelectionConfirmed = false) }
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
            current.copy(selectedJourneyId = id, selectedOutboundJourneyId = id, trip = current.trip.copy(saudiArrivalDate = arrivalDate), quote = null, packageError = null, transferSelectionConfirmed = false)
        }
    }


    fun selectOutboundJourney(id: String) {
        _state.update { current ->
            val journey = current.flightResults.firstOrNull { it.id == id } ?: return@update current
            val arrivalSegment = journey.outbound.segments?.lastOrNull()
            val zone = arrivalSegment?.destination?.timeZoneIdentifier?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.of("Asia/Riyadh")
            val arrivalDate = journey.outbound.arrivalAt.atZone(zone).toLocalDate()
            current.copy(
                selectedOutboundJourneyId = id,
                selectedJourneyId = null,
                trip = current.trip.copy(saudiArrivalDate = arrivalDate),
                quote = null,
                packageError = null,
                transferSelectionConfirmed = false,
            )
        }
    }

    fun selectReturnJourney(id: String) = selectJourney(id)

    fun chooseTransferVehicle(vehicle: TransferVehicleKind) {
        _state.update { current ->
            if (current.selectedTransferVehicle == vehicle) current
            else current.copy(selectedTransferVehicle = vehicle, transferSelectionConfirmed = false, quote = null, packageError = null)
        }
    }

    fun setHaramainTrainSelected(selected: Boolean) {
        _state.update { current ->
            val enabled = current.trip.scope == JourneyScope.MAKKAH_AND_MADINAH && selected
            val adults = if (enabled && current.haramainAdultTickets == 0 && current.haramainChildTickets == 0) maxOf(1, current.trip.adults) else current.haramainAdultTickets
            val children = if (enabled && current.haramainAdultTickets == 0 && current.haramainChildTickets == 0) maxOf(0, current.trip.children) else current.haramainChildTickets
            current.copy(haramainTrainSelected = enabled, haramainAdultTickets = adults, haramainChildTickets = children, transferSelectionConfirmed = false, quote = null, packageError = null)
        }
    }

    fun ensureHaramainTicketDefaults() {
        _state.update { current ->
            if (current.trip.scope != JourneyScope.MAKKAH_AND_MADINAH) current
            else {
                val needsDefaults = current.haramainAdultTickets == 0 && current.haramainChildTickets == 0
                if (!needsDefaults) current
                else current.copy(
                    haramainAdultTickets = maxOf(1, current.trip.adults),
                    haramainChildTickets = maxOf(0, current.trip.children),
                )
            }
        }
    }

    fun setHaramainFareClass(value: HaramainFareClass) {
        _state.update { current ->
            if (current.haramainFareClass == value) current
            else current.copy(haramainFareClass = value, transferSelectionConfirmed = false, quote = null, packageError = null)
        }
    }

    fun setHaramainAdultTickets(value: Int) {
        _state.update { current -> current.copy(haramainAdultTickets = value.coerceIn(1, maxOf(1, current.trip.adults)), transferSelectionConfirmed = false, quote = null) }
    }

    fun setHaramainChildTickets(value: Int) {
        _state.update { current -> current.copy(haramainChildTickets = value.coerceIn(0, maxOf(0, current.trip.children)), transferSelectionConfirmed = false, quote = null) }
    }

    fun confirmTransferSelection() {
        _state.update { current -> current.copy(selectedTransferVehicle = current.selectedTransferVehicle ?: TransferVehicleKind.CARNIVAL, transferSelectionConfirmed = true) }
    }

    fun resetTransferSelection() {
        _state.update { it.copy(selectedTransferVehicle = null, haramainTrainSelected = false, haramainFareClass = HaramainFareClass.ECONOMY, haramainAdultTickets = 0, haramainChildTickets = 0, transferSelectionConfirmed = false, quote = null) }
    }

    fun setQuote(quote: PackageQuote) { _state.update { it.copy(quote = quote, packageError = null) } }

    /** Applies a server-owned storefront package without recalculating its public price locally. */
    fun applyStorefrontPackage(
        trip: TripDraft,
        makkahHotel: HotelSummary,
        madinahHotel: HotelSummary?,
        flight: LiveFlightJourneyCandidate,
        quote: PackageQuote,
    ) {
        _state.value = JourneyState(
            trip = trip,
            makkahHotel = makkahHotel,
            madinahHotel = madinahHotel,
            flightResults = listOf(flight),
            selectedJourneyId = flight.id,
            selectedOutboundJourneyId = flight.id,
            quote = quote,
            isSearchingFlights = false,
            flightError = null,
            packageError = null,
        )
    }

    fun setPackageError(message: String?) { _state.update { it.copy(packageError = message, quote = if (message != null) null else it.quote) } }
}
