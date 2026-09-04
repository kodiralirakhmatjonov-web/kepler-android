package com.iumrah.beta.models.booking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class IumrahRoomCategory {
    @SerialName("DOUBLE") DOUBLE,
    @SerialName("TRIPLE") TRIPLE,
    @SerialName("QUADRUPLE") QUADRUPLE,
}

@Serializable
data class BookingGeneratorTrace(
    val quoteId: String? = null,
    val outbound: BookingGeneratorFlightSnapshot,
    val inbound: BookingGeneratorFlightSnapshot? = null,
    val makkahHotel: BookingGeneratorHotelSnapshot,
    val madinahHotel: BookingGeneratorHotelSnapshot? = null,
)

@Serializable
data class BookingGeneratorFlightSnapshot(
    val candidateId: String? = null,
    val airline: String,
    val flightNumbers: String,
    val origin: String,
    val destination: String,
    val departureAt: String,
    val arrivalAt: String,
    val source: String,
    val stops: Int? = null,
    val durationMinutes: Int? = null,
    val segments: List<BookingGeneratorFlightSegmentSnapshot>? = null,
    val connectionAirports: List<String>? = null,
)

@Serializable
data class BookingGeneratorFlightSegmentSnapshot(
    val airline: String,
    val airlineCode: String? = null,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureAt: String,
    val arrivalAt: String,
    val originTerminal: String? = null,
    val destinationTerminal: String? = null,
    val aircraft: String? = null,
    val operatingCarrier: String? = null,
    val cabin: String? = null,
)

@Serializable
data class BookingGeneratorHotelSnapshot(
    val hotelId: String,
    val hotelName: String,
    val city: String,
    val roomId: String? = null,
    val roomName: String? = null,
    val roomCategory: String? = null,
)

@Serializable
data class BookingPilgrimProfile(
    val firstName: String,
    val lastName: String,
    val telegram: String,
    val whatsapp: String,
) {
    val displayName: String get() = listOf(firstName, lastName).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")
}

@Serializable
data class BookingTravelers(val adults: Int, val children: Int, val infants: Int, val rooms: Int) {
    val totalPeople: Int get() = adults + children + infants
}

@Serializable data class BookingRoute(val originCode: String, val outboundDestination: String, val returnOrigin: String)

@Serializable
data class BookingStay(
    val totalDays: Int,
    val totalNights: Int,
    val makkahCheckIn: String,
    val makkahCheckOut: String,
    val makkahNights: Int,
    val madinahCheckIn: String? = null,
    val madinahCheckOut: String? = null,
    val madinahNights: Int? = null,
)

@Serializable
data class BookingCustomization(
    val accompaniment: Boolean,
    val guideMeetingPoint: String,
    val ziyaratMakkah: Boolean,
    val ziyaratMadinah: Boolean,
    val meals: Boolean,
    val esim: Boolean,
)

@Serializable data class BookingHotelNames(val makkah: String, val madinah: String)

@Serializable
data class BookingInputRecord(
    val startDate: String,
    val endDate: String,
    val from: String,
    val originCode: String,
    val arrivalAirportCode: String,
    val includeMadinah: Boolean,
    val travelers: BookingTravelers,
)

@Serializable
data class BookingHotelSelectionSnapshot(
    val hotelId: String,
    val hotelName: String,
    val city: String,
    val coverImageURL: String? = null,
    val roomId: String? = null,
    val roomName: String? = null,
    val roomBeds: String? = null,
    val roomSizeM2: Double? = null,
    val roomMaxGuests: Int? = null,
    val roomCategory: IumrahRoomCategory? = null,
    val roomSource: String? = null,
)

@Serializable
data class RemoteBooking(
    val id: String,
    val status: String,
    val planId: String,
    val totalUsd: Double,
    val perPilgrimUsd: Double,
    val input: BookingInputRecord,
    val route: BookingRoute,
    val stay: BookingStay,
    val hotelNames: BookingHotelNames,
    val flight: String,
    val createdAt: String,
    val updatedAt: String,
    val pilgrimProfile: BookingPilgrimProfile? = null,
    val hotelSelection: BookingHotelSelectionSnapshot? = null,
    val madinahHotelSelection: BookingHotelSelectionSnapshot? = null,
    val customization: BookingCustomization? = null,
    val includedServices: List<String>? = null,
    val generatorTrace: BookingGeneratorTrace? = null,
)

@Serializable
data class ClientESIMProfile(
    val id: String,
    val bookingID: String,
    val travelerPosition: Int? = null,
    val label: String,
    val provider: String,
    val providerEsimID: String? = null,
    val iccid: String,
    val planName: String,
    val countryCode: String,
    val totalMB: Double,
    val usedMB: Double,
    val remainingMB: Double,
    val validityDays: Int? = null,
    val status: String,
    val providerStatus: String? = null,
    val providerSmdpStatus: String? = null,
    val smdpAddress: String,
    val activationCode: String,
    val lpaString: String,
    val qrCodeURL: String? = null,
    val activatedAt: String? = null,
    val expiresAt: String? = null,
    val lastUsageSyncAt: String? = null,
    val usageSource: String,
    val createdAt: String,
    val updatedAt: String,
) {
    val totalGB: Double get() = totalMB / 1024.0
    val usedGB: Double get() = usedMB / 1024.0
    val remainingGB: Double get() = remainingMB / 1024.0
    val usageAvailable: Boolean get() = usageSource == "provider" || lastUsageSyncAt != null
    val remainingFraction: Double get() = if (usageAvailable && totalMB > 0) (remainingMB / totalMB).coerceIn(0.0, 1.0) else 0.0
    val hasActivationData: Boolean get() = lpaString.trim().isNotEmpty() || (smdpAddress.isNotEmpty() && activationCode.isNotEmpty())
}

@Serializable data class ClientBookingAssignment(val guide: BookingGuideSnapshot? = null)

@Serializable
data class BookingGuideSnapshot(
    val id: String,
    val displayName: String,
    val roleTitle: String,
    val phoneUZ: String,
    val phoneSA: String,
    val telegram: String,
    val whatsapp: String,
    val bio: String,
)

@Serializable
data class ClientTripSnapshot(
    val tripID: String,
    val bookingID: String,
    val bookingNumber: Int? = null,
    val bookingDisplayNumber: String? = null,
    val pilgrimID: String? = null,
    val status: String,
    val paymentStatus: String? = null,
    val confirmationNumber: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val updatedAt: String? = null,
)
