package com.iumrah.beta.domain.trip

import java.time.LocalDate

enum class PackageTier(val wireValue: String) {
    ECONOMY("economy"),
    STANDARD("standard"),
    COMFORT("comfort"),
    LUXURY("luxury"),
}

enum class JourneyScope(val wireValue: String) {
    MAKKAH_ONLY("makkahOnly"),
    MAKKAH_AND_MADINAH("makkahAndMadinah"),
}

enum class SaudiArrivalAirport(val iata: String) {
    JEDDAH("JED"),
    MADINAH("MED"),
}

enum class FlightTripType(val wireValue: String) {
    ROUND_TRIP("roundTrip"),
    ONE_WAY("oneWay"),
}

enum class FlightFareScope(val wireValue: String) {
    PER_PASSENGER("perPassenger"),
    TOTAL_PARTY("totalParty"),
    UNKNOWN("unknown"),
}

/**
 * Stage-1 port of the pricing-relevant TripDraft contract.
 * More UI/search fields are added during the model/API parity stage without
 * changing these pricing semantics.
 */
data class TripDraft(
    val origin: String = "TAS",
    val arrivalAirport: SaudiArrivalAirport = SaudiArrivalAirport.JEDDAH,
    val departureDate: LocalDate,
    val saudiArrivalDate: LocalDate? = null,
    val returnDate: LocalDate,
    val adults: Int = 2,
    val children: Int = 0,
    val infants: Int = 0,
    val rooms: Int = 1,
    val hotelStars: Int = 4,
    val packageTier: PackageTier = PackageTier.STANDARD,
    val scope: JourneyScope = JourneyScope.MAKKAH_AND_MADINAH,
    val flightTripType: FlightTripType? = null,
) {
    val travelerCount: Int get() = adults + children + infants
    val hotelStayStartDate: LocalDate get() = saudiArrivalDate ?: departureDate
    val resolvedFlightTripType: FlightTripType get() = flightTripType ?: FlightTripType.ROUND_TRIP
    val isRoundTripFlight: Boolean get() = resolvedFlightTripType == FlightTripType.ROUND_TRIP

    val originCode: String get() = origin.trim().uppercase()

    val outboundDestinationCode: String
        get() = if (scope == JourneyScope.MAKKAH_AND_MADINAH) arrivalAirport.iata else "JED"

    val returnOriginCode: String
        get() = when {
            scope != JourneyScope.MAKKAH_AND_MADINAH -> "JED"
            arrivalAirport == SaudiArrivalAirport.MADINAH -> "JED"
            else -> "MED"
        }

    val canContinue: Boolean
        get() = originCode.length == 3 &&
            adults > 0 &&
            travelerCount <= 9 &&
            rooms > 0 &&
            returnDate.isAfter(departureDate)
}
