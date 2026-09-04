package com.iumrah.beta.models.hotel

import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HotelsResponse(val hotels: List<HotelSummary>)

@Serializable
data class HotelCatalogPrice(
    val provider: String? = null,
    val nightlyUSD: Double? = null,
    val status: String,
    val fetchedAt: String? = null,
    val expiresAt: String? = null,
    val checkIn: String? = null,
    val checkOut: String? = null,
    val nights: Int? = null,
    val adults: Int? = null,
    val rooms: Int? = null,
) {
    fun isFresh(now: Instant = Instant.now()): Boolean {
        val nightly = nightlyUSD ?: return false
        val expiry = expiresAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return false
        return status.equals("fresh", ignoreCase = true) && nightly.isFinite() && nightly > 0 && expiry.isAfter(now)
    }

    // Supplier identity intentionally remains internal, same as iOS.
    val providerDisplayName: String get() = "iumrah Hotels"
    val identityKey: String get() = listOf(provider ?: "-", nightlyUSD?.toString() ?: "-", fetchedAt ?: "-", expiresAt ?: "-", status).joinToString("|")
}

@Serializable
data class HotelSummary(
    val id: String,
    val name: String,
    val city: String,
    val stars: Int? = null,
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val status: String,
    val coverImageURL: String? = null,
    val imageCount: Int = 0,
    val roomCount: Int = 0,
    val price: HotelCatalogPrice? = null,
    val updatedAt: String,
) {
    val hasFreshCatalogPrice: Boolean get() = price?.isFresh() == true
}

@Serializable
data class HotelDetailResponse(val ok: Boolean, val hotel: HotelDetail)

@Serializable
data class HotelDetail(
    val id: String,
    val name: String,
    val city: String,
    val country: String,
    val propertyType: String? = null,
    val stars: Int? = null,
    val rating: Double? = null,
    val ratingScale: Double? = null,
    val reviewCount: Int? = null,
    val address: String,
    val description: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val checkIn: String? = null,
    val checkOut: String? = null,
    val googleMapsURL: String? = null,
    val status: String,
    val amenities: List<String> = emptyList(),
    val rooms: List<HotelRoom> = emptyList(),
    val images: List<HotelImage> = emptyList(),
    val price: HotelCatalogPrice? = null,
)

@Serializable
data class HotelRoom(
    val id: String,
    val name: String,
    val maxGuests: Int? = null,
    val sizeM2: Double? = null,
    val beds: String? = null,
    val view: String? = null,
    val description: String? = null,
    val amenities: List<String> = emptyList(),
)

@Serializable
data class HotelImage(
    val id: String,
    val provider: String? = null,
    val category: String,
    val label: String? = null,
    val roomName: String? = null,
    val position: Int,
    val isCover: Boolean,
    val url: String,
)

@Serializable
enum class IumrahRoomCategory {
    @SerialName("DOUBLE") DOUBLE,
    @SerialName("TRIPLE") TRIPLE,
    @SerialName("QUADRUPLE") QUADRUPLE;

    val titleKey: String get() = when (this) {
        DOUBLE -> "room_type_double"
        TRIPLE -> "room_type_triple"
        QUADRUPLE -> "room_type_quad"
    }

    val bodyKey: String get() = when (this) {
        DOUBLE -> "room_type_double_body"
        TRIPLE -> "room_type_triple_body"
        QUADRUPLE -> "room_type_quad_body"
    }
}

@Serializable
data class IumrahRoomCategoryOption(
    val id: String,
    val hotelId: String,
    val category: IumrahRoomCategory,
    val displayName: String,
    val maxGuests: Int,
    val bedConfiguration: String,
    val position: Int,
    val source: String,
)

@Serializable
data class HotelRoomCategoriesResponse(
    val ok: Boolean,
    val hotelId: String,
    val categories: List<IumrahRoomCategoryOption>,
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
) {
    val migratedLegacyPrimaryRoom: BookingHotelSelectionSnapshot
        get() {
            if (roomCategory != null) return this
            val legacy = when (roomId) {
                "iumrah-double-room" -> Triple(IumrahRoomCategory.DOUBLE, "Double Room", Pair("1 King Bed", 2))
                "iumrah-triple-room" -> Triple(IumrahRoomCategory.TRIPLE, "Triple Room", Pair("3 Single Beds", 3))
                "iumrah-quad-room" -> Triple(IumrahRoomCategory.QUADRUPLE, "Quadruple Room", Pair("4 Single Beds", 4))
                else -> return this
            }
            return copy(
                roomId = null,
                roomName = legacy.second,
                roomBeds = legacy.third.first,
                roomSizeM2 = null,
                roomMaxGuests = legacy.third.second,
                roomCategory = legacy.first,
                roomSource = "iumrahPrimary",
            )
        }

    companion object {
        fun from(
            hotel: HotelSummary,
            room: HotelRoom? = null,
            roomCategory: IumrahRoomCategoryOption? = null,
            authoritativeRoomId: String? = null,
        ) = BookingHotelSelectionSnapshot(
            hotelId = hotel.id,
            hotelName = hotel.name,
            city = hotel.city,
            coverImageURL = hotel.coverImageURL,
            roomId = room?.id ?: authoritativeRoomId,
            roomName = room?.name ?: roomCategory?.displayName,
            roomBeds = room?.beds ?: roomCategory?.bedConfiguration,
            roomSizeM2 = room?.sizeM2,
            roomMaxGuests = room?.maxGuests ?: roomCategory?.maxGuests,
            roomCategory = roomCategory?.category,
            roomSource = if (roomCategory != null || authoritativeRoomId != null) "iumrahPrimary" else if (room != null) "hotelInventory" else null,
        )
    }
}

@Serializable
data class PackageEngineHealthResponse(
    val ok: Boolean,
    val service: String,
    val hotelsDbConfigured: Boolean,
    val bookingsDbConfigured: Boolean? = null,
    val primaryHotelConfigCount: Int? = null,
    val primaryHotelsReady: Boolean? = null,
    val primaryHotelConfigByCity: Map<String, Int>? = null,
    val roomCategoriesReady: Boolean? = null,
    val roomCategoryCount: Int? = null,
    val bookingRoomColumnsReady: Boolean? = null,
)

@Serializable
data class PrimaryHotelResolutionResponse(
    val ok: Boolean,
    val hotelId: String,
    val roomId: String? = null,
    val tier: String? = null,
    val stars: Int,
    val city: String,
    val pricingMode: String? = null,
)

@Serializable
enum class HotelPriceProviderID { booking, expedia }

@Serializable
data class HotelPricingSourceIdentity(
    val provider: HotelPriceProviderID,
    val sourceURL: String,
    val providerHotelID: String? = null,
    val canonicalURL: String? = null,
)

@Serializable
data class HotelPricingSourcesResponse(
    val ok: Boolean,
    val hotelId: String,
    val sources: List<HotelPricingSourceIdentity>,
)
