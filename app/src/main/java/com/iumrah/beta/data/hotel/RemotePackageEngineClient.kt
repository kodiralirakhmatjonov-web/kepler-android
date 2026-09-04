package com.iumrah.beta.data.hotel

import com.iumrah.beta.core.config.AppConfig
import com.iumrah.beta.core.network.APIClient
import com.iumrah.beta.domain.trip.PackageTier
import com.iumrah.beta.models.hotel.HotelPricingSourceIdentity
import com.iumrah.beta.models.hotel.HotelPricingSourcesResponse
import com.iumrah.beta.models.hotel.HotelRoomCategoriesResponse
import com.iumrah.beta.models.hotel.IumrahRoomCategoryOption
import com.iumrah.beta.models.hotel.PackageEngineHealthResponse
import com.iumrah.beta.models.hotel.PrimaryHotelResolutionResponse

/** Hotel/booking-support boundary; flight inventory remains a separate service. */
class RemotePackageEngineClient(private val api: APIClient) {
    suspend fun health(): PackageEngineHealthResponse =
        api.get(AppConfig.PACKAGE_HEALTH_PATH, timeoutSeconds = 8)

    suspend fun roomCategories(hotelID: String): List<IumrahRoomCategoryOption> =
        api.get<HotelRoomCategoriesResponse>("/api/package/hotel/$hotelID/room-categories")
            .categories.sortedBy { it.position }

    suspend fun hotelPricingSources(hotelID: String): List<HotelPricingSourceIdentity> {
        val response = api.get<HotelPricingSourcesResponse>("/api/package/hotel/$hotelID/pricing-sources", timeoutSeconds = 10)
        return if (response.ok) response.sources else emptyList()
    }

    suspend fun primaryHotel(tier: PackageTier, stars: Int, city: String): PrimaryHotelResolutionResponse =
        api.get(
            "/api/package/primary-hotel",
            query = mapOf("tier" to tier.wireValue, "stars" to stars.toString(), "city" to city),
        )
}
