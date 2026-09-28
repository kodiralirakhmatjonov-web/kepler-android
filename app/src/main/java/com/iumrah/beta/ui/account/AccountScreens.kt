package com.iumrah.beta.ui.account
import com.iumrah.beta.ui.cupertino.Icon
import androidx.compose.foundation.layout.statusBarsPadding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iumrah.beta.R
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppAppearance
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.core.settings.AppSettingsStore
import com.iumrah.beta.data.account.IumrahAccountService
import com.iumrah.beta.data.account.IumrahAccountStore
import com.iumrah.beta.data.booking.BookingStore
import com.iumrah.beta.data.notification.ClientNotificationStore
import com.iumrah.beta.models.account.IumrahAccountProfile
import com.iumrah.beta.models.account.IumrahSecurityOverview
import com.iumrah.beta.models.account.IumrahTravelerForm
import com.iumrah.beta.models.booking.StoredBookingSession
import com.iumrah.beta.ui.booking.IumrahSecurityConfirmationPanel
import com.iumrah.beta.ui.cupertino.CupertinoIcon
import com.iumrah.beta.ui.cupertino.CupertinoSymbol
import kotlinx.coroutines.launch

private val Care = Color(0xFF30B0C7)
private val Signal = Color(0xFF1677FF)
private val Danger = Color(0xFFFF3B30)
private val Success = Color(0xFF34C759)
private val IosGray = Color(0xFF8E8E93)
private val CardRadius = 26.dp
private val PagePad = 18.dp

private fun tr(l: AppLanguage, en: String, ru: String, uz: String, cy: String) = when (l) {
    AppLanguage.ENGLISH -> en
    AppLanguage.RUSSIAN -> ru
    AppLanguage.UZBEK -> uz
    AppLanguage.UZBEK_CYRILLIC -> cy
}

@Composable
fun AccountRootScreen(
    language: AppLanguage,
    accountStore: IumrahAccountStore,
    bookingStore: BookingStore,
    settingsStore: AppSettingsStore,
    notifications: ClientNotificationStore,
    chrome: AppChromeStore,
) {
    val accountState by accountStore.state.collectAsState()
    val bookings by bookingStore.state.collectAsState()
    val settings by settingsStore.state.collectAsState()
    val signalState by notifications.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val profile = accountState.account
    var flip by remember { mutableStateOf(false) }
    var fullID by remember { mutableStateOf(false) }
    var signOutConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = PagePad, end = PagePad, top = 12.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { AccountHeader(language, profile != null) }
        if (profile != null) {
            item {
                IdentityBlock(profile, language, flip, { flip = !flip }, { fullID = true })
            }
            bookings.sessions.firstOrNull()?.let { active ->
                item { TripWalletEntry(active, language, onOpen = { chrome.openBookingDetail(active.id) }) }
            }
            item { WalletSection(profile, language) }
            item { TripsSection(bookings.sessions, language, chrome) }
            item {
                SectionCard(
                    title = tr(language, "Travel companions", "Спутники", "Hamrohlar", "Ҳамроҳлар"),
                    subtitle = tr(language, "Pilgrims saved in your bookings", "Паломники из ваших бронирований", "Bronlaringizdagi ziyoratchilar", "Бронларингиздаги зиёратчилар"),
                    icon = CupertinoSymbol.Persons,
                ) {
                    SettingsRow(CupertinoSymbol.Persons, tr(language, "Travel companions", "Спутники", "Hamrohlar", "Ҳамроҳлар"), tr(language, "Names, passports and traveler data", "Имена, паспорта и данные паломников", "Ismlar, pasportlar va sayohatchi ma’lumotlari", "Исмлар, паспортлар ва саёҳатчи маълумотлари")) { chrome.openAccountTravelers() }
                }
            }
            item { PaymentSecuritySection(language, bookings.sessions.firstOrNull(), true, chrome) }
            item { ProfileSection(profile, language) { chrome.openAccountProfileEditor() } }
            item { SettingsSection(language, settings.appearance, signalState.unreadCount, chrome, context) }
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Danger.copy(alpha = .075f)).clickable { signOutConfirm = true }.padding(horizontal = 18.dp).height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CupertinoIcon(CupertinoSymbol.SignOut, null, Modifier.size(20.dp), Danger)
                    Text(tr(language, "Sign out", "Выйти из аккаунта", "Akkauntdan chiqish", "Аккаунтдан чиқиш"), color = Danger, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            item { LockedIdentityCard(language) }
            item { GuestLoginCard(language, accountStore, bookingStore) }
            item { PaymentSecuritySection(language, bookings.sessions.firstOrNull(), false, chrome) }
            item { SettingsSection(language, settings.appearance, signalState.unreadCount, chrome, context) }
        }
    }

    if (fullID && profile != null) {
        AlertDialog(
            onDismissRequest = { fullID = false },
            confirmButton = { TextButton(onClick = { fullID = false }) { Text(tr(language, "Close", "Закрыть", "Yopish", "Ёпиш")) } },
            title = { Text("iumrah ID", fontWeight = FontWeight.Bold) },
            text = { IdentityFace(profile, language, back = flip, modifier = Modifier.fillMaxWidth().height(226.dp)) },
        )
    }
    if (signOutConfirm) {
        AlertDialog(
            onDismissRequest = { signOutConfirm = false },
            title = { Text(tr(language, "Sign out of iumrah?", "Выйти из iumrah?", "iumrah akkauntidan chiqasizmi?", "iumrah аккаунтидан чиқасизми?")) },
            text = { Text(tr(language, "Your trips stay safely linked to your iumrah account. You will need to sign in again on this device.", "Ваши поездки останутся безопасно привязаны к аккаунту iumrah. На этом устройстве потребуется войти снова.", "Safarlaringiz iumrah akkauntingizga xavfsiz bog‘langan holda qoladi. Bu qurilmada qayta kirishingiz kerak bo‘ladi.", "Сафарларингиз iumrah аккаунтингизга хавфсиз боғланган ҳолда қолади. Бу қурилмада қайта киришингиз керак бўлади.")) },
            confirmButton = { TextButton(onClick = { signOutConfirm = false; scope.launch { accountStore.logout() } }) { Text(tr(language, "Sign out", "Выйти", "Chiqish", "Чиқиш"), color = Danger) } },
            dismissButton = { TextButton(onClick = { signOutConfirm = false }) { Text(tr(language, "Cancel", "Отмена", "Bekor qilish", "Бекор қилиш")) } },
        )
    }
}

@Composable
private fun AccountHeader(language: AppLanguage, authenticated: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Account", fontSize = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
            Text(
                if (authenticated) tr(language, "Your iumrah profile and trips", "Ваш профиль и поездки iumrah", "iumrah profilingiz va safarlaringiz", "iumrah профилингиз ва сафарларингиз")
                else tr(language, "Sign in or create your permanent iumrah account", "Войдите или создайте постоянный аккаунт iumrah", "Doimiy iumrah akkauntingizga kiring yoki uni yarating", "Доимий iumrah аккаунтингизга киринг ёки уни яратинг"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconBadge(if (authenticated) CupertinoSymbol.CheckCircle else CupertinoSymbol.PersonCircle, if (authenticated) Success else Care, 50.dp, 24.dp, true)
    }
}

@Composable
private fun IdentityBlock(profile: IumrahAccountProfile, language: AppLanguage, flipped: Boolean, onFlip: () -> Unit, onFull: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        IdentityFace(profile, language, back = flipped, modifier = Modifier.fillMaxWidth().height(226.dp).clickable(onClick = onFlip))
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                CupertinoIcon(if (flipped) CupertinoSymbol.Refresh else CupertinoSymbol.HandRaised, null, Modifier.size(15.dp), IosGray)
                Text(if (flipped) tr(language, "Front side", "Лицевая сторона", "Old tomoni", "Олд томони") else tr(language, "Tap to flip", "Нажмите, чтобы перевернуть", "Aylantirish uchun bosing", "Айлантириш учун босинг"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = IosGray)
            }
            Surface(onClick = onFull, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .06f)) {
                Row(Modifier.padding(horizontal = 12.dp).height(36.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CupertinoIcon(CupertinoSymbol.IdentityCard, null, Modifier.size(14.dp), MaterialTheme.colorScheme.onSurface)
                    Text(tr(language, "Full screen", "На весь экран", "To‘liq ekran", "Тўлиқ экран"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun IdentityFace(profile: IumrahAccountProfile, language: AppLanguage, back: Boolean, modifier: Modifier = Modifier) {
    val first = profile.firstName.ifBlank { profile.displayName.substringBefore(' ').ifBlank { tr(language, "Pilgrim", "Паломник", "Ziyoratchi", "Зиёратчи") } }
    val last = profile.lastName.ifBlank { profile.displayName.substringAfter(' ', "—").ifBlank { "—" } }
    val shape = RoundedCornerShape(30.dp)
    Box(modifier.clip(shape).background(Color.White).border(1.dp, Color.Black.copy(alpha = .07f), shape).shadow(1.dp, shape)) {
        if (!back) {
            Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("iumrah ID", color = Color.Black, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(tr(language, "PERMANENT PILGRIM ACCOUNT", "ПОСТОЯННЫЙ АККАУНТ ПАЛОМНИКА", "DOIMIY ZIYORATCHI AKKAUNTI", "ДОИМИЙ ЗИЁРАТЧИ АККАУНТИ"), color = Color.Black.copy(alpha = .45f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    }
                    CupertinoIcon(CupertinoSymbol.SignalWave, null, Modifier.size(25.dp), Color.Black.copy(alpha = .42f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(34.dp)) {
                    IdFact(tr(language, "First name", "Имя", "Ism", "Исм"), first)
                    IdFact(tr(language, "Last name", "Фамилия", "Familiya", "Фамилия"), last)
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("UMR ID", color = Color.Black.copy(alpha = .48f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(normalizedID(profile.iumrahID), color = Color.Black, fontSize = 25.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.2.sp, maxLines = 1)
                }
            }
        } else {
            Row(Modifier.fillMaxSize().padding(22.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("iumrah ID", color = Color.Black, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(tr(language, "Digital pilgrim identity", "Цифровая ID-карта паломника", "Raqamli ziyoratchi ID", "Рақамли зиёратчи ID"), color = Color.Black.copy(alpha = .54f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("UMR ID", color = Color.Black.copy(alpha = .45f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(normalizedID(profile.iumrahID), color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("iumrah.app", color = Color.Black.copy(alpha = .48f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                }
                PseudoQR(profile.iumrahID, Modifier.size(120.dp))
            }
        }
    }
}

@Composable private fun IdFact(title: String, value: String) { Column { Text(title.uppercase(), color = Color.Black.copy(alpha = .44f), fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(value, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1) } }
private fun normalizedID(raw: String): String = raw.trim().uppercase().removePrefix("UMR-").let { if (it.isBlank()) "UMR —" else "UMR $it" }

@Composable
private fun PseudoQR(seed: String, modifier: Modifier) {
    val hash = remember(seed) { seed.fold(17) { a, c -> a * 31 + c.code } }
    Canvas(modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).padding(8.dp)) {
        val n = 21; val cell = size.minDimension / n
        fun finder(x: Int, y: Int) { drawRect(Color.Black, Offset(x*cell,y*cell), androidx.compose.ui.geometry.Size(7*cell,7*cell)); drawRect(Color.White, Offset((x+1)*cell,(y+1)*cell), androidx.compose.ui.geometry.Size(5*cell,5*cell)); drawRect(Color.Black, Offset((x+2)*cell,(y+2)*cell), androidx.compose.ui.geometry.Size(3*cell,3*cell)) }
        finder(0,0); finder(14,0); finder(0,14)
        for (y in 0 until n) for (x in 0 until n) if (!((x<7&&y<7)||(x>=14&&y<7)||(x<7&&y>=14))) if (((x*13+y*17+hash) xor (hash shr ((x+y)%8))) and 3 == 0) drawRect(Color.Black, Offset(x*cell,y*cell), androidx.compose.ui.geometry.Size(cell,cell))
    }
}

@Composable
private fun TripWalletEntry(session: StoredBookingSession, language: AppLanguage, onOpen: () -> Unit) {
    Surface(onClick = onOpen, shape = RoundedCornerShape(24.dp), color = Color.Black) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            IconBadge(CupertinoSymbol.Wallet, Color.White, 46.dp, 20.dp, darkFill = true)
            Column(Modifier.weight(1f)) {
                Text(tr(language, "Your trip wallet", "Кошелёк поездки", "Safar hamyoningiz", "Сафар ҳамёнингиз"), color = Color.White, fontWeight = FontWeight.Bold)
                Text("${session.displayBookingNumber} · ${session.booking.route.originCode} → ${session.booking.route.outboundDestination}", color = Color.White.copy(alpha = .65f), fontSize = 12.sp)
            }
            CupertinoIcon(CupertinoSymbol.ChevronRight, null, Modifier.size(17.dp), Color.White.copy(alpha = .65f))
        }
    }
}

@Composable
private fun WalletSection(profile: IumrahAccountProfile, language: AppLanguage) {
    CardBlock {
        Row(horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(CupertinoSymbol.Wallet, Color.White, 48.dp, 20.dp, darkFill = true)
            Column(Modifier.weight(1f)) {
                Text(tr(language, "iumrah ID in Apple Wallet", "iumrah ID в Apple Wallet", "iumrah ID Apple Wallet’da", "iumrah ID Apple Wallet’да"), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(tr(language, "Keep your digital pilgrim ID and QR code available from Wallet.", "Храните цифровую ID-карту паломника и QR-код прямо в Wallet.", "Raqamli ziyoratchi ID va QR-kodni Wallet’da saqlang.", "Рақамли зиёратчи ID ва QR-кодни Wallet’да сақланг."), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 13.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("UMR ID", fontSize = 10.sp, color = IosGray, fontWeight = FontWeight.Bold); Text(normalizedID(profile.iumrahID), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=.06f)) { Text(tr(language, "Wallet unavailable", "Wallet недоступен", "Wallet mavjud emas", "Wallet мавжуд эмас"), Modifier.padding(horizontal=12.dp, vertical=10.dp), fontSize=11.sp, color=IosGray, fontWeight=FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun TripsSection(sessions: List<StoredBookingSession>, language: AppLanguage, chrome: AppChromeStore) {
    SectionCard(tr(language, "My trips", "Мои поездки", "Safarlarim", "Сафарларим"), tr(language, "Bookings linked to your iumrah account", "Бронирования, привязанные к аккаунту iumrah", "iumrah akkauntingizga bog‘langan bronlar", "iumrah аккаунтингизга боғланган бронлар"), CupertinoSymbol.Suitcase) {
        if (sessions.isEmpty()) EmptyRow(CupertinoSymbol.Suitcase, tr(language, "No linked trips yet", "Пока нет привязанных поездок", "Hali bog‘langan safarlar yo‘q", "Ҳали боғланган сафарлар йўқ"))
        sessions.forEachIndexed { index, session ->
            if (index > 0) HorizontalDivider(Modifier.padding(start=54.dp))
            SettingsRow(CupertinoSymbol.Airplane, session.displayBookingNumber, "${session.booking.input.startDate} · ${session.booking.route.originCode} → ${session.booking.route.outboundDestination}") { chrome.openBookingDetail(session.id) }
        }
    }
}

@Composable
private fun ProfileSection(profile: IumrahAccountProfile, language: AppLanguage, onEdit: () -> Unit) {
    CardBlock {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { SectionHeader(CupertinoSymbol.PersonCircle, tr(language,"Account details","Данные аккаунта","Akkaunt ma’lumotlari","Аккаунт маълумотлари"), tr(language,"Identity and contact details","Личные и контактные данные","Shaxsiy va aloqa ma’lumotlari","Шахсий ва алоқа маълумотлари")) }
            Surface(onClick=onEdit, shape=RoundedCornerShape(18.dp), color=MaterialTheme.colorScheme.onSurface.copy(alpha=.06f)) { Text(tr(language,"Edit","Изменить","Tahrirlash","Таҳрирлаш"), Modifier.padding(horizontal=12.dp,vertical=10.dp), fontSize=12.sp,fontWeight=FontWeight.Bold) }
        }
        Spacer(Modifier.height(12.dp))
        SummaryRow(CupertinoSymbol.PersonCircle,tr(language,"Name","Имя","Ism","Исм"),profile.displayName.ifBlank { listOf(profile.firstName,profile.lastName).filter{it.isNotBlank()}.joinToString(" ") })
        HorizontalDivider(Modifier.padding(start=52.dp)); SummaryRow(CupertinoSymbol.Phone,tr(language,"Phone","Телефон","Telefon","Телефон"),profile.phone)
        HorizontalDivider(Modifier.padding(start=52.dp)); SummaryRow(CupertinoSymbol.Mail,"Email",profile.email)
        HorizontalDivider(Modifier.padding(start=52.dp)); SummaryRow(CupertinoSymbol.Send,"Telegram",profile.telegram)
        HorizontalDivider(Modifier.padding(start=52.dp)); SummaryRow(CupertinoSymbol.Message,"WhatsApp",profile.whatsapp)
    }
}

@Composable private fun SummaryRow(icon: CupertinoSymbol,title:String,value:String){ Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){IconBadge(icon,Care,38.dp,14.dp); Column{Text(title,fontSize=11.sp,color=IosGray);Text(value.ifBlank{"—"},fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=if(value.isBlank()) IosGray else MaterialTheme.colorScheme.onSurface,maxLines=2)}} }

@Composable
private fun PaymentSecuritySection(language: AppLanguage, trip: StoredBookingSession?, authenticated: Boolean, chrome: AppChromeStore) {
    SectionCard(tr(language,"Payment & security","Оплата и безопасность","To‘lov va xavfsizlik","Тўлов ва хавфсизлик"),tr(language,"Payment, policies, KYC and account protection","Оплата, правила, KYC и защита аккаунта","To‘lov, qoidalar, KYC va akkaunt himoyasi","Тўлов, қоидалар, KYC ва аккаунт ҳимояси"),CupertinoSymbol.LockShield) {
        SettingsRow(CupertinoSymbol.CreditCard,tr(language,"Payment","Оплата","To‘lov","Тўлов"),tr(language,"Method, confirmation and payment security","Способ, подтверждение и безопасность платежа","Usul, tasdiqlash va to‘lov xavfsizligi","Усул, тасдиқлаш ва тўлов хавфсизлиги")){chrome.openAccountPolicy("payment")}
        HorizontalDivider(Modifier.padding(start=54.dp))
        SettingsRow(CupertinoSymbol.UndoCircle,tr(language,"Refund policy","Правила возврата","Qaytarish qoidalari","Қайтариш қоидалари"),tr(language,"Flights, hotels, transfer and services","Авиабилеты, отели, трансфер и сервисы","Aviachipta, mehmonxona, transfer va xizmatlar","Авиачипта, меҳмонхона, трансфер ва хизматлар")){chrome.openAccountPolicy("refund")}
        HorizontalDivider(Modifier.padding(start=54.dp))
        SettingsRow(CupertinoSymbol.HandRaised,tr(language,"Privacy","Конфиденциальность","Maxfiylik","Махфийлик"),tr(language,"Personal data and privacy","Персональные данные и конфиденциальность","Shaxsiy ma’lumotlar va maxfiylik","Шахсий маълумотлар ва махфийлик")){chrome.openAccountPolicy("privacy")}
        HorizontalDivider(Modifier.padding(start=54.dp))
        if(trip!=null) SettingsRow(CupertinoSymbol.IdentityCard,"KYC · iumrah Security",tr(language,"Identity confirmation for booking ${trip.displayBookingNumber}","Подтверждение личности для брони ${trip.displayBookingNumber}","${trip.displayBookingNumber} broni uchun shaxsni tasdiqlash","${trip.displayBookingNumber} брони учун шахсни тасдиқлаш")){chrome.openAccountKyc(trip.id)}
        else SettingsRow(CupertinoSymbol.IdentityCard,"KYC · iumrah Security",tr(language,"Available when you have a booking","Доступно после создания бронирования","Bron yaratilgandan keyin mavjud","Брон яратилгандан кейин мавжуд"),enabled=false){}
        if(authenticated){HorizontalDivider(Modifier.padding(start=54.dp));SettingsRow(CupertinoSymbol.LockShield,tr(language,"Account security","Безопасность аккаунта","Akkaunt xavfsizligi","Аккаунт хавфсизлиги"),tr(language,"Apple, Google and active sessions","Apple, Google и активные сеансы","Apple, Google va faol seanslar","Apple, Google ва фаол сеанслар")){chrome.openAccountSecurity()}}
    }
}

@Composable
private fun SettingsSection(language: AppLanguage, appearance: AppAppearance, unread: Int, chrome: AppChromeStore, context: android.content.Context) {
    SectionCard(tr(language,"Settings","Настройки","Sozlamalar","Созламалар"),tr(language,"Language, appearance and notifications","Язык, оформление и уведомления","Til, ko‘rinish va bildirishnomalar","Тил, кўриниш ва билдиришномалар"),CupertinoSymbol.Gear) {
        SettingsRow(CupertinoSymbol.Globe,tr(language,"Language","Язык","Til","Тил"),languageTitle(language)){chrome.openAccountLanguage()}
        HorizontalDivider(Modifier.padding(start=54.dp))
        SettingsRow(CupertinoSymbol.HalfCircle,tr(language,"Appearance","Оформление","Ko‘rinish","Кўриниш"),appearanceTitle(appearance,language)){chrome.openAccountAppearance()}
        HorizontalDivider(Modifier.padding(start=54.dp))
        SettingsRow(CupertinoSymbol.BellSignal,"iumrah Signal",if(unread>0) tr(language,"$unread new","$unread новых","$unread yangi","$unread янги") else tr(language,"All caught up","Новых нет","Yangi yo‘q","Янги йўқ")){chrome.openAccountSignals()}
        HorizontalDivider(Modifier.padding(start=54.dp))
        SettingsRow(CupertinoSymbol.BellBadge,tr(language,"Notifications","Уведомления","Bildirishnomalar","Билдиришномалар"),tr(language,"System notification settings","Системные настройки уведомлений","Tizim bildirishnoma sozlamalari","Тизим билдиришнома созламалари")){
            val intent=Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); runCatching{context.startActivity(intent)}
        }
    }
}

@Composable
private fun LockedIdentityCard(language:AppLanguage){
    Box(Modifier.fillMaxWidth().height(205.dp).clip(RoundedCornerShape(30.dp)).background(Color.White).border(1.dp,Color.Black.copy(alpha=.07f),RoundedCornerShape(30.dp))){
        Column(Modifier.fillMaxSize().padding(22.dp),verticalArrangement=Arrangement.SpaceBetween){
            Row{Column(Modifier.weight(1f)){Text("iumrah ID",color=Color.Black,fontSize=28.sp,fontWeight=FontWeight.Bold);Text(tr(language,"PERMANENT PILGRIM ACCOUNT","ПОСТОЯННЫЙ АККАУНТ ПАЛОМНИКА","DOIMIY ZIYORATCHI AKKAUNTI","ДОИМИЙ ЗИЁРАТЧИ АККАУНТИ"),fontSize=9.sp,fontWeight=FontWeight.Bold,color=Color.Black.copy(alpha=.42f),letterSpacing=1.2.sp)};IconBadge(CupertinoSymbol.Lock,Color.Black,42.dp,18.dp)}
            Text(tr(language,"Sign in once to restore all trips after reinstalling the app and automatically link future bookings to the same ID.","Войдите один раз, чтобы восстанавливать все поездки после переустановки приложения и автоматически привязывать новые брони к одному ID.","Ilovani qayta o‘rnatgandan keyin barcha safarlarni tiklash va yangi bronlarni bitta ID ga bog‘lash uchun kiring.","Иловани қайта ўрнатгандан кейин барча сафарларни тиклаш ва янги бронларни битта ID га боғлаш учун киринг."),color=Color.Black.copy(alpha=.62f),fontSize=13.sp)
        }
    }
}

@Composable
private fun GuestLoginCard(language:AppLanguage,accountStore:IumrahAccountStore,bookingStore:BookingStore){
    val scope=rememberCoroutineScope(); var register by remember{mutableStateOf(false)}; var method by remember{mutableStateOf("sms")}
    var identifier by remember{mutableStateOf("")};var password by remember{mutableStateOf("")};var code by remember{mutableStateOf("")};var challenge by remember{mutableStateOf<String?>(null)}
    var first by remember{mutableStateOf("")};var last by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
    CardBlock{
        SectionHeader(if(register) CupertinoSymbol.PlusPerson else CupertinoSymbol.Key,if(register) tr(language,"Register","Регистрация","Ro‘yxatdan o‘tish","Рўйхатдан ўтиш") else tr(language,"Sign in","Вход","Kirish","Кириш"),if(register) tr(language,"Create your permanent iumrah account","Создайте постоянный аккаунт iumrah","Doimiy iumrah akkauntini yarating","Доимий iumrah аккаунтини яратинг") else tr(language,"Restore trips and your digital ID","Восстановите поездки и цифровой ID","Safarlar va raqamli ID-ni tiklang","Сафарлар ва рақамли ID-ни тикланг"))
        Spacer(Modifier.height(14.dp));Segmented(listOf(tr(language,"Register","Регистрация","Ro‘yxatdan o‘tish","Рўйхатдан ўтиш"),tr(language,"Sign in","Вход","Kirish","Кириш")),if(register)0 else 1){register=it==0;challenge=null;error=null}
        Spacer(Modifier.height(12.dp));Segmented(listOf("SMS","Email","iumrah ID"),when(method){"sms"->0;"email"->1;else->2}){method=listOf("sms","email","id")[it];challenge=null;error=null}
        Spacer(Modifier.height(14.dp))
        if(register && method=="email"){
            AccountField(first,{first=it},tr(language,"First name","Имя","Ism","Исм"));Spacer(Modifier.height(9.dp));AccountField(last,{last=it},tr(language,"Last name","Фамилия","Familiya","Фамилия"));Spacer(Modifier.height(9.dp))
        }
        AccountField(identifier,{identifier=it},when(method){"sms"->"+998";"email"->"Email";else->"UMR ID"},if(method=="sms") KeyboardType.Phone else KeyboardType.Email)
        if(method=="id" || (!register && method=="email") || (register && method=="email" && challenge!=null)){Spacer(Modifier.height(9.dp));AccountField(password,{password=it},tr(language,"Password","Пароль","Parol","Пароль"),password=true)}
        if(challenge!=null){Spacer(Modifier.height(9.dp));AccountField(code,{code=it},tr(language,"Verification code","Код подтверждения","Tasdiqlash kodi","Тасдиқлаш коди"),KeyboardType.Number)}
        error?.let{Spacer(Modifier.height(10.dp));Text(it,color=Danger,fontSize=12.sp)}
        Spacer(Modifier.height(14.dp))
        Button(onClick={busy=true;error=null;scope.launch{runCatching{
            when{
                register && method=="email" && challenge==null -> challenge=accountStore.startEmailRegistration(identifier.trim(),language.localeTag).challengeID
                register && method=="email" -> accountStore.confirmEmailRegistration(challenge!!,code.trim(),password,first.trim(),last.trim(),language.localeTag)
                !register && method=="sms" && challenge==null -> challenge=accountStore.startPhoneLogin(identifier.trim(),language.localeTag).challengeID
                !register && method=="sms" -> accountStore.confirmPhoneLogin(challenge!!,code.trim(),language.localeTag)
                !register && (method=="email" || method=="id") -> accountStore.login(identifier.trim(),password,language.localeTag)
                else -> throw IllegalStateException(tr(language,"Registration by SMS/ID is completed from an active booking.","Регистрация по SMS/ID завершается из активного бронирования.","SMS/ID orqali ro‘yxatdan o‘tish faol bron ichida yakunlanadi.","SMS/ID орқали рўйхатдан ўтиш фаол брон ичида якунланади."))
            }
        }.onFailure{error=it.message ?: "Error"};busy=false}},modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(18.dp),enabled=!busy,colors=ButtonDefaults.buttonColors(containerColor=Color.Black)){if(busy) CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp,color=Color.White) else Text(if(challenge==null) tr(language,"Continue","Продолжить","Davom etish","Давом этиш") else tr(language,"Confirm","Подтвердить","Tasdiqlash","Тасдиқлаш"),fontWeight=FontWeight.Bold)}
        if(register && method!="email" && bookingStore.state.value.sessions.isNotEmpty()){Spacer(Modifier.height(10.dp));Text(tr(language,"Use the active booking to finish creating this account.","Используйте активную бронь, чтобы завершить создание аккаунта.","Akkaunt yaratishni yakunlash uchun faol brondan foydalaning.","Аккаунт яратишни якунлаш учун фаол брондан фойдаланинг."),fontSize=12.sp,color=IosGray)}
        Spacer(Modifier.height(14.dp));HorizontalDivider();Spacer(Modifier.height(12.dp));ProviderButton(CupertinoSymbol.Apple,if(register)tr(language,"Continue with Apple","Продолжить с Apple","Apple orqali davom etish","Apple орқали давом этиш") else tr(language,"Sign in with Apple","Войти с Apple","Apple orqali kirish","Apple орқали кириш"));Spacer(Modifier.height(9.dp));ProviderButton(CupertinoSymbol.Globe,if(register)tr(language,"Continue with Google","Продолжить с Google","Google orqali davom etish","Google орқали давом этиш") else tr(language,"Sign in with Google","Войти с Google","Google orqali kirish","Google орқали кириш"))
    }
}

@Composable private fun ProviderButton(icon:CupertinoSymbol,title:String){Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.onSurface.copy(alpha=.055f)){Row(Modifier.fillMaxWidth().height(50.dp).padding(horizontal=15.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){CupertinoIcon(icon,null,Modifier.size(19.dp),MaterialTheme.colorScheme.onSurface);Text(title,fontWeight=FontWeight.SemiBold)}}}

@Composable
fun AccountProfileEditorScreen(language:AppLanguage,accountStore:IumrahAccountStore,chrome:AppChromeStore){
    val state by accountStore.state.collectAsState();val p=state.account;val scope=rememberCoroutineScope()
    var first by remember(p){mutableStateOf(p?.firstName.orEmpty())};var last by remember(p){mutableStateOf(p?.lastName.orEmpty())};var phone by remember(p){mutableStateOf(p?.phone.orEmpty())};var email by remember(p){mutableStateOf(p?.email.orEmpty())};var telegram by remember(p){mutableStateOf(p?.telegram.orEmpty())};var whatsapp by remember(p){mutableStateOf(p?.whatsapp.orEmpty())};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
    AccountPage(tr(language,"Account details","Данные аккаунта","Akkaunt ma’lumotlari","Аккаунт маълумотлари"),chrome){
        CardBlock{AccountField(first,{first=it},tr(language,"First name","Имя","Ism","Исм"));Spacer(Modifier.height(10.dp));AccountField(last,{last=it},tr(language,"Last name","Фамилия","Familiya","Фамилия"));Spacer(Modifier.height(10.dp));AccountField(phone,{phone=it},tr(language,"Phone","Телефон","Telefon","Телефон"),KeyboardType.Phone);Spacer(Modifier.height(10.dp));AccountField(email,{email=it},"Email",KeyboardType.Email);Spacer(Modifier.height(10.dp));AccountField(telegram,{telegram=it},"Telegram");Spacer(Modifier.height(10.dp));AccountField(whatsapp,{whatsapp=it},"WhatsApp")}
        error?.let{Text(it,color=Danger,fontSize=12.sp)}
        Button(onClick={busy=true;scope.launch{runCatching{accountStore.updateProfile(first,last,phone,email,telegram,whatsapp)}.onSuccess{chrome.back()}.onFailure{error=it.message};busy=false}},modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(18.dp),enabled=!busy,colors=ButtonDefaults.buttonColors(containerColor=Color.Black)){Text(tr(language,"Save","Сохранить","Saqlash","Сақлаш"),fontWeight=FontWeight.Bold)}
    }
}

@Composable
fun AccountTravelersScreen(language:AppLanguage,accountStore:IumrahAccountStore,bookingStore:BookingStore,accountService:IumrahAccountService,chrome:AppChromeStore){
    val sessions by bookingStore.state.collectAsState();var travelers by remember{mutableStateOf<List<Pair<StoredBookingSession,IumrahTravelerForm>>>(emptyList())};var loading by remember{mutableStateOf(true)}
    LaunchedEffect(sessions.sessions,accountStore.bearerToken){loading=true;val rows=mutableListOf<Pair<StoredBookingSession,IumrahTravelerForm>>();sessions.sessions.forEach{s->runCatching{accountService.checkout(s.id,bookingStore.headersFor(s))}.getOrNull()?.travelers?.forEach{rows+=s to it}};travelers=rows;loading=false}
    AccountPage(tr(language,"Travel companions","Спутники","Hamrohlar","Ҳамроҳлар"),chrome){
        CardBlock{SectionHeader(CupertinoSymbol.Persons,tr(language,"Your pilgrims","Ваши паломники","Ziyoratchilaringiz","Зиёратчиларингиз"),tr(language,"Traveler details are attached to bookings and stay ready for the trip.","Данные паломников привязаны к бронированиям и готовы к поездке.","Sayohatchi ma’lumotlari bronlarga bog‘langan va safar uchun tayyor turadi.","Саёҳатчи маълумотлари бронларга боғланган ва сафар учун тайёр туради."))}
        when{loading->Box(Modifier.fillMaxWidth().padding(32.dp),contentAlignment=Alignment.Center){CircularProgressIndicator()};travelers.isEmpty()->CardBlock{EmptyRow(CupertinoSymbol.Persons,tr(language,"No companions yet","Пока нет спутников","Hali hamrohlar yo‘q","Ҳали ҳамроҳлар йўқ"))};else->travelers.forEach{(session,t)->CardBlock{Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){IconBadge(CupertinoSymbol.PersonCircle,Care,44.dp,18.dp);Column(Modifier.weight(1f)){Text(listOf(t.firstName,t.lastName).filter{it.isNotBlank()}.joinToString(" ").ifBlank{tr(language,"Pilgrim ${t.position}","Паломник ${t.position}","Ziyoratchi ${t.position}","Зиёратчи ${t.position}")},fontWeight=FontWeight.Bold);Text("${session.displayBookingNumber} · ${if(t.completed) tr(language,"Ready","Готово","Tayyor","Тайёр") else tr(language,"Needs details","Нужны данные","Ma’lumot kerak","Маълумот керак")}",fontSize=12.sp,color=IosGray)}};Spacer(Modifier.height(12.dp));Button(onClick={chrome.openPilgrimCheckout(session.id)},shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()){Text(tr(language,"Open traveler data","Открыть данные паломников","Ziyoratchi ma’lumotlarini ochish","Зиёратчи маълумотларини очиш"))}}}
        }
    }
}

@Composable
fun AccountPolicyScreen(kind:String,language:AppLanguage,chrome:AppChromeStore){
    val title=when(kind){"refund"->tr(language,"Refund policy","Правила возврата","Qaytarish qoidalari","Қайтариш қоидалари");"privacy"->tr(language,"Privacy","Конфиденциальность","Maxfiylik","Махфийлик");else->tr(language,"Payment & security","Оплата и безопасность","To‘lov va xavfsizlik","Тўлов ва хавфсизлик")}
    val sections=when(kind){
        "refund"->listOf(
            tr(language,"Flights","Авиабилеты","Aviachiptalar","Авиачипталар") to tr(language,"Flights are non-refundable by default unless the selected fare explicitly allows a refund.","Авиабилеты по умолчанию невозвратные, если выбранный тариф прямо не предусматривает возврат.","Tanlangan tarifda qaytarish aniq ko‘rsatilmagan bo‘lsa, aviachiptalar odatda qaytarilmaydi.","Танланган тарифда қайтариш аниқ кўрсатилмаган бўлса, авиачипталар одатда қайтарилмайди."),
            tr(language,"Hotels","Отели","Mehmonxonalar","Меҳмонхоналар") to tr(language,"Hotel cancellation follows the booked room rate and provider conditions.","Возврат по отелю зависит от тарифа номера и условий поставщика.","Mehmonxona bekor qilinishi xona tarifi va provayder shartlariga bog‘liq.","Меҳмонхона бекор қилиниши хона тарифи ва провайдер шартларига боғлиқ."),
            tr(language,"Transfer","Трансфер","Transfer","Трансфер") to tr(language,"Transfer is fully refundable until 120 hours before service. Later cancellation may include a \$100 fee.","Трансфер полностью возвратный до 120 часов до услуги. При более поздней отмене может удерживаться комиссия \$100.","Transfer xizmatdan 120 soat oldingacha to‘liq qaytariladi. Keyinroq bekor qilishda \$100 komissiya bo‘lishi mumkin.","Трансфер хизматдан 120 соат олдингача тўлиқ қайтарилади. Кейинроқ бекор қилишда \$100 комиссия бўлиши мумкин."),
            tr(language,"iumrah services","Сервисы iumrah","iumrah xizmatlari","iumrah хизматлари") to tr(language,"Unused iumrah services can be refunded before the service begins. The terms fixed at booking remain authoritative.","Неиспользованные сервисы iumrah можно вернуть до начала оказания услуги. Действуют условия, зафиксированные при бронировании.","Foydalanilmagan iumrah xizmatlari boshlanishidan oldin qaytarilishi mumkin. Bron paytidagi shartlar amal qiladi.","Фойдаланилмаган iumrah хизматлари бошланишидан олдин қайтарилиши мумкин. Брон пайтидаги шартлар амал қилади.")
        )
        "privacy"->listOf(
            tr(language,"Data we use","Какие данные мы используем","Qaysi ma’lumotlardan foydalanamiz","Қайси маълумотлардан фойдаланамиз") to tr(language,"Identity, contact, passport and trip information required to create and service your booking.","Идентификационные, контактные, паспортные данные и сведения о поездке, необходимые для оформления и обслуживания брони.","Bronni yaratish va xizmat ko‘rsatish uchun zarur shaxsiy, aloqa, pasport va safar ma’lumotlari.","Бронни яратиш ва хизмат кўрсатиш учун зарур шахсий, алоқа, паспорт ва сафар маълумотлари."),
            tr(language,"Why we use it","Зачем мы их используем","Nega foydalanamiz","Нега фойдаланамиз") to tr(language,"To provide booked travel services, verify identity, support payments and communicate important trip updates.","Чтобы оказывать забронированные услуги, подтверждать личность, сопровождать оплату и сообщать важные обновления поездки.","Bron qilingan xizmatlarni ko‘rsatish, shaxsni tasdiqlash, to‘lovni qo‘llab-quvvatlash va muhim safar yangiliklarini yuborish uchun.","Брон қилинган хизматларни кўрсатиш, шахсни тасдиқлаш, тўловни қўллаб-қувватлаш ва муҳим сафар янгиликларини юбориш учун."),
            tr(language,"Sharing with providers","Передача поставщикам","Provayderlarga uzatish","Провайдерларга узатиш") to tr(language,"Only the data required to fulfill a booked flight, hotel, transfer or other service is shared with the relevant provider.","Поставщику передаются только данные, необходимые для исполнения конкретного авиабилета, отеля, трансфера или другой услуги.","Faqat aniq xizmatni bajarish uchun zarur ma’lumotlar tegishli provayderga uzatiladi.","Фақат аниқ хизматни бажариш учун зарур маълумотлар тегишли провайдерга узатилади."),
            tr(language,"Retention and deletion","Хранение и удаление","Saqlash va o‘chirish","Сақлаш ва ўчириш") to tr(language,"Data is retained only as needed for service, security, accounting and legal obligations, then deleted or anonymized.","Данные хранятся столько, сколько необходимо для сервиса, безопасности, учёта и юридических обязательств, после чего удаляются или обезличиваются.","Ma’lumotlar xizmat, xavfsizlik, hisob va qonuniy talablar uchun zarur muddat saqlanadi, so‘ng o‘chiriladi yoki anonimlashtiriladi.","Маълумотлар хизмат, хавфсизлик, ҳисоб ва қонуний талаблар учун зарур муддат сақланади, сўнг ўчирилади ёки анонимлаштирилади.")
        )
        else->listOf(
            tr(language,"Manual payments","Ручная оплата","Qo‘lda to‘lov","Қўлда тўлов") to tr(language,"During the first release, payment details are provided only inside your booking. Always verify the booking amount and recipient before paying.","На первом этапе реквизиты для оплаты показываются только внутри бронирования. Перед оплатой всегда сверяйте сумму и получателя.","Dastlab to‘lov rekvizitlari faqat bron ichida ko‘rsatiladi. To‘lashdan oldin summa va qabul qiluvchini tekshiring.","Дастлаб тўлов реквизитлари фақат брон ичида кўрсатилади. Тўлашдан олдин сумма ва қабул қилувчини текширинг."),
            tr(language,"Payment security","Безопасность платежа","To‘lov xavfsizligi","Тўлов хавфсизлиги") to tr(language,"iumrah never asks for your card PIN, CVV or banking password in chat or by phone.","iumrah никогда не запрашивает PIN-код карты, CVV или банковский пароль в чате или по телефону.","iumrah chat yoki telefon orqali karta PIN, CVV yoki bank parolini so‘ramaydi.","iumrah чат ёки телефон орқали карта PIN, CVV ёки банк паролини сўрамайди."),
            tr(language,"Invoice and receipt","Счёт и чек","Hisob va chek","Ҳисоб ва чек") to tr(language,"Your booking contains the payable amount and lets you upload a payment receipt for verification.","В бронировании указана сумма к оплате и можно загрузить чек для проверки.","Bron ichida to‘lov summasi ko‘rsatiladi va tekshiruv uchun chek yuklash mumkin.","Брон ичида тўлов суммаси кўрсатилади ва текширув учун чек юклаш мумкин."),
            tr(language,"Cancellation","Отмена поездки","Safarni bekor qilish","Сафарни бекор қилиш") to tr(language,"If a trip is cancelled, each component is refunded according to its own fare and cancellation terms.","При отмене поездки каждый компонент возвращается по своим тарифным и отменным условиям.","Safar bekor qilinsa, har bir komponent o‘z tarifi va bekor qilish shartlari bo‘yicha qaytariladi.","Сафар бекор қилинса, ҳар бир компонент ўз тарифи ва бекор қилиш шартлари бўйича қайтарилади.")
        )
    }
    AccountPage(title,chrome){sections.forEach{(h,b)->CardBlock{Text(h,fontSize=17.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(7.dp));Text(b,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=14.sp,lineHeight=20.sp)}}}
}

@Composable
fun AccountKycScreen(
    bookingID: String,
    language: AppLanguage,
    bookingStore: BookingStore,
    chrome: AppChromeStore,
) {
    val session = bookingStore.booking(bookingID)
    AccountPage(
        tr(language, "Iumrah Security", "Iumrah Security", "Iumrah Security", "Iumrah Security"),
        chrome,
    ) {
        if (session == null) {
            CardBlock {
                EmptyRow(
                    CupertinoSymbol.IdentityCard,
                    tr(language, "Booking is unavailable", "Бронирование недоступно", "Bron mavjud emas", "Брон мавжуд эмас"),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    tr(language, "Security Confirmation · KYC", "Security Confirmation · KYC", "Security Confirmation · KYC", "Security Confirmation · KYC"),
                    fontSize = 12.sp,
                    color = IosGray,
                    fontWeight = FontWeight.SemiBold,
                )
                IumrahSecurityConfirmationPanel(session, bookingStore.service)
            }
        }
    }
}

@Composable
fun AccountSecurityScreen(
    language: AppLanguage,
    accountStore: IumrahAccountStore,
    chrome: AppChromeStore,
) {
    val scope = rememberCoroutineScope()
    var overview by remember { mutableStateOf<IumrahSecurityOverview?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun reload() {
        loading = true
        scope.launch {
            runCatching { accountStore.securityOverview(language.localeTag) }
                .onSuccess { overview = it; error = null }
                .onFailure { error = it.message }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    AccountPage(
        tr(language, "Account security", "Безопасность аккаунта", "Akkaunt xavfsizligi", "Аккаунт хавфсизлиги"),
        chrome,
    ) {
        CardBlock {
            SectionHeader(
                CupertinoSymbol.LockShield,
                tr(language, "Protect your iumrah ID", "Защита iumrah ID", "iumrah ID himoyasi", "iumrah ID ҳимояси"),
                tr(language, "Trusted sign-in methods and active sessions", "Надёжные способы входа и активные сеансы", "Ishonchli kirish usullari va faol seanslar", "Ишончли кириш усуллари ва фаол сеанслар"),
            )
        }

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (overview != null) {
            val current = overview!!
            CardBlock {
                SecurityLine(
                    CupertinoSymbol.Device,
                    tr(language, "Primary device", "Основное устройство", "Asosiy qurilma", "Асосий қурилма"),
                    if (current.currentDeviceIsPrimary) tr(language, "This device", "Это устройство", "Bu qurilma", "Бу қурилма") else tr(language, "Protected", "Защищено", "Himoyalangan", "Ҳимояланган"),
                )
                HorizontalDivider()
                SecurityLine(CupertinoSymbol.Mail, "Email", current.loginEmail?.email ?: tr(language, "Not verified", "Не подтверждён", "Tasdiqlanmagan", "Тасдиқланмаган"))
                HorizontalDivider()
                SecurityLine(CupertinoSymbol.Apple, "Apple", if (current.apple.linked) tr(language, "Connected", "Подключено", "Ulangan", "Уланган") else tr(language, "Not connected", "Не подключено", "Ulanmagan", "Уланмаган"))
                HorizontalDivider()
                SecurityLine(CupertinoSymbol.Globe, "Google", if (current.google?.linked == true) tr(language, "Connected", "Подключено", "Ulangan", "Уланган") else tr(language, "Not connected", "Не подключено", "Ulanmagan", "Уланмаган"))
            }

            Text(
                tr(language, "Active sessions", "Активные сеансы", "Faol seanslar", "Фаол сеанслар"),
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp),
            )

            current.sessions.forEach { session ->
                CardBlock {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconBadge(CupertinoSymbol.Device, if (session.isCurrent) Success else Care, 44.dp, 18.dp)
                        Column(Modifier.weight(1f)) {
                            Text(session.deviceName.ifBlank { session.model.ifBlank { session.platform } }, fontWeight = FontWeight.Bold)
                            Text(listOf(session.city, session.country).filter { it.isNotBlank() }.joinToString(", ").ifBlank { session.platform }, fontSize = 12.sp, color = IosGray)
                            Text(
                                if (session.isCurrent) tr(language, "Current session", "Текущий сеанс", "Joriy seans", "Жорий сеанс") else session.lastActiveAt,
                                fontSize = 11.sp,
                                color = if (session.isCurrent) Success else IosGray,
                            )
                        }
                        if (session.canTerminate && !session.isCurrent) {
                            TextButton(onClick = {
                                scope.launch {
                                    runCatching { accountStore.terminateSecuritySession(session.id) }
                                    reload()
                                }
                            }) {
                                Text(tr(language, "End", "Завершить", "Yopish", "Ёпиш"), color = Danger)
                            }
                        }
                    }
                }
            }
        } else {
            CardBlock {
                Text(
                    error ?: tr(language, "Security information is unavailable", "Информация безопасности недоступна", "Xavfsizlik ma’lumotlari mavjud emas", "Хавфсизлик маълумотлари мавжуд эмас"),
                    color = Danger,
                )
            }
        }
    }
}

@Composable private fun SecurityLine(icon:CupertinoSymbol,title:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){IconBadge(icon,Care,40.dp,16.dp);Text(title,Modifier.weight(1f),fontWeight=FontWeight.SemiBold);Text(value,fontSize=12.sp,color=IosGray,maxLines=1)}}

@Composable
fun AccountAppearanceScreen(language:AppLanguage,settingsStore:AppSettingsStore,chrome:AppChromeStore){
    val settings by settingsStore.state.collectAsState()
    val icons=listOf(
        Triple("blue","Blue",R.drawable.iumrah_icon_blue_preview),
        Triple("cyan","Cyan",R.drawable.iumrah_icon_cyan_preview),
        Triple("deep","Deep",R.drawable.iumrah_icon_deep_preview),
        Triple("world","World",R.drawable.iumrah_icon_world_preview),
        Triple("makkah","Makkah",R.drawable.iumrah_icon_makkah_preview),
    )
    AccountPage(tr(language,"Appearance","Оформление","Ko‘rinish","Кўриниш"),chrome){
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text(tr(language,"Theme","Тема","Mavzu","Мавзу"),fontSize=23.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(start=4.dp))
            Text(tr(language,"Choose how iumrah looks on this device.","Выберите, как iumrah выглядит на этом устройстве.","iumrah ushbu qurilmada qanday ko‘rinishini tanlang.","iumrah ушбу қурилмада қандай кўринишини танланг."),fontSize=13.sp,color=IosGray,modifier=Modifier.padding(start=4.dp))
            CardBlock{
                AppearanceRow(CupertinoSymbol.Device,tr(language,"System","Системная","Tizim","Тизим"),settings.appearance==AppAppearance.SYSTEM){settingsStore.setAppearance(AppAppearance.SYSTEM)}
                HorizontalDivider(Modifier.padding(start=70.dp))
                AppearanceRow(CupertinoSymbol.Sun,tr(language,"Light","Светлая","Yorug‘","Ёруғ"),settings.appearance==AppAppearance.LIGHT){settingsStore.setAppearance(AppAppearance.LIGHT)}
                HorizontalDivider(Modifier.padding(start=70.dp))
                AppearanceRow(CupertinoSymbol.Moon,tr(language,"Dark","Тёмная","Qorong‘i","Қоронғи"),settings.appearance==AppAppearance.DARK){settingsStore.setAppearance(AppAppearance.DARK)}
            }
        }
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text(tr(language,"App icon","Иконка приложения","Ilova ikonkasi","Илова иконкаси"),fontSize=23.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(start=4.dp))
            Text(tr(language,"Choose how iumrah appears on your Home Screen.","Выберите, как iumrah выглядит на главном экране.","iumrah bosh ekranda qanday ko‘rinishini tanlang.","iumrah бош экранда қандай кўринишини танланг."),fontSize=13.sp,color=IosGray,modifier=Modifier.padding(start=4.dp))
            CardBlock{
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    icons.forEach{(key,name,res)->
                        Column(Modifier.width(92.dp).clickable{settingsStore.setLauncherIcon(key)},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
                            Box{
                                Image(painterResource(res),name,Modifier.size(92.dp).clip(RoundedCornerShape(22.dp)).border(if(settings.launcherIcon==key)1.5.dp else 1.dp,if(settings.launcherIcon==key)MaterialTheme.colorScheme.onSurface.copy(alpha=.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha=.07f),RoundedCornerShape(22.dp)),contentScale=ContentScale.Crop)
                                if(settings.launcherIcon==key) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp).clip(CircleShape).background(Color.Black.copy(alpha=.82f)),contentAlignment=Alignment.Center){CupertinoIcon(CupertinoSymbol.Checkmark,null,Modifier.size(13.dp),Color.White)}
                            }
                            Text(name,fontSize=11.sp,fontWeight=if(settings.launcherIcon==key)FontWeight.Bold else FontWeight.SemiBold,color=if(settings.launcherIcon==key)MaterialTheme.colorScheme.onSurface else IosGray)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp));HorizontalDivider();Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().clickable(enabled=settings.launcherIcon!="standard"){settingsStore.setLauncherIcon("standard")}.padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    IconBadge(CupertinoSymbol.Refresh,Care,38.dp,15.dp)
                    Column(Modifier.weight(1f)){Text(tr(language,"Standard icon","Стандартная иконка","Standart ikonka","Стандарт иконка"),fontWeight=FontWeight.SemiBold);Text(tr(language,"Restore the original iumrah icon","Вернуть исходную иконку iumrah","Asl iumrah ikonkasini qaytarish","Асл iumrah иконкасини қайтариш"),fontSize=11.sp,color=IosGray)}
                    if(settings.launcherIcon=="standard") CupertinoIcon(CupertinoSymbol.CheckCircle,null,Modifier.size(20.dp),Care) else CupertinoIcon(CupertinoSymbol.ChevronRight,null,Modifier.size(14.dp),IosGray)
                }
            }
        }
    }
}

@Composable private fun AppearanceRow(icon:CupertinoSymbol,title:String,selected:Boolean,onClick:()->Unit){Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(13.dp)){IconBadge(icon,Care,42.dp,17.dp);Text(title,Modifier.weight(1f),fontWeight=FontWeight.SemiBold);if(selected) CupertinoIcon(CupertinoSymbol.CheckCircle,null,Modifier.size(21.dp),Care)}}

@Composable
fun AccountLanguageScreen(language: AppLanguage, settingsStore: AppSettingsStore, chrome: AppChromeStore) {
    val current by settingsStore.state.collectAsState()
    AccountPage(tr(language, "Language", "Язык", "Til", "Тил"), chrome) {
        CardBlock {
            AppLanguage.entries.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(Modifier.padding(start = 54.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { settingsStore.setLanguage(item) }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(CupertinoSymbol.Globe, Care, 40.dp, 16.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(languageTitle(item), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    if (current.language == item) CupertinoIcon(CupertinoSymbol.CheckCircle, null, Modifier.size(21.dp), Care)
                }
            }
        }
    }
}

@Composable
fun AccountSignalsScreen(language:AppLanguage,notifications:ClientNotificationStore,accountStore:IumrahAccountStore,chrome:AppChromeStore){
    val state by notifications.state.collectAsState();val scope=rememberCoroutineScope();LaunchedEffect(Unit){notifications.refresh(accountStore.bearerToken)}
    AccountPage("iumrah Signal",chrome){
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Color(0xFF071A35)).padding(22.dp)){Column(verticalArrangement=Arrangement.spacedBy(13.dp)){Row{Column(Modifier.weight(1f)){Text("IUMRAH SIGNAL",fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=.7.sp,color=Color.White.copy(alpha=.72f));Text(tr(language,"Everything important about your journey","Всё важное по вашей поездке","Safaringiz bo‘yicha barcha muhim xabarlar","Сафарингиз бўйича барча муҳим хабарлар"),fontSize=27.sp,fontWeight=FontWeight.Bold,color=Color.White);Spacer(Modifier.height(5.dp));Text(tr(language,"Booking changes, trip updates and reminders stay here even after a push notification disappears.","Изменения бронирования, новости поездки и напоминания остаются здесь, даже когда push уже исчез.","Bron o‘zgarishlari, safar yangiliklari va eslatmalar push yo‘qolgandan keyin ham shu yerda qoladi.","Брон ўзгаришлари, сафар янгиликлари ва эслатмалар push йўқолгандан кейин ҳам шу ерда қолади."),fontSize=13.sp,color=Color.White.copy(alpha=.78f))};IconBadge(CupertinoSymbol.BellBadge,Color.White,52.dp,20.dp,darkFill=true)};Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){SignalChip("${state.unreadCount}",tr(language,"new","новых","yangi","янги"));SignalChip("${state.inbox.size}",tr(language,"total","всего","jami","жами"))}}}
        if(state.inbox.isEmpty())CardBlock{EmptyRow(CupertinoSymbol.BellSignal,tr(language,"No notifications yet","Пока нет уведомлений","Hali bildirishnomalar yo‘q","Ҳали билдиришномалар йўқ"))}
        state.inbox.forEach{n->CardBlock{Row(verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(12.dp)){IconBadge(CupertinoSymbol.BellSignal,if(!n.isRead)Signal else IosGray,44.dp,16.dp);Column(Modifier.weight(1f)){Row{Text(n.title,Modifier.weight(1f),fontWeight=if(!n.isRead)FontWeight.Bold else FontWeight.SemiBold);if(!n.isRead)Box(Modifier.size(8.dp).clip(CircleShape).background(Signal))};Text(n.sentAt?:n.createdAt,fontSize=11.sp,color=IosGray)}};Spacer(Modifier.height(10.dp));Text(n.body,fontSize=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(10.dp));Row(verticalAlignment=Alignment.CenterVertically){Text(if(state.dismissedHomeIDs.contains(n.id))tr(language,"Hidden on Home","Скрыто на главной","Asosiyda yashirilgan","Асосийда яширилган") else tr(language,"Visible on Home","Показывается на главной","Asosiyda ko‘rinadi","Асосийда кўринади"),Modifier.weight(1f),fontSize=11.sp,color=IosGray);TextButton(onClick={if(state.dismissedHomeIDs.contains(n.id))notifications.restoreToHome(n.id) else notifications.dismissFromHome(n.id)}){CupertinoIcon(if(state.dismissedHomeIDs.contains(n.id))CupertinoSymbol.Eye else CupertinoSymbol.EyeSlash,null,Modifier.size(17.dp),IosGray)};Button(onClick={scope.launch{notifications.markOpened(n,accountStore.bearerToken)}},shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=if(!n.isRead)Signal else MaterialTheme.colorScheme.onSurface.copy(alpha=.08f),contentColor=if(!n.isRead)Color.White else MaterialTheme.colorScheme.onSurface)){Text(tr(language,"Open","Открыть","Ochish","Очиш"),fontSize=12.sp)}}}}
    }
}

@Composable private fun SignalChip(value:String,title:String){Row(Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha=.1f)).padding(horizontal=11.dp,vertical=7.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)){Text(value,color=Color.White,fontWeight=FontWeight.Bold);Text(title,color=Color.White.copy(alpha=.65f),fontSize=12.sp)}}

@Composable
private fun AccountPage(title:String,chrome:AppChromeStore,content:@Composable ColumnScope.()->Unit){
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(),contentPadding=PaddingValues(start=PagePad,end=PagePad,top=14.dp,bottom=48.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        item{Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically){Surface(onClick={chrome.back()},shape=CircleShape,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.06f)){Box(Modifier.size(38.dp),contentAlignment=Alignment.Center){CupertinoIcon(CupertinoSymbol.ChevronLeft,null,Modifier.size(18.dp),MaterialTheme.colorScheme.onSurface)}};Text(title,Modifier.weight(1f).padding(horizontal=12.dp),fontSize=17.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Spacer(Modifier.width(38.dp))}}
        item{Column(verticalArrangement=Arrangement.spacedBy(18.dp),content=content)}
    }
}

@Composable private fun SectionCard(title:String,subtitle:String,icon:CupertinoSymbol,content:@Composable ColumnScope.()->Unit){CardBlock{SectionHeader(icon,title,subtitle);Spacer(Modifier.height(10.dp));content()}}
@Composable private fun SectionHeader(icon:CupertinoSymbol,title:String,subtitle:String){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){IconBadge(icon,Care,42.dp,17.dp);Column(Modifier.weight(1f)){Text(title,fontSize=17.sp,fontWeight=FontWeight.Bold);Text(subtitle,fontSize=12.sp,color=IosGray)}}}
@Composable private fun CardBlock(content:@Composable ColumnScope.()->Unit){Surface(shape=RoundedCornerShape(CardRadius),color=MaterialTheme.colorScheme.surface,tonalElevation=1.dp,shadowElevation=1.dp){Column(Modifier.fillMaxWidth().padding(18.dp),content=content)}}

@Composable
private fun SettingsRow(icon:CupertinoSymbol,title:String,value:String,enabled:Boolean=true,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().then(if(enabled)Modifier.clickable(onClick=onClick) else Modifier).padding(vertical=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
        IconBadge(icon,Care,42.dp,17.dp);Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.SemiBold,color=if(enabled)MaterialTheme.colorScheme.onSurface else IosGray);Text(value,fontSize=12.sp,color=IosGray,maxLines=2)};if(enabled)CupertinoIcon(CupertinoSymbol.ChevronRight,null,Modifier.size(15.dp),IosGray.copy(alpha=.7f))
    }
}
@Composable private fun EmptyRow(icon:CupertinoSymbol,text:String){Row(Modifier.fillMaxWidth().padding(vertical=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){IconBadge(icon,IosGray,42.dp,17.dp);Text(text,color=IosGray,fontSize=13.sp)}}
@Composable private fun IconBadge(icon:CupertinoSymbol,tint:Color,size:androidx.compose.ui.unit.Dp,symbolSize:androidx.compose.ui.unit.Dp,darkFill:Boolean=false){val bg=if(darkFill)Color.White.copy(alpha=.12f) else tint.copy(alpha=.105f);Box(Modifier.size(size).clip(if(size>=48.dp)CircleShape else RoundedCornerShape(14.dp)).background(bg),contentAlignment=Alignment.Center){CupertinoIcon(icon,null,Modifier.size(symbolSize),tint)}}

@Composable private fun AccountField(value:String,onChange:(String)->Unit,label:String,keyboard:KeyboardType=KeyboardType.Text,password:Boolean=false){OutlinedTextField(value,onChange,Modifier.fillMaxWidth(),label={Text(label)},singleLine=true,shape=RoundedCornerShape(16.dp),keyboardOptions=KeyboardOptions(keyboardType=keyboard),visualTransformation=if(password)PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None)}
@Composable
private fun Segmented(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = .055f)).padding(3.dp)) {
        labels.forEachIndexed { index, title ->
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                    .background(if (index == selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(index) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(title, fontSize = 12.sp, fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Medium, color = if (index == selected) MaterialTheme.colorScheme.onSurface else IosGray)
            }
        }
    }
}
private fun languageTitle(l:AppLanguage)=when(l){AppLanguage.ENGLISH->"English";AppLanguage.RUSSIAN->"Русский";AppLanguage.UZBEK->"O‘zbekcha";AppLanguage.UZBEK_CYRILLIC->"Ўзбекча"}
private fun appearanceTitle(a:AppAppearance,l:AppLanguage)=when(a){AppAppearance.SYSTEM->tr(l,"System","Системная","Tizim","Тизим");AppAppearance.LIGHT->tr(l,"Light","Светлая","Yorug‘","Ёруғ");AppAppearance.DARK->tr(l,"Dark","Тёмная","Qorong‘i","Қоронғи")}
