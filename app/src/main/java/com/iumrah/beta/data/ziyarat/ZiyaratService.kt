package com.iumrah.beta.data.ziyarat

import com.iumrah.beta.core.network.APIClient
import com.iumrah.beta.models.ziyarat.ZiyaratCatalogResponse
import com.iumrah.beta.models.ziyarat.ZiyaratRoute
import com.iumrah.beta.models.ziyarat.ZiyaratSeedData

class ZiyaratService(private val api: APIClient = APIClient()) {
    private val cache = mutableMapOf<String, ZiyaratRoute>()

    suspend fun route(city: String = "Madinah"): ZiyaratRoute {
        val key = city.trim().lowercase()
        return runCatching {
            api.get<ZiyaratCatalogResponse>("/api/catalog/ziyarats", query = mapOf("city" to city)).route
                ?.takeIf { it.places.isNotEmpty() }
                ?.also { cache[key] = it }
        }.getOrNull() ?: cache[key] ?: ZiyaratSeedData.fallback(city)
    }
}
