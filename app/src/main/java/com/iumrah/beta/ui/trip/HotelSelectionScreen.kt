package com.iumrah.beta.ui.trip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.iumrah.beta.core.config.AppConfig
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.flight.CuratedFlightRecommendationService
import com.iumrah.beta.data.hotel.HotelCatalogService
import com.iumrah.beta.data.hotel.RemotePackageEngineClient
import com.iumrah.beta.domain.journey.JourneyStore
import com.iumrah.beta.domain.trip.JourneyScope
import com.iumrah.beta.domain.trip.PackageFlightPath
import com.iumrah.beta.domain.trip.PackageMealSelection
import com.iumrah.beta.domain.trip.PackageTier
import com.iumrah.beta.models.hotel.HotelSummary
import com.iumrah.beta.models.hotel.IumrahRoomCategoryOption
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
import kotlinx.coroutines.launch

/** iOS PrimaryHotelView parity layer for Android. */
@Composable
fun HotelSelectionScreen(
    language: AppLanguage,
    journey: JourneyStore,
    catalog: HotelCatalogService,
    packageEngine: RemotePackageEngineClient,
    curatedFlights: CuratedFlightRecommendationService,
    chrome: AppChromeStore,
) {
    val state by journey.state.collectAsState()
    val requiresMadinah = state.trip.scope == JourneyScope.MAKKAH_AND_MADINAH
    var makkahHotels by remember { mutableStateOf<List<HotelSummary>>(emptyList()) }
    var madinahHotels by remember { mutableStateOf<List<HotelSummary>>(emptyList()) }
    var makkahRooms by remember { mutableStateOf<List<IumrahRoomCategoryOption>>(emptyList()) }
    var madinahRooms by remember { mutableStateOf<List<IumrahRoomCategoryOption>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showMakkahAlternatives by remember { mutableStateOf(false) }
    var showMadinahAlternatives by remember { mutableStateOf(false) }
    var preparingDirectQuote by remember { mutableStateOf(false) }
    var directQuoteError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.trip.packageTier, state.trip.hotelStars, state.trip.scope) {
        loading = true
        error = null
        runCatching {
            val makkahList = catalog.listHotels(listOf("Makkah", "Mecca", "Makka"))
            makkahHotels = makkahList
            if (state.makkahHotel == null) {
                val primary = packageEngine.primaryHotel(state.trip.packageTier, state.trip.hotelStars, "Makkah")
                val hotel = makkahList.firstOrNull { it.id == primary.hotelId } ?: makkahList.firstOrNull()
                if (hotel != null) {
                    journey.selectHotel(hotel)
                    val rooms = packageEngine.roomCategories(hotel.id)
                    makkahRooms = rooms
                    rooms.firstOrNull { it.id == primary.roomId }?.let { journey.selectRoomCategory(it, false) }
                        ?: rooms.firstOrNull()?.let { journey.selectRoomCategory(it, false) }
                }
            }
            if (requiresMadinah) {
                val madinahList = catalog.listHotels(listOf(
                    "Madinah", "Medina", "Madina", "Medinah",
                    "Al Madinah", "Al Medina", "Madinah Al Munawwarah", "Al Madinah Al Munawwarah",
                ))
                madinahHotels = madinahList
                if (state.madinahHotel == null) {
                    val primary = packageEngine.primaryHotel(state.trip.packageTier, state.trip.hotelStars, "Madinah")
                    val hotel = madinahList.firstOrNull { it.id == primary.hotelId } ?: madinahList.firstOrNull()
                    if (hotel != null) {
                        journey.selectHotel(hotel)
                        val rooms = packageEngine.roomCategories(hotel.id)
                        madinahRooms = rooms
                        rooms.firstOrNull { it.id == primary.roomId }?.let { journey.selectRoomCategory(it, true) }
                            ?: rooms.firstOrNull()?.let { journey.selectRoomCategory(it, true) }
                    }
                }
            }
        }.onFailure { error = it.message ?: hotelTr(language, "Не удалось загрузить Primary Hotels.", "Could not load Primary Hotels.", "Primary Hotels yuklanmadi.", "Primary Hotels юкланмади.") }
        loading = false
    }

    LaunchedEffect(state.makkahHotel?.id) {
        val hotel = state.makkahHotel ?: return@LaunchedEffect
        makkahRooms = runCatching { packageEngine.roomCategories(hotel.id) }.getOrElse { emptyList() }
        if (!state.hasMakkahRoomSelection) makkahRooms.firstOrNull()?.let { journey.selectRoomCategory(it, false) }
    }
    LaunchedEffect(state.madinahHotel?.id) {
        val hotel = state.madinahHotel ?: return@LaunchedEffect
        madinahRooms = runCatching { packageEngine.roomCategories(hotel.id) }.getOrElse { emptyList() }
        if (!state.hasMadinahRoomSelection) madinahRooms.firstOrNull()?.let { journey.selectRoomCategory(it, true) }
    }

    val canContinue = state.makkahHotel != null && state.hasMakkahRoomSelection &&
        (!requiresMadinah || (state.madinahHotel != null && state.hasMadinahRoomSelection))

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(generatorPageColor()).statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = GeneratorGeometry.pagePadding, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item { GeneratorHeader(GeneratorStage.HOTEL, language, chrome) }
        item { HotelHeading(language) }
        if (loading && state.makkahHotel == null) {
            item { LoadingHotelCard(language) }
        } else {
            state.makkahHotel?.let { hotel ->
                item {
                    PrimaryStayCard(
                        language = language,
                        hotel = hotel,
                        room = state.makkahRoomCategory,
                        cityTitle = hotelTr(language, "Мекка", "Makkah", "Makka", "Макка"),
                        meals = state.trip.effectiveMealSelection,
                        isMadinah = false,
                        showMeals = state.trip.packageTier == PackageTier.COMFORT || state.trip.packageTier == PackageTier.LUXURY,
                        onMealChange = journey::setMealSelection,
                        onChangeHotel = { showMakkahAlternatives = !showMakkahAlternatives },
                        onRoom = { room -> journey.selectRoomCategory(room, false) },
                        rooms = makkahRooms,
                    )
                }
                item {
                    AnimatedVisibility(showMakkahAlternatives, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        AlternativeHotels(language, makkahHotels, hotel.id) { selected -> journey.selectHotel(selected); showMakkahAlternatives = false }
                    }
                }
            }
            if (requiresMadinah) {
                state.madinahHotel?.let { hotel ->
                    item {
                        PrimaryStayCard(
                            language = language,
                            hotel = hotel,
                            room = state.madinahRoomCategory,
                            cityTitle = hotelTr(language, "Медина", "Madinah", "Madina", "Мадина"),
                            meals = state.trip.effectiveMealSelection,
                            isMadinah = true,
                            showMeals = state.trip.packageTier == PackageTier.COMFORT || state.trip.packageTier == PackageTier.LUXURY,
                            onMealChange = journey::setMealSelection,
                            onChangeHotel = { showMadinahAlternatives = !showMadinahAlternatives },
                            onRoom = { room -> journey.selectRoomCategory(room, true) },
                            rooms = madinahRooms,
                        )
                    }
                    item {
                        AnimatedVisibility(showMadinahAlternatives, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            AlternativeHotels(language, madinahHotels, hotel.id) { selected -> journey.selectHotel(selected); showMadinahAlternatives = false }
                        }
                    }
                }
            }
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) } }
        directQuoteError?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        }
        item {
            val isPublishedDirect = state.packageFlightPath == PackageFlightPath.PUBLISHED_DIRECT
            GeneratorPrimaryButton(
                title = if (isPublishedDirect)
                    hotelTr(language, "Продолжить к трансферу", "Continue to transfer", "Transferga davom etish", "Трансферга давом этиш")
                else hotelTr(language, "Продолжить к перелёту", "Continue to flights", "Parvozga davom etish", "Парвозга давом этиш"),
                enabled = canContinue && !loading && !preparingDirectQuote && (!isPublishedDirect || state.hasCompletePublishedFlightSelection),
                onClick = {
                    if (!isPublishedDirect) {
                        chrome.openFlights()
                    } else {
                        scope.launch {
                            preparingDirectQuote = true
                            directQuoteError = null
                            journey.preparePublishedDirectPackage(curatedFlights, packageEngine)
                                .onSuccess { chrome.openTransferSelection() }
                                .onFailure { directQuoteError = it.message ?: hotelTr(language, "Не удалось подготовить пакет.", "Could not prepare the package.", "Paketni tayyorlab bo‘lmadi.", "Пакетни тайёрлаб бўлмади.") }
                            preparingDirectQuote = false
                        }
                    }
                },
            )
        }
        item { Spacer(Modifier.height(34.dp)) }
    }
}

@Composable private fun HotelHeading(language: AppLanguage) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(hotelTr(language, "PRIMARY HOTELS", "PRIMARY HOTELS", "PRIMARY HOTELS", "PRIMARY HOTELS"), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .5f))
        Text(hotelTr(language, "Ваши отели", "Your hotels", "Mehmonxonalaringiz", "Меҳмонхоналарингиз"), fontSize = 34.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.8).sp)
        Text(hotelTr(language, "Мы подобрали Primary Hotels вашего уровня. Можно оставить рекомендацию iumrah или выбрать другой вариант.", "We selected Primary Hotels for your trip level. Keep the iumrah recommendation or choose another stay.", "Safar darajangiz uchun Primary Hotels tanlandi. iumrah tavsiyasini qoldiring yoki boshqa variantni tanlang.", "Сафар даражангиз учун Primary Hotels танланди. iumrah тавсиясини қолдиринг ёки бошқа вариантни танланг."), fontSize = 16.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .56f))
    }
}

@Composable private fun LoadingHotelCard(language: AppLanguage) {
    GeneratorCard {
        Row(Modifier.fillMaxWidth().height(74.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.2.dp)
            Text(hotelTr(language, "Подбираем Primary Hotel…", "Finding your Primary Hotel…", "Primary Hotel tanlanmoqda…", "Primary Hotel танланмоқда…"), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
        }
    }
}

@Composable
private fun PrimaryStayCard(
    language: AppLanguage,
    hotel: HotelSummary,
    room: IumrahRoomCategoryOption?,
    cityTitle: String,
    meals: PackageMealSelection,
    isMadinah: Boolean,
    showMeals: Boolean,
    onMealChange: (PackageMealSelection) -> Unit,
    onChangeHotel: () -> Unit,
    onRoom: (IumrahRoomCategoryOption) -> Unit,
    rooms: List<IumrahRoomCategoryOption>,
) {
    val fg = MaterialTheme.colorScheme.onSurface
    val imageUrl = AppConfig.absoluteUrl(hotel.coverImageURL)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(com.iumrah.beta.ui.generator.generatorCardColor()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(214.dp).clip(RoundedCornerShape(24.dp)).background(generatorRaisedColor())) {
            if (imageUrl != null) AsyncImage(imageUrl, hotel.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(CupertinoSymbol.Hotel, null, Modifier.align(Alignment.Center).size(44.dp), tint = fg.copy(alpha=.45f))
            Row(Modifier.align(Alignment.TopStart).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                HotelPill(hotelTr(language, "Рекомендуем iumrah", "iumrah Recommended", "iumrah tavsiyasi", "iumrah тавсияси"), Color.White, Color.Black)
                HotelPill(cityTitle, Color.Black.copy(alpha=.72f), Color.White)
            }
        }
        Column(Modifier.padding(horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(hotel.name, fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.45).sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat((hotel.stars ?: 0).coerceIn(0,5)) { Icon(CupertinoSymbol.Star, null, Modifier.size(12.dp), tint = Color(0xFFFFB000)) }
                hotel.rating?.let { Text(String.format("%.1f", it), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg.copy(alpha=.58f)) }
            }
        }
        if (rooms.isNotEmpty()) {
            Column(Modifier.padding(horizontal=6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(hotelTr(language,"Номер","Room","Xona","Хона"), fontSize=11.sp, fontWeight=FontWeight.Bold, color=fg.copy(alpha=.5f), letterSpacing=.7.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    rooms.take(3).forEach { option ->
                        val active = room?.id == option.id
                        IumrahPressable(onClick={onRoom(option)}, modifier=Modifier.weight(1f).height(48.dp), cornerRadius=16.dp, background=if(active) fg else generatorRaisedColor()) {
                            Box(Modifier.fillMaxSize().padding(horizontal=6.dp), contentAlignment=Alignment.Center) {
                                Text(option.displayName, fontSize=11.sp, fontWeight=FontWeight.SemiBold, color=if(active) generatorPageColor() else fg, maxLines=2, overflow=TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        if (showMeals) MealPlanCard(language, meals, isMadinah, onMealChange)
        Row(Modifier.fillMaxWidth().padding(horizontal=6.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            IumrahPressable(onClick=onChangeHotel, modifier=Modifier.weight(1f).height(52.dp), cornerRadius=18.dp, background=generatorRaisedColor()) {
                Row(Modifier.fillMaxSize(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.Center) { Icon(CupertinoSymbol.Refresh,null,Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text(hotelTr(language,"Сменить отель","Change hotel","Mehmonxonani almashtirish","Меҳмонхонани алмаштириш"),fontSize=13.sp,fontWeight=FontWeight.SemiBold) }
            }
        }
    }
}

@Composable private fun MealPlanCard(language: AppLanguage, meals: PackageMealSelection, isMadinah: Boolean, onChange:(PackageMealSelection)->Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal=6.dp).clip(RoundedCornerShape(22.dp)).background(generatorRaisedColor()).padding(14.dp), verticalArrangement=Arrangement.spacedBy(11.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) { Icon(CupertinoSymbol.ForkKnife,null,Modifier.size(20.dp)); Spacer(Modifier.width(9.dp)); Column{Text(hotelTr(language,"Питание в отеле","Hotel meals","Mehmonxona ovqati","Меҳмонхона овқати"),fontSize=15.sp,fontWeight=FontWeight.Bold);Text(hotelTr(language,"Завтрак включён","Breakfast included","Nonushta kiritilgan","Нонушта киритилган"),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.5f))} }
        MealRow(hotelTr(language,"Завтрак","Breakfast","Nonushta","Нонушта"), true, enabled=false) {}
        if (!isMadinah) {
            MealRow(hotelTr(language,"Обед","Lunch","Tushlik","Тушлик"), meals.makkahLunch) { onChange(meals.copy(makkahLunch=it)) }
            MealRow(hotelTr(language,"Ужин","Dinner","Kechki ovqat","Кечки овқат"), meals.makkahDinner) { onChange(meals.copy(makkahDinner=it)) }
        } else {
            MealRow(hotelTr(language,"Ужин","Dinner","Kechki ovqat","Кечки овқат"), meals.madinahDinner) { onChange(meals.copy(madinahDinner=it)) }
        }
    }
}

@Composable private fun MealRow(title:String, selected:Boolean, enabled:Boolean=true, onChange:(Boolean)->Unit) {
    IumrahPressable(onClick={if(enabled)onChange(!selected)}, enabled=enabled, modifier=Modifier.fillMaxWidth().height(38.dp), cornerRadius=13.dp, background=Color.Transparent) {
        Row(Modifier.fillMaxSize(), verticalAlignment=Alignment.CenterVertically) { Text(title,fontSize=13.sp,fontWeight=FontWeight.Medium);Spacer(Modifier.weight(1f));IosToggle(selected, enabled) }
    }
}

@Composable private fun IosToggle(on:Boolean, enabled:Boolean=true) {
    val track = if(on) Color(0xFF34C759) else MaterialTheme.colorScheme.onSurface.copy(alpha=.16f)
    Box(Modifier.width(51.dp).height(31.dp).clip(CircleShape).background(track.copy(alpha=if(enabled)1f else .55f)).padding(2.dp)) {
        Box(Modifier.align(if(on)Alignment.CenterEnd else Alignment.CenterStart).size(27.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable private fun AlternativeHotels(language:AppLanguage, hotels:List<HotelSummary>, selectedId:String, onSelect:(HotelSummary)->Unit) {
    GeneratorCard {
        Column(verticalArrangement=Arrangement.spacedBy(11.dp)) {
            Text(hotelTr(language,"Другие отели","Other hotels","Boshqa mehmonxonalar","Бошқа меҳмонхоналар"),fontSize=17.sp,fontWeight=FontWeight.Bold)
            hotels.filter{it.id!=selectedId}.take(8).forEach { hotel ->
                IumrahPressable(onClick={onSelect(hotel)},modifier=Modifier.fillMaxWidth().height(68.dp),cornerRadius=19.dp,background=generatorRaisedColor()) {
                    Row(Modifier.fillMaxSize().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){
                        Box(Modifier.size(45.dp).clip(RoundedCornerShape(14.dp)).background(generatorPageColor()),contentAlignment=Alignment.Center){Icon(CupertinoSymbol.Hotel,null,Modifier.size(20.dp))}
                        Spacer(Modifier.width(11.dp));Column(Modifier.weight(1f)){Text(hotel.name,fontSize=14.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${hotel.stars ?: 0}★ · ${hotel.city}",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.5f))};Icon(CupertinoSymbol.ChevronRight,null,Modifier.size(16.dp),tint=MaterialTheme.colorScheme.onSurface.copy(alpha=.42f))
                    }
                }
            }
        }
    }
}

@Composable private fun HotelPill(text:String,bg:Color,fg:Color){Box(Modifier.clip(CircleShape).background(bg).padding(horizontal=10.dp,vertical=6.dp)){Text(text,fontSize=10.sp,fontWeight=FontWeight.Bold,color=fg)}}

private fun hotelTr(l:AppLanguage,ru:String,en:String,uz:String,uzCy:String)=when(l){AppLanguage.RUSSIAN->ru;AppLanguage.ENGLISH->en;AppLanguage.UZBEK->uz;AppLanguage.UZBEK_CYRILLIC->uzCy}
