package com.iumrah.beta.domain.trip

enum class TransferVehicleKind(
    val wireValue: String,
    val modelName: String,
    val passengerCapacity: Int,
    val luggageCapacity: Int,
) {
    MALIBU("malibu", "Chevrolet Malibu", 3, 2),
    CARNIVAL("carnival", "Kia Carnival", 7, 5),
    YUKON("yukon", "GMC Yukon", 6, 5);

    fun publicUpgradeUsd(scope: JourneyScope): Int =
        if (this == YUKON && scope == JourneyScope.MAKKAH_AND_MADINAH) 750 else 0
}

enum class HaramainFareClass(val wireValue: String, val publicSeatPriceUsd: Int) {
    ECONOMY("economy", 150),
    BUSINESS("business", 200),
}
