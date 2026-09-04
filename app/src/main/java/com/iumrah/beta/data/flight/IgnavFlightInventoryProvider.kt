package com.iumrah.beta.data.flight

import com.iumrah.beta.core.network.APIClient
import com.iumrah.beta.core.network.APIException
import com.iumrah.beta.core.serialization.BigDecimalJsonSerializer
import com.iumrah.beta.domain.trip.FlightFareScope
import com.iumrah.beta.models.flight.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class IgnavFlightProviderException(message: String) : Exception(message) {
    data object InvalidRequest : IgnavFlightProviderException("Flight search request is invalid")
    data object NotConfigured : IgnavFlightProviderException("Flight provider is not configured")
    data object ServerUnavailable : IgnavFlightProviderException("Flight search is temporarily unavailable")
    data object SearchFailed : IgnavFlightProviderException("Flight search did not return verified itineraries")
}

class IgnavFlightInventoryProvider(private val api: APIClient) {
    val sourceName: String = "Ignav"
    private val concurrency = Semaphore(3)

    suspend fun searchJourney(
        request: FlightJourneySearchRequest,
        datePairs: List<FlightJourneyDatePair>,
        onUpdate: suspend (List<LiveFlightJourneyCandidate>) -> Unit = {},
    ): List<LiveFlightJourneyCandidate> = coroutineScope {
        if (datePairs.isEmpty()) return@coroutineScope emptyList()
        val deferred = datePairs.map { pair ->
            async {
                concurrency.withPermit { runCatching { fetchPair(request, pair) } }
            }
        }
        val merged = mutableListOf<LiveFlightJourneyCandidate>()
        var firstError: Throwable? = null
        deferred.forEach { task ->
            val result = task.await()
            result.onSuccess { values ->
                mergeInto(merged, values)
                onUpdate(merged.sortedBy { it.totalFare })
            }.onFailure { if (firstError == null) firstError = it }
        }
        if (merged.isEmpty() && firstError != null) throw firstError as Throwable
        merged.sortedBy { it.totalFare }
    }

    private suspend fun fetchPair(request: FlightJourneySearchRequest, pair: FlightJourneyDatePair): List<LiveFlightJourneyCandidate> {
        val body = ProxySearchBody.from(request, pair)
        val response: ProxySearchResponse = try {
            api.post("/api/package/flights/search", body, timeoutSeconds = 32)
        } catch (error: APIException.Server) {
            if (error.serverMessage == "FLIGHT_PROVIDER_NOT_CONFIGURED") throw IgnavFlightProviderException.NotConfigured
            throw error
        } catch (error: APIException.Status) {
            if (error.code in 500..599) throw IgnavFlightProviderException.ServerUnavailable
            throw error
        }
        if (!response.ok || response.source != "ignav") throw IgnavFlightProviderException.SearchFailed
        val observedAt = runCatching { Instant.parse(response.observedAt) }.getOrElse { throw IgnavFlightProviderException.SearchFailed }
        return response.itineraries.mapNotNull { itinerary -> normalize(itinerary, request, pair, observedAt) }
    }

    private fun normalize(itinerary: ProxyItinerary, request: FlightJourneySearchRequest, pair: FlightJourneyDatePair, observedAt: Instant): LiveFlightJourneyCandidate? {
        val expectsReturn = request.inboundOrigin != null && request.inboundDestination != null && pair.inbound != null
        if (itinerary.price.amount <= BigDecimal.ZERO || !itinerary.price.currency.matches(Regex("^[A-Z]{3}$")) || !itinerary.price.status.equals("verified", true)) return null
        if (itinerary.fareScope != "total_party" || itinerary.legs.size != if (expectsReturn) 2 else 1) return null

        val outbound = legCandidate(
            itinerary = itinerary,
            leg = itinerary.legs[0],
            direction = FlightDirection.outbound,
            expectedOrigin = request.outboundOrigin,
            expectedDestination = request.outboundDestination,
            expectedDate = pair.outbound,
            observedAt = observedAt,
        ) ?: return null

        val inbound = if (expectsReturn) {
            legCandidate(
                itinerary = itinerary,
                leg = itinerary.legs[1],
                direction = FlightDirection.inbound,
                expectedOrigin = request.inboundOrigin!!,
                expectedDestination = request.inboundDestination!!,
                expectedDate = pair.inbound!!,
                observedAt = observedAt,
            ) ?: return null
        } else null

        val value = LiveFlightJourneyCandidate(
            id = "ignav:${itinerary.ignavId}",
            sourceID = "ignav",
            sourceName = "Ignav",
            totalFare = itinerary.price.amount,
            currency = itinerary.price.currency.uppercase(),
            fareScope = FlightFareScope.TOTAL_PARTY,
            observedAt = observedAt,
            providerItineraryID = itinerary.ignavId,
            outbound = outbound,
            inbound = inbound,
            baggage = itinerary.bags?.let { FlightBaggageAllowance(it.carryOn, it.checked) },
            requiresSelfTransfer = itinerary.requiresSelfTransfer,
        )
        return value.takeIf { it.isDisplayable(observedAt.plusSeconds(1)) }
    }

    private fun legCandidate(
        itinerary: ProxyItinerary,
        leg: ProxyLeg,
        direction: FlightDirection,
        expectedOrigin: String,
        expectedDestination: String,
        expectedDate: LocalDate,
        observedAt: Instant,
    ): LiveFlightCandidate? {
        val segments = leg.segments.mapIndexed { index, value ->
            val departure = runCatching { Instant.parse(value.departureTimeUTC) }.getOrNull() ?: return null
            val arrival = runCatching { Instant.parse(value.arrivalTimeUTC) }.getOrNull() ?: return null
            if (!departure.isBefore(arrival) || value.durationMinutes <= 0) return null
            FlightSegment(
                id = "ignav:${itinerary.ignavId}:${direction.name}:$index:${value.departureAirport}:${value.arrivalAirport}",
                airline = value.operatingCarrierName ?: leg.airline,
                airlineCode = value.marketingCarrierCode.takeIf { it.matches(Regex("^[A-Z0-9]{2}$")) },
                flightNumber = listOf(value.marketingCarrierCode, value.flightNumber).filter { it.isNotBlank() }.joinToString(" "),
                origin = FlightAirportSnapshot(value.departureAirport.uppercase(), timeZoneIdentifier = value.departureTimezone),
                destination = FlightAirportSnapshot(value.arrivalAirport.uppercase(), timeZoneIdentifier = value.arrivalTimezone),
                departureAt = departure,
                arrivalAt = arrival,
                durationMinutes = value.durationMinutes,
                aircraft = value.aircraft,
                operatingCarrier = value.operatingCarrierName,
                cabin = itinerary.cabinClass,
            )
        }
        if (segments.isEmpty() || segments.size != leg.segments.size) return null
        val first = segments.first(); val last = segments.last()
        if (first.origin.code != expectedOrigin.uppercase() || last.destination.code != expectedDestination.uppercase()) return null
        if (leg.stops != segments.size - 1 || leg.durationMinutes <= 0) return null
        if (localDate(first.departureAt, first.origin.timeZoneIdentifier) != expectedDate) return null
        for (index in 1 until segments.size) {
            val prev = segments[index - 1]; val next = segments[index]
            if (prev.destination.code != next.origin.code || prev.arrivalAt.isAfter(next.departureAt)) return null
        }
        val connections = if (segments.size > 1) segments.dropLast(1).map { it.destination } else null
        val candidate = LiveFlightCandidate(
            id = "ignav:${itinerary.ignavId}:${direction.name}",
            sourceID = "ignav",
            sourceName = "Ignav",
            direction = direction,
            airline = leg.airline,
            flightNumber = leg.flightNumber,
            origin = leg.origin.uppercase(),
            destination = leg.destination.uppercase(),
            departureAt = first.departureAt,
            arrivalAt = last.arrivalAt,
            stops = leg.stops,
            durationMinutes = leg.durationMinutes,
            // Critical parity rule: this is the COMPLETE journey fare and is charged once.
            observedFare = itinerary.price.amount,
            observedCurrency = itinerary.price.currency.uppercase(),
            fareScope = FlightFareScope.TOTAL_PARTY,
            observedAt = observedAt,
            rawFingerprint = itinerary.ignavId,
            airlineCode = leg.airlineCode,
            segments = segments,
            connectionAirports = connections,
            providerItineraryID = itinerary.ignavId,
            cabinClass = itinerary.cabinClass,
            baggage = itinerary.bags?.let { FlightBaggageAllowance(it.carryOn, it.checked) },
            requiresSelfTransfer = itinerary.requiresSelfTransfer,
        )
        return candidate.takeIf { it.isDisplayable(observedAt.plusSeconds(1)) }
    }

    private fun localDate(instant: Instant, zoneId: String?): LocalDate {
        val zone = zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        return instant.atZone(zone).toLocalDate()
    }

    private fun mergeInto(target: MutableList<LiveFlightJourneyCandidate>, values: List<LiveFlightJourneyCandidate>) {
        val keys = target.mapTo(mutableSetOf()) { resultKey(it) }
        values.forEach { if (keys.add(resultKey(it))) target += it }
    }

    private fun resultKey(journey: LiveFlightJourneyCandidate): String = listOf(
        journey.providerItineraryID,
        journey.totalFare.toPlainString(),
        journey.outbound.departureAt.epochSecond.toString(),
        journey.outbound.arrivalAt.epochSecond.toString(),
        journey.baggage?.carryOn?.toString() ?: "-",
        journey.baggage?.checked?.toString() ?: "-",
    ).joinToString("|")
}

@Serializable
private data class ProxySearchBody(
    val legs: List<ProxySearchLeg>,
    val adults: Int,
    val children: Int,
    @SerialName("infants_in_seat") val infantsInSeat: Int,
    @SerialName("infants_on_lap") val infantsOnLap: Int,
    @SerialName("cabin_class") val cabinClass: String,
    @SerialName("min_carry_on_bags") val minCarryOnBags: Int? = null,
    @SerialName("min_checked_bags") val minCheckedBags: Int? = null,
    @SerialName("max_price") val maxPrice: Int? = null,
    @SerialName("airlines_include") val airlinesInclude: List<String>? = null,
    @SerialName("airlines_exclude") val airlinesExclude: List<String>? = null,
    @SerialName("allow_self_transfer") val allowSelfTransfer: Boolean = true,
) {
    companion object {
        fun from(request: FlightJourneySearchRequest, pair: FlightJourneyDatePair): ProxySearchBody {
            val legs = buildList {
                add(ProxySearchLeg(request.outboundOrigin, request.outboundDestination, pair.outbound.toString()))
                if (request.inboundOrigin != null && request.inboundDestination != null && pair.inbound != null) {
                    add(ProxySearchLeg(request.inboundOrigin, request.inboundDestination, pair.inbound.toString()))
                }
            }
            val lap = request.filters.infantSeating == FlightInfantSeating.LAP
            return ProxySearchBody(
                legs = legs,
                adults = request.adults,
                children = request.children,
                infantsInSeat = if (lap) maxOf(0, request.infants - request.adults) else request.infants,
                infantsOnLap = if (lap) minOf(request.infants, request.adults) else 0,
                cabinClass = request.filters.cabinClass.wireValue,
                // Deliberately no provider-side stops/airline/price filtering: exact iOS policy.
                allowSelfTransfer = true,
            )
        }
    }
}

@Serializable private data class ProxySearchLeg(val origin: String, val destination: String, @SerialName("departure_date") val departureDate: String)
@Serializable private data class ProxySearchResponse(val ok: Boolean, val source: String, @SerialName("observed_at") val observedAt: String, val itineraries: List<ProxyItinerary>)
@Serializable private data class ProxyPrice(@Serializable(with = BigDecimalJsonSerializer::class) val amount: BigDecimal, val currency: String, val status: String)
@Serializable private data class ProxyBags(@SerialName("carry_on") val carryOn: Int? = null, val checked: Int? = null)
@Serializable private data class ProxySegment(
    @SerialName("marketing_carrier_code") val marketingCarrierCode: String,
    @SerialName("flight_number") val flightNumber: String,
    @SerialName("operating_carrier_name") val operatingCarrierName: String? = null,
    @SerialName("departure_airport") val departureAirport: String,
    @SerialName("departure_time_local") val departureTimeLocal: String,
    @SerialName("departure_timezone") val departureTimezone: String? = null,
    @SerialName("departure_time_utc") val departureTimeUTC: String,
    @SerialName("arrival_airport") val arrivalAirport: String,
    @SerialName("arrival_time_local") val arrivalTimeLocal: String,
    @SerialName("arrival_timezone") val arrivalTimezone: String? = null,
    @SerialName("arrival_time_utc") val arrivalTimeUTC: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    val aircraft: String? = null,
)
@Serializable private data class ProxyLeg(
    val airline: String,
    @SerialName("flight_number") val flightNumber: String,
    @SerialName("airline_code") val airlineCode: String,
    val origin: String,
    val destination: String,
    @SerialName("departure_at") val departureAt: String,
    @SerialName("arrival_at") val arrivalAt: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    val stops: Int,
    @SerialName("cabin_class") val cabinClass: String? = null,
    val segments: List<ProxySegment>,
)
@Serializable private data class ProxyItinerary(
    val price: ProxyPrice,
    val legs: List<ProxyLeg>,
    @SerialName("cabin_class") val cabinClass: String? = null,
    val bags: ProxyBags? = null,
    @SerialName("requires_self_transfer") val requiresSelfTransfer: Boolean? = null,
    @SerialName("fare_scope") val fareScope: String,
    @SerialName("ignav_id") val ignavId: String,
)
