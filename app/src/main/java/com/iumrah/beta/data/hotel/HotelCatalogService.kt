package com.iumrah.beta.data.hotel

import com.iumrah.beta.core.network.APIClient
import com.iumrah.beta.models.hotel.HotelDetail
import com.iumrah.beta.models.hotel.HotelDetailResponse
import com.iumrah.beta.models.hotel.HotelSummary
import com.iumrah.beta.models.hotel.HotelsResponse
import com.iumrah.beta.models.hotel.StorefrontFlightBoardResponse
import com.iumrah.beta.models.hotel.StorefrontPackagesEnvelope

class HotelCatalogService(private val api: APIClient) {
    suspend fun listHotels(city: String): List<HotelSummary> =
        api.get<HotelsResponse>("/api/catalog/hotels", query = mapOf("city" to city)).hotels

    suspend fun hotelDetail(id: String): HotelDetail =
        api.get<HotelDetailResponse>("/api/catalog/hotels/$id").hotel

    suspend fun storefrontFlightBoard(origin: String): StorefrontFlightBoardResponse =
        api.get(
            "/api/package/storefront/flights",
            query = mapOf("origin" to origin.trim().uppercase()),
            timeoutSeconds = 10,
        )

    suspend fun storefrontPackages(mode: String, origin: String, limit: Int): StorefrontPackagesEnvelope =
        api.get(
            "/api/storefront/packages",
            query = mapOf(
                "mode" to mode,
                "origins" to origin.trim().uppercase(),
                "limit" to limit.toString(),
            ),
            timeoutSeconds = 25,
        )
}
