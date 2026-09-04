package com.iumrah.beta.domain.trip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TripRoutingParityTest {
    private val departure = LocalDate.of(2026, 10, 5)
    private val returnDate = LocalDate.of(2026, 10, 12)

    @Test
    fun `makkah and madinah from Jeddah is open jaw TAS JED and MED TAS`() {
        val trip = TripDraft(
            origin = "TAS",
            arrivalAirport = SaudiArrivalAirport.JEDDAH,
            departureDate = departure,
            returnDate = returnDate,
            scope = JourneyScope.MAKKAH_AND_MADINAH,
            adults = 2,
            rooms = 1,
        )

        assertEquals("TAS", trip.originCode)
        assertEquals("JED", trip.outboundDestinationCode)
        assertEquals("MED", trip.returnOriginCode)
        assertTrue(trip.canContinue)
    }

    @Test
    fun `madinah arrival reverses open jaw`() {
        val trip = TripDraft(
            origin = "TAS",
            arrivalAirport = SaudiArrivalAirport.MADINAH,
            departureDate = departure,
            returnDate = returnDate,
            scope = JourneyScope.MAKKAH_AND_MADINAH,
            adults = 2,
            rooms = 1,
        )

        assertEquals("MED", trip.outboundDestinationCode)
        assertEquals("JED", trip.returnOriginCode)
        assertTrue(trip.canContinue)
    }

    @Test
    fun `makkah only remains Jeddah return routing`() {
        val trip = TripDraft(
            origin = "TAS",
            arrivalAirport = SaudiArrivalAirport.JEDDAH,
            departureDate = departure,
            returnDate = returnDate,
            scope = JourneyScope.MAKKAH_ONLY,
            adults = 1,
            rooms = 1,
        )

        assertEquals("JED", trip.outboundDestinationCode)
        assertEquals("JED", trip.returnOriginCode)
        assertTrue(trip.canContinue)
    }

    @Test
    fun `weekend mode forces three day Makkah only Jeddah trip`() {
        val source = TripDraft(
            origin = "TAS",
            arrivalAirport = SaudiArrivalAirport.MADINAH,
            departureDate = LocalDate.of(2026, 10, 8),
            returnDate = LocalDate.of(2026, 10, 20),
            scope = JourneyScope.MAKKAH_AND_MADINAH,
            adults = 1,
            rooms = 1,
        )
        val trip = source.withFlexibility(DateFlexibility.WEEKEND, today = LocalDate.of(2026, 10, 1))

        assertEquals(DateFlexibility.WEEKEND, trip.flexibility)
        assertEquals(JourneyScope.MAKKAH_ONLY, trip.scope)
        assertEquals(SaudiArrivalAirport.JEDDAH, trip.arrivalAirport)
        assertEquals("JED", trip.outboundDestinationCode)
        assertEquals("JED", trip.returnOriginCode)
        assertEquals(3L, java.time.temporal.ChronoUnit.DAYS.between(trip.departureDate, trip.returnDate))
    }

    @Test
    fun `invalid dates cannot continue`() {
        val trip = TripDraft(
            origin = "TAS",
            arrivalAirport = SaudiArrivalAirport.JEDDAH,
            departureDate = departure,
            returnDate = departure,
            scope = JourneyScope.MAKKAH_ONLY,
            adults = 1,
            rooms = 1,
        )
        assertFalse(trip.canContinue)
    }
}
