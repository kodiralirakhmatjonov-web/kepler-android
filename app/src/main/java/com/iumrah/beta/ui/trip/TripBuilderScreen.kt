package com.iumrah.beta.ui.trip

import android.app.DatePickerDialog
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.flight.AirportSearchService
import com.iumrah.beta.domain.journey.JourneyStore
import com.iumrah.beta.domain.trip.DateFlexibility
import com.iumrah.beta.domain.trip.JourneyScope
import com.iumrah.beta.domain.trip.PackageTier
import com.iumrah.beta.domain.trip.SaudiArrivalAirport
import com.iumrah.beta.models.flight.Airport
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.cupertino.CupertinoSymbol
import com.iumrah.beta.ui.cupertino.Icon
import com.iumrah.beta.ui.generator.GeneratorCard
import com.iumrah.beta.ui.generator.GeneratorGeometry
import com.iumrah.beta.ui.generator.GeneratorHeader
import com.iumrah.beta.ui.generator.GeneratorPrimaryButton
import com.iumrah.beta.ui.generator.GeneratorStage
import com.iumrah.beta.ui.generator.generatorPageColor
import com.iumrah.beta.ui.generator.generatorRaisedColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.delay

enum class GeneratorFlightChoice { FLEXIBLE_DATES, WEEKEND }

@Composable
fun TripBuilderScreen(
    language: AppLanguage,
    journey: JourneyStore,
    airports: AirportSearchService,
    chrome: AppChromeStore,
) {
    val initial = journey.state.value.trip
    var draft by remember { mutableStateOf(initial.copy(flightTripType = com.iumrah.beta.domain.trip.FlightTripType.ROUND_TRIP)) }
    var originQuery by remember { mutableStateOf(initial.originAirport?.compactTitle ?: initial.originCode) }
    var suggestions by remember { mutableStateOf<List<Airport>>(emptyList()) }
    var mode by remember { mutableStateOf(if (initial.isWeekendUmrah) GeneratorFlightChoice.WEEKEND else GeneratorFlightChoice.FLEXIBLE_DATES) }

    LaunchedEffect(originQuery) {
        delay(260)
        suggestions = if (originQuery.length >= 2 && originQuery != draft.originAirport?.compactTitle) {
            runCatching { airports.search(originQuery) }.getOrElse { emptyList() }
        } else emptyList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(generatorPageColor()).statusBarsPadding(),
        contentPadding = PaddingValues(start = GeneratorGeometry.pagePadding, end = GeneratorGeometry.pagePadding, top = 12.dp, bottom = 42.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item { GeneratorHeader(GeneratorStage.TRIP, language, chrome) }
        item { TripIntro(language) }
        item {
            GeneratorCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CardTitle(CupertinoSymbol.AirplaneTakeoff, tr(language, "Откуда вылетаете", "Where are you flying from?", "Qayerdan uchasiz?", "Қаердан учасиз?"))
                    AirportSelector(
                        query = originQuery,
                        onChange = {
                            originQuery = it
                            if (draft.originAirport != null) draft = draft.copy(originAirport = null, origin = it.take(3).uppercase())
                        },
                    )
                    if (suggestions.isNotEmpty()) {
                        suggestions.take(5).forEach { airport ->
                            AirportSuggestion(airport) {
                                draft = draft.copy(origin = airport.iata, originAirport = airport)
                                originQuery = airport.compactTitle
                                suggestions = emptyList()
                            }
                        }
                    }
                    if (!draft.isWeekendUmrah) {
                        Text(tr(language, "Маршрут", "Route", "Yo‘nalish", "Йўналиш"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                        SegmentedChoice(
                            labels = listOf(
                                JourneyScope.MAKKAH_ONLY to tr(language,"Только Мекка","Makkah only","Faqat Makka","Фақат Макка"),
                                JourneyScope.MAKKAH_AND_MADINAH to tr(language,"Мекка + Медина","Makkah + Madinah","Makka + Madina","Макка + Мадина"),
                            ),
                            selected = draft.scope,
                            onSelect = { draft = draft.copy(scope = it) },
                        )
                        if (draft.scope == JourneyScope.MAKKAH_AND_MADINAH) {
                            Text(tr(language,"Аэропорт прибытия","Arrival airport","Yetib borish aeroporti","Етиб бориш аэропорти"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                            SegmentedChoice(
                                labels = listOf(
                                    SaudiArrivalAirport.JEDDAH to "JED · Jeddah",
                                    SaudiArrivalAirport.MADINAH to "MED · Madinah",
                                ),
                                selected = draft.arrivalAirport,
                                onSelect = { draft = draft.copy(arrivalAirport = it) },
                            )
                            Text(
                                if (draft.arrivalAirport == SaudiArrivalAirport.MADINAH)
                                    tr(language,"Прилёт в Медину, возвращение через Джидду.","Arrive in Madinah, return via Jeddah.","Madinaga yetib boring, Jidda orqali qayting.","Мадинага етиб боринг, Жидда орқали қайтинг.")
                                else tr(language,"Прилёт в Джидду, возвращение из Медины.","Arrive in Jeddah, return from Madinah.","Jiddaga yetib boring, Madinadan qayting.","Жиддага етиб боринг, Мадинадан қайтинг."),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .52f),
                            )
                        }
                    } else WeekendRouteSummary(draft.originCode)
                }
            }
        }
        item {
            GeneratorCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CardTitle(CupertinoSymbol.Airplane, tr(language,"Выберите перелёт и даты","Choose flights and dates","Parvoz va sanalarni tanlang","Парвоз ва саналарни танланг"))
                    Text(
                        tr(language,
                            "iumrah сохраняет единый маршрут туда и обратно. Даты можно изменить до выбора перелёта.",
                            "iumrah keeps one complete outbound-and-return itinerary. Dates can be changed before flight selection.",
                            "iumrah borish va qaytishni bitta to‘liq marshrut sifatida saqlaydi. Reys tanlashdan oldin sanalarni o‘zgartirish mumkin.",
                            "iumrah бориш ва қайтишни битта тўлиқ маршрут сифатида сақлайди. Рейс танлашдан олдин саналарни ўзгартириш мумкин."),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .56f),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeChip(tr(language,"Свои даты","Dates","Sanalar","Саналар"), CupertinoSymbol.CalendarClock, mode == GeneratorFlightChoice.FLEXIBLE_DATES, Modifier.weight(1f)) {
                            mode = GeneratorFlightChoice.FLEXIBLE_DATES
                            draft = draft.copy(flexibility = DateFlexibility.EXACT)
                        }
                        ModeChip(tr(language,"Weekend","Weekend","Weekend","Weekend"), null, mode == GeneratorFlightChoice.WEEKEND, Modifier.weight(1f)) {
                            mode = GeneratorFlightChoice.WEEKEND
                            draft = draft.withFlexibility(DateFlexibility.WEEKEND)
                        }
                    }
                    DateRangeRow(language, draft.departureDate, draft.returnDate, enabled = mode != GeneratorFlightChoice.WEEKEND,
                        onDeparture = { date -> draft = draft.copy(departureDate = date, returnDate = maxOf(draft.returnDate, date.plusDays(1)), flexibility = DateFlexibility.EXACT); mode = GeneratorFlightChoice.FLEXIBLE_DATES },
                        onReturn = { date -> if (date.isAfter(draft.departureDate)) { draft = draft.copy(returnDate = date, flexibility = DateFlexibility.EXACT); mode = GeneratorFlightChoice.FLEXIBLE_DATES } },
                    )
                    val noteColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .52f)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(CupertinoSymbol.CalendarClock, null, Modifier.size(18.dp), tint = noteColor)
                        Text(
                            tr(language,"Поиск выполняется по выбранным датам после подтверждения отелей.","Flight search runs for your selected dates after the hotels are confirmed.","Mehmonxonalar tasdiqlangach, tanlangan sanalar bo‘yicha reys qidiriladi.","Меҳмонхоналар тасдиқлангач, танланган саналар бўйича рейс қидирилади."),
                            fontSize = 12.sp, color = noteColor, lineHeight = 17.sp,
                        )
                    }
                }
            }
        }
        item {
            GeneratorCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CardTitle(CupertinoSymbol.Persons, tr(language,"Паломники","Travelers","Ziyoratchilar","Зиёратчилар"))
                    Text(tr(language,"Укажите всех участников и количество комнат.","Add every traveler and the number of rooms.","Barcha ziyoratchilar va xonalar sonini kiriting.","Барча зиёратчилар ва хоналар сонини киритинг."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.55f))
                    StepperRow(tr(language,"Взрослые","Adults","Kattalar","Катталар"), draft.adults, 1, 8) { draft = draft.copy(adults = it) }
                    StepperRow(tr(language,"Дети","Children","Bolalar","Болалар"), draft.children, 0, 7) { draft = draft.copy(children = it) }
                    StepperRow(tr(language,"Младенцы","Infants","Chaqaloqlar","Чақалоқлар"), draft.infants, 0, 4) { draft = draft.copy(infants = it) }
                    StepperRow(tr(language,"Комнаты","Rooms","Xonalar","Хоналар"), draft.rooms, 1, 4) { draft = draft.copy(rooms = it) }
                }
            }
        }
        item { FlightSearchFiltersCard(filters = draft.effectiveFlightFilters, infantCount = draft.infants, language = language, onChange = { draft = draft.copy(flightFilters = it) }) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(tr(language,"Уровень поездки","Trip level","Safar darajasi","Сафар даражаси"), fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(tr(language,"Уровень определяет Primary Hotels и включённые услуги.","Your level defines Primary Hotels and included services.","Daraja Primary Hotels va kiritilgan xizmatlarni belgilaydi.","Даража Primary Hotels ва киритилган хизматларни белгилайди."), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.55f))
                PackageTier.entries.forEach { tier -> TierCard(language, tier, draft.packageTier == tier) { draft = draft.copy(packageTier = tier, hotelStars = tierStars(tier)) } }
            }
        }
        item {
            GeneratorPrimaryButton(
                title = tr(language,"Продолжить к отелю","Continue to hotel","Mehmonxonaga davom etish","Меҳмонхонага давом этиш"),
                enabled = draft.canContinue,
            ) {
                journey.updateTrip(draft)
                chrome.openHotelSelection()
            }
        }
    }
}

@Composable private fun TripIntro(language: AppLanguage) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(tr(language,"ВАША УМРА","YOUR UMRAH","SIZNING UMRANGIZ","СИЗНИНГ УМРАНГИЗ"), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.52f))
        Text(tr(language,"Соберите свою поездку","Build your Umrah","Umra safaringizni tuzing","Умра сафарингизни тузинг"), fontSize = 33.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp)
        Text(tr(language,"Маршрут, даты, паломники и уровень пакета — iumrah соберёт всё остальное.","Route, dates, travelers and package level — iumrah builds the rest.","Yo‘nalish, sanalar, ziyoratchilar va paket darajasi — qolganini iumrah yig‘adi.","Йўналиш, саналар, зиёратчилар ва пакет даражаси — қолганини iumrah йиғади."), fontSize=16.sp, lineHeight=23.sp, color=MaterialTheme.colorScheme.onSurface.copy(alpha=.58f))
    }
}

@Composable private fun CardTitle(icon: CupertinoSymbol, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Icon(icon, null, Modifier.size(21.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun AirportSelector(query: String, onChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        singleLine = true,
        leadingIcon = { Icon(CupertinoSymbol.Location, null, Modifier.size(20.dp)) },
        shape = RoundedCornerShape(20.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = generatorRaisedColor(), unfocusedContainerColor = generatorRaisedColor(),
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
        ),
        placeholder = { Text("TAS · Tashkent") },
    )
}

@Composable private fun AirportSuggestion(airport: Airport, onClick: () -> Unit) {
    IumrahPressable(onClick = onClick, modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, background = generatorRaisedColor()) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(airport.compactTitle, fontSize=15.sp, fontWeight=FontWeight.SemiBold, maxLines=1, overflow=TextOverflow.Ellipsis)
                Text(airport.name, fontSize=12.sp, color=MaterialTheme.colorScheme.onSurface.copy(alpha=.5f), maxLines=1, overflow=TextOverflow.Ellipsis)
            }
            Icon(CupertinoSymbol.ChevronRight, null, Modifier.size(16.dp))
        }
    }
}

@Composable private fun <T> SegmentedChoice(labels: List<Pair<T,String>>, selected: T, onSelect: (T)->Unit) {
    val page = generatorPageColor(); val fg = if(page==Color.White) Color.Black else Color.White
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(generatorRaisedColor()).padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        labels.forEach { (value,label) ->
            val active = selected == value
            IumrahPressable(onClick={onSelect(value)}, modifier=Modifier.weight(1f).height(34.dp), cornerRadius=9.dp, background=if(active) generatorPageColor() else Color.Transparent) {
                Box(Modifier.fillMaxSize(), contentAlignment=Alignment.Center){ Text(label,fontSize=12.sp,fontWeight=if(active)FontWeight.SemiBold else FontWeight.Medium,color=fg,maxLines=1) }
            }
        }
    }
}

@Composable private fun ModeChip(title:String, icon:CupertinoSymbol?, selected:Boolean, modifier:Modifier=Modifier, onClick:()->Unit){
    val page=generatorPageColor(); val fg=if(page==Color.White)Color.Black else Color.White
    IumrahPressable(onClick=onClick,modifier=modifier.height(42.dp),cornerRadius=99.dp,background=if(selected)fg else generatorRaisedColor()){
        Row(Modifier.fillMaxSize().padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){
            if(icon!=null){Icon(icon,null,Modifier.size(15.dp),tint=if(selected)page else fg);Spacer(Modifier.width(5.dp))}
            Text(title,fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=if(selected)page else fg,maxLines=1)
        }
    }
}

@Composable private fun DateRangeRow(language:AppLanguage, departure:LocalDate, returnDate:LocalDate, enabled:Boolean,onDeparture:(LocalDate)->Unit,onReturn:(LocalDate)->Unit){
    val context=LocalContext.current
    val formatter=remember(language){ DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(language.localeTag)) }
    fun show(date:LocalDate,onPicked:(LocalDate)->Unit){ DatePickerDialog(context,{_,y,m,d->onPicked(LocalDate.of(y,m+1,d))},date.year,date.monthValue-1,date.dayOfMonth).apply{datePicker.minDate=System.currentTimeMillis()-86400000L}.show() }
    IumrahPressable(onClick={if(enabled)show(departure,onDeparture)},modifier=Modifier.fillMaxWidth().height(76.dp),cornerRadius=20.dp,background=generatorRaisedColor()){
        Row(Modifier.fillMaxSize().padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){
            DateColumn(tr(language,"Вылет","Departure","Jo‘nash","Жўнаш"),formatter.format(departure),Modifier.weight(1f))
            Icon(CupertinoSymbol.ArrowRight,null,Modifier.size(16.dp),tint=MaterialTheme.colorScheme.onSurface.copy(alpha=.5f))
            Box(Modifier.weight(1f)) { IumrahPressable(onClick={if(enabled)show(returnDate,onReturn)},modifier=Modifier.fillMaxSize(),cornerRadius=0.dp,background=Color.Transparent){ Box(Modifier.fillMaxSize(),contentAlignment=Alignment.CenterStart){DateColumn(tr(language,"Обратно","Return","Qaytish","Қайтиш"),formatter.format(returnDate))} } }
            Icon(CupertinoSymbol.ChevronRight,null,Modifier.size(15.dp),tint=MaterialTheme.colorScheme.onSurface.copy(alpha=.5f))
        }
    }
}
@Composable private fun DateColumn(title:String,value:String,modifier:Modifier=Modifier){Column(modifier,verticalArrangement=Arrangement.spacedBy(4.dp)){Text(title,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.5f));Text(value,fontSize=15.sp,fontWeight=FontWeight.Bold,maxLines=1)}}

@Composable private fun StepperRow(title:String,value:Int,min:Int,max:Int,onChange:(Int)->Unit){
    Row(Modifier.fillMaxWidth().height(50.dp),verticalAlignment=Alignment.CenterVertically){
        Text(title,fontSize=15.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));RoundStep(CupertinoSymbol.Minus,value>min){onChange(value-1)};Text(value.toString(),modifier=Modifier.width(42.dp),fontSize=17.sp,fontWeight=FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center);RoundStep(CupertinoSymbol.Plus,value<max){onChange(value+1)}
    }
}
@Composable private fun RoundStep(icon:CupertinoSymbol,enabled:Boolean,onClick:()->Unit){IumrahPressable(onClick=onClick,enabled=enabled,modifier=Modifier.size(34.dp),cornerRadius=99.dp,background=generatorRaisedColor().copy(alpha=if(enabled)1f else .4f)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Icon(icon,null,Modifier.size(15.dp),tint=MaterialTheme.colorScheme.onSurface.copy(alpha=if(enabled)1f else .35f))}}}

@Composable private fun TierCard(language:AppLanguage,tier:PackageTier,selected:Boolean,onClick:()->Unit){
    val dark=tier==PackageTier.LUXURY
    val shape=RoundedCornerShape(28.dp)
    val brush=when(tier){
        PackageTier.ECONOMY->Brush.linearGradient(listOf(Color(0xFFF4F5F7),Color(0xFFE7E9ED)))
        PackageTier.STANDARD->Brush.linearGradient(listOf(Color(0xFFFFFFFF),Color(0xFFF0F1F4)))
        PackageTier.COMFORT->Brush.linearGradient(listOf(Color(0xFFEAF4EE),Color(0xFFD8E9DF)))
        PackageTier.LUXURY->Brush.linearGradient(listOf(Color(0xFF171719),Color(0xFF2A2A2E)))
    }
    val fg=if(dark)Color.White else Color.Black
    IumrahPressable(onClick=onClick,modifier=Modifier.fillMaxWidth().then(if(selected)Modifier.border(2.dp,if(dark)Color.White else Color.Black,shape) else Modifier),cornerRadius=28.dp,background=Color.Transparent){
        Column(Modifier.fillMaxWidth().clip(shape).background(brush).padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Icon(tierIcon(tier),null,Modifier.size(26.dp),tint=fg);Spacer(Modifier.width(10.dp));Text(tierTitle(language,tier),fontSize=21.sp,fontWeight=FontWeight.Bold,color=fg);Spacer(Modifier.weight(1f));if(selected)Icon(CupertinoSymbol.CheckCircle,null,Modifier.size(22.dp),tint=fg)}
            Text(tierBody(language,tier),fontSize=13.sp,lineHeight=18.sp,color=fg.copy(alpha=.68f))
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){MiniPill("${tierStars(tier)}★",fg);MiniPill(if(tier==PackageTier.LUXURY)"VIP" else tr(language,"Primary Hotel","Primary Hotel","Primary Hotel","Primary Hotel"),fg)}
        }
    }
}
@Composable private fun MiniPill(text:String,fg:Color){Box(Modifier.clip(CircleShape).background(fg.copy(alpha=.09f)).padding(horizontal=10.dp,vertical=6.dp)){Text(text,fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=fg)}}
private fun tierStars(t:PackageTier)=when(t){PackageTier.ECONOMY->2;PackageTier.STANDARD->3;PackageTier.COMFORT->4;PackageTier.LUXURY->5}
private fun tierIcon(t:PackageTier)=when(t){PackageTier.ECONOMY->CupertinoSymbol.Wallet;PackageTier.STANDARD->CupertinoSymbol.Suitcase;PackageTier.COMFORT->CupertinoSymbol.Star;PackageTier.LUXURY->CupertinoSymbol.Sparkles}
private fun tierTitle(l:AppLanguage,t:PackageTier)=when(t){PackageTier.ECONOMY->tr(l,"Economy","Economy","Economy","Economy");PackageTier.STANDARD->tr(l,"Standard","Standard","Standard","Standard");PackageTier.COMFORT->tr(l,"Comfort","Comfort","Comfort","Comfort");PackageTier.LUXURY->tr(l,"Luxury","Luxury","Luxury","Luxury")}
private fun tierBody(l:AppLanguage,t:PackageTier)=when(t){
    PackageTier.ECONOMY->tr(l,"Практичный вариант с базовым размещением и основными услугами.","Practical travel with essential stay and services.","Asosiy yashash va xizmatlar bilan amaliy variant.","Асосий яшаш ва хизматлар билан амалий вариант.")
    PackageTier.STANDARD->tr(l,"Сбалансированный пакет для спокойной самостоятельной умры.","Balanced package for a calm independent Umrah.","Tinch mustaqil umra uchun muvozanatli paket.","Тинч мустақил умра учун мувозанатли пакет.")
    PackageTier.COMFORT->tr(l,"4★ Primary Hotels, больше комфорта и возможность добавить питание.","4★ Primary Hotels, more comfort and optional meals.","4★ Primary Hotels, ko‘proq qulaylik va qo‘shimcha ovqat.","4★ Primary Hotels, кўпроқ қулайлик ва қўшимча овқат.")
    PackageTier.LUXURY->tr(l,"5★ Primary Hotels, премиальные детали и VIP-трансфер по выбору.","5★ Primary Hotels, premium details and an optional VIP transfer.","5★ Primary Hotels, premium tafsilotlar va VIP transfer tanlovi.","5★ Primary Hotels, премиум тафсилотлар ва VIP трансфер танлови.")
}

@Composable private fun WeekendRouteSummary(origin:String){Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(generatorRaisedColor()).padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){RouteCode(origin, Modifier.weight(1f));Icon(CupertinoSymbol.ArrowRight,null,Modifier.size(14.dp));RouteCode("JED", Modifier.weight(1f));Icon(CupertinoSymbol.ArrowRight,null,Modifier.size(14.dp));RouteCode(origin, Modifier.weight(1f))}}
@Composable private fun RouteCode(code:String, modifier:Modifier=Modifier){Box(modifier.height(42.dp).clip(RoundedCornerShape(14.dp)).background(generatorPageColor()),contentAlignment=Alignment.Center){Text(code,fontSize=16.sp,fontWeight=FontWeight.Bold)}}

private fun tr(l:AppLanguage,ru:String,en:String,uz:String,uzCy:String)=when(l){AppLanguage.RUSSIAN->ru;AppLanguage.ENGLISH->en;AppLanguage.UZBEK->uz;AppLanguage.UZBEK_CYRILLIC->uzCy}
