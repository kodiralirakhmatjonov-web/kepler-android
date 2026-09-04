package com.iumrah.beta.domain.pricing

import com.iumrah.beta.domain.trip.FlightFareScope
import com.iumrah.beta.domain.trip.FlightTripType
import com.iumrah.beta.domain.trip.JourneyScope
import com.iumrah.beta.domain.trip.PackageTier
import com.iumrah.beta.domain.trip.TripDraft
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class LocalPackagePricingEngineTest {
    private val observed = Instant.parse("2026-09-04T12:00:00Z")

    @Test
    fun standardTwoPilgrims_roundTrip_matches20PercentGoldenPrice() {
        val trip = TripDraft(
            departureDate = LocalDate.parse("2026-10-01"),
            saudiArrivalDate = LocalDate.parse("2026-10-01"),
            returnDate = LocalDate.parse("2026-10-08"),
            adults = 2,
            packageTier = PackageTier.STANDARD,
            scope = JourneyScope.MAKKAH_AND_MADINAH,
            flightTripType = FlightTripType.ROUND_TRIP,
        )
        val flight = offer("500", "perPassenger")

        val quote = LocalPackagePricingEngine.calculate(
            trip = trip,
            journeyFareUsd = BigDecimal("500"),
            journeyFareScope = FlightFareScope.PER_PASSENGER,
            pricingOffer = flight,
            outboundOffer = flight,
            inboundOffer = flight,
            makkahHotel = LocalHotelPriceComponent(BigDecimal("100"), 5, 1, "makkah-1", "room-1", "iumrah_business"),
            madinahHotel = LocalHotelPriceComponent(BigDecimal("80"), 2, 1, "madinah-1", "room-2", "iumrah_business"),
        )

        assertMoney("3600", quote.totalPackagePrice)
        assertMoney("1800", quote.pricePerPerson)
        assertEquals("local-expedia-package-v6-android-test-20pct", quote.pricingSnapshot?.pricingVersion)
        assertMoney("0.20", quote.pricingSnapshot!!.totals.markupRate)
    }

    @Test
    fun comfortFamily_usesHaramainAndTwoVehicles() {
        val trip = TripDraft(
            departureDate = LocalDate.parse("2026-10-01"),
            saudiArrivalDate = LocalDate.parse("2026-10-01"),
            returnDate = LocalDate.parse("2026-10-09"),
            adults = 2,
            children = 1,
            infants = 1,
            rooms = 2,
            packageTier = PackageTier.COMFORT,
            scope = JourneyScope.MAKKAH_AND_MADINAH,
            flightTripType = FlightTripType.ROUND_TRIP,
        )
        val flight = offer("1800", "totalParty")

        val quote = LocalPackagePricingEngine.calculate(
            trip = trip,
            journeyFareUsd = BigDecimal("1800"),
            journeyFareScope = FlightFareScope.TOTAL_PARTY,
            pricingOffer = flight,
            outboundOffer = flight,
            inboundOffer = flight,
            makkahHotel = LocalHotelPriceComponent(BigDecimal("150"), 5, 2, "makkah-2", null, "iumrah_business"),
            madinahHotel = LocalHotelPriceComponent(BigDecimal("120"), 3, 2, "madinah-2", null, "iumrah_business"),
        )

        assertMoney("8660", quote.totalPackagePrice)
        assertMoney("2165", quote.pricePerPerson)
        assertEquals(2, quote.pricingSnapshot?.context?.vehicleCount)
        assertMoney(
            "320",
            quote.pricingSnapshot!!.components.first { it.code == "haramain_train" }.supplierCostUsd,
        )
    }

    @Test
    fun makkahOnlyOneWay_doesNotAddMadinahCosts() {
        val trip = TripDraft(
            departureDate = LocalDate.parse("2026-10-01"),
            saudiArrivalDate = LocalDate.parse("2026-10-01"),
            returnDate = LocalDate.parse("2026-10-05"),
            adults = 1,
            packageTier = PackageTier.STANDARD,
            scope = JourneyScope.MAKKAH_ONLY,
            flightTripType = FlightTripType.ONE_WAY,
        )
        val flight = offer("400", "totalParty")

        val quote = LocalPackagePricingEngine.calculate(
            trip = trip,
            journeyFareUsd = BigDecimal("400"),
            journeyFareScope = FlightFareScope.TOTAL_PARTY,
            pricingOffer = flight,
            outboundOffer = flight,
            inboundOffer = null,
            makkahHotel = LocalHotelPriceComponent(BigDecimal("90"), 4, 1, "makkah-3", null, "iumrah_business"),
            madinahHotel = null,
        )

        assertMoney("1660", quote.totalPackagePrice)
        assertMoney("1660", quote.pricePerPerson)
        assertEquals(false, quote.pricingSnapshot!!.components.any { it.code == "madinah_hotel" || it.code == "ziyarat_madinah" })
    }

    @Test(expected = LocalPricingException.InvalidFlightFare::class)
    fun unknownFareScope_isRejected() {
        val trip = TripDraft(
            departureDate = LocalDate.parse("2026-10-01"),
            returnDate = LocalDate.parse("2026-10-05"),
        )
        val flight = offer("500", "unknown")
        LocalPackagePricingEngine.calculate(
            trip = trip,
            journeyFareUsd = BigDecimal("500"),
            journeyFareScope = FlightFareScope.UNKNOWN,
            pricingOffer = flight,
            outboundOffer = flight,
            inboundOffer = null,
            makkahHotel = LocalHotelPriceComponent(BigDecimal("90"), 4, 1, "makkah", null, "iumrah_business"),
            madinahHotel = null,
        )
    }

    private fun offer(amount: String, scope: String): FlightOffer = FlightOffer(
        id = "flight-$scope-$amount",
        departureAt = Instant.parse("2026-10-01T05:00:00Z"),
        totalPackagePrice = BigDecimal(amount),
        currency = "USD",
        sourceLabel = "Ignav",
        fareAmount = BigDecimal(amount),
        fareObservedAt = observed,
        providerItineraryId = "ignav-$scope-$amount",
    )

    private fun assertMoney(expected: String, actual: BigDecimal) {
        assertEquals(0, BigDecimal(expected).compareTo(actual))
    }
}
