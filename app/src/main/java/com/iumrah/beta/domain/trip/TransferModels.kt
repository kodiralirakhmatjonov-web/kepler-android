package com.iumrah.beta.domain.trip

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TransferVehicleKind(
    val wireValue: String,
    val modelName: String,
    val passengerCapacity: Int,
    val luggageCapacity: Int,
) {
    @SerialName("malibu") MALIBU("malibu", "Chevrolet Malibu", 3, 2),
    @SerialName("carnival") CARNIVAL("carnival", "Kia Carnival", 7, 5),
    @SerialName("yukon") YUKON("yukon", "GMC Yukon", 6, 5);

    fun publicUpgradeUsd(scope: JourneyScope): Int =
        if (this == YUKON && scope == JourneyScope.MAKKAH_AND_MADINAH) 750 else 0
}

enum class HaramainFareClass(val wireValue: String, val publicSeatPriceUsd: Int) {
    ECONOMY("economy", 150),
    BUSINESS("business", 200),
}
