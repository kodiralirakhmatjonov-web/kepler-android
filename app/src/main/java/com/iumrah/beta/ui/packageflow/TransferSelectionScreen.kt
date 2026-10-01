package com.iumrah.beta.ui.packageflow

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iumrah.beta.R
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.domain.journey.JourneyStore
import com.iumrah.beta.domain.pricing.PackageGenerator
import com.iumrah.beta.domain.trip.HaramainFareClass
import com.iumrah.beta.domain.trip.JourneyScope
import com.iumrah.beta.domain.trip.TransferVehicleKind
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.cupertino.CupertinoSymbol
import com.iumrah.beta.ui.cupertino.Icon
import com.iumrah.beta.ui.generator.GeneratorGeometry
import com.iumrah.beta.ui.generator.GeneratorHeader
import com.iumrah.beta.ui.generator.GeneratorPrimaryButton
import com.iumrah.beta.ui.generator.GeneratorStage
import com.iumrah.beta.ui.generator.generatorCardColor
import com.iumrah.beta.ui.generator.generatorPageColor
import com.iumrah.beta.ui.generator.generatorRaisedColor
import java.math.RoundingMode
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class TransferPhase { SEARCHING, MATCHED }

/** Pixel-oriented Android counterpart of iOS TransferSelectionView. */
@Composable
fun TransferSelectionScreen(
    language: AppLanguage,
    journey: JourneyStore,
    generator: PackageGenerator,
    chrome: AppChromeStore,
) {
    val state by journey.state.collectAsState()
    var phase by remember { mutableStateOf(if (state.transferSelectionConfirmed) TransferPhase.MATCHED else TransferPhase.SEARCHING) }
    var second by remember { mutableIntStateOf(0) }
    val duration = remember { Random.nextInt(20, 41) }

    LaunchedEffect(Unit) {
        journey.ensureHaramainTicketDefaults()
        journey.setHaramainFareClass(HaramainFareClass.ECONOMY)
        if (journey.state.value.selectedTransferVehicle == null) {
            journey.chooseTransferVehicle(TransferVehicleKind.CARNIVAL)
        }

        // iOS refreshes the server-owned package price while transfer discovery runs.
        runCatching { generator.generate(journey.state.value) }
            .onSuccess(journey::setQuote)

        if (journey.state.value.transferSelectionConfirmed) {
            phase = TransferPhase.MATCHED
            return@LaunchedEffect
        }
        while (second < duration) {
            delay(1000)
            second += 1
        }
        journey.chooseTransferVehicle(TransferVehicleKind.CARNIVAL)
        phase = TransferPhase.MATCHED
    }

    AnimatedContent(phase, label = "transfer-phase") { value ->
        if (value == TransferPhase.SEARCHING) {
            TransferSearchScreen(
                language = language,
                chrome = chrome,
                second = second,
                duration = duration,
                travelers = state.trip.travelerCount,
                includesMadinah = state.trip.scope == JourneyScope.MAKKAH_AND_MADINAH,
                currentPrice = state.quote?.let(::quoteTitle),
            )
        } else {
            TransferMatchedScreen(language, journey, generator, chrome)
        }
    }
}

@Composable
private fun TransferSearchScreen(
    language: AppLanguage,
    chrome: AppChromeStore,
    second: Int,
    duration: Int,
    travelers: Int,
    includesMadinah: Boolean,
    currentPrice: String?,
) {
    val pulse = rememberInfiniteTransition(label = "transfer-pulse")
    val phase by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Restart), label = "pulse")
    val page = generatorPageColor()
    val fg = if (page == Color.White) Color.Black else Color.White

    Box(Modifier.fillMaxSize().background(page)) {
        TransferMapCanvas(phase, page == Color.White)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(page, page.copy(alpha = .96f), page.copy(alpha = .30f), Color.Transparent, page.copy(alpha = .18f)),
                ),
            ),
        )
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = GeneratorGeometry.pagePadding, vertical = 10.dp)) {
            GeneratorHeader(GeneratorStage.TRANSFER, language, chrome, currentPriceText = currentPrice)
            Spacer(Modifier.weight(1f))
            TransferSearchBottomSheet(language, second, duration, travelers, includesMadinah, fg)
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun TransferSearchBottomSheet(
    language: AppLanguage,
    second: Int,
    duration: Int,
    travelers: Int,
    includesMadinah: Boolean,
    fg: Color,
) {
    val card = generatorCardColor()
    val raised = generatorRaisedColor()
    val nearby = minOf(11, 2 + second / 3)
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(card.copy(alpha = .96f))
            .border(.7.dp, fg.copy(alpha = .07f), RoundedCornerShape(30.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF007AFF).copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                Icon(CupertinoSymbol.Car, null, Modifier.size(20.dp), tint = Color(0xFF007AFF))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(transferTr(language, "Ищем свободный трансфер", "Finding an available transfer", "Bo‘sh transfer qidirilmoqda", "Бўш трансфер қидирилмоқда"), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = fg)
                Text(searchStatus(language, second), fontSize = 13.sp, color = fg.copy(alpha = .55f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${second}s", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg)
                Text(transferTr(language, "идёт поиск", "searching", "qidiruv", "қидирув"), fontSize = 10.sp, color = fg.copy(alpha = .48f))
            }
        }

        TransferSearchActivityBar(second, fg)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchChip(CupertinoSymbol.Location, if (includesMadinah) "Makkah ↔ Madinah" else "Makkah", raised, fg)
            SearchChip(CupertinoSymbol.Persons, travelers.toString(), raised, fg)
            SearchChip(CupertinoSymbol.Car, transferTr(language, "$nearby рядом", "$nearby nearby", "$nearby yaqin", "$nearby яқин"), raised, fg)
        }

        Text(
            transferTr(
                language,
                "Проверяем автомобили в Мекке, маршрут поездки, количество гостей и багаж. После проверки iumrah предложит подходящий свободный автомобиль.",
                "Checking vehicles around Makkah, your trip route, party size and luggage. iumrah will then match a suitable available vehicle.",
                "Makkadagi avtomobillar, safar yo‘nalishi, mehmonlar va bagaj tekshirilmoqda. So‘ng iumrah mos bo‘sh avtomobilni tanlaydi.",
                "Маккадаги автомобиллар, сафар йўналиши, меҳмонлар ва багаж текширилмоқда. Сўнг iumrah мос бўш автомобилни танлайди.",
            ),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = fg.copy(alpha = .54f),
        )

        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(fg.copy(alpha = .08f))) {
            Box(Modifier.fillMaxWidth((second.toFloat() / duration.coerceAtLeast(1)).coerceIn(0f, 1f)).height(4.dp).clip(CircleShape).background(fg.copy(alpha = .78f)))
        }
    }
}

@Composable
private fun SearchChip(icon: CupertinoSymbol, text: String, bg: Color, fg: Color) {
    Row(
        Modifier.height(32.dp).clip(CircleShape).background(bg.copy(alpha = .92f)).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, null, Modifier.size(12.dp), tint = fg.copy(alpha = .7f))
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
    }
}

@Composable
private fun TransferSearchActivityBar(second: Int, fg: Color) {
    val active = (second / 5).coerceIn(0, 5)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(6) { index ->
            Box(Modifier.weight(1f).height(if (index == active) 5.dp else 3.dp).clip(CircleShape).background(fg.copy(alpha = if (index <= active) .78f else .10f)))
        }
    }
}

private fun searchStatus(language: AppLanguage, second: Int): String = when (second) {
    in 0..4 -> transferTr(language, "Сканируем дороги рядом с Харамом", "Scanning roads around the Haram", "Haram atrofidagi yo‘llar tekshirilmoqda", "Ҳарам атрофидаги йўллар текширилмоқда")
    in 5..9 -> transferTr(language, "Проверяем доступность водителей", "Checking driver availability", "Haydovchilar mavjudligi tekshirilmoqda", "Ҳайдовчилар мавжудлиги текширилмоқда")
    in 10..14 -> transferTr(language, "Сопоставляем ваши даты", "Matching your travel dates", "Safar sanalari moslashtirilmoqda", "Сафар саналари мослаштирилмоқда")
    in 15..19 -> transferTr(language, "Учитываем гостей и багаж", "Matching guests and luggage", "Mehmonlar va bagaj hisoblanmoqda", "Меҳмонлар ва бағаж ҳисобланмоқда")
    in 20..24 -> transferTr(language, "Проверяем маршрут Мекка — Медина", "Checking the Makkah–Madinah route", "Makka–Madina yo‘nalishi tekshirilmoqda", "Макка–Мадина йўналиши текширилмоқда")
    else -> transferTr(language, "Закрепляем лучший доступный вариант", "Securing the best available match", "Eng mos variant biriktirilmoqda", "Энг мос вариант бириктирилмоқда")
}

@Composable
private fun TransferMapCanvas(phase: Float, light: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val road = if (light) Color(0xFFD8D4CC) else Color.White.copy(alpha = .10f)
        for (i in 0..9) drawLine(road, Offset(0f, h * (i / 10f)), Offset(w, h * (((i + 3) % 10) / 10f)), strokeWidth = 1.2f)
        val path = Path().apply {
            moveTo(w * .12f, h * .70f)
            cubicTo(w * .30f, h * .52f, w * .52f, h * .64f, w * .72f, h * .40f)
            cubicTo(w * .80f, h * .30f, w * .86f, h * .34f, w * .92f, h * .20f)
        }
        drawPath(path, if (light) Color.Black.copy(alpha = .17f) else Color.White.copy(alpha = .16f), style = Stroke(width = 5f))
        val center = Offset(w * .53f, h * .54f)
        drawCircle(Color(0xFF007AFF).copy(alpha = .12f * (1f - phase)), radius = 38f + phase * 100f, center = center)
        drawCircle(Color(0xFF007AFF), radius = 12f, center = center)
        listOf(Offset(w * .25f, h * .60f), Offset(w * .68f, h * .48f), Offset(w * .77f, h * .31f), Offset(w * .37f, h * .43f)).forEachIndexed { index, point ->
            val active = ((phase + index * .23f) % 1f)
            drawCircle(if (light) Color.White else Color(0xFF2C2C2E), radius = 18f, center = point)
            drawCircle(if (light) Color.Black.copy(alpha = .78f) else Color.White.copy(alpha = .82f), radius = 8f + active * 2f, center = point)
        }
    }
}

@Composable
private fun TransferMatchedScreen(language: AppLanguage, journey: JourneyStore, generator: PackageGenerator, chrome: AppChromeStore) {
    val state by journey.state.collectAsState()
    val vehicles = TransferVehicleKind.entries
    val selected = state.resolvedTransferVehicle
    val initial = vehicles.indexOf(selected).coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = initial, pageCount = { vehicles.size })
    val scope = rememberCoroutineScope()
    var confirming by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pager.currentPage) {
        val vehicle = vehicles[pager.currentPage]
        if (journey.state.value.selectedTransferVehicle != vehicle) journey.chooseTransferVehicle(vehicle)
    }

    LaunchedEffect(selected, state.haramainTrainSelected, state.haramainAdultTickets, state.haramainChildTickets) {
        delay(180)
        refreshing = true
        runCatching { generator.generate(journey.state.value) }
            .onSuccess { quote -> journey.setQuote(quote); error = null }
            .onFailure { cause -> error = cause.message }
        refreshing = false
    }

    MaterialTheme(colorScheme = if (selected == TransferVehicleKind.YUKON) darkColorScheme() else lightColorScheme()) {
        val fg = currentFg(selected)
        Column(
            Modifier.fillMaxSize()
                .background(generatorPageColor())
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GeneratorGeometry.pagePadding, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            GeneratorHeader(GeneratorStage.TRANSFER, language, chrome, currentPriceText = state.quote?.let(::quoteTitle))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("IUMRAH TRANSFER", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = fg.copy(alpha = .5f))
                Text(
                    if (selected == TransferVehicleKind.YUKON) transferTr(language, "Ваш VIP-трансфер", "Your VIP transfer", "VIP transferringiz", "VIP трансферингиз") else transferTr(language, "Трансфер найден", "Transfer matched", "Transfer topildi", "Трансфер топилди"),
                    fontSize = 34.sp,
                    lineHeight = 37.sp,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                )
                Text(
                    transferTr(language, "Выберите автомобиль. Мы сохраняем один трансферный план для аэропорта, отелей и междугороднего маршрута.", "Choose your vehicle. One transfer plan covers the airport, hotels and intercity route.", "Avtomobilni tanlang. Bitta transfer rejasi aeroport, mehmonxonalar va shaharlararo yo‘lni qamrab oladi.", "Автомобилни танланг. Битта трансфер режаси аэропорт, меҳмонхоналар ва шаҳарлараро йўлни қамраб олади."),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = fg.copy(alpha = .58f),
                )
            }

            HorizontalPager(state = pager, contentPadding = PaddingValues(horizontal = 18.dp), pageSpacing = 12.dp, modifier = Modifier.fillMaxWidth().height(330.dp)) { page ->
                VehicleHero(language, vehicles[page], state.trip.scope, active = page == pager.currentPage)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                vehicles.forEachIndexed { index, _ ->
                    Box(Modifier.padding(horizontal = 3.dp).width(if (index == pager.currentPage) 18.dp else 6.dp).height(6.dp).clip(CircleShape).background(fg.copy(alpha = if (index == pager.currentPage) .9f else .18f)))
                }
            }

            TransferIncludedCard(language, selected, state.trip.scope == JourneyScope.MAKKAH_AND_MADINAH, state.haramainTrainSelected)

            if (state.trip.scope == JourneyScope.MAKKAH_AND_MADINAH) {
                HaramainCard(
                    language = language,
                    enabled = state.haramainTrainSelected,
                    adults = state.haramainAdultTickets,
                    children = state.haramainChildTickets,
                    maxAdults = state.trip.adults,
                    maxChildren = state.trip.children,
                    onToggle = {
                        journey.ensureHaramainTicketDefaults()
                        journey.setHaramainFareClass(HaramainFareClass.ECONOMY)
                        journey.setHaramainTrainSelected(!state.haramainTrainSelected)
                    },
                    onAdults = journey::setHaramainAdultTickets,
                    onChildren = journey::setHaramainChildTickets,
                )
            }

            if (refreshing) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp, color = fg)
                    Text(transferTr(language, "Обновляем итоговую цену…", "Updating package total…", "Paket narxi yangilanmoqda…", "Пакет нархи янгиланмоқда…"), fontSize = 12.sp, color = fg.copy(alpha = .54f))
                }
            }
            error?.let { Text(it, color = Color(0xFFFF453A), fontSize = 13.sp) }

            GeneratorPrimaryButton(
                title = if (confirming) transferTr(language, "Подтверждаем трансфер…", "Confirming transfer…", "Transfer tasdiqlanmoqda…", "Трансфер тасдиқланмоқда…") else transferTr(language, "Подтвердить трансфер", "Confirm transfer", "Transferni tasdiqlash", "Трансферни тасдиқлаш"),
                enabled = !confirming,
            ) {
                if (!confirming) {
                    confirming = true
                    error = null
                    journey.confirmTransferSelection()
                    scope.launch {
                        runCatching { generator.generate(journey.state.value) }
                            .onSuccess { quote ->
                                journey.setQuote(quote)
                                if (quote.quoteId?.startsWith("server-") == true && !quote.quoteProof.isNullOrBlank() && quote.totalPackagePrice.signum() > 0 && quote.pricePerPerson.signum() > 0) {
                                    chrome.openFinalPackage()
                                } else {
                                    error = transferTr(language, "Не удалось подтвердить итоговую конфигурацию поездки. Попробуйте ещё раз.", "The trip configuration could not be confirmed. Please try again.", "Safar konfiguratsiyasini tasdiqlab bo‘lmadi. Qayta urinib ko‘ring.", "Сафар конфигурациясини тасдиқлаб бўлмади. Қайта уриниб кўринг.")
                                }
                                confirming = false
                            }
                            .onFailure { cause ->
                                confirming = false
                                error = cause.message ?: transferTr(language, "Не удалось получить актуальную цену.", "Could not fetch the current package price.", "Joriy paket narxini olib bo‘lmadi.", "Жорий пакет нархини олиб бўлмади.")
                            }
                    }
                }
            }
            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
private fun VehicleHero(language: AppLanguage, vehicle: TransferVehicleKind, scope: JourneyScope, active: Boolean) {
    val vip = vehicle == TransferVehicleKind.YUKON
    val fg = if (vip) Color.White else Color.Black
    val bg = if (vip) Brush.linearGradient(listOf(Color(0xFF121214), Color(0xFF28282C))) else Brush.linearGradient(listOf(Color.White, Color(0xFFEDEFF2)))
    Column(
        Modifier.fillMaxSize().clip(RoundedCornerShape(34.dp)).background(bg)
            .border(if (active) 1.5.dp else .5.dp, fg.copy(alpha = if (active) .16f else .06f), RoundedCornerShape(34.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(vehicleClass(language, vehicle), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp, color = fg.copy(alpha = .55f))
                Text(vehicle.modelName, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = fg)
            }
            if (vehicle == TransferVehicleKind.CARNIVAL) {
                Box(Modifier.clip(CircleShape).background(Color(0xFF34C759).copy(alpha = .16f)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(transferTr(language, "Рекомендуем", "Recommended", "Tavsiya", "Тавсия"), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D7F3E))
                }
            }
        }
        Image(painterResource(vehicleDrawable(vehicle)), vehicle.modelName, Modifier.fillMaxWidth().height(175.dp), contentScale = ContentScale.Fit)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VehicleStat(CupertinoSymbol.Persons, "${vehicle.passengerCapacity}", fg)
            VehicleStat(CupertinoSymbol.Suitcase, "${vehicle.luggageCapacity}", fg)
            VehicleStat(CupertinoSymbol.Car, if (vehicle.publicUpgradeUsd(scope) > 0) "+$${vehicle.publicUpgradeUsd(scope)}" else transferTr(language, "Включён", "Included", "Kiritilgan", "Киритилган"), fg)
        }
    }
}

@Composable
private fun VehicleStat(icon: CupertinoSymbol, text: String, fg: Color) {
    Row(Modifier.clip(CircleShape).background(fg.copy(alpha = .08f)).padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, null, Modifier.size(13.dp), tint = fg.copy(alpha = .75f))
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun TransferIncludedCard(language: AppLanguage, selected: TransferVehicleKind, includesMadinah: Boolean, usesTrain: Boolean) {
    val fg = currentFg(selected)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(generatorCardColor()).border(.7.dp, fg.copy(alpha = .06f), RoundedCornerShape(28.dp)).padding(19.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text(transferTr(language, "Что входит в трансфер", "Included in your transfer", "Transferga kiradi", "Трансферга киради"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg.copy(alpha = .55f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
            CoverageStop(CupertinoSymbol.AirplaneLand, transferTr(language, "Аэропорт", "Airport", "Aeroport", "Аэропорт"), fg)
            CoverageLine(fg)
            CoverageStop(CupertinoSymbol.Hotel, "Makkah", fg)
            if (includesMadinah) {
                CoverageLine(fg)
                CoverageStop(if (usesTrain) CupertinoSymbol.Route else CupertinoSymbol.Car, transferTr(language, "Межгород", "Intercity", "Shaharlararo", "Шаҳарлараро"), fg)
                CoverageLine(fg)
                CoverageStop(CupertinoSymbol.Hotel, "Madinah", fg)
            }
            CoverageLine(fg)
            CoverageStop(CupertinoSymbol.AirplaneTakeoff, transferTr(language, "Вылет", "Departure", "Jo‘nab ketish", "Жўнаб кетиш"), fg)
        }
    }
}

@Composable
private fun CoverageStop(icon: CupertinoSymbol, title: String, fg: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(fg.copy(alpha = .08f)), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(14.dp), tint = fg) }
        Text(title, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
    }
}

@Composable
private fun CoverageLine(fg: Color) {
    Box(Modifier.padding(top = 16.dp).width(15.dp).height(2.dp).clip(CircleShape).background(fg.copy(alpha = .18f)))
}

@Composable
private fun HaramainCard(
    language: AppLanguage,
    enabled: Boolean,
    adults: Int,
    children: Int,
    maxAdults: Int,
    maxChildren: Int,
    onToggle: () -> Unit,
    onAdults: (Int) -> Unit,
    onChildren: (Int) -> Unit,
) {
    val fg = MaterialTheme.colorScheme.onSurface
    val standardPrice = HaramainFareClass.ECONOMY.publicSeatPriceUsd
    val preview = (adults + children) * standardPrice
    val shape = RoundedCornerShape(30.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(generatorCardColor())
            .border(.8.dp, if (enabled) Color(0xFF34C759).copy(alpha = .35f) else fg.copy(alpha = .06f), shape).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.haramain_mark), "Haramain", Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Haramain High Speed Railway", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = fg)
                Text("Makkah ↔ Madinah", fontSize = 11.sp, color = fg.copy(alpha = .52f))
            }
            if (enabled) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(CupertinoSymbol.CheckCircle, null, Modifier.size(14.dp), tint = Color(0xFF34C759))
                    Text(transferTr(language, "Добавлено", "Added", "Qo‘shildi", "Қўшилди"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34C759))
                }
            }
        }

        HaramainPhotoGallery()

        Text(
            transferTr(language, "Замените только междугородний участок Мекка ↔ Медина поездом. Автомобиль до станции и после прибытия остаётся частью вашего iumrah Transfer.", "Replace only the Makkah ↔ Madinah intercity segment by train. Your car transfer to the station and after arrival remains part of iumrah Transfer.", "Faqat Makka ↔ Madina shaharlararo qismini poyezdga almashtiring. Vokzalgacha va keyingi avtomobil transferi iumrah Transfer tarkibida qoladi.", "Фақат Макка ↔ Мадина шаҳарлараро қисмини поездга алмаштиринг. Вокзалгача ва кейинги автомобиль трансфери iumrah Transfer таркибида қолади."),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = fg.copy(alpha = .56f),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrainFact("300 km/h", Modifier.weight(1f))
            TrainFact("≈ 2h 20m", Modifier.weight(1f))
            TrainFact("Wi‑Fi", Modifier.weight(1f))
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(generatorRaisedColor()).padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(transferTr(language, "Билеты для вашей группы", "Tickets for your party", "Guruhingiz uchun chiptalar", "Гуруҳингиз учун чипталар"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = fg)
                    Text("Standard · $$standardPrice / ${transferTr(language, "паломник", "pilgrim", "ziyoratchi", "зиёратчи")}", fontSize = 10.sp, color = fg.copy(alpha = .52f))
                }
                Text("+$$preview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = fg)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(fg.copy(alpha = .09f)))
            TicketStepper(transferTr(language, "Взрослые", "Adults", "Kattalar", "Катталар"), adults, 1, maxOf(1, maxAdults), onAdults)
            if (maxChildren > 0) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(fg.copy(alpha = .09f)))
                TicketStepper(transferTr(language, "Дети", "Children", "Bolalar", "Болалар"), children, 0, maxChildren, onChildren)
            }
        }

        IumrahPressable(onClick = onToggle, modifier = Modifier.fillMaxWidth().height(62.dp), cornerRadius = 20.dp, background = if (enabled) Color(0xFF34C759) else Color.Black) {
            Row(Modifier.fillMaxSize().padding(horizontal = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (enabled) CupertinoSymbol.CheckCircle else CupertinoSymbol.Plus, null, Modifier.size(20.dp), tint = Color.White)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (enabled) transferTr(language, "Поезд подключён", "Train added", "Poyezd qo‘shildi", "Поезд қўшилди") else transferTr(language, "Подключить поезд к поездке", "Add train to this trip", "Poyezdni safarga qo‘shish", "Поездни сафарга қўшиш"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(if (enabled) transferTr(language, "Нажмите, чтобы отключить", "Tap to remove", "Olib tashlash uchun bosing", "Олиб ташлаш учун босинг") else "+$$preview · Standard", fontSize = 10.sp, color = Color.White.copy(alpha = .72f))
                }
                Icon(if (enabled) CupertinoSymbol.Checkmark else CupertinoSymbol.ArrowRight, null, Modifier.size(15.dp), tint = Color.White)
            }
        }
    }
}

@Composable
private fun HaramainPhotoGallery() {
    Row(Modifier.fillMaxWidth().height(104.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf(R.drawable.haramain_gallery_station, R.drawable.haramain_gallery_interior, R.drawable.haramain_gallery_train).forEach { drawable ->
            Image(painterResource(drawable), null, Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun TrainFact(title: String, modifier: Modifier) {
    val fg = MaterialTheme.colorScheme.onSurface
    Box(modifier.height(36.dp).clip(CircleShape).background(generatorRaisedColor()), contentAlignment = Alignment.Center) {
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun TicketStepper(title: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    val fg = MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = fg)
        Spacer(Modifier.weight(1f))
        MiniStep(CupertinoSymbol.Minus, value > min) { onChange(value - 1) }
        Text(value.toString(), Modifier.width(36.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = fg)
        MiniStep(CupertinoSymbol.Plus, value < max) { onChange(value + 1) }
    }
}

@Composable
private fun MiniStep(icon: CupertinoSymbol, enabled: Boolean, onClick: () -> Unit) {
    val fg = MaterialTheme.colorScheme.onSurface
    IumrahPressable(onClick = onClick, enabled = enabled, modifier = Modifier.size(30.dp), cornerRadius = 99.dp, background = fg.copy(alpha = .08f)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(13.dp), tint = fg.copy(alpha = if (enabled) 1f else .3f)) }
    }
}

private fun quoteTitle(quote: com.iumrah.beta.domain.pricing.PackageQuote): String =
    "${quote.totalPackagePrice.setScale(0, RoundingMode.HALF_UP)} ${quote.currency}"

private fun vehicleDrawable(vehicle: TransferVehicleKind) = when (vehicle) {
    TransferVehicleKind.MALIBU -> R.drawable.transfer_malibu
    TransferVehicleKind.CARNIVAL -> R.drawable.transfer_carnival
    TransferVehicleKind.YUKON -> R.drawable.transfer_yukon
}

private fun vehicleClass(language: AppLanguage, vehicle: TransferVehicleKind) = when (vehicle) {
    TransferVehicleKind.MALIBU -> transferTr(language, "PRIVATE", "PRIVATE", "PRIVATE", "PRIVATE")
    TransferVehicleKind.CARNIVAL -> transferTr(language, "FAMILY", "FAMILY", "FAMILY", "FAMILY")
    TransferVehicleKind.YUKON -> "VIP"
}

@Composable
private fun currentFg(selected: TransferVehicleKind) = if (selected == TransferVehicleKind.YUKON) Color.White else MaterialTheme.colorScheme.onBackground

private fun transferTr(l: AppLanguage, ru: String, en: String, uz: String, uzCy: String) = when (l) {
    AppLanguage.RUSSIAN -> ru
    AppLanguage.ENGLISH -> en
    AppLanguage.UZBEK -> uz
    AppLanguage.UZBEK_CYRILLIC -> uzCy
}
