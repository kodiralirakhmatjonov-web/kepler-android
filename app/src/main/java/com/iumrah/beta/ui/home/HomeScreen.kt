package com.iumrah.beta.ui.home

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.iumrah.beta.ui.components.IumrahPill
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.components.IumrahRootPageHeader
import com.iumrah.beta.ui.media.LoopingRawVideo
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(language: AppLanguage, chrome: AppChromeStore) {
    var showStory by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            IumrahRootPageHeader(
                title = L10n.text("tab_home", language),
                chrome = chrome,
                usesBrandLogo = true,
            )
        }
        item { EmotionalPrompt(language = language, onOpen = { showStory = true }) }
        item { HomeVideoCarousel() }
        item { AudienceSection(language) }
        item { ServicesSection(language, chrome) }
        item { ReadyPackagesSection(language, chrome) }
        item { BuildMyUmrahSection(language, chrome) }
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
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(17.dp))
            }
        }
    }
}

private data class AudienceItem(
    val icon: ImageVector,
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
            AudienceItem(Icons.Rounded.Tune, "Соберите поездку сами", "Перелёт, отели, трансфер и сервисы — один персональный пакет Умры, который Вы собираете под себя.", blueBg, blueFg),
            AudienceItem(Icons.Rounded.People, "Семья и близкие", "Организуйте Умру для семьи или друзей вместе, сохраняя приватность и удобный темп поездки.", greenBg, greenFg),
            AudienceItem(Icons.Rounded.Star, "Индивидуальный и VIP", "Премиальные отели, приватный транспорт, индивидуальный сервис и максимум личного пространства.", sandBg, sandFg),
        )
        AppLanguage.ENGLISH -> listOf(
            AudienceItem(Icons.Rounded.Tune, "Build it your way", "Flights, hotels, transfer and services in one personal Umrah package you configure for yourself.", blueBg, blueFg),
            AudienceItem(Icons.Rounded.People, "Family & friends", "Organize Umrah together while keeping the journey private, comfortable and paced around your group.", greenBg, greenFg),
            AudienceItem(Icons.Rounded.Star, "Private & VIP", "Premium hotels, private transport, individual service and more personal space throughout the journey.", sandBg, sandFg),
        )
        AppLanguage.UZBEK -> listOf(
            AudienceItem(Icons.Rounded.Tune, "Safarni o‘zingiz tuzing", "Parvoz, mehmonxona, transfer va servislar — o‘zingizga mos bitta shaxsiy Umra paketi.", blueBg, blueFg),
            AudienceItem(Icons.Rounded.People, "Oila va yaqinlar", "Oila yoki do‘stlar bilan guruhingizga mos, qulay va xususiy tempda Umra safarini tashkil qiling.", greenBg, greenFg),
            AudienceItem(Icons.Rounded.Star, "Individual va VIP", "Premium mehmonxonalar, xususiy transport, individual servis va safar davomida maksimal maxfiylik.", sandBg, sandFg),
        )
        AppLanguage.UZBEK_CYRILLIC -> listOf(
            AudienceItem(Icons.Rounded.Tune, "Сафарни ўзингиз тузинг", "Парвоз, меҳмонхона, трансфер ва сервислар — ўзингизга мос битта шахсий Умра пакети.", blueBg, blueFg),
            AudienceItem(Icons.Rounded.People, "Оила ва яқинлар", "Оила ёки дўстлар билан гуруҳингизга мос, қулай ва хусусий темпда Умра сафарини ташкил қилинг.", greenBg, greenFg),
            AudienceItem(Icons.Rounded.Star, "Индивидуал ва VIP", "Премиум меҳмонхоналар, хусусий транспорт, индивидуал сервис ва сафар давомида максимал махфийлик.", sandBg, sandFg),
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
    val images: List<Int>, val title: String, val body: String, val badge: String, val icon: ImageVector, val action: () -> Unit,
)

@Composable
private fun ServicesSection(language: AppLanguage, chrome: AppChromeStore) {
    val transferImages = listOf(R.drawable.transfer_carnival, R.drawable.transfer_malibu, R.drawable.transfer_yukon)
    val ziyaratImages = listOf(R.drawable.ziyarat_quba_1, R.drawable.ziyarat_quba_2, R.drawable.ziyarat_quba_3, R.drawable.ziyarat_quba_4, R.drawable.ziyarat_quba_5)
    val serviceItems = when (language) {
        AppLanguage.RUSSIAN -> listOf(
            ServiceItem(transferImages, "Iumrah Transfer", "Встреча в аэропорту и приватные поездки между ключевыми точками маршрута. Комфортный автомобиль под Ваш формат поездки.", "В пакете", Icons.Rounded.Route, chrome::startNewTrip),
            ServiceItem(ziyaratImages, "Iumrah Ziyarat", "Места Мекки и Медины в одном маршруте. История, навигация и понятный порядок посещения без лишней суеты.", "Маршруты", Icons.Rounded.LocationOn, {}),
            ServiceItem(listOf(R.drawable.iumrah_esim_home_card), "Iumrah eSIM", "Интернет в Саудовской Аравии готов к подключению сразу после приземления — без поиска SIM-карты в аэропорту.", "Связь", Icons.Rounded.Security, {}),
            ServiceItem(listOf(R.drawable.iumrah_flights_home_card), "Iumrah Flights", "Статус Вашего рейса в реальном времени: изменения времени, задержки и важные обновления поездки в одном месте.", "Live status", Icons.Rounded.Flight, chrome::openFlights),
            ServiceItem(listOf(R.drawable.iumrah_care_showcase), "Iumrah Care", "Человеческая поддержка, когда она действительно нужна: до поездки, в Саудовской Аравии и во время возвращения домой.", "Поддержка", Icons.Rounded.Favorite) { chrome.navigate(AppTab.CARE) },
        )
        AppLanguage.ENGLISH -> listOf(
            ServiceItem(transferImages, "Iumrah Transfer", "Airport pickup and private rides between key stops, with the right vehicle for your journey.", "Included", Icons.Rounded.Route, chrome::startNewTrip),
            ServiceItem(ziyaratImages, "Iumrah Ziyarat", "Makkah and Madinah places in one route, with context, navigation and a clear visit sequence.", "Routes", Icons.Rounded.LocationOn, {}),
            ServiceItem(listOf(R.drawable.iumrah_esim_home_card), "Iumrah eSIM", "Saudi internet ready from arrival, without having to search for a local SIM card at the airport.", "Connectivity", Icons.Rounded.Security, {}),
            ServiceItem(listOf(R.drawable.iumrah_flights_home_card), "Iumrah Flights", "Real-time flight status with schedule changes, delays and important journey updates in one place.", "Live status", Icons.Rounded.Flight, chrome::openFlights),
            ServiceItem(listOf(R.drawable.iumrah_care_showcase), "Iumrah Care", "Human support when it matters — before the trip, in Saudi Arabia and on the way home.", "Support", Icons.Rounded.Favorite) { chrome.navigate(AppTab.CARE) },
        )
        AppLanguage.UZBEK -> listOf(
            ServiceItem(transferImages, "Iumrah Transfer", "Aeroportdan kutib olish va yo‘nalishning muhim nuqtalari orasida safaringizga mos xususiy transport.", "Paketda", Icons.Rounded.Route, chrome::startNewTrip),
            ServiceItem(ziyaratImages, "Iumrah Ziyarat", "Makka va Madina ziyorat joylari bitta yo‘nalishda: ma’lumot, navigatsiya va tushunarli tashrif tartibi.", "Yo‘nalishlar", Icons.Rounded.LocationOn, {}),
            ServiceItem(listOf(R.drawable.iumrah_esim_home_card), "Iumrah eSIM", "Saudiya Arabistonida internet qo‘nganingizdan boshlab tayyor — aeroportda SIM-karta izlash shart emas.", "Internet", Icons.Rounded.Security, {}),
            ServiceItem(listOf(R.drawable.iumrah_flights_home_card), "Iumrah Flights", "Parvoz holati real vaqtda: vaqt o‘zgarishi, kechikish va safar uchun muhim yangilanishlar bir joyda.", "Live status", Icons.Rounded.Flight, chrome::openFlights),
            ServiceItem(listOf(R.drawable.iumrah_care_showcase), "Iumrah Care", "Kerak bo‘lgan paytda insoniy yordam — safardan oldin, Saudiya Arabistonida va uyga qaytishda.", "Yordam", Icons.Rounded.Favorite) { chrome.navigate(AppTab.CARE) },
        )
        AppLanguage.UZBEK_CYRILLIC -> listOf(
            ServiceItem(transferImages, "Iumrah Transfer", "Аэропортдан кутиб олиш ва йўналишнинг муҳим нуқталари орасида сафарингизга мос хусусий транспорт.", "Пакетда", Icons.Rounded.Route, chrome::startNewTrip),
            ServiceItem(ziyaratImages, "Iumrah Ziyarat", "Макка ва Мадина зиёрат жойлари битта йўналишда: маълумот, навигация ва тушунарли ташриф тартиби.", "Йўналишлар", Icons.Rounded.LocationOn, {}),
            ServiceItem(listOf(R.drawable.iumrah_esim_home_card), "Iumrah eSIM", "Саудия Арабистонида интернет қўнганингиздан бошлаб тайёр — аэропортда SIM-карта излаш шарт эмас.", "Интернет", Icons.Rounded.Security, {}),
            ServiceItem(listOf(R.drawable.iumrah_flights_home_card), "Iumrah Flights", "Парвоз ҳолати реал вақтда: вақт ўзгариши, кечикиш ва сафар учун муҳим янгиланишлар бир жойда.", "Live status", Icons.Rounded.Flight, chrome::openFlights),
            ServiceItem(listOf(R.drawable.iumrah_care_showcase), "Iumrah Care", "Керак бўлган пайтда инсоний ёрдам — сафардан олдин, Саудия Арабистонида ва уйга қайтишда.", "Ёрдам", Icons.Rounded.Favorite) { chrome.navigate(AppTab.CARE) },
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
private fun ReadyPackagesSection(language: AppLanguage, chrome: AppChromeStore) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        SectionHeader(
            title = tr(language, "Готовые пакеты", "Ready-made packages", "Tayyor paketlar", "Тайёр пакетлар"),
            subtitle = tr(language,
                "Актуальные варианты перелёта уже собраны с отелем и сервисами в единую цену пакета.",
                "Current flight options are already combined with hotel and services into one package price.",
                "Amaldagi parvoz variantlari mehmonxona va servislar bilan bitta paket narxiga yig‘ilgan.",
                "Амалдаги парвоз вариантлари меҳмонхона ва сервислар билан битта пакет нархига йиғилган.")
        )
        IumrahPressable(onClick = chrome::openFlights, modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp, shadowElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().background(Color.White)) {
                Image(painterResource(R.drawable.iumrah_flights_showcase), contentDescription = null, modifier = Modifier.fillMaxWidth().height(118.dp), contentScale = ContentScale.Crop)
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Flight, null, tint = Color.Black.copy(alpha = .52f), modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp))
                        Text("Iumrah Flights", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = .52f))
                    }
                    Text(tr(language, "Все готовые варианты", "All ready packages", "Barcha tayyor paketlar", "Барча тайёр пакетлар"), fontSize = 23.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(tr(language, "Откройте опубликованные перелёты и собранные варианты поездки.", "Open published flights and assembled journey options.", "E’lon qilingan reyslar va tayyor safar variantlarini oching.", "Эълон қилинган рейслар ва тайёр сафар вариантларини очинг."), style = MaterialTheme.typography.bodyMedium, color = Color.Black.copy(alpha = .58f))
                    DarkCTA(tr(language, "Смотреть пакеты", "View packages", "Paketlarni ko‘rish", "Пакетларни кўриш"))
                }
            }
        }
    }
}

@Composable
private fun BuildMyUmrahSection(language: AppLanguage, chrome: AppChromeStore) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        SectionHeader(
            tr(language, "Соберите свою Умру", "Build your Umrah", "Umrangizni tuzing", "Умрангизни тузинг"),
            tr(language, "Соберите пакет сами или передайте подбор команде Iumrah Care.", "Build the package yourself or let Iumrah Care help arrange it.", "Paketni o‘zingiz tuzing yoki Iumrah Care jamoasiga topshiring.", "Пакетни ўзингиз тузинг ёки Iumrah Care жамоасига топширинг.")
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 1.dp)) {
            item { ConfiguratorCard(language, chrome) }
            item { CareBuilderCard(language) { chrome.navigate(AppTab.CARE) } }
        }
    }
}

@Composable
private fun ConfiguratorCard(language: AppLanguage, chrome: AppChromeStore) {
    IumrahPressable(onClick = chrome::startNewTrip, modifier = Modifier.width(336.dp).height(540.dp), cornerRadius = 34.dp, background = Color.Black, shadowElevation = 12.dp) {
        Column(Modifier.fillMaxSize().background(Color.Black)) {
            Image(painterResource(R.drawable.iumrah_configurator_hero), null, Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Crop)
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Tune, null, tint = Color.White.copy(alpha = .78f), modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp))
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
private fun CareBuilderCard(language: AppLanguage, onClick: () -> Unit) {
    IumrahPressable(onClick = onClick, modifier = Modifier.width(336.dp).height(540.dp), cornerRadius = 34.dp, shadowElevation = 10.dp) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            Image(painterResource(R.drawable.iumrah_care_showcase), null, Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Crop)
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Favorite, null, tint = Color.Black.copy(alpha = .58f), modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp))
                    Text("Iumrah Care", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = .45.sp, color = Color.Black.copy(alpha = .58f))
                    Spacer(Modifier.weight(1f))
                    IumrahPill(tr(language, "≈ 10 минут", "≈ 10 min", "≈ 10 daqiqa", "≈ 10 дақиқа"), background = Color.Black.copy(alpha = .055f), foreground = Color.Black.copy(alpha = .62f))
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
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        SectionHeader(tr(language, "Продукты Iumrah", "Iumrah products", "Iumrah mahsulotlari", "Iumrah маҳсулотлари"))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 1.dp)) {
            item { BackendSystemCard(language) }
            item { AdvisorCard(language, chrome) }
        }
    }
}

@Composable
private fun BackendSystemCard(language: AppLanguage) {
    val purple = Color(0xFF5C38E0)
    Column(
        Modifier.width(336.dp).height(472.dp).clip(RoundedCornerShape(34.dp)).background(
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
            Icon(Icons.Rounded.Route, null, tint = Color.White.copy(alpha = .92f), modifier = Modifier.size(44.dp))
        }
        Text(tr(language, "Одна система для всей поездки", "One system for the whole journey", "Butun safar uchun bitta tizim", "Бутун сафар учун битта тизим"), fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.65).sp, color = Color.White)
        Spacer(Modifier.height(9.dp))
        Text(tr(language, "Перелёт, отель, бронь, Care и статусы остаются связанными внутри одной поездки.", "Flights, hotel, booking, Care and statuses stay connected inside one journey.", "Parvoz, mehmonxona, bron, Care va statuslar bitta safarda bog‘langan qoladi.", "Парвоз, меҳмонхона, брон, Care ва статуслар битта сафарда боғланган қолади."), fontSize = 14.sp, lineHeight = 19.sp, color = Color.White.copy(alpha = .62f))
    }
}

@Composable
private fun AdvisorCard(language: AppLanguage, chrome: AppChromeStore) {
    val aura = Brush.linearGradient(listOf(Color(0xFF102B27), Color(0xFF1A4B42), Color(0xFF13232D), Color.Black))
    IumrahPressable(onClick = { chrome.navigate(AppTab.CARE) }, modifier = Modifier.width(336.dp).height(472.dp), cornerRadius = 34.dp, background = Color.Black) {
        Box(Modifier.fillMaxSize().background(aura).padding(20.dp)) {
            Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Speaker, null, tint = Color.White.copy(alpha = .92f), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("iumrah Advisor", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = .45.sp, color = Color.White.copy(alpha = .92f))
                    Spacer(Modifier.weight(1f)); Text("↗", color = Color.White.copy(alpha = .92f), fontSize = 18.sp)
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(106.dp).clip(CircleShape).background(Color.White.copy(alpha = .075f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Speaker, null, tint = Color.White.copy(alpha = .92f), modifier = Modifier.size(42.dp))
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
        item { SmallChip(Icons.Rounded.Hotel, L10n.text("tab_hotels", language)) }
        item { SmallChip(Icons.Rounded.Flight, L10n.text("step_flight", language)) }
        item { SmallChip(Icons.Rounded.Favorite, "iumrah Care") }
    }
}

@Composable
private fun SmallChip(icon: ImageVector, text: String) {
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
            JourneyIcon(Icons.Rounded.Flight); Connector(Modifier.weight(1f)); JourneyIcon(Icons.Rounded.Hotel); Connector(Modifier.weight(1f)); JourneyIcon(Icons.Rounded.Route); Connector(Modifier.weight(1f)); JourneyIcon(Icons.Rounded.Star); Connector(Modifier.weight(1f)); JourneyIcon(Icons.Rounded.Favorite)
        }
        Text(L10n.text("home_connected_title", language), fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold)
        Text(L10n.text("home_connected_body", language), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
    }
}

@Composable private fun JourneyIcon(icon: ImageVector) { Box(Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(14.dp)) } }
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
                            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f), modifier = Modifier.graphicsLayer { rotationZ = if (expanded == index) 180f else 0f })
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Text("Since 2026", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f))
        Text("iumrah", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(tr(language, "iumrah — проект персональной и независимой Умры: собрать маршрут, отель, трансфер и сопровождение в одном спокойном приложении.", "iumrah is a personal independent Umrah project: build your route, hotel, transfer and care in one calm application.", "iumrah — shaxsiy va mustaqil Umra loyihasi: yo‘nalish, mehmonxona, transfer va yordamni bitta sokin ilovada jamlash uchun yaratilgan.", "iumrah — шахсий ва мустақил Умра лойиҳаси: йўналиш, меҳмонхона, трансфер ва ёрдамни битта сокин иловада жамлаш учун яратилган."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
    }
}

@Composable
private fun DarkCTA(title: String) {
    Row(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Icon(Icons.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun LightCTA(title: String) {
    Row(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(18.dp)).background(Color.White).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.Black, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Icon(Icons.Rounded.ArrowForward, null, tint = Color.Black, modifier = Modifier.size(18.dp))
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
                ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(if (muted) Icons.Rounded.VolumeOff else Icons.Rounded.Speaker, contentDescription = null, tint = Color.White) } }
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
                Spacer(Modifier.weight(1f)); CircleControl(if (muted) Icons.Rounded.VolumeOff else Icons.Rounded.Speaker) { muted = !muted }; Spacer(Modifier.width(8.dp)); CircleControl(Icons.Rounded.Close, onClose)
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
private fun CircleControl(icon: ImageVector, onClick: () -> Unit) {
    IumrahPressable(onClick = onClick, modifier = Modifier.height(44.dp).aspectRatio(1f), cornerRadius = 99.dp, background = Color.White.copy(alpha=.14f), pressedScale = .94f) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = Color.White) }
    }
}
