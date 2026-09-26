package com.iumrah.beta.ui.packageflow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.FlightLand
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iumrah.beta.R
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.domain.journey.JourneyState
import com.iumrah.beta.domain.journey.JourneyStore
import com.iumrah.beta.domain.pricing.PackageQuote
import com.iumrah.beta.domain.trip.JourneyScope
import com.iumrah.beta.domain.trip.PackageTier
import com.iumrah.beta.models.flight.LiveFlightCandidate
import com.iumrah.beta.models.hotel.HotelSummary
import com.iumrah.beta.ui.components.IumrahPressable
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import kotlin.math.max

private enum class PackageService {
    OUTBOUND, MAKKAH, MADINAH, INBOUND, TRANSFER, VISA, MEALS
}

private enum class SupportExplainer { VISA, CARE, GUIDE }

@Composable
fun FinalPackageScreen(language: AppLanguage, journey: JourneyStore, chrome: AppChromeStore) {
    val state by journey.state.collectAsState()
    val quote = state.quote

    if (quote == null) {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    finalText(language, "Пакет ещё не готов", "Your package is not ready yet", "Paket hali tayyor emas", "Пакет ҳали тайёр эмас"),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    finalText(language, "Завершите выбор отелей и перелёта, чтобы увидеть итоговое предложение.", "Finish choosing your hotels and flight to see the final offer.", "Yakuniy taklifni ko‘rish uchun mehmonxona va reys tanlovini yakunlang.", "Якуний таклифни кўриш учун меҳмонхона ва рейс танловини якунланг."),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = .56f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                FinalBlackButton(
                    title = finalText(language, "Назад", "Back", "Orqaga", "Орқага"),
                    onClick = chrome::back,
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        GeneratorReadyHeader(language = language, onBack = chrome::back)
        FinalPackageHeader(language)
        PackageTierSection(language, state, quote)
        PackageRecommendationCard(language, state.trip.packageTier)
        PackageSupportCard(language)
        IncludedServicesCard(language, state)
        RefundPolicyCard(language)
        ManualPaymentCard(language)
        CareReassuranceCard(language)
        NotificationCard(language)
        FinalBlackButton(
            title = finalText(language, "Продолжить бронирование", "Continue booking", "Bron qilishni davom ettirish", "Брон қилишни давом эттириш"),
            onClick = chrome::openBookingCheckout,
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun GeneratorReadyHeader(language: AppLanguage, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IumrahPressable(
            onClick = onBack,
            modifier = Modifier.size(44.dp),
            cornerRadius = 99.dp,
            background = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = finalText(language, "Назад", "Back", "Orqaga", "Орқага"))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                finalText(language, "КОНФИГУРАТОР УМРЫ", "UMRAH CONFIGURATOR", "UMRAH KONFIGURATORI", "UMRAH КОНФИГУРАТОРИ"),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.05.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .48f),
            )
            Text(
                finalText(language, "Пакет готов", "Package ready", "Paket tayyor", "Пакет тайёр"),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFE9F7EE)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(20.dp), tint = Color(0xFF247D49))
        }
    }
}

@Composable
private fun FinalPackageHeader(language: AppLanguage) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            finalText(language, "ВАША УМРА", "YOUR UMRAH", "SIZNING UMRANGIZ", "СИЗНИНГ УМРАНГИЗ"),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.25.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .52f),
        )
        Text(
            finalText(language, "Проверьте пакет перед бронированием", "Review your package before booking", "Bron qilishdan oldin paketingizni tekshiring", "Брон қилишдан олдин пакетингизни текширинг"),
            fontSize = 34.sp,
            lineHeight = 37.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-.9).sp,
        )
        Text(
            finalText(language, "Вся поездка собрана в один пакет — перелёты, отели, трансферы и поддержка iumrah.", "Your whole journey is combined into one package — flights, hotels, transfers and iumrah support.", "Butun safaringiz bitta paketga jamlangan — reyslar, mehmonxonalar, transferlar va iumrah yordami.", "Бутун сафарингиз битта пакетга жамланган — рейслар, меҳмонхоналар, трансферлар ва iumrah ёрдами."),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .56f),
        )
    }
}

@Composable
private fun PackageTierSection(language: AppLanguage, state: JourneyState, quote: PackageQuote) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                finalText(language, "Уровень поездки", "Trip level", "Safar darajasi", "Сафар даражаси"),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                finalText(language, "Свайпните для сравнения", "Swipe to compare", "Taqqoslash uchun suring", "Таққослаш учун суринг"),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .52f),
            )
        }

        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val cardWidth = max(286f, (screenWidth.value * .80f)).dp
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PackageTierCard(
                    language = language,
                    tier = state.trip.packageTier,
                    quote = quote,
                    travelers = state.trip.travelerCount,
                    makkahHotel = state.makkahHotel,
                    madinahHotel = state.madinahHotel,
                    needsMadinah = state.trip.scope == JourneyScope.MAKKAH_AND_MADINAH,
                    modifier = Modifier.width(cardWidth).height(492.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PackageTier.entries.forEach { tier ->
                val selected = tier == state.trip.packageTier
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (selected) MaterialTheme.colorScheme.onBackground.copy(alpha = .08f) else Color.Transparent)
                        .padding(horizontal = 9.dp, vertical = 7.dp),
                ) {
                    Text(
                        tierTitle(language, tier),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (selected) 1f else .48f),
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Flight, null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = .52f))
            Text(
                finalText(language, "Выбранный авиабилет и даты не меняются при сравнении", "Your selected flight and dates stay fixed while comparing", "Taqqoslashda tanlangan reys va sanalar o‘zgarmaydi", "Таққослашда танланган рейс ва саналар ўзгармайди"),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .52f),
            )
        }
    }
}

@Composable
private fun PackageTierCard(
    language: AppLanguage,
    tier: PackageTier,
    quote: PackageQuote,
    travelers: Int,
    makkahHotel: HotelSummary?,
    madinahHotel: HotelSummary?,
    needsMadinah: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = tierGradient(tier)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(colors))
            .padding(21.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                tierTitle(language, tier).uppercase(language.locale),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.15.sp,
                color = Color.White.copy(alpha = .78f),
            )
            Box(
                Modifier.clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha = .16f)).padding(horizontal = 9.dp, vertical = 5.dp),
            ) {
                Text(
                    finalText(language, "Ваш пакет", "Your package", "Sizning paketingiz", "Сизнинг пакетингиз"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha = .12f)).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Rounded.Person, null, modifier = Modifier.size(13.dp), tint = Color.White)
                Text("$travelers", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            money(quote.totalPackagePrice, quote.currency, language),
            fontSize = 50.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.9).sp,
            color = Color.White,
            maxLines = 1,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${money(quote.pricePerPerson, quote.currency, language)} / ${finalText(language, "чел.", "person", "kishi", "киши")}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = .76f),
            )
            Text("·", fontSize = 14.sp, color = Color.White.copy(alpha = .76f))
            Text(
                finalText(language, "пакет для $travelers", "package for $travelers", "$travelers kishi uchun paket", "$travelers киши учун пакет"),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = .76f),
            )
        }

        Spacer(Modifier.height(13.dp))
        Text(packagePositionTitle(language, tier), fontSize = 24.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            packagePositionBody(language, tier),
            modifier = Modifier.padding(top = 6.dp),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = .72f),
        )

        Spacer(Modifier.height(13.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            packageBenefits(language, tier, makkahHotel, madinahHotel, needsMadinah).forEach { benefit ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(15.dp).padding(top = 1.dp), tint = Color.White.copy(alpha = .90f))
                    Text(
                        benefit,
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = .84f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.ArrowDownward, null, modifier = Modifier.size(15.dp), tint = Color.White.copy(alpha = .88f))
            Text(
                finalText(language, "Продолжение — внизу страницы", "Continue below on this page", "Davomi sahifa pastida", "Давоми саҳифа пастида"),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = .88f),
            )
        }
    }
}

@Composable
private fun PackageRecommendationCard(language: AppLanguage, tier: PackageTier) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFE6DEFF), Color(0xFFF7F0FF))))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(15.dp), tint = Color(0xFF593B99))
            Text(
                finalText(language, "РЕКОМЕНДАЦИЯ IUMRAH", "IUMRAH RECOMMENDATION", "IUMRAH TAVSIYASI", "IUMRAH ТАВСИЯСИ"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = .6.sp,
                color = Color(0xFF593B99),
            )
            Spacer(Modifier.weight(1f))
            Box(Modifier.clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha = .75f)).padding(horizontal = 8.dp, vertical = 5.dp)) {
                Text(
                    finalText(language, "Ваш пакет", "Your package", "Sizning paketingiz", "Сизнинг пакетингиз"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF593B99),
                )
            }
        }
        Text(packageRecommendationHeadline(language, tier), fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF482E7A))
        Text(packageRecommendationText(language, tier), fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = Color(0xFF543D8A))
    }
}

@Composable
private fun PackageSupportCard(language: AppLanguage) {
    var expanded by remember { mutableStateOf<SupportExplainer?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            finalText(language, "Важные пояснения по поездке", "Important trip details", "Safar bo‘yicha muhim izohlar", "Сафар бўйича муҳим изоҳлар"),
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            finalText(language, "Если хотите заранее понять детали, откройте пояснения по визе, iumrah Care и iumrah Guide.", "If you want to understand the details before booking, open the explainers for the visa, iumrah Care and iumrah Guide.", "Bron qilishdan oldin tafsilotlarni tushunmoqchi bo‘lsangiz, viza, iumrah Care va iumrah Guide izohlarini oching.", "Брон қилишдан олдин тафсилотларни тушунмоқчи бўлсангиз, виза, iumrah Care ва iumrah Guide изоҳларини очинг."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f),
        )
        SupportRow(language, Icons.Rounded.Description, SupportExplainer.VISA, expanded, { expanded = if (expanded == it) null else it })
        SupportRow(language, Icons.Rounded.Favorite, SupportExplainer.CARE, expanded, { expanded = if (expanded == it) null else it })
        SupportRow(language, Icons.Rounded.Group, SupportExplainer.GUIDE, expanded, { expanded = if (expanded == it) null else it })
    }
}

@Composable
private fun SupportRow(
    language: AppLanguage,
    icon: ImageVector,
    kind: SupportExplainer,
    expanded: SupportExplainer?,
    onClick: (SupportExplainer) -> Unit,
) {
    val title = when (kind) {
        SupportExplainer.VISA -> finalText(language, "Виза для поездки", "Trip visa", "Safar vizasi", "Сафар визаси")
        SupportExplainer.CARE -> "iumrah Care"
        SupportExplainer.GUIDE -> "iumrah Guide"
    }
    val subtitle = when (kind) {
        SupportExplainer.VISA -> finalText(language, "Срок действия, въезд и что именно входит в ваш пакет.", "Validity, entries and what is included in your package.", "Amal qilish muddati, kirish va paketingizga nimalar kirishi.", "Амал қилиш муддати, кириш ва пакетингизга нималар кириши.")
        SupportExplainer.CARE -> finalText(language, "Как работает дополнительная поддержка по вашей поездке.", "How the additional support layer works for your trip.", "Safaringiz bo‘yicha qo‘shimcha yordam qanday ishlashi.", "Сафарингиз бўйича қўшимча ёрдам қандай ишлаши.")
        SupportExplainer.GUIDE -> finalText(language, "Что включает персональное сопровождение по маршруту.", "What personal assistance includes along the journey.", "Yo‘nalish bo‘yicha shaxsiy hamrohlik nimalarni o‘z ichiga olishi.", "Йўналиш бўйича шахсий ҳамроҳлик нималарни ўз ичига олиши.")
    }
    IumrahPressable(
        onClick = { onClick(kind) },
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        cornerRadius = 18.dp,
        background = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(18.dp), tint = if (kind == SupportExplainer.CARE) Color(0xFFE24C6A) else MaterialTheme.colorScheme.onSurface)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .52f))
                }
                Icon(Icons.Rounded.ArrowOutward, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .35f))
            }
            AnimatedVisibility(expanded == kind) {
                Text(
                    supportBody(language, kind),
                    modifier = Modifier.padding(start = 52.dp, end = 8.dp, bottom = 2.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f),
                )
            }
        }
    }
}

@Composable
private fun IncludedServicesCard(language: AppLanguage, state: JourneyState) {
    var expanded by remember { mutableStateOf<PackageService?>(null) }
    val outbound = state.selectedJourney?.outbound
    val inbound = state.selectedJourney?.inbound

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
    ) {
        Text(
            finalText(language, "В ваш пакет входит", "Included in your package", "Paketingizga kiradi", "Пакетингизга киради"),
            modifier = Modifier.padding(bottom = 12.dp),
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
        )

        outbound?.let {
            ExpandableServiceRow(
                language = language,
                service = PackageService.OUTBOUND,
                expanded = expanded,
                onToggle = { expanded = if (expanded == it) null else it },
                icon = Icons.Rounded.FlightTakeoff,
                title = finalText(language, "Перелёт в Саудовскую Аравию", "Outbound flight", "Saudiya Arabistoniga parvoz", "Саудия Арабистонига парвоз"),
                subtitle = listOf(it.airline, it.flightNumber).filter { value -> value.isNotBlank() }.joinToString(" · "),
                detail = flightDetail(language, it),
            )
        }

        state.makkahHotel?.let { hotel ->
            ExpandableServiceRow(
                language = language,
                service = PackageService.MAKKAH,
                expanded = expanded,
                onToggle = { expanded = if (expanded == it) null else it },
                icon = Icons.Rounded.Hotel,
                title = finalText(language, "Отель в Мекке", "Makkah hotel", "Makkadagi mehmonxona", "Маккадаги меҳмонхона"),
                subtitle = hotel.name,
                detail = hotelDetail(language, hotel, state.makkahRoomCategory?.displayName ?: state.makkahRoom?.name),
            )
        }

        if (state.trip.scope == JourneyScope.MAKKAH_AND_MADINAH) {
            state.madinahHotel?.let { hotel ->
                ExpandableServiceRow(
                    language = language,
                    service = PackageService.MADINAH,
                    expanded = expanded,
                    onToggle = { expanded = if (expanded == it) null else it },
                    icon = Icons.Rounded.Hotel,
                    title = finalText(language, "Отель в Медине", "Madinah hotel", "Madinadagi mehmonxona", "Мадинадаги меҳмонхона"),
                    subtitle = hotel.name,
                    detail = hotelDetail(language, hotel, state.madinahRoomCategory?.displayName ?: state.madinahRoom?.name),
                )
            }
        }

        inbound?.let {
            ExpandableServiceRow(
                language = language,
                service = PackageService.INBOUND,
                expanded = expanded,
                onToggle = { expanded = if (expanded == it) null else it },
                icon = Icons.Rounded.FlightLand,
                title = finalText(language, "Обратный перелёт", "Return flight", "Qaytish parvozi", "Қайтиш парвози"),
                subtitle = listOf(it.airline, it.flightNumber).filter { value -> value.isNotBlank() }.joinToString(" · "),
                detail = flightDetail(language, it),
            )
        }

        ExpandableServiceRow(
            language = language,
            service = PackageService.TRANSFER,
            expanded = expanded,
            onToggle = { expanded = if (expanded == it) null else it },
            icon = Icons.Rounded.DirectionsCar,
            title = finalText(language, "Полный трансфер", "Full transfer", "To‘liq transfer", "Тўлиқ трансфер"),
            subtitle = "Kia Carnival",
            detail = finalText(language, "Аэропорт → отель → межгородской маршрут → аэропорт. Маршрут адаптируется под выбранные города.", "Airport → hotel → intercity route → airport. The route adapts to your selected cities.", "Aeroport → mehmonxona → shaharlararo yo‘nalish → aeroport. Yo‘nalish tanlangan shaharlarga moslashadi.", "Аэропорт → меҳмонхона → шаҳарлараро йўналиш → аэропорт. Йўналиш танланган шаҳарларга мослашади."),
        )

        StaticServiceRow(Icons.Rounded.LocationOn, finalText(language, "Зияраты в Мекке", "Makkah ziyarat", "Makka ziyoratlari", "Макка зиёратлари"), finalText(language, "Включено", "Included", "Kiritilgan", "Киритилган"))
        if (state.trip.scope == JourneyScope.MAKKAH_AND_MADINAH) {
            StaticServiceRow(Icons.Rounded.LocationOn, finalText(language, "Зияраты в Медине", "Madinah ziyarat", "Madina ziyoratlari", "Мадина зиёратлари"), finalText(language, "Включено", "Included", "Kiritilgan", "Киритилган"))
        }
        StaticServiceRow(Icons.Rounded.Favorite, "iumrah Care", finalText(language, "Включено", "Included", "Kiritilgan", "Киритилган"), tint = Color(0xFFE24C6A))
        StaticServiceRow(Icons.Rounded.Group, "iumrah Guide", finalText(language, "Включено", "Included", "Kiritilgan", "Киритилган"))

        ExpandableServiceRow(
            language = language,
            service = PackageService.VISA,
            expanded = expanded,
            onToggle = { expanded = if (expanded == it) null else it },
            icon = Icons.Rounded.Description,
            title = finalText(language, "Виза", "Visa", "Viza", "Виза"),
            subtitle = finalText(language, "Включено", "Included", "Kiritilgan", "Киритилган"),
            detail = finalText(language, "Визовая часть поездки включена в состав пакета. Перед оформлением данные и требования будут дополнительно проверены.", "The visa part of the trip is included in the package. Details and requirements are checked again before processing.", "Safarning viza qismi paketga kiritilgan. Rasmiylashtirishdan oldin ma’lumotlar va talablar yana tekshiriladi.", "Сафарнинг виза қисми пакетга киритилган. Расмийлаштиришдан олдин маълумотлар ва талаблар яна текширилади."),
        )
        ExpandableServiceRow(
            language = language,
            service = PackageService.MEALS,
            expanded = expanded,
            onToggle = { expanded = if (expanded == it) null else it },
            icon = Icons.Rounded.Restaurant,
            title = finalText(language, "Питание", "Meals", "Ovqatlanish", "Овқатланиш"),
            subtitle = mealsSummary(language, state.trip.packageTier),
            detail = mealsDetail(language, state.trip.packageTier),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(painterResource(R.drawable.umrah_mobile_logo), null, modifier = Modifier.width(38.dp).height(30.dp), contentScale = ContentScale.Fit)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("iumrah Mobile eSIM", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(finalText(language, "Включено", "Included", "Kiritilgan", "Киритилган"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .52f))
            }
        }
    }
}

@Composable
private fun ExpandableServiceRow(
    language: AppLanguage,
    service: PackageService,
    expanded: PackageService?,
    onToggle: (PackageService) -> Unit,
    icon: ImageVector,
    title: String,
    subtitle: String,
    detail: String,
) {
    val isExpanded = expanded == service
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        IumrahPressable(
            onClick = { onToggle(service) },
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 0.dp,
            background = Color.Transparent,
            pressedBackgroundAlpha = .96f,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .52f))
                }
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) finalText(language, "Свернуть", "Collapse", "Yopish", "Ёпиш") else finalText(language, "Открыть", "Open", "Ochish", "Очиш"),
                    modifier = Modifier.size(20.dp).rotate(if (isExpanded) 180f else 0f),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .38f),
                )
            }
        }
        AnimatedVisibility(isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(14.dp),
            ) {
                Text(detail, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .055f))
    }
}

@Composable
private fun StaticServiceRow(icon: ImageVector, title: String, subtitle: String, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = tint)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .52f))
        }
    }
}

@Composable
private fun RefundPolicyCard(language: AppLanguage) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFFFF4E8)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.VerifiedUser, null, modifier = Modifier.size(18.dp), tint = Color(0xFF9A5B10))
            }
            Text(finalText(language, "Условия возврата пакета", "Package refund policy", "Paketni qaytarish shartlari", "Пакетни қайтариш шартлари"), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            finalText(language, "Авиабилеты и подтверждённые тарифы отелей могут быть невозвратными. Возврат по дополнительным услугам рассчитывается отдельно по фактически неоказанной части.", "Flights and confirmed hotel rates may be non-refundable. Refunds for additional services are calculated separately for the unused portion.", "Aviabiletlar va tasdiqlangan mehmonxona tariflari qaytarilmasligi mumkin. Qo‘shimcha xizmatlar bo‘yicha qaytarish foydalanilmagan qism uchun alohida hisoblanadi.", "Авиабилетлар ва тасдиқланган меҳмонхона тарифлари қайтарилмаслиги мумкин. Қўшимча хизматлар бўйича қайтариш фойдаланилмаган қисм учун алоҳида ҳисобланади."),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .57f),
        )
    }
}

@Composable
private fun ManualPaymentCard(language: AppLanguage) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFEFF4FF)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.VerifiedUser, null, modifier = Modifier.size(19.dp), tint = Color(0xFF3559A8))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(finalText(language, "Оплата после подтверждения", "Payment after confirmation", "Tasdiqlangandan keyin to‘lov", "Тасдиқлангандан кейин тўлов"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                finalText(language, "После проверки наличия iumrah подтвердит пакет и отправит инструкции для оплаты. До подтверждения списания не будет.", "After availability is checked, iumrah confirms the package and sends payment instructions. Nothing is charged before confirmation.", "Mavjudlik tekshirilgach, iumrah paketni tasdiqlaydi va to‘lov bo‘yicha ko‘rsatmalarni yuboradi. Tasdiqlashgacha mablag‘ yechilmaydi.", "Мавжудлик текширилгач, iumrah пакетни тасдиқлайди ва тўлов бўйича кўрсатмаларни юборади. Тасдиқлашгача маблағ ечилмайди."),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f),
            )
        }
    }
}

@Composable
private fun CareReassuranceCard(language: AppLanguage) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(MaterialTheme.colorScheme.surface),
    ) {
        Image(
            painter = painterResource(R.drawable.care_price_support),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(188.dp).background(Color.Black),
            contentScale = ContentScale.Crop,
        )
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Rounded.Favorite, null, modifier = Modifier.size(13.dp), tint = Color(0xFFE24C6A))
                Text("iumrah Care", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE24C6A))
            }
            Text(
                finalText(language, "Мы проверим баланс цены и маршрута", "We review the balance between price and itinerary", "Narx va yo‘nalish muvozanatini tekshiramiz", "Нарх ва йўналиш мувозанатини текширамиз"),
                fontSize = 23.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                finalText(language, "Если текущая цена выше ожидаемой или даты гибкие, специалисты iumrah Care дополнительно проверят более удобные прямые рейсы, логичное распределение ночей и отели ближе к ключевым местам — без потери качества поездки.", "If the current price is higher than expected or your dates are flexible, iumrah Care will review more convenient direct flights, sensible night allocation and closer hotels without compromising the journey.", "Agar joriy narx kutilganidan yuqori bo‘lsa yoki sanalaringiz moslashuvchan bo‘lsa, iumrah Care qulayroq to‘g‘ridan-to‘g‘ri reyslar, tunlarning mantiqiy taqsimoti va yaqinroq mehmonxonalarni qo‘shimcha tekshiradi.", "Агар жорий нарх кутилганидан юқори бўлса ёки саналарингиз мослашувчан бўлса, iumrah Care қулайроқ тўғридан-тўғри рейслар, тунларнинг мантиқий тақсимоти ва яқинроқ меҳмонхоналарни қўшимча текширади."),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .56f),
            )
            IumrahPressable(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                cornerRadius = 18.dp,
                background = Color.Black,
            ) {
                Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(finalText(language, "Как это работает", "How it works", "Qanday ishlaydi", "Қандай ишлайди"), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.ArrowOutward, null, modifier = Modifier.size(17.dp), tint = Color.White)
                }
            }
            AnimatedVisibility(expanded) {
                Text(
                    finalText(
                        language,
                        "После бронирования команда Care проверит логику маршрута и доступные альтернативы. Если найдётся вариант заметно удобнее без потери качества, его предложат до окончательного подтверждения компонентов.",
                        "After booking, the Care team reviews the route logic and available alternatives. If a clearly more convenient option is available without reducing quality, it can be proposed before the components are finally confirmed.",
                        "Bron qilingandan keyin Care jamoasi yo‘nalish mantiqini va mavjud alternativalarni tekshiradi. Sifatni pasaytirmasdan ancha qulay variant topilsa, komponentlar yakuniy tasdiqlanishidan oldin taklif qilinadi.",
                        "Брон қилингандан кейин Care жамоаси йўналиш мантиғини ва мавжуд альтернативаларни текширади. Сифатни пасайтирмасдан анча қулай вариант топилса, компонентлар якуний тасдиқланишидан олдин таклиф қилинади.",
                    ),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f),
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(language: AppLanguage) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFEFF4FF)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Notifications, null, modifier = Modifier.size(19.dp), tint = Color(0xFF4D6FB6))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(L10n.text("notifications_title", language), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                finalText(language, "Статусы бронирования и важные изменения поездки будут приходить в приложение.", "Booking statuses and important trip changes will appear in the app.", "Bron holatlari va safardagi muhim o‘zgarishlar ilovada ko‘rinadi.", "Брон ҳолатлари ва сафардаги муҳим ўзгаришлар иловада кўринади."),
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f),
            )
        }
    }
}

@Composable
private fun FinalBlackButton(title: String, onClick: () -> Unit) {
    IumrahPressable(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        cornerRadius = 20.dp,
        background = Color.Black,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp), tint = Color.White)
        }
    }
}

private fun tierTitle(language: AppLanguage, tier: PackageTier): String = when (tier) {
    PackageTier.ECONOMY -> finalText(language, "Эконом", "Economy", "Economy", "Economy")
    PackageTier.STANDARD -> finalText(language, "Стандарт", "Standard", "Standard", "Standard")
    PackageTier.COMFORT -> finalText(language, "Комфорт", "Comfort", "Comfort", "Comfort")
    PackageTier.LUXURY -> finalText(language, "Люкс", "Luxury", "Luxury", "Luxury")
}

private fun tierGradient(tier: PackageTier): List<Color> = when (tier) {
    PackageTier.ECONOMY -> listOf(Color(0xFF385745), Color(0xFF263F33), Color(0xFF17261F), Color(0xFF0B110E))
    PackageTier.STANDARD -> listOf(Color(0xFF2B3880), Color(0xFF1F2B61), Color(0xFF131A40), Color(0xFF090B1F))
    PackageTier.COMFORT -> listOf(Color(0xFF0F6166), Color(0xFF0B4A52), Color(0xFF082E36), Color(0xFF051318))
    PackageTier.LUXURY -> listOf(Color(0xFFA37D2E), Color(0xFF7A571B), Color(0xFF4D3011), Color(0xFF211307))
}

private fun packagePositionTitle(language: AppLanguage, tier: PackageTier): String = when (tier) {
    PackageTier.ECONOMY -> finalText(language, "Максимальная экономия", "Maximum savings", "Maksimal tejamkorlik", "Максимал тежамкорлик")
    PackageTier.STANDARD -> finalText(language, "Оптимальный баланс", "Balanced choice", "Muvozanatli tanlov", "Мувозанатли танлов")
    PackageTier.COMFORT -> finalText(language, "Больше комфорта каждый день", "More comfort every day", "Har kuni ko‘proq qulaylik", "Ҳар куни кўпроқ қулайлик")
    PackageTier.LUXURY -> finalText(language, "Премиальная Umrah без компромиссов", "Premium Umrah with fewer compromises", "Kamroq murosali premium Umra", "Камроқ муросали премиум Умра")
}

private fun packagePositionBody(language: AppLanguage, tier: PackageTier): String = when (tier) {
    PackageTier.ECONOMY -> finalText(language, "Для тех, кому важнее итоговая стоимость, чем категория проживания.", "For travelers who prioritize the final price over hotel category.", "Yakuniy narx mehmonxona toifasidan muhimroq bo‘lganlar uchun.", "Якуний нарх меҳмонхона тоифасидан муҳимроқ бўлганлар учун.")
    PackageTier.STANDARD -> finalText(language, "Основные удобства сохранены, а стоимость остаётся под контролем.", "Core conveniences stay intact while the total remains controlled.", "Asosiy qulayliklar saqlanadi, umumiy narx nazoratda qoladi.", "Асосий қулайликлар сақланади, умумий нарх назоратда қолади.")
    PackageTier.COMFORT -> finalText(language, "Доплата направлена прежде всего на уровень и расположение отеля.", "The upgrade is focused primarily on hotel level and location.", "Qo‘shimcha qiymat asosan mehmonxona darajasi va joylashuviga ketadi.", "Қўшимча қиймат асосан меҳмонхона даражаси ва жойлашувига кетади.")
    PackageTier.LUXURY -> finalText(language, "Вы платите не за больше услуг, а за более высокий уровень ключевых частей поездки.", "You are not paying for more items, but for a higher level of the journey's key parts.", "Ko‘proq xizmat uchun emas, safarning asosiy qismlarining yuqori darajasi uchun to‘laysiz.", "Кўпроқ хизмат учун эмас, сафарнинг асосий қисмларининг юқори даражаси учун тўлайсиз.")
}

private fun packageBenefits(language: AppLanguage, tier: PackageTier, makkah: HotelSummary?, madinah: HotelSummary?, needsMadinah: Boolean): List<String> {
    val makkahName = makkah?.name ?: "Makkah · Primary Hotel"
    val madinahName = madinah?.name ?: "Madinah · Primary Hotel"
    return when (tier) {
        PackageTier.ECONOMY -> listOf(
            finalText(language, "Практичное размещение 1–2★ / Primary Hotel", "Practical 1–2★ / Primary Hotel stay", "Amaliy 1–2★ / Primary Hotel", "Амалий 1–2★ / Primary Hotel"),
            makkahName,
            finalText(language, "Более доступная Umrah без ухода в удалённые от Харама районы", "A more accessible Umrah without sending you far from the Haram area", "Haramdan juda uzoq bo‘lmagan, ko‘proq hamyonbop Umra", "Ҳарамдан жуда узоқ бўлмаган, кўпроқ ҳамёнбоп Умра"),
        )
        PackageTier.STANDARD -> listOf(
            "$makkahName · 3★",
            if (needsMadinah) madinahName else finalText(language, "Оптимальная логистика поездки", "Balanced trip logistics", "Muvozanatli safar logistikasi", "Мувозанатли сафар логистикаси"),
            finalText(language, "Хороший комфорт и честная цена без лишней переплаты", "Good comfort with a fair price and no unnecessary overpayment", "Yaxshi qulaylik va ortiqcha to‘lovsiz halol narx", "Яхши қулайлик ва ортиқча тўловсиз ҳалол нарх"),
        )
        PackageTier.COMFORT -> listOf(
            "$makkahName · 4★",
            finalText(language, "Завтрак включён без доплаты", "Breakfast included at no extra charge", "Nonushta qo‘shimcha to‘lovsiz", "Нонушта қўшимча тўловсиз"),
            finalText(language, "Ближе к Хараму и комфортнее по качеству проживания", "Closer to the Haram with stronger day-to-day comfort", "Haramga yaqinroq va yashash sifati qulayroq", "Ҳарамга яқинроқ ва яшаш сифати қулайроқ"),
        )
        PackageTier.LUXURY -> listOf(
            "$makkahName · 5★",
            if (needsMadinah) "$madinahName · 5★" else finalText(language, "Размещение непосредственно у Харама", "Stay directly by the Haram", "Haram yonidagi joylashuv", "Ҳарам ёнидаги жойлашув"),
            finalText(language, "Самый высокий уровень ключевых частей поездки", "The highest level across the journey's key parts", "Safarning asosiy qismlarida eng yuqori daraja", "Сафарнинг асосий қисмларида энг юқори даража"),
        )
    }
}

private fun packageRecommendationHeadline(language: AppLanguage, tier: PackageTier): String = when (tier) {
    PackageTier.ECONOMY -> finalText(language, "Доступная Umrah без тяжёлой логистики", "Accessible Umrah without difficult logistics", "Qiyin logistikasiz hamyonbop Umra", "Қийин логистикасиз ҳамёнбоп Умра")
    PackageTier.STANDARD -> finalText(language, "Проверенный баланс цены и качества", "A proven balance of price and quality", "Narx va sifatning sinalgan muvozanati", "Нарх ва сифатнинг синалган мувозанати")
    PackageTier.COMFORT -> finalText(language, "Ближе к Хараму и легче каждый день", "Closer to the Haram and easier every day", "Haramga yaqinroq va har kuni yengilroq", "Ҳарамга яқинроқ ва ҳар куни енгилроқ")
    PackageTier.LUXURY -> finalText(language, "Премиальный уровень рядом с Харамом", "A premium level right by the Haram", "Haram yonidagi premium daraja", "Ҳарам ёнидаги премиум даража")
}

private fun packageRecommendationText(language: AppLanguage, tier: PackageTier): String = when (tier) {
    PackageTier.ECONOMY -> finalText(language, "Мы старались сделать этот уровень действительно доступным: эконом‑отели не уводят Вас на 5 километров от Харама. Обычно это размещение примерно в 1–2 км, с понятной логистикой, хорошими комнатами и очень приемлемой ценой.", "We intentionally keep this level genuinely accessible: Economy hotels do not push you 5 km away from the Haram. They are typically around 1–2 km away, with clear logistics, decent rooms and an approachable price.", "Bu darajani haqiqatan hamyonbop qilishga harakat qildik: Economy mehmonxonalari Sizni Haramdan 5 km uzoqqa olib ketmaydi. Odatda ular 1–2 km atrofida, logistika tushunarli, xonalar yaxshi va narx maqbul.", "Бу даражани ҳақиқатан ҳамёнбоп қилишга ҳаракат қилдик: Economy меҳмонхоналари Сизни Ҳарамдан 5 км узоққа олиб кетмайди. Одатда улар 1–2 км атрофида, логистика тушунарли, хоналар яхши ва нарх мақбул.")
    PackageTier.STANDARD -> finalText(language, "Для Standard мы подбираем качественный и аккуратный отель с хорошим уровнем комфорта и честной стоимостью. Это вариант для тех, кто хочет сохранить бюджет, но всё равно получить удачное расположение и надёжный уровень комнат.", "For Standard we choose a solid, comfortable hotel with an honest price. It fits travelers who want to protect the budget while still getting a good location and reliable room quality.", "Standard uchun biz qulay va ishonchli mehmonxonani halol narx bilan tanlaymiz. Bu byudjetni saqlab, yaxshi joylashuv va ishonchli xona sifatini xohlaydiganlar uchun.", "Standard учун биз қулай ва ишончли меҳмонхонани ҳалол нарх билан танлаймиз. Бу бюджетни сақлаб, яхши жойлашув ва ишончли хона сифатини хоҳлайдиганлар учун.")
    PackageTier.COMFORT -> finalText(language, "Comfort мы рекомендуем тем, кому важна близость к Хараму каждый день: ориентир — около 150 метров, то есть буквально несколько минут пешком. Здесь лучше чувствуется баланс цены, близости и качества комнат.", "We recommend Comfort to travelers who value being closer to the Haram every day: the reference point is around 150 meters, just a few minutes on foot. This tier gives a stronger balance of price, proximity and room quality.", "Comfort'ni Haramga har kuni yaqin bo‘lish muhim bo‘lganlar uchun tavsiya qilamiz: mo‘ljal taxminan 150 metr. Bu darajada narx, yaqinlik va xona sifati balansi yaxshiroq.", "Comfort'ни Ҳарамга ҳар куни яқин бўлиш муҳим бўлганлар учун тавсия қиламиз: мўлжал тахминан 150 метр. Бу даражада нарх, яқинлик ва хона сифати баланси яхшироқ.")
    PackageTier.LUXURY -> finalText(language, "Luxury подойдёт тем, кто хочет максимально убрать бытовую нагрузку из поездки. Главное преимущество — расположение непосредственно у Харама, поэтому Вы почти не теряете время на дорогу между отелем и мечетью.", "Luxury suits travelers who want to remove as much everyday friction as possible. Its main advantage is being directly by the Haram, so you lose almost no time between the hotel and the mosque.", "Luxury safardagi kundalik tashvishlarni kamaytirishni istaganlar uchun. Asosiy ustunlik — Haramning o‘ziga juda yaqin joylashuv, mehmonxona va masjid o‘rtasida deyarli vaqt ketmaydi.", "Luxury сафардаги кундалик ташвишларни камайтиришни истаганлар учун. Асосий устунлик — Ҳарамнинг ўзига жуда яқин жойлашув, меҳмонхона ва масжид ўртасида деярли вақт кетмайди.")
}

private fun supportBody(language: AppLanguage, kind: SupportExplainer): String = when (kind) {
    SupportExplainer.VISA -> finalText(language, "Виза входит в пакет. Перед подачей мы ещё раз проверяем паспортные данные, допустимые сроки поездки и актуальные условия въезда.", "The visa is included in the package. Before submission we recheck passport details, eligible travel dates and current entry conditions.", "Viza paketga kiradi. Topshirishdan oldin pasport ma’lumotlari, safar muddatlari va kirish shartlarini qayta tekshiramiz.", "Виза пакетга киради. Топширишдан олдин паспорт маълумотлари, сафар муддатлари ва кириш шартларини қайта текширамиз.")
    SupportExplainer.CARE -> finalText(language, "Care — дополнительный слой поддержки: команда проверяет маршрут, помогает по изменениям и остаётся точкой связи по поездке.", "Care is an additional support layer: the team reviews the itinerary, helps with changes and remains your trip contact point.", "Care — qo‘shimcha yordam qatlami: jamoa yo‘nalishni tekshiradi, o‘zgarishlarda yordam beradi va safar davomida aloqa nuqtasi bo‘lib qoladi.", "Care — қўшимча ёрдам қатлами: жамоа йўналишни текширади, ўзгаришларда ёрдам беради ва сафар давомида алоқа нуқтаси бўлиб қолади.")
    SupportExplainer.GUIDE -> finalText(language, "Guide помогает пройти ключевые этапы маршрута и объясняет, что делать дальше, без необходимости двигаться большой группой.", "Guide helps you move through the key stages of the route and explains what comes next without requiring a large group.", "Guide yo‘nalishning asosiy bosqichlaridan o‘tishga yordam beradi va katta guruhsiz keyingi qadamlarni tushuntiradi.", "Guide йўналишнинг асосий босқичларидан ўтишга ёрдам беради ва катта гуруҳсиз кейинги қадамларни тушунтиради.")
}

private fun flightDetail(language: AppLanguage, flight: LiveFlightCandidate): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM · HH:mm", language.locale).withZone(ZoneId.systemDefault())
    val durationHours = flight.durationMinutes / 60
    val durationMinutes = flight.durationMinutes % 60
    val stops = if (flight.stops == 0) finalText(language, "прямой", "direct", "to‘g‘ridan-to‘g‘ri", "тўғридан-тўғри") else "${flight.stops} ${finalText(language, "перес.", "stop", "to‘xtash", "тўхташ")}"
    return "${flight.origin} → ${flight.destination}\n${formatter.format(flight.departureAt)} → ${formatter.format(flight.arrivalAt)}\n${flight.airline} · ${flight.flightNumber} · ${durationHours}h ${durationMinutes}m · $stops"
}

private fun hotelDetail(language: AppLanguage, hotel: HotelSummary, roomName: String?): String {
    val stars = hotel.stars?.let { " · ${it}★" }.orEmpty()
    val rating = hotel.rating?.let { " · ${finalText(language, "рейтинг", "rating", "reyting", "рейтинг")} ${String.format(language.locale, "%.1f", it)}" }.orEmpty()
    val room = roomName?.takeIf { it.isNotBlank() }?.let { "\n${finalText(language, "Номер", "Room", "Xona", "Хона")}: $it" }.orEmpty()
    return "${hotel.name}\n${hotel.city}$stars$rating$room"
}

private fun mealsSummary(language: AppLanguage, tier: PackageTier): String = when (tier) {
    PackageTier.COMFORT, PackageTier.LUXURY -> finalText(language, "Завтрак включён", "Breakfast included", "Nonushta kiritilgan", "Нонушта киритилган")
    else -> finalText(language, "Питание в пакете", "Meals in package", "Ovqat paketda", "Овқат пакетда")
}

private fun mealsDetail(language: AppLanguage, tier: PackageTier): String = when (tier) {
    PackageTier.COMFORT -> finalText(language, "Для Comfort завтрак включён. Дополнительные приёмы пищи формируются по выбранному составу поездки.", "Comfort includes breakfast. Additional meals follow the selected trip setup.", "Comfort paketida nonushta bor. Qo‘shimcha ovqatlar tanlangan safar tarkibiga ko‘ra shakllanadi.", "Comfort пакетида нонушта бор. Қўшимча овқатлар танланган сафар таркибига кўра шаклланади.")
    PackageTier.LUXURY -> finalText(language, "Для Luxury завтрак включён, а питание рассчитано на более высокий уровень сервиса.", "Luxury includes breakfast, with meals budgeted for a higher service level.", "Luxury paketida nonushta bor va ovqatlanish yuqoriroq xizmat darajasiga hisoblangan.", "Luxury пакетида нонушта бор ва овқатланиш юқорироқ хизмат даражасига ҳисобланган.")
    else -> finalText(language, "Питание учтено в структуре пакета согласно выбранному уровню поездки.", "Meals are included in the package structure according to the selected trip level.", "Ovqatlanish tanlangan safar darajasiga muvofiq paket tarkibida hisobga olingan.", "Овқатланиш танланган сафар даражасига мувофиқ пакет таркибида ҳисобга олинган.")
}

private fun money(amount: BigDecimal, currencyCode: String, language: AppLanguage): String {
    val formatter = NumberFormat.getCurrencyInstance(language.locale).apply {
        currency = runCatching { Currency.getInstance(currencyCode) }.getOrElse { Currency.getInstance("USD") }
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    return formatter.format(amount)
}

private fun finalText(language: AppLanguage, ru: String, en: String, uz: String, uzCyrl: String): String = when (language) {
    AppLanguage.RUSSIAN -> ru
    AppLanguage.ENGLISH -> en
    AppLanguage.UZBEK -> uz
    AppLanguage.UZBEK_CYRILLIC -> uzCyrl
}
