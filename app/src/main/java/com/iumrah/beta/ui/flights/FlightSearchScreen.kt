package com.iumrah.beta.ui.flights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iumrah.beta.R
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.flight.IgnavFlightInventoryProvider
import com.iumrah.beta.domain.journey.JourneyStore
import com.iumrah.beta.models.flight.LiveFlightCandidate
import com.iumrah.beta.models.flight.LiveFlightJourneyCandidate
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.cupertino.CupertinoSymbol
import com.iumrah.beta.ui.cupertino.Icon
import com.iumrah.beta.ui.generator.GeneratorCard
import com.iumrah.beta.ui.generator.GeneratorGeometry
import com.iumrah.beta.ui.generator.GeneratorHeader
import com.iumrah.beta.ui.generator.GeneratorPrimaryButton
import com.iumrah.beta.ui.generator.GeneratorStage
import com.iumrah.beta.ui.generator.generatorCardColor
import com.iumrah.beta.ui.generator.generatorPageColor
import com.iumrah.beta.ui.generator.generatorRaisedColor
import com.iumrah.beta.ui.media.LoopingRawVideo
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

/** OutboundFlightView parity. Search remains backed by Ignav's verified round-trip itinerary inventory. */
@Composable
fun FlightSearchScreen(
    language: AppLanguage,
    journey: JourneyStore,
    provider: IgnavFlightInventoryProvider,
    chrome: AppChromeStore,
) {
    val state by journey.state.collectAsState()
    var showReadyMoment by remember { mutableStateOf(false) }

    LaunchedEffect(state.trip, state.makkahHotel?.id, state.madinahHotel?.id) {
        if (state.flightResults.isEmpty() && !state.isSearchingFlights && state.flightError == null) journey.searchFlights(provider)
    }
    LaunchedEffect(state.isSearchingFlights, state.flightResults.size) {
        if (!state.isSearchingFlights && state.flightResults.isNotEmpty() && state.selectedOutboundJourneyId == null) {
            showReadyMoment = true
            delay(650)
            showReadyMoment = false
        }
    }

    when {
        state.isSearchingFlights && state.flightResults.isEmpty() -> {
            FlightDiscoveryScreen(language, chrome::back)
            return
        }
        showReadyMoment -> {
            FlightReadyScreen(language)
            return
        }
        state.flightError != null && state.flightResults.isEmpty() -> {
            FlightFailureScreen(language, state.flightError, chrome::back) { journey.clearFlights() }
            return
        }
    }

    val offers = state.flightResults
    val selectedId = state.selectedOutboundJourneyId
    LazyColumn(
        Modifier.fillMaxSize().background(generatorPageColor()).statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = GeneratorGeometry.pagePadding, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { GeneratorHeader(GeneratorStage.FLIGHT, language, chrome) }
        item { FlightHeading(language, outbound = true, count = offers.size) }
        item { FlightFilterSummary(language, state.trip.effectiveFlightFilters.cabinClass.wireValue, state.trip.effectiveFlightFilters.stops.wireValue) }
        items(offers, key = { "out-${it.id}" }) { offer ->
            FlightOfferCard(
                language = language,
                journey = offer,
                leg = offer.outbound,
                selected = selectedId == offer.id,
                recommended = offer == offers.minByOrNull { it.totalFare },
                onSelect = { journey.selectOutboundJourney(offer.id) },
                onDetails = { chrome.openFlightDetails(offer.id, "outbound") },
            )
        }
        item {
            GeneratorPrimaryButton(
                title = if (state.trip.isRoundTripFlight) flightTr(language,"Продолжить к обратному рейсу","Continue to return flight","Qaytish reysiga davom etish","Қайтиш рейсига давом этиш") else flightTr(language,"Продолжить к трансферу","Continue to transfer","Transferga davom etish","Трансферга давом этиш"),
                enabled = selectedId != null,
            ) {
                if (state.trip.isRoundTripFlight) chrome.openReturnFlights() else {
                    selectedId?.let { journey.selectReturnJourney(it) }
                    chrome.openTransferSelection()
                }
            }
        }
        item { Spacer(Modifier.height(34.dp)) }
    }
}

/** ReturnFlightView parity using paired itinerary rows that share the chosen outbound leg. */
@Composable
fun ReturnFlightScreen(language: AppLanguage, journey: JourneyStore, chrome: AppChromeStore) {
    val state by journey.state.collectAsState()
    val outbound = state.selectedOutboundJourney
    val candidates = remember(state.flightResults, outbound?.id) {
        if (outbound == null) emptyList() else {
            state.flightResults.filter { sameFlight(it.outbound, outbound.outbound) && it.inbound != null }
        }
    }
    val selected = state.selectedJourneyId

    LazyColumn(
        Modifier.fillMaxSize().background(generatorPageColor()).statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = GeneratorGeometry.pagePadding, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { GeneratorHeader(GeneratorStage.FLIGHT, language, chrome) }
        item { FlightHeading(language, outbound = false, count = candidates.size) }
        outbound?.let { chosen ->
            item { SelectedOutboundSummary(language, chosen.outbound) }
        }
        if (candidates.isEmpty()) {
            item {
                GeneratorCard {
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Icon(CupertinoSymbol.ExclamationCircle,null,Modifier.size(25.dp),tint=Color(0xFFFF9500))
                        Text(flightTr(language,"Для выбранного рейса нет обратных вариантов.","No return option is available for the selected outbound flight.","Tanlangan uchish reysi uchun qaytish varianti yo‘q.","Танланган учиш рейси учун қайтиш варианти йўқ."),fontSize=15.sp,fontWeight=FontWeight.SemiBold)
                        Text(flightTr(language,"Вернитесь и выберите другой перелёт.","Go back and select another outbound itinerary.","Ortga qaytib boshqa reysni tanlang.","Орқага қайтиб бошқа рейсни танланг."),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.55f))
                    }
                }
            }
        }
        items(candidates, key = { "ret-${it.id}" }) { offer ->
            val leg = offer.inbound ?: return@items
            FlightOfferCard(
                language = language,
                journey = offer,
                leg = leg,
                selected = selected == offer.id,
                recommended = offer == candidates.minByOrNull { it.totalFare },
                onSelect = { journey.selectReturnJourney(offer.id) },
                onDetails = { chrome.openFlightDetails(offer.id, "inbound") },
            )
        }
        item {
            GeneratorPrimaryButton(
                title = flightTr(language,"Продолжить к трансферу","Continue to transfer","Transferga davom etish","Трансферга давом этиш"),
                enabled = selected != null,
                onClick = chrome::openTransferSelection,
            )
        }
        item { Spacer(Modifier.height(34.dp)) }
    }
}

@Composable
fun FlightDetailsScreen(journeyId:String, direction:String, language:AppLanguage, journey:JourneyStore, chrome:AppChromeStore) {
    val state by journey.state.collectAsState()
    val pair = state.flightResults.firstOrNull { it.id == journeyId }
    val leg = if(direction == "inbound") pair?.inbound else pair?.outbound
    LazyColumn(
        Modifier.fillMaxSize().background(generatorPageColor()).statusBarsPadding(),
        contentPadding=PaddingValues(horizontal=GeneratorGeometry.pagePadding,vertical=10.dp),
        verticalArrangement=Arrangement.spacedBy(18.dp),
    ) {
        item { GeneratorHeader(GeneratorStage.FLIGHT,language,chrome) }
        item {
            Text(flightTr(language,"Детали перелёта","Flight details","Parvoz tafsilotlari","Парвоз тафсилотлари"),fontSize=32.sp,fontWeight=FontWeight.Bold,letterSpacing=(-.6).sp)
        }
        if(pair == null || leg == null) {
            item { Text(flightTr(language,"Рейс больше недоступен.","This flight is no longer available.","Bu reys endi mavjud emas.","Бу рейс энди мавжуд эмас.")) }
        } else {
            item { FlightHeroDetail(language,pair,leg) }
            val segments = leg.segments.orEmpty()
            items(segments, key={it.id}) { segment ->
                GeneratorCard {
                    Column(verticalArrangement=Arrangement.spacedBy(11.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically){Icon(CupertinoSymbol.Airplane,null,Modifier.size(20.dp),tint=Color(0xFF007AFF));Spacer(Modifier.width(9.dp));Text("${segment.airline} ${segment.flightNumber}",fontSize=16.sp,fontWeight=FontWeight.Bold)}
                        TimelineRow(segment.origin.code, formatInstant(segment.departureAt,segment.origin.timeZoneIdentifier), segment.origin.displayAirport)
                        Row(Modifier.padding(start=11.dp).height(32.dp)){Box(Modifier.width(2.dp).fillMaxSize().background(MaterialTheme.colorScheme.onSurface.copy(alpha=.13f)))}
                        TimelineRow(segment.destination.code, formatInstant(segment.arrivalAt,segment.destination.timeZoneIdentifier), segment.destination.displayAirport)
                        Text(durationText(language, segment.durationMinutes),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.52f))
                    }
                }
            }
            item {
                GeneratorCard {
                    Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
                        Text(flightTr(language,"Багаж и тариф","Baggage & fare","Bagaj va tarif","Багаж ва тариф"),fontSize=17.sp,fontWeight=FontWeight.Bold)
                        DetailLine(flightTr(language,"Класс","Cabin","Klass","Класс"), leg.cabinClass ?: state.trip.effectiveFlightFilters.cabinClass.wireValue)
                        DetailLine(flightTr(language,"Ручная кладь","Carry-on","Qo‘l yuki","Қўл юки"), pair.baggage?.carryOn?.let{"$it"} ?: "—")
                        DetailLine(flightTr(language,"Багаж","Checked baggage","Bagaj","Багаж"), pair.baggage?.checked?.let{"$it"} ?: "—")
                        DetailLine(flightTr(language,"Источник","Source","Manba","Манба"), pair.sourceName)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(34.dp)) }
    }
}

@Composable private fun FlightDiscoveryScreen(language:AppLanguage,onBack:()->Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        LoopingRawVideo("flight_search", Modifier.fillMaxSize(), fallback={Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF101216),Color.Black))))})
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.12f),Color.Transparent,Color.Black.copy(alpha=.76f)))))
        IumrahPressable(onClick=onBack,modifier=Modifier.statusBarsPadding().padding(18.dp).size(42.dp),cornerRadius=99.dp,background=Color.Black.copy(alpha=.42f)) { Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Icon(CupertinoSymbol.ChevronLeft,"Back",Modifier.size(19.dp),tint=Color.White)} }
        Column(Modifier.align(Alignment.BottomStart).padding(24.dp,24.dp,24.dp,52.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){CircularProgressIndicator(Modifier.size(18.dp),color=Color.White,strokeWidth=2.dp);Text(flightTr(language,"Живой поиск","Live search","Jonli qidiruv","Жонли қидирув"),fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color.White.copy(alpha=.7f),letterSpacing=1.sp)}
            Text(flightTr(language,"Ищем лучший перелёт","Finding the right flights","Mos reyslarni qidiryapmiz","Мос рейсларни қидиряпмиз"),fontSize=34.sp,lineHeight=37.sp,fontWeight=FontWeight.Bold,color=Color.White)
            Text(flightTr(language,"Проверяем доступность, расписание, багаж и полный маршрут туда и обратно.","Checking availability, schedules, baggage and the complete outbound-and-return itinerary.","Mavjudlik, jadval, bagaj va to‘liq borish-qaytish marshruti tekshirilmoqda.","Мавжудлик, жадвал, багаж ва тўлиқ бориш-қайтиш маршрути текширилмоқда."),fontSize=15.sp,lineHeight=21.sp,color=Color.White.copy(alpha=.68f))
        }
    }
}

@Composable private fun FlightReadyScreen(language:AppLanguage) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        LoopingRawVideo("flight_ready",Modifier.fillMaxSize(),fallback={Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF13261B),Color.Black))))})
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.25f)))
        Column(Modifier.align(Alignment.BottomStart).padding(24.dp,24.dp,24.dp,60.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
            Icon(CupertinoSymbol.CheckCircle,null,Modifier.size(31.dp),tint=Color(0xFF63D98B))
            Text(flightTr(language,"Перелёты готовы","Flights are ready","Reyslar tayyor","Рейслар тайёр"),fontSize=34.sp,fontWeight=FontWeight.Bold,color=Color.White)
        }
    }
}

@Composable private fun FlightFailureScreen(language:AppLanguage,error:String?,onBack:()->Unit,onRetry:()->Unit) {
    Column(Modifier.fillMaxSize().background(generatorPageColor()).statusBarsPadding().padding(GeneratorGeometry.pagePadding),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(74.dp).clip(CircleShape).background(Color(0xFFFF3B30).copy(alpha=.1f)),contentAlignment=Alignment.Center){Icon(CupertinoSymbol.ExclamationCircle,null,Modifier.size(34.dp),tint=Color(0xFFFF3B30))}
        Spacer(Modifier.height(20.dp));Text(flightTr(language,"Не удалось найти перелёт","Flight search failed","Reys topilmadi","Рейс топилмади"),fontSize=26.sp,fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(8.dp));Text(error.orEmpty(),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.45f),maxLines=2,overflow=TextOverflow.Ellipsis)
        Spacer(Modifier.height(24.dp));GeneratorPrimaryButton(flightTr(language,"Повторить","Try again","Qayta urinish","Қайта уриниш"),onClick=onRetry)
        Spacer(Modifier.height(10.dp));IumrahPressable(onClick=onBack,modifier=Modifier.fillMaxWidth().height(54.dp),cornerRadius=18.dp,background=generatorRaisedColor()){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(flightTr(language,"Назад","Back","Orqaga","Орқага"),fontWeight=FontWeight.SemiBold)}}
    }
}

@Composable private fun FlightHeading(language:AppLanguage,outbound:Boolean,count:Int) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(if(outbound)flightTr(language,"ТУДА","OUTBOUND","BORISH","БОРИШ") else flightTr(language,"ОБРАТНО","RETURN","QAYTISH","ҚАЙТИШ"),fontSize=12.sp,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp,color=MaterialTheme.colorScheme.onBackground.copy(alpha=.5f))
        Text(if(outbound)flightTr(language,"Выберите перелёт","Choose your flight","Reysni tanlang","Рейсни танланг") else flightTr(language,"Выберите обратный рейс","Choose your return","Qaytish reysini tanlang","Қайтиш рейсини танланг"),fontSize=34.sp,lineHeight=37.sp,fontWeight=FontWeight.Bold,letterSpacing=(-.8).sp)
        Text(flightTr(language,"Найдено вариантов: $count","$count verified options found","$count ta tekshirilgan variant topildi","$count та текширилган вариант топилди"),fontSize=14.sp,color=MaterialTheme.colorScheme.onBackground.copy(alpha=.55f))
    }
}

@Composable private fun FlightFilterSummary(language:AppLanguage,cabin:String,stops:String) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        SummaryPill(CupertinoSymbol.Suitcase,cabin.replaceFirstChar{it.uppercase()})
        SummaryPill(CupertinoSymbol.Route,stops.replace('_',' ').replaceFirstChar{it.uppercase()})
    }
}
@Composable private fun SummaryPill(icon:CupertinoSymbol,title:String){Row(Modifier.clip(CircleShape).background(generatorCardColor()).padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){Icon(icon,null,Modifier.size(14.dp));Text(title,fontSize=11.sp,fontWeight=FontWeight.SemiBold)}}

@Composable private fun FlightOfferCard(language:AppLanguage,journey:LiveFlightJourneyCandidate,leg:LiveFlightCandidate,selected:Boolean,recommended:Boolean,onSelect:()->Unit,onDetails:()->Unit) {
    val fg=MaterialTheme.colorScheme.onSurface
    val shape=RoundedCornerShape(28.dp)
    IumrahPressable(onClick=onSelect,modifier=Modifier.fillMaxWidth().then(if(selected)Modifier.border(2.dp,fg,shape) else Modifier),cornerRadius=28.dp,background=generatorCardColor(),shadowElevation=if(selected)7.dp else 2.dp) {
        Column(Modifier.fillMaxWidth().padding(17.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(generatorRaisedColor()),contentAlignment=Alignment.Center){Icon(CupertinoSymbol.Airplane,null,Modifier.size(21.dp),tint=Color(0xFF007AFF))}
                Spacer(Modifier.width(11.dp));Column(Modifier.weight(1f)){Text(leg.airline,fontSize=15.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(leg.flightNumber,fontSize=11.sp,color=fg.copy(alpha=.5f))}
                if(recommended) Box(Modifier.clip(CircleShape).background(Color(0xFF34C759).copy(alpha=.12f)).padding(horizontal=9.dp,vertical=6.dp)){Text(flightTr(language,"Рекомендуем","Recommended","Tavsiya","Тавсия"),fontSize=9.sp,fontWeight=FontWeight.Bold,color=Color(0xFF248A3D))}
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                FlightTimeBlock(leg.origin,formatTime(leg,true),Modifier.weight(1f),Alignment.Start)
                Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)){Text(durationText(language,leg.durationMinutes),fontSize=10.sp,color=fg.copy(alpha=.45f));Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.width(30.dp).height(1.dp).background(fg.copy(alpha=.16f)));Icon(CupertinoSymbol.Airplane,null,Modifier.size(14.dp),tint=fg.copy(alpha=.55f));Box(Modifier.width(30.dp).height(1.dp).background(fg.copy(alpha=.16f)))};Text(if(leg.stops==0)flightTr(language,"Прямой","Direct","To‘g‘ri","Тўғри") else flightTr(language,"${leg.stops} пересадка","${leg.stops} stop","${leg.stops} ta transfer","${leg.stops} та трансфер"),fontSize=9.sp,color=fg.copy(alpha=.45f))}
                FlightTimeBlock(leg.destination,formatTime(leg,false),Modifier.weight(1f),Alignment.End)
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(formatFare(journey.totalFare,journey.currency),fontSize=20.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IumrahPressable(onClick=onDetails,modifier=Modifier.height(36.dp),cornerRadius=99.dp,background=generatorRaisedColor()){Row(Modifier.padding(horizontal=12.dp).height(36.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Text(flightTr(language,"Детали","Details","Tafsilot","Тафсилот"),fontSize=11.sp,fontWeight=FontWeight.SemiBold);Icon(CupertinoSymbol.ChevronRight,null,Modifier.size(13.dp))}}
                if(selected){Spacer(Modifier.width(9.dp));Icon(CupertinoSymbol.CheckCircle,null,Modifier.size(22.dp),tint=Color(0xFF34C759))}
            }
        }
    }
}

@Composable private fun FlightTimeBlock(code:String,time:String,modifier:Modifier,alignment:Alignment.Horizontal){Column(modifier,horizontalAlignment=alignment){Text(time,fontSize=20.sp,fontWeight=FontWeight.Bold);Text(code,fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.5f))}}

@Composable private fun SelectedOutboundSummary(language:AppLanguage,leg:LiveFlightCandidate){GeneratorCard { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Icon(CupertinoSymbol.CheckCircle,null,Modifier.size(22.dp),tint=Color(0xFF34C759));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(flightTr(language,"Выбранный рейс туда","Selected outbound","Tanlangan borish reysi","Танланган бориш рейси"),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.5f));Text("${leg.origin} → ${leg.destination} · ${leg.airline} ${leg.flightNumber}",fontSize=14.sp,fontWeight=FontWeight.SemiBold)}}}}

@Composable private fun FlightHeroDetail(language:AppLanguage,pair:LiveFlightJourneyCandidate,leg:LiveFlightCandidate){
    val dark = generatorPageColor()!=Color.White
    val gradient=if(dark) listOf(Color(0xFF17191D),Color(0xFF252A33)) else listOf(Color(0xFFEEF4FF),Color(0xFFE1EAFA))
    val fg=if(dark)Color.White else Color.Black
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(34.dp)).background(Brush.linearGradient(gradient)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(fg.copy(alpha=.08f)),contentAlignment=Alignment.Center){Icon(CupertinoSymbol.Airplane,null,Modifier.size(25.dp),tint=Color(0xFF007AFF))};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(leg.airline,fontSize=18.sp,fontWeight=FontWeight.Bold,color=fg);Text("${leg.flightNumber} · ${pair.sourceName}",fontSize=12.sp,color=fg.copy(alpha=.55f))}}
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){FlightTimeBlock(leg.origin,formatTime(leg,true),Modifier.weight(1f),Alignment.Start);Icon(CupertinoSymbol.ArrowRight,null,Modifier.size(21.dp),tint=fg.copy(alpha=.5f));FlightTimeBlock(leg.destination,formatTime(leg,false),Modifier.weight(1f),Alignment.End)}
        Text(formatFare(pair.totalFare,pair.currency),fontSize=28.sp,fontWeight=FontWeight.Bold,color=fg)
    }
}

@Composable private fun TimelineRow(code:String,time:String,name:String){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(22.dp).clip(CircleShape).border(2.dp,Color(0xFF007AFF),CircleShape));Spacer(Modifier.width(11.dp));Column(Modifier.weight(1f)){Text("$time · $code",fontSize=15.sp,fontWeight=FontWeight.Bold);Text(name,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.5f),maxLines=1,overflow=TextOverflow.Ellipsis)}}}
@Composable private fun DetailLine(title:String,value:String){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(title,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.5f));Spacer(Modifier.weight(1f));Text(value,fontSize=13.sp,fontWeight=FontWeight.SemiBold,maxLines=1)}}

private fun sameFlight(a:LiveFlightCandidate,b:LiveFlightCandidate)=a.flightNumber==b.flightNumber && a.departureAt==b.departureAt && a.origin==b.origin && a.destination==b.destination
private fun formatFare(value:BigDecimal,currency:String):String="${value.setScale(0,java.math.RoundingMode.HALF_UP).toPlainString()} ${currency.uppercase()}"
private fun formatTime(leg:LiveFlightCandidate,departure:Boolean):String { val segment=if(departure)leg.segments?.firstOrNull()?.origin else leg.segments?.lastOrNull()?.destination; val zone=segment?.timeZoneIdentifier?.let{runCatching{ZoneId.of(it)}.getOrNull()}?:ZoneOffset.UTC; val instant=if(departure)leg.departureAt else leg.arrivalAt; return DateTimeFormatter.ofPattern("HH:mm").format(instant.atZone(zone)) }
private fun formatInstant(instant:java.time.Instant,zoneId:String?):String { val zone=zoneId?.let{runCatching{ZoneId.of(it)}.getOrNull()}?:ZoneOffset.UTC; return DateTimeFormatter.ofPattern("d MMM · HH:mm",Locale.ENGLISH).format(instant.atZone(zone)) }
private fun durationText(l:AppLanguage,minutes:Int):String { val h=minutes/60;val m=minutes%60;return if(h>0)"${h}h ${m}m" else "${m}m" }
private fun flightTr(l:AppLanguage,ru:String,en:String,uz:String,uzCy:String)=when(l){AppLanguage.RUSSIAN->ru;AppLanguage.ENGLISH->en;AppLanguage.UZBEK->uz;AppLanguage.UZBEK_CYRILLIC->uzCy}
