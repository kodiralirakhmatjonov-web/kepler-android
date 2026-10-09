package com.iumrah.beta.data.flight

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlightDataParityTest {
    private fun offer(
        departure: String = "2026-12-10T10:00:00Z",
        flight: String = "HY611",
        price: Double = 420.0,
        returnAirline: String? = "SV",
    ): FlightDiscoveryOffer = FlightDiscoveryOffer(
        id = "data-611", origin = "TAS", destination = "JED", price = price,
        departureAt = departure, returnAt = "2026-12-20T20:00:00Z",
        airlineCode = "HY", flightNumber = flight,
        returnAirlineCode = returnAirline, returnFlightNumber = "SV605",
    )

    @Test fun matchingIsoOffsetDoesNotDiscardSameFlight() {
        assertTrue(FlightDataParity.isSameFare(
            offer(), offer(departure = "2026-12-10T15:00:45+05:00", flight = "611"),
        ))
    }

    @Test fun refreshToleranceIs120SecondsInclusive() {
        assertTrue(FlightDataParity.matchesInstant("2026-12-10T10:00:00Z", "2026-12-10T15:02:00+05:00"))
        assertFalse(FlightDataParity.matchesInstant("2026-12-10T10:00:00Z", "2026-12-10T15:02:01+05:00"))
    }

    @Test fun neverSubstitutesOtherFlightEvenIfCheaper() {
        val cheapDifferent = offer(flight = "HY612", price = 1.0)
        val exact = offer(flight = "611", departure = "2026-12-10T15:00:00+05:00", price = 470.0)
        assertEquals(470.0, FlightDataParity.matchingFare(offer(), listOf(cheapDifferent, exact))?.price ?: 0.0, 0.01)
    }

    @Test fun knownReturnCarrierCannotChange() {
        assertFalse(FlightDataParity.isSameFare(offer(), offer(returnAirline = "TK")))
        assertNull(FlightDataParity.matchingFare(offer(), listOf(offer(returnAirline = "TK"))))
    }

    @Test fun airportTimeZonesAndMissingDurations() {
        assertEquals("15:00", FlightDataParity.localTime("2026-12-10T10:00:00Z", "TAS"))
        assertEquals("18:00", FlightDataParity.arrivalTime("2026-12-10T10:00:00Z", 300, "JED"))
        assertEquals("—", FlightDataParity.arrivalTime("2026-12-10T10:00:00Z", 0, "JED"))
        assertTrue(FlightDataParity.localDate("2026-12-10T10:00:00Z", "TAS", Locale.ENGLISH).contains("10"))
    }

    @Test fun absentReturnMetadataNotClassifiedAsDirectRoundTrip() {
        val offer = offer().copy(transfers = 0, returnTransfers = null)
        assertFalse(offer.isDirect)
    }
}
