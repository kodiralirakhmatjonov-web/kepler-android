package com.iumrah.beta.models.flight

import kotlinx.serialization.Serializable

@Serializable
data class AirportSearchResponse(val airports: List<Airport>)

@Serializable
data class Airport(
    val iata: String,
    val icao: String? = null,
    val name: String,
    val city: String,
    val country: String,
    val countryCode: String,
    val region: String,
    val lat: Double,
    val lon: Double,
    val type: String,
    val score: Double,
    val aliases: List<String> = emptyList(),
) {
    val compactTitle: String get() = "${iata.uppercase()} · $city"
    val subtitle: String get() = "$name · $country"
}
