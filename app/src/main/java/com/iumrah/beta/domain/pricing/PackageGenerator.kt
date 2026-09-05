package com.iumrah.beta.domain.pricing

import com.iumrah.beta.data.pricing.LocalFXRateService
import com.iumrah.beta.domain.journey.JourneyState
import com.iumrah.beta.domain.trip.JourneyScope
import com.iumrah.beta.domain.trip.TripStayPlanner
import java.math.BigDecimal

/**
 * Pure Android package-generation boundary. It deliberately reuses the exact
 * LocalPackagePricingEngine arithmetic instead of inventing Android prices.
 */
class PackageGenerator(
    private val fx: LocalFXRateService,
) {
    suspend fun generate(state: JourneyState): PackageQuote {
        val trip = state.trip
        val journey = state.selectedJourney ?: throw IllegalStateException("Выберите перелёт.")
        val makkahHotel = state.makkahHotel ?: throw IllegalStateException("Выберите отель в Мекке.")
        val makkahRoomId = state.makkahRoom?.id ?: state.makkahRoomCategory?.id
            ?: throw IllegalStateException("Выберите категорию номера в Мекке.")
        val stay = TripStayPlanner.breakdown(trip)
        val makkahNightly = makkahHotel.price?.nightlyUSD?.takeIf { it > 0 }
            ?: throw LocalPricingException.MissingHotelPrice("Makkah")
        val makkah = LocalHotelPriceComponent(
            nightlyUsd = BigDecimal.valueOf(makkahNightly),
            nights = maxOf(1, stay.makkahNights),
            rooms = maxOf(1, trip.rooms),
            hotelId = makkahHotel.id,
            roomId = makkahRoomId,
            source = makkahHotel.price?.provider ?: "iumrah-catalog",
        )

        val madinah = if (trip.scope == JourneyScope.MAKKAH_AND_MADINAH) {
            val hotel = state.madinahHotel ?: throw IllegalStateException("Выберите отель в Медине.")
            val roomId = state.madinahRoom?.id ?: state.madinahRoomCategory?.id
                ?: throw IllegalStateException("Выберите категорию номера в Медине.")
            val nightly = hotel.price?.nightlyUSD?.takeIf { it > 0 }
                ?: throw LocalPricingException.MissingHotelPrice("Madinah")
            LocalHotelPriceComponent(
                nightlyUsd = BigDecimal.valueOf(nightly),
                nights = maxOf(1, stay.madinahNights ?: 1),
                rooms = maxOf(1, trip.rooms),
                hotelId = hotel.id,
                roomId = roomId,
                source = hotel.price?.provider ?: "iumrah-catalog",
            )
        } else null

        // The selected journey total is the complete round-trip/open-jaw itinerary.
        // It is normalized to USD exactly once and never summed from independent legs.
        val journeyFareUsd = fx.usd(journey.totalFare, journey.currency)
        val pricingOffer = FlightOffer(
            id = journey.id,
            departureAt = journey.outbound.departureAt,
            totalPackagePrice = journeyFareUsd,
            currency = "USD",
            sourceLabel = journey.sourceName,
            fareAmount = journeyFareUsd,
            fareScope = journey.fareScope,
            fareObservedAt = journey.observedAt,
            providerItineraryId = journey.providerItineraryID,
            sourceCandidateId = journey.id,
        )
        val outbound = pricingOffer.copy(
            id = journey.outbound.id,
            departureAt = journey.outbound.departureAt,
            sourceCandidateId = journey.outbound.id,
        )
        val inbound = journey.inbound?.let {
            pricingOffer.copy(
                id = it.id,
                departureAt = it.departureAt,
                sourceCandidateId = it.id,
            )
        }

        return LocalPackagePricingEngine.calculate(
            trip = trip,
            journeyFareUsd = journeyFareUsd,
            journeyFareScope = journey.fareScope,
            pricingOffer = pricingOffer,
            outboundOffer = outbound,
            inboundOffer = inbound,
            makkahHotel = makkah,
            madinahHotel = madinah,
        )
    }
}
