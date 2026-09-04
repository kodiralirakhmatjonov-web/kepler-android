package com.iumrah.beta.domain.pricing

import com.iumrah.beta.domain.trip.FlightFareScope
import com.iumrah.beta.domain.trip.JourneyScope
import com.iumrah.beta.domain.trip.PackageTier
import com.iumrah.beta.domain.trip.TripDraft
import com.iumrah.beta.domain.trip.TripStayPlanner
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.ZoneOffset
import kotlin.math.ceil

/**
 * Android port of Sources/Services/LocalPackagePricingEngine.swift.
 *
 * TEST-BUILD EXCEPTION requested for the Android port:
 * package markup = 20% instead of the current iOS 50%.
 * Every other launch-pricing rule is intentionally kept aligned with Swift.
 */
object LocalPackagePricingEngine {
    private val mc = MathContext.DECIMAL128

    val packageMarkupRate: BigDecimal = BigDecimal("0.20")
    val paymentFeeRate: BigDecimal = BigDecimal("0.02")
    val publicRoundingStep: BigDecimal = BigDecimal("5")

    val visaPerTravellerUsd: BigDecimal = BigDecimal("120")
    val makkahZiyaratPerGroupUsd: BigDecimal = BigDecimal("100")
    val madinahZiyaratPerGroupUsd: BigDecimal = BigDecimal("100")
    val accompanimentWithMadinahPerGroupUsd: BigDecimal = BigDecimal("300")
    val accompanimentMakkahOnlyPerGroupUsd: BigDecimal = BigDecimal("100")
    val roadWithMadinahPerSedanUsd: BigDecimal = BigDecimal("300")
    val localWithTrainPerSedanUsd: BigDecimal = BigDecimal("200")
    val makkahOnlyPerSedanUsd: BigDecimal = BigDecimal("200")
    val haramainSarPerTraveller: BigDecimal = BigDecimal("300")
    val sarPerUsd: BigDecimal = BigDecimal("3.75")
    const val sedanCapacity: Int = 3

    // Different string on purpose: the arithmetic differs from iOS v6 only by
    // the explicitly requested temporary Android test markup.
    const val pricingVersion = "local-expedia-package-v6-android-test-20pct"

    fun calculate(
        trip: TripDraft,
        journeyFareUsd: BigDecimal,
        journeyFareScope: FlightFareScope,
        pricingOffer: FlightOffer,
        outboundOffer: FlightOffer,
        inboundOffer: FlightOffer?,
        makkahHotel: LocalHotelPriceComponent,
        madinahHotel: LocalHotelPriceComponent?,
    ): PackageQuote {
        val travelers = maxOf(1, trip.travelerCount)
        val vehicles = maxOf(1, ceil(travelers.toDouble() / sedanCapacity.toDouble()).toInt())
        val stay = TripStayPlanner.breakdown(trip)

        val flights = groupFare(journeyFareUsd, journeyFareScope, travelers)
        val makkahHotelCost = hotelCost(makkahHotel)
        val madinahHotelCost = if (trip.scope == JourneyScope.MAKKAH_AND_MADINAH) hotelCost(madinahHotel) else BigDecimal.ZERO
        val hotels = makkahHotelCost.add(madinahHotelCost, mc)
        val visa = visaPerTravellerUsd.multiply(BigDecimal.valueOf(travelers.toLong()), mc)
        val mealTravellers = maxOf(0, trip.adults + trip.children)
        val meals = mealRate(trip.packageTier)
            .multiply(BigDecimal.valueOf(maxOf(1, stay.totalDays).toLong()), mc)
            .multiply(BigDecimal.valueOf(mealTravellers.toLong()), mc)

        val includeMadinah = trip.scope == JourneyScope.MAKKAH_AND_MADINAH
        val usesTrain = includeMadinah && (trip.packageTier == PackageTier.COMFORT || trip.packageTier == PackageTier.LUXURY)
        val transfer = when {
            includeMadinah && usesTrain -> localWithTrainPerSedanUsd
            includeMadinah -> roadWithMadinahPerSedanUsd
            else -> makkahOnlyPerSedanUsd
        }.multiply(BigDecimal.valueOf(vehicles.toLong()), mc)

        val intercity = if (usesTrain) {
            haramainSarPerTraveller
                .divide(sarPerUsd, mc)
                .multiply(BigDecimal.valueOf(travelers.toLong()), mc)
        } else BigDecimal.ZERO

        val guide = if (includeMadinah) accompanimentWithMadinahPerGroupUsd else accompanimentMakkahOnlyPerGroupUsd
        val ziyarat = makkahZiyaratPerGroupUsd.add(if (includeMadinah) madinahZiyaratPerGroupUsd else BigDecimal.ZERO, mc)

        val totalCost = listOf(flights, hotels, visa, meals, transfer, intercity, guide, ziyarat)
            .fold(BigDecimal.ZERO) { total, value -> total.add(value, mc) }
        if (totalCost <= BigDecimal.ZERO) throw LocalPricingException.InvalidComponents

        val markupAmount = totalCost.multiply(packageMarkupRate, mc)
        val baseSelling = totalCost.add(markupAmount, mc)
        val calculatedSelling = baseSelling.divide(BigDecimal.ONE.subtract(paymentFeeRate, mc), mc)
        val perPerson = roundPublic(calculatedSelling.divide(BigDecimal.valueOf(travelers.toLong()), mc))
        val total = perPerson.multiply(BigDecimal.valueOf(travelers.toLong()), mc)
        val quoteId = newLocalQuoteId()
        val paymentFeeAmount = calculatedSelling.subtract(baseSelling, mc)
        val roundingDifference = total.subtract(calculatedSelling, mc)
        val estimatedProfit = total.subtract(totalCost, mc).subtract(paymentFeeAmount, mc)

        val components = buildList {
            add(
                GeneratorPricingComponent(
                    code = if (trip.isRoundTripFlight) "flight_roundtrip" else "flight_outbound",
                    label = if (trip.isRoundTripFlight) "Авиаперелёт туда-обратно" else "Авиабилет туда",
                    supplierCostUsd = flights,
                ),
            )
            add(GeneratorPricingComponent("makkah_hotel", "Отель в Мекке", makkahHotelCost))
            if (includeMadinah) add(GeneratorPricingComponent("madinah_hotel", "Отель в Медине", madinahHotelCost))
            add(GeneratorPricingComponent("visa", "Визы", visa))
            add(GeneratorPricingComponent("meals", "Питание", meals))
            add(GeneratorPricingComponent("transfers", "Трансферы", transfer))
            if (intercity > BigDecimal.ZERO) add(GeneratorPricingComponent("haramain_train", "Поезд Haramain", intercity))
            add(GeneratorPricingComponent("accompaniment", "Сопровождение", guide))
            add(GeneratorPricingComponent("ziyarat_makkah", "Зиярат в Мекке", makkahZiyaratPerGroupUsd))
            if (includeMadinah) add(GeneratorPricingComponent("ziyarat_madinah", "Зиярат в Медине", madinahZiyaratPerGroupUsd))
            add(GeneratorPricingComponent("care", "iumrah Care", BigDecimal.ZERO))
        }

        val journeyInput = fareInput(
            offer = pricingOffer,
            originalAmount = pricingOffer.fareAmount ?: journeyFareUsd,
            scope = journeyFareScope,
            normalizedGroupUsd = flights,
            travelDate = outboundOffer.departureAt.atZone(ZoneOffset.UTC).toLocalDate(),
        )

        val snapshot = GeneratorPricingSnapshot(
            quoteId = quoteId,
            pricingVersion = pricingVersion,
            currency = "USD",
            context = GeneratorPricingContext(
                tier = trip.packageTier.wireValue,
                tripType = trip.resolvedFlightTripType.wireValue,
                includeMadinah = includeMadinah,
                totalDays = stay.totalDays,
                travelers = BookingTravelers(trip.adults, trip.children, trip.infants, trip.rooms),
                roomCount = maxOf(makkahHotel.rooms, madinahHotel?.rooms ?: 0),
                vehicleCount = vehicles,
            ),
            selectedPricingInputs = GeneratorPricingInputs(
                journeyFare = journeyInput,
                outbound = null,
                inbound = null,
                makkahHotel = hotelInput(makkahHotel),
                madinahHotel = if (includeMadinah) madinahHotel?.let(::hotelInput) else null,
            ),
            components = components,
            totals = GeneratorPricingTotals(
                supplierCostUsd = totalCost,
                markupRate = packageMarkupRate,
                markupAmountUsd = markupAmount,
                subtotalAfterMarkupUsd = baseSelling,
                paymentFeeRate = paymentFeeRate,
                paymentFeeAmountUsd = paymentFeeAmount,
                calculatedSellingPriceUsd = calculatedSelling,
                publicPricePerPilgrimUsd = perPerson,
                publicTotalUsd = total,
                roundingDifferenceUsd = roundingDifference,
                estimatedProfitUsd = estimatedProfit,
            ),
        )

        // Referencing inboundOffer here is deliberate: the current pricing contract
        // prices one complete journey fare; it never sums independent one-way legs.
        @Suppress("UNUSED_VARIABLE") val parityInboundOffer = inboundOffer

        return PackageQuote(
            totalPackagePrice = total,
            pricePerPerson = perPerson,
            currency = "USD",
            isEstimated = true,
            quoteId = quoteId,
            pricingSnapshot = snapshot,
        )
    }

    private fun groupFare(amount: BigDecimal, scope: FlightFareScope, travelers: Int): BigDecimal {
        if (amount <= BigDecimal.ZERO) throw LocalPricingException.InvalidFlightFare
        return when (scope) {
            FlightFareScope.TOTAL_PARTY -> amount
            FlightFareScope.PER_PASSENGER -> amount.multiply(BigDecimal.valueOf(travelers.toLong()), mc)
            FlightFareScope.UNKNOWN -> throw LocalPricingException.InvalidFlightFare
        }
    }

    private fun hotelCost(value: LocalHotelPriceComponent?): BigDecimal =
        if (value == null || value.nightlyUsd <= BigDecimal.ZERO) BigDecimal.ZERO else value.totalStayUsd

    private fun fareInput(
        offer: FlightOffer,
        originalAmount: BigDecimal,
        scope: FlightFareScope,
        normalizedGroupUsd: BigDecimal,
        travelDate: java.time.LocalDate,
    ): GeneratorPricingFare {
        return GeneratorPricingFare(
            candidateId = offer.providerItineraryId ?: offer.sourceCandidateId ?: offer.id,
            amount = originalAmount,
            currency = offer.currency.uppercase(),
            fareScope = scope.wireValue,
            providerId = offer.sourceLabel,
            observedAt = (offer.fareObservedAt ?: java.time.Instant.now()).toString(),
            travelDate = travelDate,
            normalizedGroupUsd = normalizedGroupUsd,
        )
    }

    private fun hotelInput(value: LocalHotelPriceComponent): GeneratorPricingHotelInput =
        GeneratorPricingHotelInput(
            amountUsd = value.nightlyUsd,
            unit = "perRoomNight",
            nights = value.nights,
            hotelId = value.hotelId,
            roomId = value.roomId,
            pricingMode = value.source,
        )

    private fun mealRate(tier: PackageTier): BigDecimal = when (tier) {
        PackageTier.ECONOMY, PackageTier.STANDARD -> BigDecimal("15")
        PackageTier.COMFORT -> BigDecimal("50")
        PackageTier.LUXURY -> BigDecimal("100")
    }

    private fun roundPublic(value: BigDecimal): BigDecimal {
        val stepUnits = value.divide(publicRoundingStep, mc)
        val roundedUnits = stepUnits.setScale(0, RoundingMode.HALF_UP)
        return roundedUnits.max(BigDecimal.ONE).multiply(publicRoundingStep, mc)
    }
}

sealed class LocalPricingException(message: String) : IllegalArgumentException(message) {
    data object InvalidFlightFare : LocalPricingException("Не удалось получить текущую стоимость выбранного перелёта.")
    data object InvalidComponents : LocalPricingException("Компоненты пакета неполные. Повторите расчёт.")
    data class MissingHotelPrice(val city: String) : LocalPricingException(
        "Цена Primary Hotel в городе $city сейчас недоступна или устарела. Выберите другой доступный отель или повторите позже.",
    )
}
