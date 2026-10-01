package com.iumrah.beta.ui.home
import com.iumrah.beta.ui.cupertino.Icon

import com.iumrah.beta.ui.cupertino.CupertinoSymbol

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.iumrah.beta.R
import com.iumrah.beta.core.design.IumrahMotion
import com.iumrah.beta.core.design.IumrahHaptics
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.navigation.AppTab
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.hotel.HotelCatalogService
import com.iumrah.beta.data.notification.ClientNotificationStore
import com.iumrah.beta.domain.journey.JourneyStore
import com.iumrah.beta.models.hotel.StorefrontFlightOption
import com.iumrah.beta.models.hotel.StorefrontPackageSnapshot
import com.iumrah.beta.models.notification.ClientSystemNotification
import com.iumrah.beta.ui.components.IumrahPill
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.components.IumrahRootPageHeader
import com.iumrah.beta.ui.media.LoopingRawVideo
import coil3.compose.AsyncImage
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    language: AppLanguage,
    chrome: AppChromeStore,
    hotelCatalog: HotelCatalogService,
    journey: JourneyStore,
    notifications: ClientNotificationStore,
) {
    var showStory by remember { mutableStateOf(false) }
    val journeyState by journey.state.collectAsState()
    val notificationState by notifications.state.collectAsState()
    val storefrontOrigin = journeyState.trip.originCode.ifBlank { "TAS" }.uppercase()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(30.dp),
    ) {
        item {
            IumrahRootPageHeader(
                title = L10n.text("tab_home", language),
                chrome = chrome,
                usesBrandLogo = true,
                brandScale = 1.25f,
                showsConnectivityStatus = true,
                unreadCount = notificationState.unreadCount,
            )
        }
        if (notificationState.home.isNotEmpty()) {
            item { HomeNotificationCarousel(language, notificationState.home, chrome, notifications) }
        }
        item { EmotionalPrompt(language = language, onOpen = { showStory = true }) }
        item { HomeVideoCarousel() }
        item { AudienceSection(language) }
        item { ServicesSection(language, chrome) }
        item { ReadyPackagesSection(language, chrome, hotelCatalog, storefrontOrigin) }
        item { BuildMyUmrahSection(language, chrome) }
        item { HotelFirstPackagesSection(language, chrome, hotelCatalog, storefrontOrigin) }
        item { HomeIntegrationsSection(language, chrome) }
        item { ProductsSection(language, chrome) }
        item { ConfidenceStrip(language) }
        item { PhilosophyCard(language) }
        item { ConnectedTripCard(language) }
        item { PersonalUmrahFAQ(language) }
        item { AboutFooter(language) }
    }

    if (showStory) EmotionalJourneyFullscreen(language = language, onClose = { showStory = false })
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            title,
            fontSize = 31.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.8).sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .56f),
            )
        }
    }
}

@Composable
private fun HomeNotificationCarousel(
    language: AppLanguage,
    notifications: List<ClientSystemNotification>,
    chrome: AppChromeStore,
    store: ClientNotificationStore,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 1.dp)) {
        items(notifications.take(8), key = { it.id }) { item ->
            val accent = if (item.isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = .08f) else Color(0xFF34C759).copy(alpha = .13f)
            IumrahPressable(
                onClick = chrome::openNotifications,
                modifier = Modifier.width(318.dp),
                cornerRadius = 24.dp,
                background = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
            ) {
                Row(
                    Modifier.fillMaxWidth().background(accent).padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(if (item.isRead) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF34C759).copy(alpha = .16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(CupertinoSymbol.BellSignal, null, tint = if (item.isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = .56f) else Color(0xFF248A3D), modifier = Modifier.size(17.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.title, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                        Text(item.body, fontSize = 13.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f), maxLines = 3)
                    }
                    IumrahPressable(
                        onClick = { store.dismissFromHome(item.id) },
                        modifier = Modifier.size(30.dp),
                        cornerRadius = 99.dp,
                        background = MaterialTheme.colorScheme.onSurface.copy(alpha = .06f),
                        shadowElevation = 0.dp,
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(CupertinoSymbol.Close, tr(language, "Закрыть", "Close", "Yopish", "Ёпиш"), modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .56f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmotionalPrompt(language: AppLanguage, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            HomeEmotionalCopy.prompt(language),
            fontSize = 21.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.35).sp,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        IumrahPressable(
            onClick = onOpen,
            background = MaterialTheme.colorScheme.surface,
            cornerRadius = 999.dp,
            pressedScale = IumrahMotion.PressedScale,
            shadowElevation = 2.dp,
        ) {
            Row(
                Modifier.height(44.dp).padding(horizontal = 17.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(HomeEmotionalCopy.action(language), style = MaterialTheme.typography.labelLarge)
                Icon(CupertinoSymbol.Play, contentDescription = null, modifier = Modifier.size(17.dp))
            }
        }
    }
}

private data class AudienceItem(
    val icon: CupertinoSymbol,
    val title: String,
    val body: String,
    val background: Color,
    val foreground: Color,
)

private fun audienceItems(language: AppLanguage): List<AudienceItem> {
    val blueBg = Color(0xFFE8F2FF); val blueFg = Color(0xFF0D2957)
    val greenBg = Color(0xFFEFFAE9); val greenFg = Color(0xFF194721)
    val sandBg = Color(0xFFFAF0E3); val sandFg = Color(0xFF503012)
    return when (language) {
        AppLanguage.RUSSIAN -> listOf(
            AudienceItem(CupertinoSymbol.Sliders, "Соберите поездку сами", "Перелёт, отели, трансфер и сервисы — один персональный пакет Умры, который Вы собираете под себя.", blueBg, blueFg),
            AudienceItem(CupertinoSymbol.Persons, "Семья и близкие", "Организуйте Умру для семьи или друзей вместе, сохраняя приватность и удобный темп поездки.", greenBg, greenFg),
            AudienceItem(CupertinoSymbol.Star, "Индивидуальный и VIP", "Премиальные отели, приватный транспорт, индивидуальный сервис и максимум личного пространства.", sandBg, sandFg),
        )
        AppLanguage.ENGLISH -> listOf(
            AudienceItem(CupertinoSymbol.Sliders, "Build it your way", "Flights, hotels, transfer and services in one personal Umrah package you configure for yourself.", blueBg, blueFg),
            AudienceItem(CupertinoSymbol.Persons, "Family & friends", "Organize Umrah together while keeping the journey private, comfortable and paced around your group.", greenBg, greenFg),
            AudienceItem(CupertinoSymbol.Star, "Private & VIP", "Premium hotels, private transport, individual service and more personal space throughout the journey.", sandBg, sandFg),
        )
        AppLanguage.UZBEK -> listOf(
            AudienceItem(CupertinoSymbol.Sliders, "Safarni o‘zingiz tuzing", "Parvoz, mehmonxona, transfer va servislar — o‘zingizga mos bitta shaxsiy Umra paketi.", blueBg, blueFg),
            AudienceItem(CupertinoSymbol.Persons, "Oila va yaqinlar", "Oila yoki do‘stlar bilan guruhingizga mos, qulay va xususiy tempda Umra safarini tashkil qiling.", greenBg, greenFg),
            AudienceItem(CupertinoSymbol.Star, "Individual va VIP", "Premium mehmonxonalar, xususiy transport, individual servis va safar davomida maksimal maxfiylik.", sandBg, sandFg),
        )
        AppLanguage.UZBEK_CYRILLIC -> listOf(
            AudienceItem(CupertinoSymbol.Sliders, "Сафарни ўзингиз тузинг", "Парвоз, меҳмонхона, трансфер ва сервислар — ўзингизга мос битта шахсий Умра пакети.", blueBg, blueFg),
            AudienceItem(CupertinoSymbol.Persons, "Оила ва яқинлар", "Оила ёки дўстлар билан гуруҳингизга мос, қулай ва хусусий темпда Умра сафарини ташкил қилинг.", greenBg, greenFg),
            AudienceItem(CupertinoSymbol.Star, "Индивидуал ва VIP", "Премиум меҳмонхоналар, хусусий транспорт, индивидуал сервис ва сафар давомида максимал махфийлик.", sandBg, sandFg),
        )
    }
}

@Composable
private fun AudienceSection(language: AppLanguage) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        SectionHeader(
            when (language) {
                AppLanguage.RUSSIAN -> "Для кого создан Iumrah"
                AppLanguage.ENGLISH -> "Who Iumrah is for"
                AppLanguage.UZBEK -> "Iumrah kimlar uchun"
                AppLanguage.UZBEK_CYRILLIC -> "Iumrah кимлар учун"
            }
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 1.dp)) {
            items(audienceItems(language)) { item ->
                Column(
                    Modifier.width(286.dp).height(235.dp).clip(RoundedCornerShape(31.dp)).background(item.background).padding(20.dp),
                ) {
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(item.foreground.copy(alpha = .10f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(item.icon, contentDescription = null, tint = item.foreground, modifier = Modifier.size(24.dp)) }
                    Spacer(Modifier.weight(1f))
                    Text(item.title, fontSize = 23.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, color = item.foreground)
                    Spacer(Modifier.height(7.dp))
                    Text(item.body, fontSize = 14.sp, lineHeight = 19.sp, color = item.foreground.copy(alpha = .68f), maxLines = 4)
                }
            }
        }
    }
}

private data class ServiceItem(
    val images: List<Int>, val title: String, val body: String, val badge: String, val icon: CupertinoSymbol, val action: () -> Unit,
)

@Composable
private fun ServicesSection(language: AppLanguage, chrome: AppChromeStore) {
    val transferImages = listOf(R.drawable.transfer_carnival, R.drawable.transfer_malibu, R.drawable.transfer_yukon)
    val ziyaratImages = listOf(R.drawable.ziyarat_quba_1, R.drawable.ziyarat_quba_2, R.drawable.ziyarat_quba_3, R.drawable.ziyarat_quba_4, R.drawable.ziyarat_quba_5)
    val serviceItems = when (language) {
        AppLanguage.RUSSIAN -> listOf(
            ServiceItem(transferImages, "Iumrah Transfer", "Встреча в аэропорту и приватные поездки между ключевыми точками маршрута. Комфортный автомобиль под Ваш формат поездки.", "В пакете", CupertinoSymbol.Route, chrome::startNewTrip),
            ServiceItem(ziyaratImages, "Iumrah Ziyarat", "Места Мекки и Медины в одном маршруте. История, навигация и понятный порядок посещения без лишней суеты.", "Маршруты", CupertinoSymbol.Location, {}),
            ServiceItem(listOf(R.drawable.iumrah_esim_home_card), "Iumrah eSIM", "Интернет в Саудовской Аравии готов к подключению сразу после приземления — без поиска SIM-карты в аэропорту.", "Связь", CupertinoSymbol.LockShield, {}),
            ServiceItem(listOf(R.drawable.iumrah_flights_home_card), "Iumrah Flights", "Статус Вашего рейса в реальном времени: изменения времени, задержки и важные обновления поездки в одном месте.", "Live status", CupertinoSymbol.Airplane, chrome::openFlights),
            ServiceItem(listOf(R.drawable.iumrah_care_showcase), "Iumrah Care", "Человеческая поддержка, когда она действительно нужна: до поездки, в Саудовской Аравии и во время возвращения домой.", "Поддержка", CupertinoSymbol.Heart) { chrome.navigate(AppTab.CARE) },
        )
        AppLanguage.ENGLISH -> listOf(
            ServiceItem(transferImages, "Iumrah Transfer", "Airport pickup and private rides between key stops, with the right vehicle for your journey.", "Included", CupertinoSymbol.Route, chrome::startNewTrip),
            ServiceItem(ziyaratImages, "Iumrah Ziyarat", "Makkah and Madinah places in one route, with context, navigation and a clear visit sequence.", "Routes", CupertinoSymbol.Location, {}),
            ServiceItem(listOf(R.drawable.iumrah_esim_home_card), "Iumrah eSIM", "Saudi internet ready from arrival, without having to search for a local SIM card at the airport.", "Connectivity", CupertinoSymbol.LockShield, {}),
            ServiceItem(listOf(R.drawable.iumrah_flights_home_card), "Iumrah Flights", "Real-time flight status with schedule changes, delays and important journey updates in one place.", "Live status", CupertinoSymbol.Airplane, chrome::openFlights),
            ServiceItem(listOf(R.drawable.iumrah_care_showcase), "Iumrah Care", "Human support when it matters — before the trip, in Saudi Arabia and on the way home.", "Support", CupertinoSymbol.Heart) { chrome.navigate(AppTab.CARE) },
        )
        AppLanguage.UZBEK -> listOf(
            ServiceItem(transferImages, "Iumrah Transfer", "Aeroportdan kutib olish va yo‘nalishning muhim nuqtalari orasida safaringizga mos xususiy transport.", "Paketda", CupertinoSymbol.Route, chrome::startNewTrip),
            ServiceItem(ziyaratImages, "Iumrah Ziyarat", "Makka va Madina ziyorat joylari bitta yo‘nalishda: ma’lumot, navigatsiya va tushunarli tashrif tartibi.", "Yo‘nalishlar", CupertinoSymbol.Location, {}),
            ServiceItem(listOf(R.drawable.iumrah_esim_home_card), "Iumrah eSIM", "Saudiya Arabistonida internet qo‘nganingizdan boshlab tayyor — aeroportda SIM-karta izlash shart emas.", "Internet", CupertinoSymbol.LockShield, {}),
            ServiceItem(listOf(R.drawable.iumrah_flights_home_card), "Iumrah Flights", "Parvoz holati real vaqtda: vaqt o‘zgarishi, kechikish va safar uchun muhim yangilanishlar bir joyda.", "Live status", CupertinoSymbol.Airplane, chrome::openFlights),
            ServiceItem(listOf(R.drawable.iumrah_care_showcase), "Iumrah Care", "Kerak bo‘lgan paytda insoniy yordam — safardan oldin, Saudiya Arabistonida va uyga qaytishda.", "Yordam", CupertinoSymbol.Heart) { chrome.navigate(AppTab.CARE) },
        )
        AppLanguage.UZBEK_CYRILLIC -> listOf(
            ServiceItem(transferImages, "Iumrah Transfer", "Аэропортдан кутиб олиш ва йўналишнинг муҳим нуқталари орасида сафарингизга мос хусусий транспорт.", "Пакетда", CupertinoSymbol.Route, chrome::startNewTrip),
            ServiceItem(ziyaratImages, "Iumrah Ziyarat", "Макка ва Мадина зиёрат жойлари битта йўналишда: маълумот, навигация ва тушунарли ташриф тартиби.", "Йўналишлар", CupertinoSymbol.Location, {}),
            ServiceItem(listOf(R.drawable.iumrah_esim_home_card), "Iumrah eSIM", "Саудия Арабистонида интернет қўнганингиздан бошлаб тайёр — аэропортда SIM-карта излаш шарт эмас.", "Интернет", CupertinoSymbol.LockShield, {}),
            ServiceItem(listOf(R.drawable.iumrah_flights_home_card), "Iumrah Flights", "Парвоз ҳолати реал вақтда: вақт ўзгариши, кечикиш ва сафар учун муҳим янгиланишлар бир жойда.", "Live status", CupertinoSymbol.Airplane, chrome::openFlights),
            ServiceItem(listOf(R.drawable.iumrah_care_showcase), "Iumrah Care", "Керак бўлган пайтда инсоний ёрдам — сафардан олдин, Саудия Арабистонида ва уйга қайтишда.", "Ёрдам", CupertinoSymbol.Heart) { chrome.navigate(AppTab.CARE) },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        SectionHeader(
            title = when (language) {
                AppLanguage.RUSSIAN -> "Что входит в Iumrah Services"
                AppLanguage.ENGLISH -> "What’s inside Iumrah Services"
                AppLanguage.UZBEK -> "Iumrah Services nimalarni o‘z ichiga oladi"
                AppLanguage.UZBEK_CYRILLIC -> "Iumrah Services нималарни ўз ичига олади"
            },
            subtitle = when (language) {
                AppLanguage.RUSSIAN -> "Основные сервисы уже встроены в пакеты Iumrah и сопровождают поездку от вылета до возвращения."
                AppLanguage.ENGLISH -> "Core services are built into Iumrah packages and stay with the journey from departure to return."
                AppLanguage.UZBEK -> "Asosiy servislar Iumrah paketlariga kiritilgan va safarni uchishdan qaytishgacha kuzatadi."
                AppLanguage.UZBEK_CYRILLIC -> "Асосий сервислар Iumrah пакетларига киритилган ва сафарни учишдан қайтишгача кузатади."
            },
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(13.dp), contentPadding = PaddingValues(horizontal = 1.dp)) {
            items(serviceItems) { item -> ServiceCard(item) }
        }
    }
}

@Composable
private fun ServiceCard(item: ServiceItem) {
    val pager = rememberPagerState(pageCount = { item.images.size })
    IumrahPressable(onClick = item.action, modifier = Modifier.width(306.dp).height(455.dp), cornerRadius = 31.dp, shadowElevation = 8.dp) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            Box(Modifier.fillMaxWidth().height(246.dp)) {
                HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { index ->
                    Image(painterResource(item.images[index]), contentDescription = item.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                if (item.images.size > 1) {
                    Row(
                        Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).clip(RoundedCornerShape(99.dp)).background(Color.Black.copy(alpha = .26f)).padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        item.images.indices.forEach { index ->
                            Box(Modifier.size(if (index == pager.currentPage) 7.dp else 6.dp).clip(CircleShape).background(Color.White.copy(alpha = if (index == pager.currentPage) .96f else .48f)))
                        }
                    }
                }
            }
            Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(item.icon, contentDescription = null, tint = Color.Black.copy(alpha = .52f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(item.badge, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = .52f))
                    Spacer(Modifier.weight(1f))
                    Text("↗", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = .42f))
                }
                Text(item.title, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.55).sp, color = Color.Black)
                Text(item.body, fontSize = 14.sp, lineHeight = 19.sp, color = Color.Black.copy(alpha = .58f), maxLines = 4)
            }
        }
    }
}

@Composable
private fun ReadyPackagesSection(
    language: AppLanguage,
    chrome: AppChromeStore,
    service: HotelCatalogService,
    origin: String,
) {
    var options by remember(origin) { mutableStateOf<List<StorefrontFlightOption>>(emptyList()) }
    var packages by remember(origin) { mutableStateOf<List<StorefrontPackageSnapshot>>(emptyList()) }
    var loading by remember(origin) { mutableStateOf(true) }
    var failed by remember(origin) { mutableStateOf(false) }

    LaunchedEffect(origin) {
        loading = true
        failed = false
        val board = runCatching { service.storefrontFlightBoard(origin) }
        val packagePage = runCatching { service.storefrontPackages("flight-first", origin, 500) }
        options = board.getOrNull()?.options.orEmpty()
        packages = packagePage.getOrNull()?.items.orEmpty()
        failed = board.isFailure && packagePage.isFailure
        loading = false
    }

    val packagesByOffer = remember(packages) {
        val map = linkedMapOf<String, StorefrontPackageSnapshot>()
        packages.asSequence()
            .filter { it.entryMode == "flight-first" && it.status.equals("ready", true) }
            .filter { (it.pricePerPerson ?: 0.0) > 0.0 && (it.totalPackagePrice ?: 0.0) > 0.0 }
            .forEach { snapshot ->
                snapshot.outboundOfferId?.takeIf { it.isNotBlank() }?.let { if (map[it] == null) map[it] = snapshot }
                snapshot.inboundOfferId?.takeIf { it.isNotBlank() }?.let { if (map[it] == null) map[it] = snapshot }
            }
        map
    }
    val readyEntries = remember(options, packagesByOffer) {
        options.mapNotNull { option -> packagesByOffer[option.id]?.let { option to it } }.take(8)
    }

    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        SectionHeader(
            title = tr(language, "Готовые пакеты", "Ready-made packages", "Tayyor paketlar", "Тайёр пакетлар"),
            subtitle = tr(language,
                "Актуальные варианты перелёта уже собраны с отелем и сервисами в единую цену пакета.",
                "Current flight options are already combined with hotel and services into one package price.",
                "Amaldagi parvoz variantlari mehmonxona va servislar bilan bitta paket narxiga yig‘ilgan.",
                "Амалдаги парвоз вариантлари меҳмонхона ва сервислар билан битта пакет нархига йиғилган.")
        )

        when {
            readyEntries.isNotEmpty() -> {
                val rowState = rememberLazyListState()
                val selected by remember { derivedStateOf { rowState.firstVisibleItemIndex.coerceIn(0, readyEntries.size) } }
                LazyRow(state = rowState, horizontalArrangement = Arrangement.spacedBy(13.dp), contentPadding = PaddingValues(horizontal = 1.dp)) {
                    items(readyEntries, key = { it.second.id }) { (option, snapshot) ->
                        ReadyPackageCard(language, option, snapshot) { chrome.openFlightPackage(snapshot.id) }
                    }
                    item { AllFlightPackagesCard(language, chrome) }
                }
                HomeCarouselDots(readyEntries.size + 1, selected)
            }
            loading -> {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    androidx.compose.material3.CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(tr(language, "Подбираем актуальные пакеты", "Loading current packages", "Amaldagi paketlar yuklanmoqda", "Амалдаги пакетлар юкланмоқда"), fontWeight = FontWeight.Bold)
                        Text(tr(language, "Цены и рейсы обновляются из витрины Iumrah.", "Prices and flights are refreshing from the Iumrah storefront.", "Narxlar va reyslar Iumrah vitrinasidan yangilanmoqda.", "Нархлар ва рейслар Iumrah витринасидан янгиланмоқда."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .56f))
                    }
                }
            }
            failed -> {
                Text(
                    tr(language, "Не удалось обновить витрину пакетов. Откройте полный список и повторите.", "Could not refresh package storefront. Open the full list and retry.", "Paketlar vitrinasini yangilab bo‘lmadi. To‘liq ro‘yxatni ochib qayta urinib ko‘ring.", "Пакетлар витринасини янгилаб бўлмади. Тўлиқ рўйхатни очиб қайта уриниб кўринг."),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f),
                )
            }
        }

    }
}

@Composable
private fun AllFlightPackagesCard(language: AppLanguage, chrome: AppChromeStore) {
    IumrahPressable(onClick = chrome::openStorefrontFlights, modifier = Modifier.width(318.dp), cornerRadius = 28.dp, background = Color.White, shadowElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().background(Color.White)) {
            Image(painterResource(R.drawable.iumrah_flights_showcase), contentDescription = null, modifier = Modifier.fillMaxWidth().height(118.dp), contentScale = ContentScale.Crop)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(CupertinoSymbol.Airplane, null, tint = Color.Black.copy(alpha = .52f), modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Iumrah Flights", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = .52f))
                }
                Text(tr(language, "Больше вариантов поездки", "More journey options", "Ko‘proq safar variantlari", "Кўпроқ сафар вариантлари"), fontSize = 23.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(tr(language, "Откройте полную витрину авиабилетов и готовых пакетов Iumrah.", "Open the complete Iumrah flights and ready-package storefront.", "Iumrah parvozlari va tayyor paketlarining to‘liq vitrinasini oching.", "Iumrah парвозлари ва тайёр пакетларининг тўлиқ витринасини очинг."), fontSize = 14.sp, lineHeight = 19.sp, color = Color.Black.copy(alpha = .58f))
                Row(Modifier.fillMaxWidth().height(45.dp).clip(RoundedCornerShape(15.dp)).background(Color.Black).padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr(language, "Посмотреть все пакеты", "View all packages", "Barcha paketlarni ko‘rish", "Барча пакетларни кўриш"), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Icon(CupertinoSymbol.ArrowRight, null, tint = Color.White, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
private fun ReadyPackageCard(
    language: AppLanguage,
    option: StorefrontFlightOption,
    snapshot: StorefrontPackageSnapshot,
    onOpen: () -> Unit,
) {
    val image = snapshot.imageUrl ?: snapshot.hotelImages.firstOrNull()
    val route = option.inbound?.let { "${option.outbound.origin} → ${option.outbound.destination} · ${it.origin} → ${it.destination}" }
        ?: "${option.outbound.origin} → ${option.outbound.destination}"
    val airline = buildList {
        add("${option.outbound.airline} ${option.outbound.flightNumber}")
        option.inbound?.let { add("${it.airline} ${it.flightNumber}") }
    }.joinToString(" · ")
    val price = snapshot.pricePerPerson?.let { "$" + it.toInt().toString() } ?: "—"
    val routeSummary = snapshot.routeSummary ?: buildString {
        append(snapshot.totalDays?.let { "$it " + tr(language, "дн.", "days", "kun", "кун") } ?: "")
        snapshot.tier?.takeIf { it.isNotBlank() }?.let { if (isNotEmpty()) append(" · "); append(it.replaceFirstChar(Char::uppercase)) }
    }

    IumrahPressable(onClick = onOpen, modifier = Modifier.width(318.dp), cornerRadius = 28.dp, shadowElevation = 4.dp) {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
            Box(Modifier.fillMaxWidth().height(118.dp)) {
                if (!image.isNullOrBlank()) {
                    AsyncImage(model = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Image(painterResource(R.drawable.iumrah_flights_showcase), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .58f)))))
                snapshot.tier?.takeIf { it.isNotBlank() }?.let { tier ->
                    Text(
                        tier.replaceFirstChar(Char::uppercase),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(13.dp).clip(CircleShape).background(Color.Black.copy(alpha = .36f)).padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(route, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
                        Text(airline, fontSize = 11.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f), maxLines = 2)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(price, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(tr(language, "пакет · 1 человек", "package · 1 person", "paket · 1 kishi", "пакет · 1 киши"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .50f))
                    }
                }
                if (routeSummary.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(CupertinoSymbol.SuitcaseFill, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .50f))
                        Text(routeSummary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f), maxLines = 2)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IumrahPill(
                        tr(language, "Сгенерировано Iumrah Configurator", "Generated by Iumrah Configurator", "Iumrah Configurator yaratdi", "Iumrah Configurator яратди"),
                        background = MaterialTheme.colorScheme.surfaceVariant,
                        foreground = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f),
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(CupertinoSymbol.ChevronRight, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .35f))
                }
            }
        }
    }
}

@Composable
private fun HotelFirstPackagesSection(
    language: AppLanguage,
    chrome: AppChromeStore,
    service: HotelCatalogService,
    origin: String,
) {
    var packages by remember(origin) { mutableStateOf<List<StorefrontPackageSnapshot>>(emptyList()) }
    var loading by remember(origin) { mutableStateOf(true) }
    LaunchedEffect(origin) {
        loading = true
        packages = runCatching { service.storefrontPackages("hotel-first", origin, 500).items }
            .getOrDefault(emptyList())
            .filter { it.entryMode == "hotel-first" && it.status.equals("ready", true) }
            .distinctBy { it.hotelFirstAnchorHotelId ?: it.hotelName ?: it.id }
            .take(12)
        loading = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        SectionHeader(
            tr(language, "Пакеты с выбранным отелем", "Packages by hotel", "Mehmonxona bo‘yicha paketlar", "Меҳмонхона бўйича пакетлар"),
            tr(language, "Выберите отель — даты, перелёт и услуги уже собраны в готовый пакет.", "Choose the hotel — dates, flights and services are already assembled into a ready package.", "Mehmonxonani tanlang — sanalar, parvoz va xizmatlar tayyor paketga yig‘ilgan.", "Меҳмонхонани танланг — саналар, парвоз ва хизматлар тайёр пакетга йиғилган."),
        )
        if (packages.isEmpty() && loading) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                androidx.compose.material3.CircularProgressIndicator(Modifier.size(21.dp), strokeWidth = 2.dp)
                Text(tr(language, "Готовим пакеты по отелям…", "Preparing hotel packages…", "Mehmonxona paketlari tayyorlanmoqda…", "Меҳмонхона пакетлари тайёрланмоқда…"), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(13.dp), contentPadding = PaddingValues(horizontal = 1.dp)) {
                items(packages, key = { it.id }) { snapshot ->
                    HomeHotelFirstCard(language, snapshot) {
                        val id = snapshot.hotelFirstAnchorHotelId ?: snapshot.makkahHotelId ?: snapshot.madinahHotelId
                        if (!id.isNullOrBlank()) chrome.openHotel(id) else chrome.navigate(AppTab.HOTELS)
                    }
                }
                item { AllHotelPackagesCard(language) { chrome.navigate(AppTab.HOTELS) } }
            }
        }
    }
}

@Composable
private fun HomeHotelFirstCard(language: AppLanguage, snapshot: StorefrontPackageSnapshot, onOpen: () -> Unit) {
    val image = snapshot.imageUrl ?: snapshot.hotelImages.firstOrNull()
    val price = snapshot.pricePerPerson?.let { "$${it.toInt()}" } ?: "—"
    IumrahPressable(onClick = onOpen, modifier = Modifier.width(340.dp).height(204.dp), cornerRadius = 28.dp, background = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        Row(Modifier.fillMaxSize()) {
            if (!image.isNullOrBlank()) AsyncImage(model = image, contentDescription = null, modifier = Modifier.width(116.dp).fillMaxHeight(), contentScale = ContentScale.Crop)
            else Image(painterResource(R.drawable.iumrah_hotels_showcase), null, Modifier.width(116.dp).fillMaxHeight(), contentScale = ContentScale.Crop)
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(snapshot.hotelCity ?: "Iumrah Hotels", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = .35.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .48f))
                Text(snapshot.hotelName ?: tr(language, "Отель", "Hotel", "Mehmonxona", "Меҳмонхона"), fontSize = 20.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                snapshot.routeSummary?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .56f), maxLines = 2) }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(tr(language, "от", "from", "dan", "дан"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .44f))
                        Text(price, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(CupertinoSymbol.ArrowRight, null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f))
                }
            }
        }
    }
}

@Composable
private fun AllHotelPackagesCard(language: AppLanguage, onOpen: () -> Unit) {
    IumrahPressable(onClick = onOpen, modifier = Modifier.width(340.dp).height(204.dp), cornerRadius = 28.dp, background = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        Column(Modifier.fillMaxSize()) {
            Image(painterResource(R.drawable.iumrah_hotels_showcase), null, Modifier.fillMaxWidth().height(102.dp), contentScale = ContentScale.Crop)
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr(language, "Посмотреть все пакеты по отелям", "See all hotel packages", "Barcha mehmonxona paketlarini ko‘rish", "Барча меҳмонхона пакетларини кўриш"), fontSize = 20.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr(language, "Все пакеты", "All packages", "Barcha paketlar", "Барча пакетлар"), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(CupertinoSymbol.ArrowRight, null, Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeIntegrationsSection(language: AppLanguage, chrome: AppChromeStore) {
    val available = LocalConfiguration.current.screenWidthDp.dp - 36.dp
    val cardWidth = (available * .88f).coerceAtLeast(286.dp).coerceAtMost(available)
    val rowState = rememberLazyListState()
    val selected by remember { derivedStateOf { rowState.firstVisibleItemIndex.coerceIn(0, 2) } }
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        SectionHeader(
            tr(language, "Интеграции", "Integrations", "Integratsiyalar", "Интеграциялар"),
            tr(language, "Планируйте следующую Umrah, подключайте Telegram и используйте новые возможности iumrah в одном месте.", "Plan your next Umrah, connect Telegram and access new iumrah integrations in one place.", "Keyingi Umrani rejalashtiring, Telegram’ni ulang va yangi iumrah integratsiyalaridan bir joyda foydalaning.", "Кейинги Умрани режалаштиринг, Telegram’ни уланг ва янги iumrah интеграцияларидан бир жойда фойдаланинг."),
        )
        LazyRow(state = rowState, horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(horizontal = 1.dp)) {
            item { IntegrationCard(language, cardWidth, R.drawable.home_integration_calendar, tr(language, "ПЛАНИРОВАНИЕ", "PLANNING", "REJALASHTIRISH", "РЕЖАЛАШТИРИШ"), tr(language, "Запланировать Umrah", "Plan your Umrah", "Umrani rejalashtirish", "Умрани режалаштириш"), tr(language, "Выберите будущие даты и настройте напоминания за 2 месяца, месяц и последние дни перед поездкой.", "Choose future dates and set reminders for two months, one month and the final days before departure.", "Kelajakdagi sanalarni tanlang va safargacha 2 oy, 1 oy hamda so‘nggi kunlar uchun eslatmalarni sozlang.", "Келажакдаги саналарни танланг ва сафаргача 2 ой, 1 ой ҳамда сўнгги кунлар учун эслатмаларни созланг."), tr(language, "Запланировать", "Plan trip", "Rejalashtirish", "Режалаштириш")) { chrome.startNewTrip() } }
            item { IntegrationCard(language, cardWidth, R.drawable.telegram_integration_hero, "Telegram", tr(language, "Статус бронирования в Telegram", "Booking status in Telegram", "Bron holati Telegram’da", "Брон ҳолати Telegram’да"), tr(language, "Получайте изменения статуса, оплаты, подтверждения и документов прямо в Telegram.", "Receive status, payment, confirmation and document updates directly in Telegram.", "Status, to‘lov, tasdiq va hujjat yangilanishlarini to‘g‘ridan-to‘g‘ri Telegram’da oling.", "Статус, тўлов, тасдиқ ва ҳужжат янгиланишларини тўғридан-тўғри Telegram’да олинг."), tr(language, "Открыть Telegram", "Open Telegram", "Telegram’ni ochish", "Telegram’ни очиш")) { chrome.navigate(AppTab.ACCOUNT) } }
            item { IntegrationCard(language, cardWidth, R.drawable.home_integration_soon, tr(language, "СКОРО", "COMING SOON", "TEZ ORADA", "ТЕЗ ОРАДА"), tr(language, "Следующая интеграция", "Next integration", "Keyingi integratsiya", "Кейинги интеграция"), tr(language, "Мы готовим ещё один способ связать iumrah с сервисами, которыми Вы пользуетесь каждый день.", "We are preparing another way to connect iumrah with the services you use every day.", "iumrah’ni har kuni foydalanadigan servislaringiz bilan bog‘lashning yana bir usulini tayyorlayapmiz.", "iumrah’ни ҳар куни фойдаланадиган сервисларингиз билан боғлашнинг яна бир усулини тайёрлаяпмиз."), null, null) }
        }
        HomeCarouselDots(3, selected)
    }
}

@Composable
private fun IntegrationCard(language: AppLanguage, width: androidx.compose.ui.unit.Dp, imageRes: Int, badge: String, title: String, body: String, cta: String?, onOpen: (() -> Unit)?) {
    val content: @Composable () -> Unit = {
        Column(Modifier.width(width).height(366.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surface)) {
            Image(painterResource(imageRes), null, Modifier.fillMaxWidth().height(176.dp), contentScale = ContentScale.Crop)
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(badge, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = .5.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .52f))
                Text(title, fontSize = 23.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.45).sp, maxLines = 2)
                Text(body, fontSize = 14.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f), maxLines = 3)
                Spacer(Modifier.weight(1f))
                if (cta != null) DarkCTA(cta)
            }
        }
    }
    if (onOpen != null) IumrahPressable(onClick = onOpen, modifier = Modifier.width(width).height(366.dp), cornerRadius = 28.dp, shadowElevation = 2.dp) { content() } else content()
}

@Composable
private fun BuildMyUmrahSection(language: AppLanguage, chrome: AppChromeStore) {
    val available = LocalConfiguration.current.screenWidthDp.dp - 36.dp
    val cardWidth = (available * .94f).coerceIn(298.dp, 352.dp)
    val rowState = rememberLazyListState()
    val selected by remember { derivedStateOf { rowState.firstVisibleItemIndex.coerceIn(0, 1) } }
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        SectionHeader(
            tr(language, "Собрать свою Умру", "Build your Umrah", "Umrangizni tuzing", "Умрангизни тузинг"),
            tr(language, "Соберите пакет сами за несколько минут или передайте подбор Iumrah Care.", "Build the package yourself in minutes or let Iumrah Care prepare it for you.", "Paketni bir necha daqiqada o‘zingiz tuzing yoki tanlovni Iumrah Care’ga topshiring.", "Пакетни бир неча дақиқада ўзингиз тузинг ёки танловни Iumrah Care’га топширинг.")
        )
        LazyRow(state = rowState, horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 1.dp)) {
            item { ConfiguratorCard(language, chrome, cardWidth) }
            item { CareBuilderCard(language, cardWidth) { chrome.navigate(AppTab.CARE) } }
        }
        HomeCarouselDots(2, selected)
    }
}

@Composable
private fun ConfiguratorCard(language: AppLanguage, chrome: AppChromeStore, cardWidth: androidx.compose.ui.unit.Dp) {
    IumrahPressable(onClick = chrome::startNewTrip, modifier = Modifier.width(cardWidth).height(540.dp), cornerRadius = 34.dp, background = Color.Black, shadowElevation = 12.dp) {
        Column(Modifier.fillMaxSize().background(Color.Black)) {
            Image(painterResource(R.drawable.store_configurator_phones), null, Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Crop)
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(CupertinoSymbol.Sliders, null, tint = Color.White.copy(alpha = .78f), modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp))
                    Text("Iumrah Configurator", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = .45.sp, color = Color.White.copy(alpha = .78f))
                    Spacer(Modifier.weight(1f))
                    IumrahPill(tr(language, "≈ 5 минут", "≈ 5 min", "≈ 5 daqiqa", "≈ 5 дақиқа"), background = Color.White.copy(alpha = .10f), foreground = Color.White.copy(alpha = .82f))
                }
                Text(tr(language, "Соберите свою Умру за 5 минут", "Build your Umrah in 5 minutes", "Umrangizni 5 daqiqada tuzing", "Умрангизни 5 дақиқада тузинг"), fontSize = 31.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.75).sp, color = Color.White)
                Text(tr(language,
                    "Персональный пакет для вас, вашей семьи или друзей — без обязательной туристической группы из 30–50 человек. Перелёт, отель, трансфер и Iumrah Services собираются в одну поездку.",
                    "A personal package for you, your family or friends — without having to join a 30–50 person tour group. Flights, hotel, transfer and Iumrah Services come together as one journey.",
                    "Siz, oilangiz yoki do‘stlaringiz uchun shaxsiy paket — 30–50 kishilik majburiy tur guruhisiz. Parvoz, mehmonxona, transfer va Iumrah Services bitta safarga birlashadi.",
                    "Сиз, оилангиз ёки дўстларингиз учун шахсий пакет — 30–50 кишилик мажбурий тур гуруҳисиз. Парвоз, меҳмонхона, трансфер ва Iumrah Services битта сафарга бирлашади."),
                    fontSize = 15.sp, lineHeight = 20.sp, color = Color.White.copy(alpha = .68f), maxLines = 4)
                Spacer(Modifier.weight(1f))
                LightCTA(tr(language, "Создать мою Умру", "Create my Umrah", "Umramni yaratish", "Умрамни яратиш"))
            }
        }
    }
}

@Composable
private fun CareBuilderCard(language: AppLanguage, cardWidth: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    IumrahPressable(onClick = onClick, modifier = Modifier.width(cardWidth).height(540.dp), cornerRadius = 34.dp, shadowElevation = 10.dp) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            Image(painterResource(R.drawable.iumrah_care_showcase), null, Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Crop)
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(CupertinoSymbol.Heart, null, tint = Color.Black.copy(alpha = .58f), modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp))
                    Text("Iumrah Care", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = .45.sp, color = Color.Black.copy(alpha = .58f))
                    Spacer(Modifier.weight(1f))
                    IumrahPill(tr(language, "ответ ≤ 2 ч", "reply ≤ 2h", "javob ≤ 2 soat", "жавоб ≤ 2 соат"), background = Color.Black.copy(alpha = .055f), foreground = Color.Black.copy(alpha = .62f))
                }
                Text(tr(language, "Передайте подбор Iumrah Care", "Let Iumrah Care arrange it", "Tanlovni Iumrah Care’ga topshiring", "Танловни Iumrah Care’га топширинг"), fontSize = 31.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.75).sp, color = Color.Black)
                Text(tr(language, "Расскажите о датах, составе поездки и предпочтениях — Care поможет собрать персональный вариант без большой обязательной группы.", "Share your dates, travelers and preferences — Care will help assemble a personal option without a required large group.", "Sana, sayohatchilar va istaklaringizni ayting — Care katta majburiy guruhsiz shaxsiy variant tuzishga yordam beradi.", "Сана, саёҳатчилар ва истакларингизни айтинг — Care катта мажбурий гуруҳсиз шахсий вариант тузишга ёрдам беради."), fontSize = 15.sp, lineHeight = 20.sp, color = Color.Black.copy(alpha = .62f), maxLines = 4)
                Spacer(Modifier.weight(1f))
                DarkCTA(tr(language, "Запросить подбор", "Request help", "Tanlov so‘rash", "Танлов сўраш"))
            }
        }
    }
}

@Composable
private fun ProductsSection(language: AppLanguage, chrome: AppChromeStore) {
    val available = LocalConfiguration.current.screenWidthDp.dp - 36.dp
    val cardWidth = (available * .88f).coerceAtLeast(286.dp).coerceAtMost(available)
    val rowState = rememberLazyListState()
    val selected by remember { derivedStateOf { rowState.firstVisibleItemIndex.coerceIn(0, 2) } }
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        SectionHeader(tr(language, "Наши продукты", "Our products", "Mahsulotlarimiz", "Маҳсулотларимиз"))
        LazyRow(state = rowState, horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 1.dp)) {
            item { BackendSystemCard(language, cardWidth) }
            item { AdvisorCard(language, chrome, cardWidth) }
            item { SundayUmrahClubCard(language, chrome, cardWidth) }
        }
        HomeCarouselDots(3, selected)
    }
}

@Composable
private fun SundayUmrahClubCard(language: AppLanguage, chrome: AppChromeStore, cardWidth: androidx.compose.ui.unit.Dp) {
    IumrahPressable(onClick = chrome::openSundayClub, modifier = Modifier.width(cardWidth).height(472.dp), cornerRadius = 34.dp, background = Color.White, shadowElevation = 4.dp) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            Box(Modifier.fillMaxWidth().height(236.dp).background(Color.White), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.sunday_umrah_club_home), null, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp), contentScale = ContentScale.Fit)
            }
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sunday Umrah Club", fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.6).sp, color = Color.Black)
                Text(tr(language, "Короткая Умра на выходные: готовые даты, удобный маршрут и минимум времени вне работы.", "A weekend-sized Umrah with ready dates, a compact route and less time away from work.", "Dam olish kunlariga mos qisqa Umra: tayyor sanalar, qulay yo‘nalish va ishdan kamroq uzilish.", "Дам олиш кунларига мос қисқа Умра: тайёр саналар, қулай йўналиш ва ишдан камроқ узилиш."), fontSize = 15.sp, lineHeight = 20.sp, color = Color.Black.copy(alpha = .60f), maxLines = 4)
                Spacer(Modifier.weight(1f))
                DarkCTA(tr(language, "Открыть Sunday Club", "Open Sunday Club", "Sunday Club’ni ochish", "Sunday Club’ни очиш"))
            }
        }
    }
}

@Composable
private fun BackendSystemCard(language: AppLanguage, cardWidth: androidx.compose.ui.unit.Dp) {
    val purple = Color(0xFF5C38E0)
    Column(
        Modifier.width(cardWidth).height(472.dp).clip(RoundedCornerShape(34.dp)).background(
            Brush.verticalGradient(listOf(Color(0xFF07070A), Color(0xFF0C0A13), Color(0xFF18102A)))
        ).padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("IUMRAH SYSTEM", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = .7.sp, color = Color.White.copy(alpha = .62f))
            Spacer(Modifier.weight(1f)); Text("↗", color = Color.White.copy(alpha = .82f), fontSize = 18.sp)
        }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
            listOf(118, 86, 54).forEachIndexed { i, size ->
                Box(Modifier.size(size.dp).clip(CircleShape).background(purple.copy(alpha = .10f + i * .04f)))
            }
            Icon(CupertinoSymbol.Route, null, tint = Color.White.copy(alpha = .92f), modifier = Modifier.size(44.dp))
        }
        Text(tr(language, "Одна система для всей поездки", "One system for the whole journey", "Butun safar uchun bitta tizim", "Бутун сафар учун битта тизим"), fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.65).sp, color = Color.White)
        Spacer(Modifier.height(9.dp))
        Text(tr(language, "Перелёт, отель, бронь, Care и статусы остаются связанными внутри одной поездки.", "Flights, hotel, booking, Care and statuses stay connected inside one journey.", "Parvoz, mehmonxona, bron, Care va statuslar bitta safarda bog‘langan qoladi.", "Парвоз, меҳмонхона, брон, Care ва статуслар битта сафарда боғланган қолади."), fontSize = 14.sp, lineHeight = 19.sp, color = Color.White.copy(alpha = .62f))
    }
}

@Composable
private fun AdvisorCard(language: AppLanguage, chrome: AppChromeStore, cardWidth: androidx.compose.ui.unit.Dp) {
    val aura = Brush.linearGradient(listOf(Color(0xFF102B27), Color(0xFF1A4B42), Color(0xFF13232D), Color.Black))
    IumrahPressable(onClick = { chrome.navigate(AppTab.CARE) }, modifier = Modifier.width(cardWidth).height(472.dp), cornerRadius = 34.dp, background = Color.Black) {
        Box(Modifier.fillMaxSize().background(aura).padding(20.dp)) {
            Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(CupertinoSymbol.Speaker, null, tint = Color.White.copy(alpha = .92f), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("iumrah Advisor", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = .45.sp, color = Color.White.copy(alpha = .92f))
                    Spacer(Modifier.weight(1f)); Text("↗", color = Color.White.copy(alpha = .92f), fontSize = 18.sp)
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(106.dp).clip(CircleShape).background(Color.White.copy(alpha = .075f)), contentAlignment = Alignment.Center) {
                        Icon(CupertinoSymbol.Speaker, null, tint = Color.White.copy(alpha = .92f), modifier = Modifier.size(42.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(tr(language, "Голосовой iumrah Advisor", "Voice iumrah Advisor", "Ovozli iumrah Advisor", "Овозли iumrah Advisor"), fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.65).sp, color = Color.White)
                Spacer(Modifier.height(9.dp))
                Text(tr(language, "Пошаговый голосовой гид по Умре с поддержкой нескольких языков, чтобы паломник не оставался один во время ритуалов.", "A step-by-step voice guide for Umrah in multiple languages, so the pilgrim is not left alone during the rituals.", "Umra marosimlari davomida ziyoratchi yolg‘iz qolmasligi uchun bir nechta tillarda bosqichma-bosqich ovozli gid.", "Умра маросимлари давомида зиёратчи ёлғиз қолмаслиги учун бир нечта тилларда босқичма-босқич овозли гид."), fontSize = 14.sp, lineHeight = 19.sp, color = Color.White.copy(alpha = .70f), maxLines = 3)
                Spacer(Modifier.height(12.dp))
                LightCTA(tr(language, "Открыть Advisor", "Open Advisor", "Advisorni ochish", "Advisorни очиш"))
            }
        }
    }
}

@Composable
private fun ConfidenceStrip(language: AppLanguage) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SmallChip(CupertinoSymbol.Hotel, L10n.text("tab_hotels", language)) }
        item { SmallChip(CupertinoSymbol.Airplane, L10n.text("step_flight", language)) }
        item { SmallChip(CupertinoSymbol.Heart, "iumrah Care") }
    }
}

@Composable
private fun SmallChip(icon: CupertinoSymbol, text: String) {
    Row(
        Modifier.height(40.dp).clip(RoundedCornerShape(99.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) { Icon(icon, null, modifier = Modifier.size(15.dp)); Text(text, style = MaterialTheme.typography.labelMedium) }
}

@Composable
private fun PhilosophyCard(language: AppLanguage) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(MaterialTheme.colorScheme.surface).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IumrahPill("iumrah")
        Text(L10n.text("home_philosophy_title", language), fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
        Text(L10n.text("home_philosophy_body", language), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
    }
}

@Composable
private fun ConnectedTripCard(language: AppLanguage) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(MaterialTheme.colorScheme.surface).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            JourneyIcon(CupertinoSymbol.Airplane); Connector(Modifier.weight(1f)); JourneyIcon(CupertinoSymbol.Hotel); Connector(Modifier.weight(1f)); JourneyIcon(CupertinoSymbol.Route); Connector(Modifier.weight(1f)); JourneyIcon(CupertinoSymbol.Star); Connector(Modifier.weight(1f)); JourneyIcon(CupertinoSymbol.Heart)
        }
        Text(L10n.text("home_connected_title", language), fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold)
        Text(L10n.text("home_connected_body", language), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
    }
}

@Composable private fun JourneyIcon(icon: CupertinoSymbol) { Box(Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(14.dp)) } }
@Composable private fun Connector(modifier: Modifier = Modifier) { Box(modifier.padding(horizontal = 5.dp).height(2.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = .10f))) }

private data class FAQItem(val q: String, val a: String)

private fun faqItems(language: AppLanguage): List<FAQItem> = when (language) {
    AppLanguage.RUSSIAN -> listOf(
        FAQItem("Что такое iumrah?", "iumrah — платформа для самостоятельной и персональной Умры. Она помогает собрать перелёт, отель, трансфер и сервисы в один понятный пакет и затем вести поездку в одном приложении."),
        FAQItem("Почему был создан iumrah?", "Чтобы паломнику не приходилось зависеть от большой туристической группы или разбираться в десятках разрозненных бронирований. Идея iumrah — дать больше контроля, прозрачности и заботы на каждом этапе поездки."),
        FAQItem("Что значит «персональная Умра»?", "Поездка собирается вокруг вас: ваших дат, бюджета, уровня отеля и выбранных услуг. Это не обязательная группа из 30–50 незнакомых людей — вы сами выбираете, с кем совершать Умру."),
        FAQItem("Можно поехать только с семьёй или друзьями?", "Да. Пакет можно собрать для одного человека, пары, семьи или друзей. В поездке остаются только те люди, которых вы сами добавили."),
        FAQItem("А если я не хочу собирать всё самостоятельно?", "Обратитесь в iumrah Care. Мы поможем подобрать вариант, проверить детали и оформить поездку, сохранив персональный формат без обязательной большой группы."),
    )
    AppLanguage.ENGLISH -> listOf(
        FAQItem("What is iumrah?", "iumrah is a platform for independent, personal Umrah. It brings flights, hotel, transfer and services into one clear package and then keeps the journey in one app."),
        FAQItem("Why was iumrah created?", "So a pilgrim does not have to depend on a large tour group or manage many disconnected bookings. iumrah is built around more control, transparency and care throughout the journey."),
        FAQItem("What does ‘personal Umrah’ mean?", "The journey is built around your dates, budget, hotel level and chosen services. There is no required group of 30–50 strangers — you decide who travels with you."),
        FAQItem("Can I travel only with family or friends?", "Yes. Build a package for one person, a couple, family or friends. Your journey contains only the people you choose to add."),
        FAQItem("What if I do not want to build everything myself?", "Contact iumrah Care. We can help select, verify and arrange the trip while keeping the personal format without a required large group."),
    )
    AppLanguage.UZBEK -> listOf(
        FAQItem("iumrah nima?", "iumrah — mustaqil va shaxsiy Umra uchun platforma. U parvoz, mehmonxona, transfer va xizmatlarni bitta tushunarli paketga birlashtiradi va safarni bitta ilovada boshqarishga yordam beradi."),
        FAQItem("iumrah nima uchun yaratildi?", "Ziyoratchi katta tur guruhiga bog‘lanib qolmasligi va ko‘plab alohida bronlarni boshqarmasligi uchun. iumrah safar davomida ko‘proq nazorat, shaffoflik va g‘amxo‘rlik berish uchun yaratilgan."),
        FAQItem("«Shaxsiy Umra» nimani anglatadi?", "Safar sizning sanalaringiz, budjetingiz, mehmonxona darajasi va tanlagan xizmatlaringiz asosida tuziladi. 30–50 nafar notanish kishilik majburiy guruh yo‘q — kim bilan borishni o‘zingiz tanlaysiz."),
        FAQItem("Faqat oilam yoki do‘stlarim bilan bora olamanmi?", "Ha. Paketni bir kishi, juftlik, oila yoki do‘stlar uchun tuzish mumkin. Safarda faqat o‘zingiz qo‘shgan insonlar bo‘ladi."),
        FAQItem("Hammasini o‘zim tuzishni istamasam-chi?", "iumrah Care’ga murojaat qiling. Biz variant tanlash, tafsilotlarni tekshirish va safarni rasmiylashtirishga yordam beramiz — majburiy katta guruhsiz."),
    )
    AppLanguage.UZBEK_CYRILLIC -> listOf(
        FAQItem("iumrah нима?", "iumrah — мустақил ва шахсий Умра учун платформа. У парвоз, меҳмонхона, трансфер ва хизматларни битта тушунарли пакетга бирлаштиради ва сафарни битта иловада бошқаришга ёрдам беради."),
        FAQItem("iumrah нима учун яратилди?", "Зиёратчи катта тур гуруҳига боғланиб қолмаслиги ва кўплаб алоҳида бронларни бошқармаслиги учун. iumrah сафар давомида кўпроқ назорат, шаффофлик ва ғамхўрлик бериш учун яратилган."),
        FAQItem("«Шахсий Умра» нимани англатади?", "Сафар сизнинг саналарингиз, бюджетингиз, меҳмонхона даражаси ва танлаган хизматларингиз асосида тузилади. 30–50 нафар нотаниш кишилик мажбурий гуруҳ йўқ — ким билан боришни ўзингиз танлайсиз."),
        FAQItem("Фақат оилам ёки дўстларим билан бора оламанми?", "Ҳа. Пакетни бир киши, жуфтлик, оила ёки дўстлар учун тузиш мумкин. Сафарда фақат ўзингиз қўшган инсонлар бўлади."),
        FAQItem("Ҳаммасини ўзим тузишни истамасам-чи?", "iumrah Care’га мурожаат қилинг. Биз вариант танлаш, тафсилотларни текшириш ва сафарни расмийлаштиришга ёрдам берамиз — мажбурий катта гуруҳсиз."),
    )
}

@Composable
private fun PersonalUmrahFAQ(language: AppLanguage) {
    var expanded by remember { mutableStateOf<Int?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("iumrah", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = .9.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .52f))
            Text(tr(language, "Персональная Умра — для вас и ваших близких", "A personal Umrah — for you and the people you choose", "Shaxsiy Umra — siz va yaqinlaringiz uchun", "Шахсий Умра — сиз ва яқинларингиз учун"), fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
            Text(tr(language, "iumrah не привязывает вас к стандартной группе. Соберите поездку для себя, семьи или друзей и управляйте ею как одной персональной Umrah.", "iumrah does not tie you to a standard tour group. Build one personal Umrah for yourself, your family or friends and manage the journey in one place.", "iumrah sizni standart tur guruhiga bog‘lamaydi. O‘zingiz, oilangiz yoki do‘stlaringiz uchun shaxsiy Umra tuzing va safarni bitta joydan boshqaring.", "iumrah сизни стандарт тур гуруҳига боғламайди. Ўзингиз, оилангиз ёки дўстларингиз учун шахсий Умра тузинг ва сафарни битта жойдан бошқаринг."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .58f))
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 18.dp)) {
            faqItems(language).forEachIndexed { index, item ->
                IumrahPressable(onClick = { expanded = if (expanded == index) null else index }, modifier = Modifier.fillMaxWidth(), cornerRadius = 0.dp, background = Color.Transparent, shadowElevation = 0.dp) {
                    Column(Modifier.fillMaxWidth().animateContentSize()) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.q, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Icon(CupertinoSymbol.ChevronDown, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f), modifier = Modifier.graphicsLayer { rotationZ = if (expanded == index) 180f else 0f })
                        }
                        if (expanded == index) Text(item.a, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f), modifier = Modifier.padding(bottom = 17.dp))
                    }
                }
                if (index < faqItems(language).lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))
            }
        }
    }
}

@Composable
private fun AboutFooter(language: AppLanguage) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Image(painterResource(R.drawable.about_iumrah_kaaba_corner), null, Modifier.fillMaxWidth().height(184.dp), contentScale = ContentScale.Crop)
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("Since 2026", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f))
            Text(tr(language, "О проекте iumrah", "About iumrah", "iumrah haqida", "iumrah ҳақида"), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(tr(language, "iumrah — проект персональной и независимой Умры: собрать маршрут, отель, трансфер и сопровождение в одном спокойном приложении.", "iumrah is a personal independent Umrah project: build your route, hotel, transfer and care in one calm application.", "iumrah — shaxsiy va mustaqil Umra loyihasi: yo‘nalish, mehmonxona, transfer va yordamni bitta sokin ilovada jamlash uchun yaratilgan.", "iumrah — шахсий ва мустақил Умра лойиҳаси: йўналиш, меҳмонхона, трансфер ва ёрдамни битта сокин иловада жамлаш учун яратилган."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
        }
    }
}

@Composable
private fun HomeCarouselDots(count: Int, selectedIndex: Int) {
    if (count <= 1) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            val selected = index == selectedIndex.coerceIn(0, count - 1)
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .width(if (selected) 18.dp else 6.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = if (selected) .34f else .12f))
            )
        }
    }
}

@Composable
private fun DarkCTA(title: String) {
    Row(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Icon(CupertinoSymbol.ArrowRight, null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun LightCTA(title: String) {
    Row(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(18.dp)).background(Color.White).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.Black, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Icon(CupertinoSymbol.ArrowRight, null, tint = Color.Black, modifier = Modifier.size(18.dp))
    }
}

private fun tr(language: AppLanguage, ru: String, en: String, uz: String, uzCy: String): String = when (language) {
    AppLanguage.RUSSIAN -> ru
    AppLanguage.ENGLISH -> en
    AppLanguage.UZBEK -> uz
    AppLanguage.UZBEK_CYRILLIC -> uzCy
}

private val storyResources = listOf("home_story_01", "home_story_03", "home_story_04", "home_story_05", "home_story_07", "home_story_08")

@Composable
private fun HomeVideoCarousel() {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val carouselHeight = (screenHeight * .72f).coerceIn(460.dp, 680.dp)
    val pager = rememberPagerState(pageCount = { storyResources.size })
    var muted by remember { mutableStateOf(true) }
    val hapticView = LocalView.current

    Box(Modifier.fillMaxWidth().height(carouselHeight), contentAlignment = Alignment.BottomCenter) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 4.dp),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
        ) { index ->
            val offset = ((pager.currentPage - index) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
            val cardScale = 1f - (.015f * offset)
            val cardAlpha = 1f - (.10f * offset)
            Box(
                Modifier.fillMaxSize().graphicsLayer { scaleX = cardScale; scaleY = cardScale; alpha = cardAlpha }.clip(RoundedCornerShape(34.dp)).background(Color.Black),
            ) {
                LoopingRawVideo(
                    resourceName = storyResources[index], modifier = Modifier.fillMaxSize(), play = pager.currentPage == index, muted = muted,
                    fallback = { Image(painterResource(R.drawable.iumrah_makkah_background), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) },
                )
                IumrahPressable(
                    onClick = { muted = !muted; IumrahHaptics.soft(hapticView) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(44.dp), cornerRadius = 99.dp,
                    background = Color.White.copy(alpha=.16f), pressedScale = .92f,
                ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(if (muted) CupertinoSymbol.SpeakerSlash else CupertinoSymbol.Speaker, contentDescription = null, tint = Color.White) } }
            }
        }
        Row(
            modifier = Modifier.padding(bottom = 14.dp).clip(RoundedCornerShape(99.dp)).background(Color.Black.copy(alpha=.28f)).padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            storyResources.indices.forEach { index ->
                val width by animateFloatAsState(if (pager.currentPage == index) 18f else 6f, IumrahMotion.selection, label = "home-carousel-dot-$index")
                Box(Modifier.width(width.dp).height(6.dp).clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha = if (pager.currentPage == index) .96f else .46f)))
            }
        }
    }
}

@Composable
private fun EmotionalJourneyFullscreen(language: AppLanguage, onClose: () -> Unit) {
    val pager = rememberPagerState(pageCount = { storyResources.size })
    var muted by remember { mutableStateOf(false) }
    var captionVisible by remember { mutableStateOf(false) }
    val hapticView = LocalView.current
    BackHandler { onClose() }

    LaunchedEffect(pager.currentPage) {
        captionVisible = false
        IumrahHaptics.selection(hapticView)
        delay(if (pager.currentPage == 0) 340 else 240)
        captionVisible = true
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { index ->
                LoopingRawVideo(
                    resourceName = storyResources[index], modifier = Modifier.fillMaxSize(), play = pager.currentPage == index, muted = muted,
                    fallback = { Image(painterResource(R.drawable.iumrah_makkah_background), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) },
                )
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.42f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha=.90f)))))
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IumrahPill("${pager.currentPage + 1} / ${storyResources.size}", background = Color.White.copy(alpha=.14f), foreground = Color.White)
                Spacer(Modifier.weight(1f)); CircleControl(if (muted) CupertinoSymbol.SpeakerSlash else CupertinoSymbol.Speaker) { muted = !muted }; Spacer(Modifier.width(8.dp)); CircleControl(CupertinoSymbol.Close, onClose)
            }
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp)) {
                AnimatedVisibility(visible = captionVisible, enter = fadeIn(IumrahMotion.vapor), exit = fadeOut(IumrahMotion.fastFade)) {
                    val scale by animateFloatAsState(if (captionVisible) 1f else .985f, IumrahMotion.softReveal, label = "caption-scale")
                    val blur by animateDpAsState(if (captionVisible) 0.dp else 13.dp, IumrahMotion.vaporDp, label = "caption-blur")
                    Column(Modifier.graphicsLayer { scaleX = scale; scaleY = scale; translationY = if (captionVisible) 0f else 14f }.blur(blur), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(HomeEmotionalCopy.title(pager.currentPage, language), color = Color.White, fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold)
                        HomeEmotionalCopy.subtitle(pager.currentPage, language)?.let { Text(it, color = Color.White.copy(alpha=.82f), fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium) }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    storyResources.indices.forEach { index ->
                        val indicatorWidth by animateFloatAsState(if (index == pager.currentPage) 22f else 7f, IumrahMotion.selection, label = "story-indicator-$index")
                        Box(Modifier.width(indicatorWidth.dp).height(6.dp).clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha = if (index == pager.currentPage) .98f else .34f)))
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleControl(icon: CupertinoSymbol, onClick: () -> Unit) {
    IumrahPressable(onClick = onClick, modifier = Modifier.height(44.dp).aspectRatio(1f), cornerRadius = 99.dp, background = Color.White.copy(alpha=.14f), pressedScale = .94f) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = Color.White) }
    }
}
