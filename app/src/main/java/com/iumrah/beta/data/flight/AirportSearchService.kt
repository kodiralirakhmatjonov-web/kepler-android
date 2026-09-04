package com.iumrah.beta.data.flight

import com.iumrah.beta.core.network.APIClient
import com.iumrah.beta.models.flight.Airport
import com.iumrah.beta.models.flight.AirportSearchResponse

class AirportSearchService(private val api: APIClient) {
    suspend fun search(query: String, limit: Int = 10): List<Airport> {
        val value = query.trim()
        if (value.isEmpty()) return emptyList()
        return api.get<AirportSearchResponse>(
            "/api/airports",
            query = mapOf("q" to value, "limit" to limit.coerceIn(1, 12).toString()),
        ).airports
    }
}
