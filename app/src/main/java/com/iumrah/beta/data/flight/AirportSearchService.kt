package com.iumrah.beta.data.flight

import com.iumrah.beta.core.network.APIClient
import com.iumrah.beta.models.flight.Airport
import com.iumrah.beta.models.flight.AirportSearchResponse

class AirportSearchService(private val api: APIClient) {
    suspend fun search(query: String, limit: Int = 10): List<Airport> {
        val value = query.trim()
        if (value.isEmpty()) return emptyList()
        val boundedLimit = limit.coerceIn(1, 12)
        val needle = value.lowercase()
        val local = AirportMapBootstrapCatalog.airports
            .asSequence()
            .filter { airport ->
                airport.iata.lowercase().contains(needle) ||
                    airport.icao.orEmpty().lowercase().contains(needle) ||
                    airport.city.lowercase().contains(needle) ||
                    airport.name.lowercase().contains(needle) ||
                    airport.country.lowercase().contains(needle)
            }
            .sortedWith(
                compareByDescending<Airport> { it.iata.equals(value, ignoreCase = true) }
                    .thenByDescending { it.city.equals(value, ignoreCase = true) }
                    .thenByDescending { it.score },
            )
            .take(boundedLimit)
            .toList()

        val remote = runCatching {
            api.get<AirportSearchResponse>(
                "/api/airports",
                query = mapOf("q" to value, "limit" to boundedLimit.toString()),
            ).airports
        }.getOrDefault(emptyList())

        return (local + remote)
            .distinctBy { it.iata.uppercase() }
            .take(boundedLimit)
    }
}
