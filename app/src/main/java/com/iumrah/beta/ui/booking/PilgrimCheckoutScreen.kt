package com.iumrah.beta.ui.booking

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iumrah.beta.core.media.AndroidImageCodec
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.account.IumrahAccountService
import com.iumrah.beta.data.account.IumrahAccountStore
import com.iumrah.beta.data.booking.BookingStore
import com.iumrah.beta.models.account.IumrahCheckoutResponse
import com.iumrah.beta.models.account.IumrahTravelerForm
import com.iumrah.beta.models.account.IumrahTravelDocument
import com.iumrah.beta.models.booking.StoredBookingSession
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.cupertino.CupertinoIcon
import com.iumrah.beta.ui.cupertino.CupertinoSymbol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * iOS `PilgrimCheckoutView(presentation: .screen)` is only a compatibility entry
 * point: it opens BookingDetail with the Status page preselected. Android mirrors
 * that structure instead of keeping a second unrelated Material checkout screen.
 */
@Composable
fun PilgrimCheckoutScreen(
    bookingID: String,
    language: AppLanguage,
    bookingStore: BookingStore,
    accountStore: IumrahAccountStore,
    accountService: IumrahAccountService,
    chrome: AppChromeStore,
) {
    BookingDetailScreen(
        bookingID = bookingID,
        language = language,
        bookingStore = bookingStore,
        accountStore = accountStore,
        accountService = accountService,
        chrome = chrome,
        initialPage = BookingPrimaryPageAndroid.STATUS,
    )
}

private enum class CheckoutActivationMethod { IUMRAH_ID, EMAIL, SMS }

@Composable
internal fun PilgrimCheckoutEmbedded(
    bookingID: String,
    language: AppLanguage,
    session: StoredBookingSession,
    bookingStore: BookingStore,
    accountStore: IumrahAccountStore,
    accountService: IumrahAccountService,
    chrome: AppChromeStore,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountState by accountStore.state.collectAsState()
    var checkout by remember(bookingID) { mutableStateOf<IumrahCheckoutResponse?>(null) }
    var loading by remember(bookingID) { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var activationMethod by remember { mutableStateOf(CheckoutActivationMethod.IUMRAH_ID) }
    var travelerEditor by remember { mutableStateOf<IumrahTravelerForm?>(null) }

    suspend fun load(showLoader: Boolean = true) {
        if (showLoader) loading = true
        error = null
        val headers = accountStore.authorizationHeaders(session.accessToken)
        runCatching { accountService.checkout(bookingID, headers) }
            .onSuccess { checkout = it }
            .onFailure { error = it.message }
        loading = false
    }

    val receiptLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null || accountStore.bearerToken.isNullOrBlank()) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    AndroidImageCodec.jpeg(context.contentResolver, uri, 2048, 88)
                }
                accountService.uploadReceipt(bookingID, "manual_card", bytes, "image/jpeg", accountStore.bearerToken!!)
            }.onSuccess { load(showLoader = false) }
                .onFailure { error = it.message }
            busy = false
        }
    }

    LaunchedEffect(bookingID, accountStore.bearerToken) { load() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (loading) {
            BookingCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        bookingText(language, "Загружаем защищённое оформление…", "Loading secure checkout…", "Himoyalangan sahifa yuklanmoqda…", "Ҳимояланган саҳифа юкланмоқда…"),
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        checkout?.let { value ->
            val accountID = accountState.iumrahID?.filter(Char::isDigit).orEmpty()
            val checkoutID = value.iumrahID.filter(Char::isDigit)
            val accountMatchesTrip = accountID.isNotEmpty() && accountID == checkoutID
            val availability = value.status.lowercase() == "availability_check"
            val paymentPending = value.status.lowercase() == "payment_pending"
            val postPayment = value.status.lowercase() in setOf("paid", "booking_confirmed", "documents_ready", "ready_to_travel", "in_trip", "completed")
            val travelerEditingAllowed = availability || paymentPending

            if (!accountMatchesTrip) {
                if (value.accountActive) {
                    CheckoutLoginCard(
                        language = language,
                        value = value,
                        password = loginPassword,
                        onPassword = { loginPassword = it },
                        busy = busy,
                        onRecover = { chrome.openPasswordRecovery() },
                        onLogin = {
                            if (loginPassword.isBlank()) return@CheckoutLoginCard
                            busy = true
                            scope.launch {
                                runCatching { accountStore.login(value.iumrahID, loginPassword) }
                                    .onSuccess { load(showLoader = false) }
                                    .onFailure { error = it.message }
                                busy = false
                            }
                        },
                    )
                } else {
                    CheckoutActivationCard(
                        language = language,
                        value = value,
                        method = activationMethod,
                        onMethod = { activationMethod = it },
                        password = password,
                        onPassword = { password = it },
                        confirm = confirmPassword,
                        onConfirm = { confirmPassword = it },
                        busy = busy,
                        onActivate = {
                            if (password.length < 8 || password != confirmPassword) return@CheckoutActivationCard
                            busy = true
                            scope.launch {
                                runCatching { accountStore.activate(bookingID, session.accessToken, password) }
                                    .onSuccess { load(showLoader = false) }
                                    .onFailure { error = it.message }
                                busy = false
                            }
                        },
                    )
                }
            } else {
                TravelersCard(language, value, travelerEditingAllowed, onEdit = { travelerEditor = it })

                if (availability) {
                    AvailabilityPaymentLockedCard(language)
                } else if (paymentPending) {
                    PaymentCard(
                        language = language,
                        value = value,
                        busy = busy,
                        canUpload = !accountStore.bearerToken.isNullOrBlank(),
                        onUpload = { receiptLauncher.launch("image/*") },
                        chrome = chrome,
                    )
                } else if (postPayment) {
                    if (value.receipts.isNotEmpty()) PaidReceiptCard(language, value)
                    DocumentsCard(language, value)
                } else {
                    PaymentCard(
                        language = language,
                        value = value,
                        busy = busy,
                        canUpload = !accountStore.bearerToken.isNullOrBlank(),
                        onUpload = { receiptLauncher.launch("image/*") },
                        chrome = chrome,
                    )
                    if (value.documents.isNotEmpty() || value.receipts.isNotEmpty()) DocumentsCard(language, value)
                }
            }
        }

        error?.takeIf { it.isNotBlank() }?.let {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CupertinoIcon(CupertinoSymbol.ExclamationCircle, null, Modifier.size(16.dp), MaterialTheme.colorScheme.error)
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, lineHeight = 17.sp)
            }
        }
    }

    travelerEditor?.let { traveler ->
        val token = accountStore.bearerToken
        if (!token.isNullOrBlank()) {
            TravelerEditorSheet(
                bookingID = bookingID,
                traveler = traveler,
                language = language,
                accountService = accountService,
                token = token,
                onDismiss = { travelerEditor = null },
                onSaved = {
                    travelerEditor = null
                    scope.launch { load(showLoader = false) }
                },
            )
        } else {
            travelerEditor = null
        }
    }
}

@Composable
private fun CheckoutActivationCard(
    language: AppLanguage,
    value: IumrahCheckoutResponse,
    method: CheckoutActivationMethod,
    onMethod: (CheckoutActivationMethod) -> Unit,
    password: String,
    onPassword: (String) -> Unit,
    confirm: String,
    onConfirm: (String) -> Unit,
    busy: Boolean,
    onActivate: () -> Unit,
) {
    BookingCard {
        StageHeader("01", CupertinoSymbol.Key, bookingText(language, "Активируйте аккаунт iumrah", "Activate your iumrah account", "iumrah akkauntingizni faollashtiring", "iumrah аккаунтингизни фаоллаштиринг"))
        Spacer(Modifier.height(14.dp))
        Text(
            bookingText(language, "Ваш iumrah ID уже создан для этой поездки. Создайте пароль, чтобы защищённо продолжить оформление.", "Your iumrah ID is already created for this trip. Create a password to continue securely.", "Bu safar uchun iumrah ID allaqachon yaratilgan. Davom etish uchun parol yarating.", "Бу сафар учун iumrah ID аллақачон яратилган. Давом этиш учун парол яратинг."),
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f),
        )
        Spacer(Modifier.height(14.dp))
        BookingSegmentedControl(
            items = listOf(
                CheckoutActivationMethod.IUMRAH_ID to "iumrah ID",
                CheckoutActivationMethod.EMAIL to "Email",
                CheckoutActivationMethod.SMS to "SMS",
            ),
            selected = method,
            onSelect = onMethod,
        )
        Spacer(Modifier.height(14.dp))

        if (method == CheckoutActivationMethod.IUMRAH_ID) {
            CheckoutIdentityRow("iumrah ID", value.iumrahID)
            Spacer(Modifier.height(10.dp))
            BookingPasswordField(
                bookingText(language, "Создайте пароль", "Create password", "Parol yarating", "Парол яратинг"),
                password,
                onPassword,
            )
            Spacer(Modifier.height(10.dp))
            BookingPasswordField(
                bookingText(language, "Подтвердите пароль", "Confirm password", "Parolni tasdiqlang", "Паролни тасдиқланг"),
                confirm,
                onConfirm,
            )
            Spacer(Modifier.height(10.dp))
            RequirementRow(bookingText(language, "Минимум 8 символов", "At least 8 characters", "Kamida 8 belgi", "Камида 8 белги"), password.length >= 8)
            Spacer(Modifier.height(5.dp))
            RequirementRow(bookingText(language, "Пароли совпадают", "Passwords match", "Parollar mos", "Пароллар мос"), confirm.isNotEmpty() && confirm == password)
            Spacer(Modifier.height(15.dp))
            BookingPrimaryAction(
                bookingText(language, if (busy) "Активируем…" else "Активировать аккаунт", if (busy) "Activating…" else "Activate account", if (busy) "Faollashtirilmoqda…" else "Akkauntni faollashtirish", if (busy) "Фаоллаштирилмоқда…" else "Аккаунтни фаоллаштириш"),
                onClick = onActivate,
                trailing = CupertinoSymbol.ArrowRight,
            )
        } else {
            BookingRaisedCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    CupertinoIcon(if (method == CheckoutActivationMethod.EMAIL) CupertinoSymbol.Mail else CupertinoSymbol.Message, null, Modifier.size(19.dp), MaterialTheme.colorScheme.onBackground.copy(alpha = .65f))
                    Text(
                        if (method == CheckoutActivationMethod.EMAIL)
                            bookingText(language, "Email-подтверждение остаётся доступно в iOS-потоке. Для этой Android-сборки используйте iumrah ID — он уже привязан к бронированию.", "Email confirmation remains available in the iOS flow. In this Android build, use the iumrah ID already linked to the booking.", "Email tasdiqlash iOS oqimida mavjud. Androidda bron bilan bog‘langan iumrah ID dan foydalaning.", "Email тасдиқлаш iOS оқимида мавжуд. Androidда брон билан боғланган iumrah ID дан фойдаланинг.")
                        else bookingText(language, "SMS-подтверждение доступно для +998 в основном потоке. Здесь используйте iumrah ID, уже созданный для поездки.", "SMS confirmation is available for +998 in the main flow. Here, use the iumrah ID already created for the trip.", "SMS tasdiqlash +998 uchun asosiy oqimda mavjud. Bu yerda safar uchun yaratilgan iumrah ID dan foydalaning.", "SMS тасдиқлаш +998 учун асосий оқимда мавжуд. Бу ерда сафар учун яратилган iumrah ID дан фойдаланинг."),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = .58f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckoutLoginCard(
    language: AppLanguage,
    value: IumrahCheckoutResponse,
    password: String,
    onPassword: (String) -> Unit,
    busy: Boolean,
    onRecover: () -> Unit,
    onLogin: () -> Unit,
) {
    BookingCard {
        StageHeader("01", CupertinoSymbol.LockShield, bookingText(language, "Войдите в iumrah", "Sign in to iumrah", "iumrah'ga kiring", "iumrah'га киринг"))
        Spacer(Modifier.height(14.dp))
        Text(
            bookingText(language, "Этот iumrah ID уже активирован. Войдите, чтобы открыть данные паломников, оплату и документы.", "This iumrah ID is already active. Sign in to open pilgrim details, payment and documents.", "Bu iumrah ID allaqachon faol. Ma’lumotlar, to‘lov va hujjatlarni ochish uchun kiring.", "Бу iumrah ID аллақачон фаол. Маълумотлар, тўлов ва ҳужжатларни очиш учун киринг."),
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f),
        )
        Spacer(Modifier.height(14.dp))
        CheckoutIdentityRow("iumrah ID", value.iumrahID)
        Spacer(Modifier.height(10.dp))
        BookingPasswordField(bookingText(language, "Пароль", "Password", "Parol", "Парол"), password, onPassword)
        Spacer(Modifier.height(15.dp))
        BookingPrimaryAction(
            bookingText(language, if (busy) "Входим…" else "Войти", if (busy) "Signing in…" else "Sign in", if (busy) "Kirilmoqda…" else "Kirish", if (busy) "Кирилмоқда…" else "Кириш"),
            onClick = onLogin,
            trailing = CupertinoSymbol.ArrowRight,
        )
        TextButton(onClick = onRecover, modifier = Modifier.fillMaxWidth()) {
            Text(bookingText(language, "Забыли пароль?", "Forgot password?", "Parolni unutdingizmi?", "Паролни унутдингизми?"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CheckoutIdentityRow(title: String, value: String) {
    Row(
        Modifier.fillMaxWidth().height(54.dp).background(bookingIosRaised(), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        CupertinoIcon(CupertinoSymbol.IdentityCard, null, Modifier.size(20.dp), MaterialTheme.colorScheme.onBackground.copy(alpha = .58f))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .50f))
            Text(value, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BookingPasswordField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        placeholder = { Text(label, fontSize = 14.sp) },
        leadingIcon = { CupertinoIcon(CupertinoSymbol.Lock, null, Modifier.size(18.dp), MaterialTheme.colorScheme.onBackground.copy(alpha = .55f)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = bookingIosRaised().copy(alpha = .60f),
            unfocusedContainerColor = bookingIosRaised().copy(alpha = .60f),
            focusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .10f),
            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .06f),
        ),
    )
}

@Composable
private fun RequirementRow(title: String, ready: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        CupertinoIcon(if (ready) CupertinoSymbol.CheckCircleFill else CupertinoSymbol.CheckCircle, null, Modifier.size(14.dp), if (ready) BookingCareDark else MaterialTheme.colorScheme.onBackground.copy(alpha = .44f))
        Text(title, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold, color = if (ready) BookingCareDark else MaterialTheme.colorScheme.onBackground.copy(alpha = .55f))
    }
}

@Composable
private fun TravelersCard(language: AppLanguage, value: IumrahCheckoutResponse, editable: Boolean, onEdit: (IumrahTravelerForm) -> Unit) {
    BookingCard {
        StageHeader("02", CupertinoSymbol.Persons, bookingText(language, "Данные паломников", "Pilgrim details", "Ziyoratchilar ma’lumotlari", "Зиёратчилар маълумотлари"))
        Spacer(Modifier.height(12.dp))
        Text(
            if (editable)
                bookingText(language, "Для каждого участника поездки — отдельная защищённая анкета.", "One secure form for every traveler in this booking.", "Har bir sayohatchi uchun alohida himoyalangan anketa.", "Ҳар бир саёҳатчи учун алоҳида ҳимояланган анкета.")
            else bookingText(language, "Данные каждого паломника закреплены за этим бронированием до конца поездки.", "Traveler details stay attached to this booking for the rest of the journey.", "Har bir ziyoratchining ma’lumotlari safar oxirigacha shu bronga biriktiriladi.", "Ҳар бир зиёратчининг маълумотлари сафар охиригача шу бронга бириктирилади."),
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f),
        )
        Spacer(Modifier.height(15.dp))
        value.travelers.sortedBy { it.position }.forEachIndexed { index, traveler ->
            TravelerRow(language, traveler, editable, onEdit)
            if (index != value.travelers.lastIndex) Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun TravelerRow(language: AppLanguage, traveler: IumrahTravelerForm, editable: Boolean, onEdit: (IumrahTravelerForm) -> Unit) {
    IumrahPressable(
        onClick = { if (editable) onEdit(traveler) },
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        background = Color.Transparent,
        pressedScale = if (editable) .985f else 1f,
    ) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(bookingIosRaised().copy(alpha = .72f), RoundedCornerShape(20.dp))
            .border(.65.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = .055f), RoundedCornerShape(20.dp))
            .padding(13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            BookingIconBadge(
                symbol = if (traveler.completed) CupertinoSymbol.Checkmark else CupertinoSymbol.PersonCircle,
                tint = if (traveler.completed) Color(0xFF34C759) else Color(0xFF5856D6),
                size = 46.dp,
                symbolSize = 17.dp,
                radius = 15.dp,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(travelerDisplayName(language, traveler), fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    bookingText(language, "Паломник ${traveler.position}", "Pilgrim ${traveler.position}", "Ziyoratchi ${traveler.position}", "Зиёратчи ${traveler.position}"),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = .52f),
                )
                Text(
                    if (traveler.completed) bookingText(language, "Анкета готова", "Completed", "Anketa tayyor", "Анкета тайёр")
                    else bookingText(language, "Нужны данные и паспорт", "Passport and travel details required", "Ma’lumot va pasport kerak", "Маълумот ва паспорт керак"),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = .52f),
                )
            }
            CupertinoIcon(if (editable) CupertinoSymbol.ChevronRight else CupertinoSymbol.Lock, null, Modifier.size(13.dp), MaterialTheme.colorScheme.onBackground.copy(alpha = .30f))
        }

        if (!editable) {
            val facts = listOf(
                bookingText(language, "Дата рождения", "Date of birth", "Tug‘ilgan sana", "Туғилган сана") to traveler.dateOfBirth,
                bookingText(language, "Гражданство", "Nationality", "Fuqarolik", "Фуқаролик") to traveler.nationality,
                bookingText(language, "Паспорт", "Passport", "Pasport", "Паспорт") to traveler.passportNumber,
                bookingText(language, "Срок паспорта", "Passport expiry", "Pasport muddati", "Паспорт муддати") to traveler.passportExpiryDate,
                bookingText(language, "Телефон", "Phone", "Telefon", "Телефон") to traveler.phone,
                "Email" to traveler.email,
            ).filter { it.second.isNotBlank() }
            if (facts.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = .08f))
                Spacer(Modifier.height(9.dp))
                facts.forEach { (title, fact) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .50f))
                        Spacer(Modifier.weight(1f))
                        Text(fact, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
    }
}

private fun travelerDisplayName(language: AppLanguage, traveler: IumrahTravelerForm): String =
    listOf(traveler.firstName, traveler.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank {
        bookingText(language, "Паломник ${traveler.position}", "Pilgrim ${traveler.position}", "Ziyoratchi ${traveler.position}", "Зиёратчи ${traveler.position}")
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TravelerEditorSheet(
    bookingID: String,
    traveler: IumrahTravelerForm,
    language: AppLanguage,
    accountService: IumrahAccountService,
    token: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var form by remember(traveler) { mutableStateOf(traveler) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var passportBytes by remember { mutableStateOf<ByteArray?>(null) }
    val passportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { AndroidImageCodec.jpeg(context.contentResolver, uri, 2048, 90) } }
                .onSuccess { passportBytes = it }
                .onFailure { error = it.message }
        }
    }

    val requiredReady = form.firstName.isNotBlank() && form.lastName.isNotBlank() && form.gender.isNotBlank() &&
        form.dateOfBirth.isNotBlank() && form.nationality.isNotBlank() && form.passportNumber.isNotBlank() &&
        form.passportExpiryDate.isNotBlank() && form.phone.isNotBlank() && form.emergencyName.isNotBlank() &&
        form.emergencyPhone.isNotBlank() && (form.hasPassport || passportBytes != null)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = bookingIosPage(), dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = BOOKING_PAGE_PADDING.dp).padding(top = 2.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(bookingText(language, "Паломник ${traveler.position}", "Pilgrim ${traveler.position}", "Ziyoratchi ${traveler.position}", "Зиёратчи ${traveler.position}"), fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(bookingText(language, "Защищённая анкета iumrah", "Secure iumrah traveler form", "Himoyalangan iumrah anketasi", "Ҳимояланган iumrah анкетаси"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .50f))
                }
                IumrahPressable(onClick = onDismiss, modifier = Modifier.size(38.dp), cornerRadius = 19.dp, background = bookingIosRaised(), pressedScale = .94f) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CupertinoIcon(CupertinoSymbol.Close, null, Modifier.size(14.dp), MaterialTheme.colorScheme.onBackground) }
                }
            }

            TravelerFormSection(CupertinoSymbol.PersonCircle, bookingText(language, "Личные данные", "Personal details", "Shaxsiy ma’lumotlar", "Шахсий маълумотлар")) {
                TravelerField(bookingText(language, "Имя", "First name", "Ism", "Исм"), form.firstName) { form = form.copy(firstName = it) }
                TravelerField(bookingText(language, "Отчество / второе имя", "Middle name", "Otasining ismi", "Отасининг исми"), form.middleName) { form = form.copy(middleName = it) }
                TravelerField(bookingText(language, "Фамилия", "Last name", "Familiya", "Фамилия"), form.lastName) { form = form.copy(lastName = it) }
                BookingSegmentedControl(
                    items = listOf("male" to bookingText(language, "Мужчина", "Male", "Erkak", "Эркак"), "female" to bookingText(language, "Женщина", "Female", "Ayol", "Аёл")),
                    selected = form.gender.ifBlank { "male" },
                    onSelect = { form = form.copy(gender = it) },
                )
                TravelerField(bookingText(language, "Дата рождения · YYYY-MM-DD", "Date of birth · YYYY-MM-DD", "Tug‘ilgan sana · YYYY-MM-DD", "Туғилган сана · YYYY-MM-DD"), form.dateOfBirth) { form = form.copy(dateOfBirth = it) }
                TravelerField(bookingText(language, "Гражданство", "Citizenship", "Fuqarolik", "Фуқаролик"), form.nationality) { form = form.copy(nationality = it, passportIssuingCountry = it) }
            }

            TravelerFormSection(CupertinoSymbol.Passport, bookingText(language, "Паспорт", "Passport", "Pasport", "Паспорт")) {
                TravelerField(bookingText(language, "Номер паспорта", "Passport number", "Pasport raqami", "Паспорт рақами"), form.passportNumber) { form = form.copy(passportNumber = it.uppercase()) }
                TravelerField(bookingText(language, "Срок действия · YYYY-MM-DD", "Expiry date · YYYY-MM-DD", "Amal qilish muddati · YYYY-MM-DD", "Амал қилиш муддати · YYYY-MM-DD"), form.passportExpiryDate) { form = form.copy(passportExpiryDate = it) }
                IumrahPressable(
                    onClick = { passportLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 66.dp),
                    cornerRadius = 19.dp,
                    background = bookingIosRaised().copy(alpha = .78f),
                    pressedScale = .985f,
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BookingIconBadge(if (passportBytes != null || form.hasPassport) CupertinoSymbol.CheckCircleFill else CupertinoSymbol.Document, if (passportBytes != null || form.hasPassport) Color(0xFF34C759) else Color(0xFF007AFF), 40.dp, 17.dp, 13.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(if (passportBytes != null || form.hasPassport) bookingText(language, "Фото паспорта прикреплено", "Passport photo attached", "Pasport rasmi biriktirildi", "Паспорт расми бириктирилди") else bookingText(language, "Прикрепить фото паспорта", "Attach passport photo", "Pasport rasmini biriktirish", "Паспорт расмини бириктириш"), fontSize=14.sp, fontWeight=FontWeight.SemiBold)
                            Text(bookingText(language,"Чёткое фото страницы с данными","Clear photo of the information page","Ma’lumotlar sahifasining aniq rasmi","Маълумотлар саҳифасининг аниқ расми"), fontSize=12.sp, color=MaterialTheme.colorScheme.onBackground.copy(alpha=.52f))
                        }
                        CupertinoIcon(CupertinoSymbol.ChevronRight,null,Modifier.size(12.dp),MaterialTheme.colorScheme.onBackground.copy(alpha=.28f))
                    }
                }
            }

            TravelerFormSection(CupertinoSymbol.Phone, bookingText(language, "Контакты", "Contacts", "Aloqa", "Алоқа")) {
                TravelerField(bookingText(language, "Номер телефона", "Phone", "Telefon", "Телефон"), form.phone) { form = form.copy(phone = it) }
                TravelerField("Email", form.email) { form = form.copy(email = it) }
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = .08f))
                Text(bookingText(language,"Экстренный контакт","Emergency contact","Favqulodda kontakt","Фавқулодда контакт"), fontSize=14.sp, fontWeight=FontWeight.SemiBold)
                TravelerField(bookingText(language, "Имя контакта", "Contact name", "Kontakt ismi", "Контакт исми"), form.emergencyName) { form = form.copy(emergencyName = it) }
                TravelerField(bookingText(language, "Экстренный номер телефона", "Emergency phone", "Favqulodda telefon", "Фавқулодда телефон"), form.emergencyPhone) { form = form.copy(emergencyPhone = it) }
                TravelerField(bookingText(language, "Кем приходится", "Relationship", "Qarindoshlik", "Қариндошлик"), form.emergencyRelation) { form = form.copy(emergencyRelation = it) }
            }

            if (!requiredReady) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.error.copy(alpha=.07f)).border(.8.dp, MaterialTheme.colorScheme.error.copy(alpha=.16f), RoundedCornerShape(18.dp)).padding(14.dp), verticalAlignment=Alignment.Top, horizontalArrangement=Arrangement.spacedBy(11.dp)) {
                    CupertinoIcon(CupertinoSymbol.ExclamationCircle,null,Modifier.size(17.dp),MaterialTheme.colorScheme.error)
                    Column(Modifier.weight(1f), verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text(bookingText(language,"Заполните обязательные поля","Complete the required fields","Majburiy maydonlarni to‘ldiring","Мажбурий майдонларни тўлдиринг"), fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.error)
                        Text(bookingText(language,"Имя, фамилия, пол, дата рождения, гражданство, паспорт, телефон и экстренный контакт.","Name, gender, date of birth, citizenship, passport, phone and emergency contact are required.","Ism, jins, tug‘ilgan sana, fuqarolik, pasport, telefon va favqulodda kontakt majburiy.","Исм, жинс, туғилган сана, фуқаролик, паспорт, телефон ва фавқулодда контакт мажбурий."),fontSize=12.sp,lineHeight=16.sp,color=MaterialTheme.colorScheme.onBackground.copy(alpha=.52f))
                    }
                }
            }
            error?.let { Text(it, color=MaterialTheme.colorScheme.error,fontSize=13.sp,lineHeight=17.sp) }
            BookingPrimaryAction(
                title = if (busy) bookingText(language,"Сохраняем…","Saving…","Saqlanmoqda…","Сақланмоқда…") else bookingText(language,"Сохранить анкету","Save pilgrim","Anketani saqlash","Анкетани сақлаш"),
                onClick = {
                    if (!requiredReady || busy) return@BookingPrimaryAction
                    busy = true; error = null
                    scope.launch {
                        runCatching {
                            val payload = form.copy(passportIssuingCountry = form.nationality)
                            accountService.saveTraveler(bookingID, traveler.position, payload, token)
                            passportBytes?.let { accountService.uploadPassport(bookingID, traveler.position, it, "image/jpeg", token) }
                        }.onSuccess { onSaved() }.onFailure { error = it.message }
                        busy = false
                    }
                },
                leading = CupertinoSymbol.CheckCircleFill,
            )
        }
    }
}

@Composable
private fun TravelerFormSection(symbol: CupertinoSymbol, title: String, content: @Composable ColumnScope.() -> Unit) {
    BookingCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BookingIconBadge(symbol, Color(0xFF5856D6), 38.dp, 16.dp, 13.dp)
            Text(title, fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun TravelerField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        placeholder = { Text(label, fontSize=14.sp, color=MaterialTheme.colorScheme.onBackground.copy(alpha=.46f)) },
        singleLine = true,
        shape = RoundedCornerShape(19.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = bookingIosRaised().copy(alpha=.72f), unfocusedContainerColor = bookingIosRaised().copy(alpha=.72f),
            focusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha=.10f), unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha=.055f),
        ),
    )
}

@Composable
private fun AvailabilityPaymentLockedCard(language: AppLanguage) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(bookingIosCard(), RoundedCornerShape(22.dp))
            .border(.7.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = .06f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            BookingIconBadge(CupertinoSymbol.CalendarClock, Color(0xFFFF9500), 44.dp, 17.dp, 15.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(bookingText(language, "Оплата откроется после подтверждения", "Payment opens after confirmation", "To‘lov tasdiqdan keyin ochiladi", "Тўлов тасдиқдан кейин очилади"), fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    bookingText(language, "Сейчас заполните анкеты паломников. Инвойс и реквизиты оплаты появятся только после подтверждения наличия.", "Complete the pilgrim forms now. Invoice and payment details stay hidden until availability is confirmed.", "Hozir ziyoratchilar anketalarini to‘ldiring. Invoice va to‘lov rekvizitlari faqat mavjudlik tasdiqlangach ko‘rinadi.", "Ҳозир зиёратчилар анкеталарини тўлдиринг. Invoice ва тўлов реквизитлари фақат мавжудлик тасдиқлангач кўринади."),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f),
                )
            }
        }
    }
}

@Composable
private fun PaymentCard(
    language: AppLanguage,
    value: IumrahCheckoutResponse,
    busy: Boolean,
    canUpload: Boolean,
    onUpload: () -> Unit,
    chrome: AppChromeStore,
) {
    BookingCard {
        StageHeader("03", CupertinoSymbol.CreditCard, bookingText(language, "Оплата", "Payment", "To‘lov", "Тўлов"))
        Spacer(Modifier.height(14.dp))

        BookingRaisedCard(padding = 15.dp, radius = 20.dp) {
            Text("Visa", fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .50f))
            Spacer(Modifier.height(5.dp))
            Text(groupedCard(value.payment.visaCardNumber), fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (value.payment.visaHolder.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(value.payment.visaHolder, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .52f))
            }
        }
        if (value.payment.humoCardNumber.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            BookingRaisedCard(padding = 15.dp, radius = 20.dp) {
                Text("Humo", fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .50f))
                Spacer(Modifier.height(5.dp))
                Text(groupedCard(value.payment.humoCardNumber), fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (value.payment.instructions.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(value.payment.instructions, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f))
        }
        Spacer(Modifier.height(14.dp))
        BookingPrimaryAction(
            bookingText(language, if (busy) "Загружаем…" else "Прикрепить чек оплаты", if (busy) "Uploading…" else "Attach payment receipt", if (busy) "Yuklanmoqda…" else "To‘lov chekini biriktirish", if (busy) "Юкланмоқда…" else "Тўлов чекини бириктириш"),
            onClick = onUpload,
            leading = CupertinoSymbol.Paperclip,
        )
        if (!canUpload) {
            Spacer(Modifier.height(9.dp))
            Text(
                bookingText(language, "Войдите в аккаунт iumrah, чтобы загрузить чек.", "Sign in to your iumrah account to upload the receipt.", "Chekni yuklash uchun iumrah akkauntiga kiring.", "Чекни юклаш учун iumrah аккаунтига киринг."),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .52f),
            )
        }
    }
}

@Composable
private fun PaidReceiptCard(language: AppLanguage, value: IumrahCheckoutResponse) {
    val receipt = value.receipts.firstOrNull()
    BookingCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            BookingIconBadge(CupertinoSymbol.CheckCircleFill, Color(0xFF34C759), 48.dp, 18.dp, 16.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(bookingText(language, "Оплата получена", "Payment received", "To‘lov qabul qilindi", "Тўлов қабул қилинди"), fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(receipt?.reviewStatus ?: value.status, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .52f))
            }
        }
    }
}

@Composable
private fun DocumentsCard(language: AppLanguage, value: IumrahCheckoutResponse) {
    BookingCard {
        StageHeader("04", CupertinoSymbol.Document, bookingText(language, "Документы поездки", "Travel documents", "Safar hujjatlari", "Сафар ҳужжатлари"))
        Spacer(Modifier.height(12.dp))
        Text(
            bookingText(language, "Каждый документ появится здесь отдельно сразу после готовности.", "Each document appears here as soon as it is ready.", "Har bir hujjat tayyor bo‘lishi bilan shu yerda alohida paydo bo‘ladi.", "Ҳар бир ҳужжат тайёр бўлиши билан шу ерда алоҳида пайдо бўлади."),
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f),
        )
        Spacer(Modifier.height(14.dp))
        val rows = listOf(
            Triple("ticket", bookingText(language, "Авиабилет", "Airline ticket", "Aviachipta", "Авиачипта"), CupertinoSymbol.Airplane),
            Triple("voucher", bookingText(language, "Подтверждение отеля", "Hotel confirmation", "Mehmonxona tasdig‘i", "Меҳмонхона тасдиғи"), CupertinoSymbol.Hotel),
            Triple("visa", bookingText(language, "Виза", "Visa", "Viza", "Виза"), CupertinoSymbol.CheckCircleFill),
            Triple("insurance", bookingText(language, "Страховка", "Insurance", "Sug‘urta", "Суғурта"), CupertinoSymbol.ShieldCheck),
        )
        rows.forEachIndexed { index, (kind, title, symbol) ->
            DocumentStatusRow(language, title, symbol, value.documents.firstOrNull { it.documentKind.lowercase() == kind })
            if (index != rows.lastIndex) Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun DocumentStatusRow(language: AppLanguage, title: String, symbol: CupertinoSymbol, document: IumrahTravelDocument?) {
    Row(
        Modifier.fillMaxWidth().background(bookingIosRaised(), RoundedCornerShape(20.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        BookingIconBadge(symbol, if (document != null) Color(0xFF34C759) else Color(0xFF007AFF), 48.dp, 18.dp, 16.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (document != null) bookingText(language, "Готов · можно открыть", "Ready to open", "Tayyor · ochish mumkin", "Тайёр · очиш мумкин")
                else bookingText(language, "Готовится", "Being prepared", "Tayyorlanmoqda", "Тайёрланмоқда"),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .52f),
            )
        }
        if (document != null) CupertinoIcon(CupertinoSymbol.ArrowUpRight, null, Modifier.size(16.dp), MaterialTheme.colorScheme.onBackground.copy(alpha = .32f))
        else CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 1.5.dp)
    }
}

@Composable
private fun StageHeader(number: String, icon: CupertinoSymbol, title: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
        BookingIconBadge(icon, Color(0xFF5856D6), 38.dp, 15.dp, 12.dp)
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(number, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .50f))
            Text(title, fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun groupedCard(raw: String): String {
    val value = raw.filterNot(Char::isWhitespace)
    return if (value.isBlank()) "—" else value.chunked(4).joinToString(" ")
}
