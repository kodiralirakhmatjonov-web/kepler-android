package com.iumrah.beta.domain.pricing

import com.iumrah.beta.domain.trip.FlightFareScope
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class BookingTravelers(
    val adults: Int,
    val children: Int,
    val infants: Int,
    val rooms: Int,
)

data class FlightOffer(
    val id: String,
    val departureAt: Instant,
    val totalPackagePrice: BigDecimal,
    val currency: String,
    val sourceLabel: String,
    val fareAmount: BigDecimal? = null,
    val fareScope: FlightFareScope? = null,
    val fareObservedAt: Instant? = null,
    val providerItineraryId: String? = null,
    val sourceCandidateId: String? = null,
)

data class LocalHotelPriceComponent(
    val nightlyUsd: BigDecimal,
    val nights: Int,
    val rooms: Int,
    val hotelId: String,
    val roomId: String? = null,
    val source: String,
) {
    val totalStayUsd: BigDecimal
        get() = nightlyUsd
            .multiply(BigDecimal.valueOf(maxOf(1, rooms).toLong()))
            .multiply(BigDecimal.valueOf(maxOf(1, nights).toLong()))
}

data class GeneratorPricingFare(
    val candidateId: String,
    val amount: BigDecimal,
    val currency: String,
    val fareScope: String,
    val providerId: String,
    val observedAt: String,
    val travelDate: LocalDate,
    val normalizedGroupUsd: BigDecimal,
)

data class GeneratorPricingHotelInput(
    val amountUsd: BigDecimal,
    val unit: String,
    val nights: Int,
    val hotelId: String?,
    val roomId: String?,
    val pricingMode: String?,
)

data class GeneratorPricingComponent(
    val code: String,
    val label: String,
    val supplierCostUsd: BigDecimal,
)

data class GeneratorPricingContext(
    val tier: String,
    val tripType: String,
    val includeMadinah: Boolean,
    val totalDays: Int,
    val travelers: BookingTravelers,
    val roomCount: Int,
    val vehicleCount: Int,
)

data class GeneratorPricingInputs(
    val journeyFare: GeneratorPricingFare? = null,
    val outbound: GeneratorPricingFare? = null,
    val inbound: GeneratorPricingFare? = null,
    val makkahHotel: GeneratorPricingHotelInput,
    val madinahHotel: GeneratorPricingHotelInput?,
)

data class GeneratorPricingTotals(
    val supplierCostUsd: BigDecimal,
    val markupRate: BigDecimal,
    val markupAmountUsd: BigDecimal,
    val subtotalAfterMarkupUsd: BigDecimal,
    val paymentFeeRate: BigDecimal,
    val paymentFeeAmountUsd: BigDecimal,
    val calculatedSellingPriceUsd: BigDecimal,
    val publicPricePerPilgrimUsd: BigDecimal,
    val publicTotalUsd: BigDecimal,
    val roundingDifferenceUsd: BigDecimal,
    val estimatedProfitUsd: BigDecimal,
)

data class GeneratorPricingSnapshot(
    val quoteId: String,
    val pricingVersion: String,
    val currency: String,
    val context: GeneratorPricingContext,
    val selectedPricingInputs: GeneratorPricingInputs,
    val components: List<GeneratorPricingComponent>,
    val totals: GeneratorPricingTotals,
)

data class PackageQuote(
    val totalPackagePrice: BigDecimal,
    val pricePerPerson: BigDecimal,
    val currency: String,
    val isEstimated: Boolean,
    val quoteId: String? = null,
    val pricingSnapshot: GeneratorPricingSnapshot? = null,
)

internal fun newLocalQuoteId(): String = "local-${UUID.randomUUID().toString().lowercase()}"
