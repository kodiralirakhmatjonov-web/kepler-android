package com.iumrah.beta.ui.booking

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iumrah.beta.R
import com.iumrah.beta.core.design.IumrahHaptics
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.navigation.AppTab
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.account.IumrahAccountService
import com.iumrah.beta.data.booking.BookingStore
import com.iumrah.beta.models.account.IumrahCheckoutResponse
import com.iumrah.beta.models.booking.StoredBookingSession
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.components.IumrahRootPageHeader
import com.iumrah.beta.ui.cupertino.CupertinoIcon
import com.iumrah.beta.ui.cupertino.CupertinoSymbol
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.delay

private enum class BookingPanel { BOOKING, STATUS }
private enum class BookingScope { ACTIVE, PAST }

@Composable
fun BookingsHomeScreen(
    language: AppLanguage,
    bookingStore: BookingStore,
    accountService: IumrahAccountService,
    chrome: AppChromeStore,
) {
    val state by bookingStore.state.collectAsState()
    var scope by remember { mutableStateOf(BookingScope.ACTIVE) }
    var panel by remember { mutableStateOf(BookingPanel.BOOKING) }
    var checkout by remember { mutableStateOf<IumrahCheckoutResponse?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    val active = state.sessions.filterNot { it.effectiveStatus.uppercase() in setOf("COMPLETED", "CANCELLED") }
    val past = state.sessions.filter { it.effectiveStatus.uppercase() in setOf("COMPLETED", "CANCELLED") }
    val activeSession = active.firstOrNull()

    suspend fun refresh() {
        state.sessions.toList().forEach { session -> runCatching { bookingStore.refresh(session.id) } }
        activeSession?.let { session ->
            checkout = runCatching { accountService.checkout(session.id, bookingStore.headersFor(session)) }.getOrNull()
        }
    }

    LaunchedEffect(activeSession?.id, state.sessions.size) {
        refresh()
        while (true) {
            delay(60_000)
            refresh()
        }
    }

    AnimatedContent(
        targetState = scope,
        transitionSpec = { fadeIn().togetherWith(fadeOut()) },
        label = "booking-scope",
    ) { selectedScope ->
        when (selectedScope) {
            BookingScope.ACTIVE -> {
                if (activeSession == null) {
                    EmptyBookingHome(
                        language = language,
                        scope = scope,
                        onScope = { scope = it },
                        chrome = chrome,
                    )
                } else {
                    ActiveBookingHome(
                        language = language,
                        session = activeSession,
                        otherSessions = active.drop(1),
                        panel = panel,
                        onPanel = { panel = it },
                        scope = scope,
                        onScope = { scope = it },
                        checkout = checkout,
                        chrome = chrome,
                        onDelete = { id ->
                            deleteError = null
                            runCatching { bookingStore.deleteBooking(id) }.onFailure { deleteError = it.message }
                        },
                        deleteError = deleteError,
                    )
                }
            }
            BookingScope.PAST -> PastBookingsHome(
                language = language,
                sessions = past,
                scope = scope,
                onScope = { scope = it },
                chrome = chrome,
                onDelete = { id -> runCatching { bookingStore.deleteBooking(id) } },
            )
        }
    }
}

@Composable
private fun ActiveBookingHome(
    language: AppLanguage,
    session: StoredBookingSession,
    otherSessions: List<StoredBookingSession>,
    panel: BookingPanel,
    onPanel: (BookingPanel) -> Unit,
    scope: BookingScope,
    onScope: (BookingScope) -> Unit,
    checkout: IumrahCheckoutResponse?,
    chrome: AppChromeStore,
    onDelete: suspend (String) -> Unit,
    deleteError: String?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            IumrahRootPageHeader(
                title = t(language, "Бронирование", "Booking", "Bron", "Брон"),
                chrome = chrome,
                usesBrandLogo = true,
            )
            Spacer(Modifier.height(18.dp))
            BookingPanelPicker(language, panel, onPanel)
            Spacer(Modifier.height(12.dp))
            BookingScopePicker(language, scope, onScope)
            Spacer(Modifier.height(24.dp))
            BookingIdentity(language, session)
            Spacer(Modifier.height(28.dp))
        }

        if (panel == BookingPanel.BOOKING) {
            item {
                BookingProgress(language, session, chrome)
                Spacer(Modifier.height(28.dp))
                TripWalletEntry(language, session) { chrome.openBookingDetail(session.id) }
                Spacer(Modifier.height(38.dp))
            }
        } else {
            item {
                BookingStatusOverview(language, session, checkout)
                Spacer(Modifier.height(28.dp))
                FulfillmentCenter(language, session, checkout, chrome)
                Spacer(Modifier.height(34.dp))
                TripPlanPreview(language, session, chrome)
                Spacer(Modifier.height(34.dp))
                TripManagement(language, session, chrome)
                Spacer(Modifier.height(if (otherSessions.isNotEmpty()) 36.dp else 12.dp))
            }
        }

        if (otherSessions.isNotEmpty()) {
            item {
                SectionHeader(t(language, "Другие поездки", "Other trips", "Boshqa safarlar", "Бошқа сафарлар"), null)
                Spacer(Modifier.height(14.dp))
            }
            items(otherSessions, key = { it.id }) { other ->
                CompactBookingCard(language, other, chrome, onDelete)
                Spacer(Modifier.height(10.dp))
            }
            item { Spacer(Modifier.height(12.dp)) }
        }

        deleteError?.let { message ->
            item {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = .08f), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                )
            }
        }
    }
}

@Composable
private fun BookingIdentity(language: AppLanguage, session: StoredBookingSession) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(94.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(.7.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = .065f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) {
            CupertinoIcon(
                CupertinoSymbol.Suitcase,
                contentDescription = null,
                modifier = Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = .58f),
            )
        }
        Spacer(Modifier.height(17.dp))
        Text(
            t(language, "Ваша Umrah", "Your Umrah", "Sizning Umrangiz", "Сизнинг Умрангиз"),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f),
        )
        Spacer(Modifier.height(7.dp))
        Text(
            "${session.booking.route.originCode} → ${session.booking.route.outboundDestination}",
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-.9).sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            "${L10n.date(session.booking.input.startDate, language)} – ${L10n.date(session.booking.input.endDate, language)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(17.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdentityPill(session.displayBookingNumber)
            IdentityPill(
                text = pilgrimCount(language, session.booking.input.travelers.totalPeople),
                icon = CupertinoSymbol.Persons,
            )
        }
        session.travelerName?.trim()?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(11.dp))
            Text(
                it,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .54f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun IdentityPill(text: String, icon: CupertinoSymbol? = null) {
    Row(
        modifier = Modifier
            .height(31.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon?.let { CupertinoIcon(it, null, Modifier.size(13.dp), MaterialTheme.colorScheme.onBackground.copy(alpha = .55f)) }
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f))
    }
}

@Composable
private fun BookingPanelPicker(language: AppLanguage, selected: BookingPanel, onSelect: (BookingPanel) -> Unit) {
    SegmentedPicker(
        items = listOf(
            BookingPanel.BOOKING to t(language, "Бронирование", "Booking", "Bron", "Брон"),
            BookingPanel.STATUS to t(language, "Статус бронирования", "Booking status", "Bron holati", "Брон ҳолати"),
        ),
        selected = selected,
        onSelect = onSelect,
    )
}

@Composable
private fun BookingScopePicker(language: AppLanguage, selected: BookingScope, onSelect: (BookingScope) -> Unit) {
    SegmentedPicker(
        items = listOf(
            BookingScope.ACTIVE to t(language, "Активные", "Upcoming", "Faol", "Фаол"),
            BookingScope.PAST to t(language, "Прошлые", "Past", "O‘tgan", "Ўтган"),
        ),
        selected = selected,
        onSelect = onSelect,
    )
}

@Composable
private fun <T> SegmentedPicker(items: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val view = LocalView.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = .055f))
            .padding(2.dp),
    ) {
        items.forEach { (value, label) ->
            val active = selected == value
            IumrahPressable(
                onClick = {
                    if (!active) IumrahHaptics.selection(view)
                    onSelect(value)
                },
                modifier = Modifier.weight(1f).fillMaxSize(),
                cornerRadius = 7.dp,
                background = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                shadowElevation = if (active) 1.dp else 0.dp,
                pressedScale = .985f,
                haptic = false,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(label, fontSize = 13.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}

private data class ProgressStage(
    val key: String,
    val titleRu: String,
    val titleEn: String,
    val titleUz: String,
    val titleCy: String,
    val subtitleRu: String,
    val subtitleEn: String,
    val subtitleUz: String,
    val subtitleCy: String,
)

private val progressStages = listOf(
    ProgressStage("CREATED", "Пакет создан", "Package created", "Paket yaratildi", "Пакет яратилди", "Поездка добавлена в iumrah", "Trip added to iumrah", "Safar iumrah'ga qo‘shildi", "Сафар iumrah'га қўшилди"),
    ProgressStage("AVAILABILITY_CHECK", "Проверка наличия", "Availability check", "Mavjudlik tekshiruvi", "Мавжудлик текшируви", "Подтверждаем перелёт, отель и услуги", "Confirming flight, hotel and services", "Parvoz, mehmonxona va xizmatlar tasdiqlanmoqda", "Парвоз, меҳмонхона ва хизматлар тасдиқланмоқда"),
    ProgressStage("PAYMENT_PENDING", "Оплата и данные паломников", "Payment and pilgrim details", "To‘lov va ziyoratchi ma’lumotlari", "Тўлов ва зиёратчи маълумотлари", "Наличие подтверждено · требуется действие", "Availability confirmed · action required", "Mavjudlik tasdiqlandi · amal kerak", "Мавжудлик тасдиқланди · амал керак"),
    ProgressStage("BOOKING_CONFIRMED", "Бронирование подтверждено", "Booking confirmed", "Bron tasdiqlandi", "Брон тасдиқланди", "Позиции закреплены за вами", "Your trip components are secured", "Safar xizmatlari siz uchun band qilindi", "Сафар хизматлари сиз учун банд қилинди"),
    ProgressStage("READY_TO_TRAVEL", "Документы готовы", "Documents ready", "Hujjatlar tayyor", "Ҳужжатлар тайёр", "Всё готово к поездке", "Everything is ready for travel", "Safar uchun hammasi tayyor", "Сафар учун ҳаммаси тайёр"),
    ProgressStage("IN_TRIP", "Паломник в поездке", "Pilgrim in trip", "Ziyoratchi safarda", "Зиёратчи сафарда", "iumrah сопровождает вашу поездку", "iumrah is accompanying your trip", "iumrah safaringizga hamroh", "iumrah сафарингизга ҳамроҳ"),
    ProgressStage("COMPLETED", "Поездка завершена", "Trip completed", "Safar yakunlandi", "Сафар якунланди", "История поездки сохранена", "Your trip history is saved", "Safar tarixi saqlandi", "Сафар тарихи сақланди"),
)

@Composable
private fun BookingProgress(language: AppLanguage, session: StoredBookingSession, chrome: AppChromeStore) {
    val current = progressIndex(session.effectiveStatus)
    val cancelled = session.effectiveStatus.equals("CANCELLED", true)
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(
            t(language, "Статус бронирования", "Booking status", "Bron holati", "Брон ҳолати"),
            if (cancelled) t(language, "Отменено", "Cancelled", "Bekor qilingan", "Бекор қилинган") else "${(current + 1).coerceAtMost(progressStages.size)} / ${progressStages.size}",
        )
        Spacer(Modifier.height(18.dp))
        progressStages.forEachIndexed { index, stage ->
            val completed = !cancelled && index < current
            val active = if (cancelled) index == 0 else index == current
            val future = index > current
            ProcessStep(language, stage, session, completed, active, future, index == progressStages.lastIndex, chrome)
        }
    }
}

@Composable
private fun ProcessStep(
    language: AppLanguage,
    stage: ProgressStage,
    session: StoredBookingSession,
    completed: Boolean,
    active: Boolean,
    future: Boolean,
    isLast: Boolean,
    chrome: AppChromeStore,
) {
    val tint = statusColor(session.effectiveStatus)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(25.dp)
                    .clip(CircleShape)
                    .background(if (completed || active) if (completed) Color(0xFF34C759) else tint else MaterialTheme.colorScheme.background)
                    .then(if (future) Modifier.border(1.6.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = .32f), CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    completed -> CupertinoIcon(CupertinoSymbol.Checkmark, null, Modifier.size(11.dp), Color.White)
                    active -> Box(Modifier.size(7.dp).clip(CircleShape).background(if (tint.luminance() < .45f) Color.White else Color.Black))
                }
            }
            if (!isLast) Box(Modifier.width(1.dp).height(if (active) 292.dp else 46.dp).background(if (completed) Color(0xFF34C759).copy(alpha = .36f) else MaterialTheme.colorScheme.onBackground.copy(alpha = .12f)))
        }
        Spacer(Modifier.width(17.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stageTitle(language, stage),
                fontSize = if (active) 18.sp else 17.sp,
                lineHeight = 22.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                color = if (future) MaterialTheme.colorScheme.onBackground.copy(alpha = .42f) else MaterialTheme.colorScheme.onBackground,
            )
            if (active) {
                Spacer(Modifier.height(4.dp))
                Text(stageSubtitle(language, stage), fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f))
                Spacer(Modifier.height(14.dp))
                ActiveStageCard(language, session, tint, chrome)
                Spacer(Modifier.height(18.dp))
            } else {
                Spacer(Modifier.height(19.dp))
            }
        }
    }
}

@Composable
private fun ActiveStageCard(language: AppLanguage, session: StoredBookingSession, tint: Color, chrome: AppChromeStore) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(tint.copy(alpha = .13f), MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)))
            .border(.8.dp, tint.copy(alpha = .22f), shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                CupertinoIcon(statusIcon(session.effectiveStatus), null, Modifier.size(16.dp), Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(activeCardTitle(language, session.effectiveStatus), fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.25).sp)
                Spacer(Modifier.height(4.dp))
                Text(activeCardBody(language, session.effectiveStatus), fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onBackground.copy(alpha = .05f)))
        ProgressFact(t(language, "Маршрут", "Route", "Yo‘nalish", "Йўналиш"), "${session.booking.route.originCode} → ${session.booking.route.outboundDestination}")
        ProgressFact(t(language, "Даты", "Dates", "Sanalar", "Саналар"), "${L10n.date(session.booking.input.startDate, language)} – ${L10n.date(session.booking.input.endDate, language)}")
        session.booking.hotelNames.makkah.takeIf { it.isNotBlank() }?.let { ProgressFact(t(language, "Отель", "Hotel", "Mehmonxona", "Меҳмонхона"), it) }
        Row(verticalAlignment = Alignment.Bottom) {
            Column {
                Text(t(language, "На паломника", "Per pilgrim", "Bir ziyoratchiga", "Бир зиёратчига"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .52f))
                Text(formatPrice(session.booking.perPilgrimUsd), fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Text(pilgrimCount(language, session.booking.input.travelers.totalPeople), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .52f))
        }
        IumrahPressable(
            onClick = { chrome.openBookingDetail(session.id) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            cornerRadius = 18.dp,
            background = MaterialTheme.colorScheme.primary,
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(t(language, "Открыть бронирование", "Open booking", "Bronni ochish", "Бронни очиш"), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.weight(1f))
                CupertinoIcon(CupertinoSymbol.ArrowRight, null, Modifier.size(17.dp), MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun ProgressFact(title: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .52f), modifier = Modifier.width(92.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

@Composable
private fun TripWalletEntry(language: AppLanguage, session: StoredBookingSession, onClick: () -> Unit) {
    IumrahPressable(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 28.dp,
        background = Color(0xFF111214),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Color.White.copy(alpha = .10f)), contentAlignment = Alignment.Center) {
                    CupertinoIcon(CupertinoSymbol.Wallet, null, Modifier.size(23.dp), Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("iumrah Wallet", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(session.displayBookingNumber, color = Color.White.copy(alpha = .58f), fontSize = 13.sp)
                }
                CupertinoIcon(CupertinoSymbol.ChevronRight, null, Modifier.size(18.dp), Color.White.copy(alpha = .55f))
            }
            Spacer(Modifier.height(18.dp))
            Text("${session.booking.route.originCode} → ${session.booking.route.outboundDestination}", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("${L10n.date(session.booking.input.startDate, language)} – ${L10n.date(session.booking.input.endDate, language)}", color = Color.White.copy(alpha = .62f), fontSize = 14.sp)
        }
    }
}

@Composable
private fun BookingStatusOverview(language: AppLanguage, session: StoredBookingSession, checkout: IumrahCheckoutResponse?) {
    val tint = statusColor(session.effectiveStatus)
    val shape = RoundedCornerShape(26.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surface).border(.7.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = .06f), shape).padding(18.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(tint.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
            CupertinoIcon(statusIcon(session.effectiveStatus), null, Modifier.size(22.dp), tint)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(statusOverviewTitle(language, session, checkout), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(statusOverviewBody(language, session, checkout), fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f))
        }
    }
}

@Composable
private fun FulfillmentCenter(language: AppLanguage, session: StoredBookingSession, checkout: IumrahCheckoutResponse?, chrome: AppChromeStore) {
    val total = checkout?.travelers?.size ?: session.booking.input.travelers.totalPeople
    val completed = checkout?.travelers?.count { it.completed } ?: 0
    val receiptReady = !checkout?.receipts.isNullOrEmpty()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(t(language, "Что нужно завершить", "What needs attention", "Nimani yakunlash kerak", "Нимани якунлаш керак"), null)
        BookingActionCard(
            icon = CupertinoSymbol.Passport,
            title = t(language, "Данные паломников", "Pilgrim details", "Ziyoratchilar ma’lumotlari", "Зиёратчилар маълумотлари"),
            subtitle = "$completed / $total",
            completed = total > 0 && completed >= total,
            onClick = { chrome.openPilgrimCheckout(session.id) },
        )
        BookingActionCard(
            icon = CupertinoSymbol.CreditCard,
            title = t(language, "Оплата", "Payment", "To‘lov", "Тўлов"),
            subtitle = if (receiptReady) t(language, "Чек получен", "Receipt received", "Chek qabul qilindi", "Чек қабул қилинди") else t(language, "Требуется действие", "Action required", "Amal kerak", "Амал керак"),
            completed = receiptReady,
            onClick = { chrome.openPilgrimCheckout(session.id) },
        )
        BookingActionCard(
            icon = CupertinoSymbol.ShieldCheck,
            title = "iumrah Security",
            subtitle = t(language, "Подтверждение личности", "Identity confirmation", "Shaxsni tasdiqlash", "Шахсни тасдиқлаш"),
            completed = session.effectiveStatus.uppercase() in setOf("BOOKING_CONFIRMED", "READY_TO_TRAVEL", "IN_TRIP", "COMPLETED"),
            onClick = { chrome.openAccountKyc(session.id) },
        )
    }
}

@Composable
private fun BookingActionCard(icon: CupertinoSymbol, title: String, subtitle: String, completed: Boolean, onClick: () -> Unit) {
    IumrahPressable(onClick = onClick, modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, background = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                CupertinoIcon(icon, null, Modifier.size(21.dp), MaterialTheme.colorScheme.onSurface.copy(alpha = .72f))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .52f))
            }
            if (completed) CupertinoIcon(CupertinoSymbol.CheckCircle, null, Modifier.size(20.dp), Color(0xFF34C759))
            else CupertinoIcon(CupertinoSymbol.ChevronRight, null, Modifier.size(17.dp), MaterialTheme.colorScheme.onSurface.copy(alpha = .38f))
        }
    }
}

@Composable
private fun TripPlanPreview(language: AppLanguage, session: StoredBookingSession, chrome: AppChromeStore) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(t(language, "План поездки", "Trip plan", "Safar rejasi", "Сафар режаси"), null)
        Spacer(Modifier.height(14.dp))
        val items = buildList {
            add(Triple(CupertinoSymbol.AirplaneTakeoff, L10n.date(session.booking.input.startDate, language), "${session.booking.route.originCode} → ${session.booking.route.outboundDestination}"))
            add(Triple(CupertinoSymbol.Hotel, session.booking.hotelNames.makkah, "Makkah · ${session.booking.stay.makkahNights} nights"))
            if (session.booking.input.includeMadinah) add(Triple(CupertinoSymbol.Hotel, session.booking.hotelNames.madinah, "Madinah · ${session.booking.stay.madinahNights ?: 0} nights"))
            add(Triple(CupertinoSymbol.AirplaneLand, L10n.date(session.booking.input.endDate, language), "${session.booking.route.returnOrigin} → ${session.booking.route.originCode}"))
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
            items.forEachIndexed { index, (icon, title, subtitle) ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { CupertinoIcon(icon, null, Modifier.size(19.dp)) }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) { Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .52f)) }
                }
                if (index != items.lastIndex) Spacer(Modifier.height(14.dp))
            }
            Spacer(Modifier.height(16.dp))
            IumrahPressable(onClick = { chrome.openBookingDetail(session.id) }, modifier = Modifier.fillMaxWidth().height(50.dp), cornerRadius = 18.dp, background = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(t(language, "Открыть полное расписание", "Open full schedule", "To‘liq jadvalni ochish", "Тўлиқ жадвални очиш"), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f)); CupertinoIcon(CupertinoSymbol.ArrowRight, null, Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
private fun TripManagement(language: AppLanguage, session: StoredBookingSession, chrome: AppChromeStore) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(t(language, "Управление поездкой", "Trip management", "Safarni boshqarish", "Сафарни бошқариш"), null)
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surface)) {
            ManagementRow(CupertinoSymbol.Suitcase, t(language, "Управлять бронированием", "Manage booking", "Bronni boshqarish", "Бронни бошқариш"), t(language, "Отели, данные, услуги и документы", "Hotels, details, services and documents", "Mehmonxona, ma’lumotlar, xizmatlar va hujjatlar", "Меҳмонхона, маълумотлар, хизматлар ва ҳужжатлар")) { chrome.openBookingDetail(session.id) }
            DividerLine()
            ManagementRow(CupertinoSymbol.PlusPerson, t(language, "Добавить паломника", "Add pilgrim", "Ziyoratchi qo‘shish", "Зиёратчи қўшиш"), t(language, "Запрос через iumrah Care", "Request via iumrah Care", "iumrah Care orqali so‘rov", "iumrah Care орқали сўров")) { chrome.navigate(AppTab.CARE) }
            DividerLine()
            ManagementRow(CupertinoSymbol.Route, t(language, "Зияраты", "Ziyarat", "Ziyorat", "Зиёрат"), t(language, "Маршрут и места посещения", "Route and places to visit", "Yo‘nalish va tashrif joylari", "Йўналиш ва ташриф жойлари")) { chrome.openBookingDetail(session.id) }
            DividerLine()
            ManagementRow(CupertinoSymbol.Plus, t(language, "Новая Umrah", "New Umrah", "Yangi Umra", "Янги Умра"), t(language, "Собрать новый пакет", "Build a new package", "Yangi paket tuzish", "Янги пакет тузиш")) { chrome.startNewTrip() }
        }
    }
}

@Composable
private fun ManagementRow(icon: CupertinoSymbol, title: String, subtitle: String, onClick: () -> Unit) {
    IumrahPressable(onClick = onClick, modifier = Modifier.fillMaxWidth(), cornerRadius = 0.dp, background = Color.Transparent, shadowElevation = 0.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            CupertinoIcon(icon, null, Modifier.size(22.dp), MaterialTheme.colorScheme.onSurface.copy(alpha = .64f))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f), maxLines = 2) }
            CupertinoIcon(CupertinoSymbol.ChevronRight, null, Modifier.size(16.dp), MaterialTheme.colorScheme.onSurface.copy(alpha = .32f))
        }
    }
}

@Composable
private fun DividerLine() { Box(Modifier.fillMaxWidth().padding(start = 50.dp).height(.7.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = .07f))) }

@Composable
private fun PastBookingsHome(
    language: AppLanguage,
    sessions: List<StoredBookingSession>,
    scope: BookingScope,
    onScope: (BookingScope) -> Unit,
    chrome: AppChromeStore,
    onDelete: suspend (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            IumrahRootPageHeader(t(language, "Бронирование", "Booking", "Bron", "Брон"), chrome)
            Spacer(Modifier.height(18.dp))
            BookingScopePicker(language, scope, onScope)
            Spacer(Modifier.height(24.dp))
        }
        if (sessions.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surface).border(.7.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = .06f), RoundedCornerShape(28.dp)).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(58.dp).clip(RoundedCornerShape(19.dp)).background(MaterialTheme.colorScheme.onBackground.copy(alpha = .055f)), contentAlignment = Alignment.Center) {
                        CupertinoIcon(CupertinoSymbol.CalendarClock, null, Modifier.size(25.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(t(language, "Прошлых поездок пока нет", "No past trips yet", "O‘tgan safarlar hozircha yo‘q", "Ўтган сафарлар ҳозирча йўқ"), fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(t(language, "Завершённые и отменённые поездки будут храниться здесь.", "Completed and cancelled trips will appear here.", "Yakunlangan va bekor qilingan safarlar shu yerda ko‘rinadi.", "Якунланган ва бекор қилинган сафарлар шу ерда кўринади."), fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f), textAlign = TextAlign.Center)
                }
            }
        } else {
            items(sessions, key = { it.id }) { session -> CompactBookingCard(language, session, chrome, onDelete) }
        }
    }
}

@Composable
private fun CompactBookingCard(language: AppLanguage, session: StoredBookingSession, chrome: AppChromeStore, onDelete: suspend (String) -> Unit) {
    IumrahPressable(onClick = { chrome.openBookingDetail(session.id) }, modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, background = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(statusColor(session.effectiveStatus).copy(alpha = .11f)), contentAlignment = Alignment.Center) {
                CupertinoIcon(CupertinoSymbol.Suitcase, null, Modifier.size(22.dp), statusColor(session.effectiveStatus))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${session.booking.route.originCode} → ${session.booking.route.outboundDestination}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${L10n.date(session.booking.input.startDate, language)} – ${L10n.date(session.booking.input.endDate, language)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .52f))
                Text(L10n.status(session.effectiveStatus, language), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = statusColor(session.effectiveStatus))
            }
            CupertinoIcon(CupertinoSymbol.ChevronRight, null, Modifier.size(17.dp), MaterialTheme.colorScheme.onSurface.copy(alpha = .34f))
        }
    }
}

@Composable
private fun EmptyBookingHome(language: AppLanguage, scope: BookingScope, onScope: (BookingScope) -> Unit, chrome: AppChromeStore) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { IumrahRootPageHeader(t(language, "Бронирование", "Booking", "Bron", "Брон"), chrome) }
        item { BookingScopePicker(language, scope, onScope) }
        item { EmptyStatusCard(language) }
        item {
            IumrahPressable(onClick = { chrome.navigate(AppTab.HOTELS) }, modifier = Modifier.fillMaxWidth().height(58.dp), cornerRadius = 20.dp, background = Color.Black) {
                Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    CupertinoIcon(CupertinoSymbol.Suitcase, null, Modifier.size(18.dp), Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text(t(language, "Смотреть готовые пакеты", "Explore Packages", "Tayyor paketlarni ko‘rish", "Тайёр пакетларни кўриш"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f)); CupertinoIcon(CupertinoSymbol.ArrowRight, null, Modifier.size(17.dp), Color.White)
                }
            }
        }
        item {
            EmptyShowcaseCard(
                imageRes = R.drawable.iumrah_configurator_hero,
                eyebrow = "Iumrah Configurator",
                badge = t(language, "5 минут", "5 minutes", "5 daqiqa", "5 дақиқа"),
                title = L10n.text("booking_hero_title", language),
                body = L10n.text("booking_hero_body", language),
                cta = L10n.text("booking_hero_cta", language),
                dark = true,
                icon = CupertinoSymbol.Sliders,
                onClick = chrome::startNewTrip,
            )
        }
        item {
            EmptyShowcaseCard(
                imageRes = R.drawable.iumrah_care_showcase,
                eyebrow = "Iumrah Care",
                badge = t(language, "За вас", "For you", "Siz uchun", "Сиз учун"),
                title = t(language, "Соберите Umrah за меня", "Build my Umrah for me", "Umramni men uchun yig‘ing", "Умрамни мен учун йиғинг"),
                body = t(language, "Расскажите даты, бюджет и пожелания. Iumrah Care соберёт для вас персональный вариант поездки.", "Tell us your dates, budget and preferences. Iumrah Care will prepare a personal Umrah option for you.", "Sanalar, budjet va istaklaringizni ayting. Iumrah Care siz uchun shaxsiy Umra variantini tayyorlaydi.", "Саналар, бюджет ва истакларингизни айтинг. Iumrah Care сиз учун шахсий Умра вариантини тайёрлайди."),
                cta = t(language, "Собрать мою Umrah", "Build my Umrah", "Mening Umramni yig‘ish", "Менинг Умрамни йиғиш"),
                dark = false,
                icon = CupertinoSymbol.HeartFill,
                onClick = { chrome.navigate(AppTab.CARE) },
            )
        }
    }
}

@Composable
private fun EmptyStatusCard(language: AppLanguage) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surface).border(.7.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = .06f), shape).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(58.dp).clip(RoundedCornerShape(19.dp)).background(MaterialTheme.colorScheme.onBackground.copy(alpha = .055f)), contentAlignment = Alignment.Center) {
            CupertinoIcon(CupertinoSymbol.Suitcase, null, Modifier.size(25.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(t(language, "Пока бронирований нет", "No bookings yet", "Hozircha bron yo‘q", "Ҳозирча брон йўқ"), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(t(language, "Пока здесь нет активных бронирований. Начните с Конфигуратора или передайте сборку iumrah Care.", "There are no active bookings here yet. Start with the Configurator or let iumrah Care prepare the trip for you.", "Hozircha bu yerda faol bronlar yo‘q. Konfiguratorni oching yoki safarni iumrah Care’ga topshiring.", "Ҳозирча бу ерда фаол бронлар йўқ. Конфигураторни очинг ёки сафарни iumrah Care’га топширинг."), fontSize = 14.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f))
        }
    }
}

@Composable
private fun EmptyShowcaseCard(
    imageRes: Int,
    eyebrow: String,
    badge: String,
    title: String,
    body: String,
    cta: String,
    dark: Boolean,
    icon: CupertinoSymbol,
    onClick: () -> Unit,
) {
    val bg = if (dark) Color.Black else Color.White
    val fg = if (dark) Color.White else Color.Black
    val buttonBg = if (dark) Color.White else Color.Black
    val buttonFg = if (dark) Color.Black else Color.White
    IumrahPressable(onClick = onClick, modifier = Modifier.fillMaxWidth(), cornerRadius = 34.dp, background = bg, shadowElevation = 0.dp) {
        Column(Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(236.dp),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CupertinoIcon(icon, null, Modifier.size(16.dp), fg.copy(alpha = .72f))
                    Spacer(Modifier.width(7.dp))
                    Text(eyebrow, color = fg.copy(alpha = .72f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = .45.sp)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.height(29.dp).clip(RoundedCornerShape(999.dp)).background(fg.copy(alpha = .08f)).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                        Text(badge, color = fg.copy(alpha = .72f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(15.dp))
                Text(title, color = fg, fontSize = 31.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.75).sp)
                Spacer(Modifier.height(8.dp))
                Text(body, color = fg.copy(alpha = .64f), fontSize = 15.sp, lineHeight = 21.sp)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(18.dp)).background(buttonBg).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(cta, color = buttonFg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f)); CupertinoIcon(CupertinoSymbol.ArrowRight, null, Modifier.size(18.dp), buttonFg)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(title, fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.35).sp, modifier = Modifier.weight(1f))
        trailing?.let { Text(it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .52f)) }
    }
}

private fun progressIndex(status: String): Int = when (status.uppercase()) {
    "NEW", "CREATED" -> 0
    "AVAILABILITY_CHECK" -> 1
    "PAYMENT_PENDING" -> 2
    "PAID", "BOOKING_CONFIRMED" -> 3
    "DOCUMENTS_READY", "READY_TO_TRAVEL" -> 4
    "IN_TRIP" -> 5
    "COMPLETED" -> 6
    "CANCELLED" -> 0
    else -> 1
}

private fun statusColor(status: String): Color = when (status.uppercase()) {
    "NEW", "AVAILABILITY_CHECK" -> Color(0xFF007AFF)
    "PAYMENT_PENDING" -> Color(0xFFFF9500)
    "PAID", "BOOKING_CONFIRMED" -> Color(0xFF34C759)
    "DOCUMENTS_READY", "READY_TO_TRAVEL" -> Color(0xFF32ADE6)
    "IN_TRIP" -> Color(0xFFAF52DE)
    "COMPLETED" -> Color(0xFF34C759)
    "CANCELLED" -> Color(0xFFFF3B30)
    else -> Color(0xFF8E8E93)
}

private fun statusIcon(status: String): CupertinoSymbol = when (status.uppercase()) {
    "NEW", "AVAILABILITY_CHECK" -> CupertinoSymbol.CalendarClock
    "PAYMENT_PENDING" -> CupertinoSymbol.CreditCard
    "PAID", "BOOKING_CONFIRMED" -> CupertinoSymbol.Document
    "DOCUMENTS_READY", "READY_TO_TRAVEL" -> CupertinoSymbol.CheckCircle
    "IN_TRIP" -> CupertinoSymbol.Airplane
    "COMPLETED" -> CupertinoSymbol.CheckCircle
    "CANCELLED" -> CupertinoSymbol.ExclamationCircle
    else -> CupertinoSymbol.CalendarClock
}

private fun activeCardTitle(language: AppLanguage, status: String): String = when (status.uppercase()) {
    "NEW", "AVAILABILITY_CHECK" -> t(language, "Проверяем ваш пакет", "Checking your package", "Paketingiz tekshirilmoqda", "Пакетингиз текширилмоқда")
    "PAYMENT_PENDING" -> t(language, "Наличие подтверждено", "Availability confirmed", "Mavjudlik tasdiqlandi", "Мавжудлик тасдиқланди")
    "PAID", "BOOKING_CONFIRMED" -> t(language, "Бронирование подтверждено", "Booking confirmed", "Bron tasdiqlandi", "Брон тасдиқланди")
    "DOCUMENTS_READY", "READY_TO_TRAVEL" -> t(language, "Готово к поездке", "Ready to travel", "Safarga tayyor", "Сафарга тайёр")
    "IN_TRIP" -> t(language, "Ваша Umrah идёт", "Your Umrah is underway", "Umrangiz davom etmoqda", "Умрангиз давом этмоқда")
    "COMPLETED" -> t(language, "Поездка завершена", "Trip completed", "Safar yakunlandi", "Сафар якунланди")
    else -> L10n.status(status, language)
}

private fun activeCardBody(language: AppLanguage, status: String): String = when (status.uppercase()) {
    "NEW", "AVAILABILITY_CHECK" -> t(language, "iumrah подтверждает выбранные позиции. Пока идёт проверка, можно заранее заполнить анкеты паломников.", "iumrah is confirming the selected items. While the check is running, you can complete pilgrim forms in advance.", "iumrah tanlangan xizmatlarni tasdiqlamoqda. Tekshiruv davomida ziyoratchilar anketalarini oldindan to‘ldirish mumkin.", "iumrah танланган хизматларни тасдиқламоқда. Текширув давомида зиёратчилар анкеталарини олдиндан тўлдириш мумкин.")
    "PAYMENT_PENDING" -> t(language, "Проверьте данные паломников и перейдите к оплате, чтобы закрепить бронирование.", "Review pilgrim details and continue to payment to secure the booking.", "Bronni mustahkamlash uchun ziyoratchilar ma’lumotlarini tekshiring va to‘lovga o‘ting.", "Бронни мустаҳкамлаш учун зиёратчилар маълумотларини текширинг ва тўловга ўтинг.")
    "PAID", "BOOKING_CONFIRMED" -> t(language, "Перелёт, проживание и выбранные услуги закреплены. Все детали доступны внутри бронирования.", "Flight, stay and selected services are secured. Full details are available inside the booking.", "Parvoz, yashash va tanlangan xizmatlar band qilindi. Barcha tafsilotlar bron ichida mavjud.", "Парвоз, яшаш ва танланган хизматлар банд қилинди. Барча тафсилотлар брон ичида мавжуд.")
    "DOCUMENTS_READY", "READY_TO_TRAVEL" -> t(language, "Проверьте билеты, бронирования и документы перед выездом.", "Review tickets, reservations and travel documents before departure.", "Jo‘nashdan oldin chiptalar, bronlar va hujjatlarni tekshiring.", "Жўнашдан олдин чипталар, бронлар ва ҳужжатларни текширинг.")
    "IN_TRIP" -> t(language, "Маршрут, отель, расписание и помощь iumrah остаются под рукой на протяжении поездки.", "Your route, hotel, schedule and iumrah support stay close throughout the trip.", "Yo‘nalish, mehmonxona, jadval va iumrah yordami safar davomida doimo yoningizda.", "Йўналиш, меҳмонхона, жадвал ва iumrah ёрдами сафар давомида доимо ёнингизда.")
    else -> t(language, "Статус обновится автоматически при следующем изменении.", "The status will update automatically when it changes.", "Holat keyingi o‘zgarishda avtomatik yangilanadi.", "Ҳолат кейинги ўзгаришда автоматик янгиланади.")
}

private fun statusOverviewTitle(language: AppLanguage, session: StoredBookingSession, checkout: IumrahCheckoutResponse?): String {
    val status = session.effectiveStatus.uppercase()
    val total = checkout?.travelers?.size ?: session.booking.input.travelers.totalPeople
    val completed = checkout?.travelers?.count { it.completed } ?: 0
    val travelersReady = total > 0 && completed >= total
    val receiptReady = !checkout?.receipts.isNullOrEmpty()
    return when (status) {
        "NEW", "AVAILABILITY_CHECK" -> t(language, "Проверяем наличие", "Checking availability", "Mavjudlik tekshirilmoqda", "Мавжудлик текширилмоқда")
        "PAYMENT_PENDING" -> when {
            receiptReady && travelersReady -> t(language, "Проверяем оплату", "Checking payment", "To‘lov tekshirilmoqda", "Тўлов текширилмоқда")
            receiptReady -> t(language, "Ожидаем данные паломников", "Waiting for pilgrim details", "Ziyoratchilar ma’lumotlari kutilmoqda", "Зиёратчилар маълумотлари кутилмоқда")
            travelersReady -> t(language, "Ожидаем оплату", "Waiting for payment", "To‘lov kutilmoqda", "Тўлов кутилмоқда")
            else -> t(language, "Ожидаем оплату и данные паломников", "Waiting for payment and pilgrim details", "To‘lov va ziyoratchilar ma’lumotlari kutilmoqda", "Тўлов ва зиёратчилар маълумотлари кутилмоқда")
        }
        "PAID", "BOOKING_CONFIRMED" -> t(language, "Готовим документы", "Preparing documents", "Hujjatlar tayyorlanmoqda", "Ҳужжатлар тайёрланмоқда")
        else -> L10n.status(session.effectiveStatus, language)
    }
}

private fun statusOverviewBody(language: AppLanguage, session: StoredBookingSession, checkout: IumrahCheckoutResponse?): String {
    val status = session.effectiveStatus.uppercase()
    val total = checkout?.travelers?.size ?: session.booking.input.travelers.totalPeople
    val completed = checkout?.travelers?.count { it.completed } ?: 0
    val travelersReady = total > 0 && completed >= total
    val receiptReady = !checkout?.receipts.isNullOrEmpty()
    return when (status) {
        "NEW", "AVAILABILITY_CHECK" -> t(language, "Пока iumrah подтверждает авиабилеты, отель и услуги, можно заранее заполнить анкеты всех паломников — это ускорит следующий этап.", "While iumrah confirms flights, hotel and services, you can complete every pilgrim form in advance to make the next step faster.", "iumrah aviachiptalar, mehmonxona va xizmatlarni tasdiqlayotganda barcha ziyoratchilar anketalarini oldindan to‘ldirishingiz mumkin — keyingi bosqich tezroq o‘tadi.", "iumrah авиачипталар, меҳмонхона ва хизматларни тасдиқлаётганда барча зиёратчилар анкеталарини олдиндан тўлдиришингиз мумкин — кейинги босқич тезроқ ўтади.")
        "PAYMENT_PENDING" -> when {
            receiptReady && travelersReady -> t(language, "Чек и анкеты получены. Бронирование автоматически обновится после проверки оплаты.", "Receipt and pilgrim forms are received. The booking will update automatically after payment verification.", "Chek va anketalar qabul qilindi. To‘lov tekshirilgach bron avtomatik yangilanadi.", "Чек ва анкеталар қабул қилинди. Тўлов текширилгач брон автоматик янгиланади.")
            receiptReady -> t(language, "Оплата получена. Осталось заполнить паспортные данные всех паломников.", "Payment is received. Complete the passport details for every pilgrim.", "To‘lov qabul qilindi. Endi barcha ziyoratchilarning pasport ma’lumotlarini to‘ldiring.", "Тўлов қабул қилинди. Энди барча зиёратчиларнинг паспорт маълумотларини тўлдиринг.")
            travelersReady -> t(language, "Анкеты паломников заполнены. Осталось оплатить по реквизитам и прикрепить чек.", "Pilgrim forms are complete. Pay using the provided details and attach the receipt.", "Ziyoratchilar anketalari tayyor. Rekvizitlar bo‘yicha to‘lang va chekni biriktiring.", "Зиёратчилар анкеталари тайёр. Реквизитлар бўйича тўланг ва чекни бириктиринг.")
            else -> t(language, "Заполните паспортные данные паломников и оплатите бронирование. Оба действия можно выполнить в любом порядке.", "Complete pilgrim passport details and pay for the booking. You can do these in either order.", "Ziyoratchilar pasport ma’lumotlarini to‘ldiring va bron uchun to‘lang. Ikkalasini istalgan tartibda bajarish mumkin.", "Зиёратчилар паспорт маълумотларини тўлдиринг ва брон учун тўланг. Иккаласини исталган тартибда бажариш мумкин.")
        }
        "PAID", "BOOKING_CONFIRMED" -> t(language, "Оплата и данные получены. iumrah готовит билеты, подтверждения и документы поездки.", "Payment and details are received. iumrah is preparing tickets, confirmations and travel documents.", "To‘lov va ma’lumotlar qabul qilindi. iumrah chiptalar, tasdiqlar va safar hujjatlarini tayyorlamoqda.", "Тўлов ва маълумотлар қабул қилинди. iumrah чипталар, тасдиқлар ва сафар ҳужжатларини тайёрламоқда.")
        else -> t(language, "Статус обновится автоматически при следующем изменении.", "The status will update automatically when it changes.", "Holat keyingi o‘zgarishda avtomatik yangilanadi.", "Ҳолат кейинги ўзгаришда автоматик янгиланади.")
    }
}

private fun stageTitle(language: AppLanguage, stage: ProgressStage) = t(language, stage.titleRu, stage.titleEn, stage.titleUz, stage.titleCy)
private fun stageSubtitle(language: AppLanguage, stage: ProgressStage) = t(language, stage.subtitleRu, stage.subtitleEn, stage.subtitleUz, stage.subtitleCy)

private fun formatPrice(amount: Double): String = NumberFormat.getCurrencyInstance(Locale.US).apply {
    currency = Currency.getInstance("USD")
    maximumFractionDigits = 0
}.format(amount)

private fun pilgrimCount(language: AppLanguage, count: Int): String = when (language) {
    AppLanguage.RUSSIAN -> "$count паломн."
    AppLanguage.ENGLISH -> if (count == 1) "1 pilgrim" else "$count pilgrims"
    AppLanguage.UZBEK -> "$count ziyoratchi"
    AppLanguage.UZBEK_CYRILLIC -> "$count зиёратчи"
}

private fun t(language: AppLanguage, ru: String, en: String, uz: String, cy: String): String = when (language) {
    AppLanguage.RUSSIAN -> ru
    AppLanguage.ENGLISH -> en
    AppLanguage.UZBEK -> uz
    AppLanguage.UZBEK_CYRILLIC -> cy
}

private fun Color.luminance(): Float = .2126f * red + .7152f * green + .0722f * blue
