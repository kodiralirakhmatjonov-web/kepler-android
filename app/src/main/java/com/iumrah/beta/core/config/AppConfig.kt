package com.iumrah.beta.core.config

/**
 * Android reads the same public iumrah backend as the SwiftUI client and
 * iumrah Business. There is intentionally no Android-specific database.
 */
object AppConfig {
    const val APP_NAME = "iumrah Beta"
    const val API_BASE_URL = "https://iumrah.app"

    const val PACKAGE_HEALTH_PATH = "/api/package/health"
    const val PACKAGE_BOOKING_PATH = "/api/bookings"

    const val FLIGHT_INVENTORY_CONFIGURED = true
    const val USES_SERVER_PRIMARY_HOTEL_RESOLVER = true
    const val USES_LOCAL_PRIMARY_HOTEL_FALLBACK = true

    const val HOTEL_PRICE_PROVIDER_TIMEOUT_SECONDS = 17L
    const val HOTEL_PRICE_SEARCH_HARD_TIMEOUT_SECONDS = 46L

    fun absoluteUrl(rawValue: String?): String? {
        val value = rawValue?.trim().orEmpty()
        if (value.isEmpty()) return null
        if (value.startsWith("https://") || value.startsWith("http://")) return value
        return API_BASE_URL.trimEnd('/') + "/" + value.trimStart('/')
    }
}
