package com.iumrah.beta.data.hotel

import com.iumrah.beta.core.network.APIClient
import com.iumrah.beta.models.hotel.HotelDetail
import com.iumrah.beta.models.hotel.HotelDetailResponse
import com.iumrah.beta.models.hotel.HotelSummary
import com.iumrah.beta.models.hotel.HotelsResponse

class HotelCatalogService(private val api: APIClient) {
    suspend fun listHotels(city: String): List<HotelSummary> =
        api.get<HotelsResponse>("/api/catalog/hotels", query = mapOf("city" to city)).hotels

    suspend fun hotelDetail(id: String): HotelDetail =
        api.get<HotelDetailResponse>("/api/catalog/hotels/$id").hotel
}
