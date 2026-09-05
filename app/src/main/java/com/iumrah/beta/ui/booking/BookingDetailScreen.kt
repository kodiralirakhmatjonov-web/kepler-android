package com.iumrah.beta.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.booking.BookingStore
import com.iumrah.beta.models.booking.BookingItineraryItem
import com.iumrah.beta.ui.components.IumrahPill
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.components.IumrahPrimaryButton
import com.iumrah.beta.ui.components.IumrahSecondaryButton
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun BookingDetailScreen(bookingID: String, language: AppLanguage, bookingStore: BookingStore, chrome: AppChromeStore) {
    val storeState by bookingStore.state.collectAsState()
    val session = storeState.sessions.firstOrNull { it.id == bookingID }
    val scope = rememberCoroutineScope()
    var itinerary by remember(bookingID) { mutableStateOf<List<BookingItineraryItem>>(emptyList()) }
    var telegram by remember(session?.telegram) { mutableStateOf(session?.telegram.orEmpty()) }
    var whatsapp by remember(session?.whatsapp) { mutableStateOf(session?.whatsapp.orEmpty()) }
    var editContacts by remember { mutableStateOf(false) }
    var ziyaratMakkah by remember(session?.id) { mutableStateOf(session?.ziyaratMakkahOverride ?: session?.booking?.customization?.ziyaratMakkah ?: true) }
    var ziyaratMadinah by remember(session?.id) { mutableStateOf(session?.ziyaratMadinahOverride ?: session?.booking?.customization?.ziyaratMadinah ?: true) }
    var esim by remember(session?.id) { mutableStateOf(session?.esimOverride ?: session?.booking?.customization?.esim ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(bookingID) {
        bookingStore.refresh(bookingID)
        bookingStore.booking(bookingID)?.let { current ->
            itinerary = runCatching { bookingStore.service.fetchItinerary(bookingID, bookingStore.headersFor(current)) }.getOrDefault(emptyList())
        }
    }
    if (session == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) { Text("Booking session not found") }
        return
    }
    val money = NumberFormat.getCurrencyInstance(Locale.US).apply { currency = java.util.Currency.getInstance("USD"); maximumFractionDigits = 0 }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IumrahPressable(onClick = chrome::back, modifier = Modifier.size(44.dp), cornerRadius = 99.dp, background = MaterialTheme.colorScheme.surfaceVariant) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ArrowBack, "Back") } }
        Text(L10n.text("booking_detail_title", language), style = MaterialTheme.typography.headlineLarge)
        IumrahBookingDomeCard(session) {}

        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(30.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(L10n.text("booking_total_package", language), style = MaterialTheme.typography.titleLarge); Spacer(Modifier.weight(1f)); Text(money.format(session.booking.totalUsd), style = MaterialTheme.typography.titleLarge) }
            Text(L10n.text("booking_all_in_one_price", language), color = MaterialTheme.colorScheme.onSurface.copy(alpha=.54f))
            IumrahPill(session.effectiveStatus.replace('_', ' '))
        }

        HotelSection("Makkah", session.hotelSelection?.hotelName ?: session.booking.hotelNames.makkah, session.hotelSelection?.roomName) { chrome.openBookingHotelChange(bookingID, "makkah") }
        if (session.booking.input.includeMadinah) HotelSection("Madinah", session.madinahHotelSelection?.hotelName ?: session.booking.hotelNames.madinah, session.madinahHotelSelection?.roomName) { chrome.openBookingHotelChange(bookingID, "madinah") }

        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(30.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(L10n.text("booking_contacts", language), style = MaterialTheme.typography.titleLarge)
            if (editContacts) {
                OutlinedTextField(telegram, { telegram = it }, label = { Text("Telegram") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(20.dp))
                OutlinedTextField(whatsapp, { whatsapp = it }, label = { Text("WhatsApp") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(20.dp))
                IumrahPrimaryButton(L10n.text("booking_save_changes", language), enabled = telegram.isNotBlank() || whatsapp.isNotBlank()) {
                    scope.launch { runCatching { bookingStore.updateContacts(bookingID, telegram, whatsapp) }.onSuccess { editContacts = false }.onFailure { error = it.message } }
                }
            } else {
                Text(listOfNotNull(telegram.takeIf { it.isNotBlank() }?.let { "Telegram: $it" }, whatsapp.takeIf { it.isNotBlank() }?.let { "WhatsApp: $it" }).joinToString("\n").ifBlank { L10n.text("booking_contact_empty", language) })
                IumrahSecondaryButton(L10n.text("booking_contact_edit", language)) { editContacts = true }
            }
        }

        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(30.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(L10n.text("booking_ziyarat_title", language), style = MaterialTheme.typography.titleLarge)
            SettingsSwitch(L10n.text("booking_ziyarat_makkah", language), ziyaratMakkah) { ziyaratMakkah = it }
            if (session.booking.input.includeMadinah) SettingsSwitch(L10n.text("booking_ziyarat_madinah", language), ziyaratMadinah) { ziyaratMadinah = it }
            SettingsSwitch("eSIM", esim) { esim = it }
            IumrahSecondaryButton(L10n.text("booking_save_changes", language)) {
                scope.launch {
                    runCatching { bookingStore.updateZiyarat(bookingID, ziyaratMakkah, ziyaratMadinah); bookingStore.updateESIM(bookingID, esim) }
                        .onFailure { error = it.message }
                }
            }
        }

        if (itinerary.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(30.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("Itinerary", style = MaterialTheme.typography.titleLarge)
                itinerary.forEach { item ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(34.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Text(item.icon.take(2)) }
                        Spacer(Modifier.width(10.dp))
                        Column { Text(item.title, style = MaterialTheme.typography.titleMedium); Text(listOf(item.dateLocal, item.subtitle, item.location).filter { it.isNotBlank() }.joinToString(" · "), color = MaterialTheme.colorScheme.onSurface.copy(alpha=.52f), style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }

        IumrahSecurityConfirmationPanel(session, bookingStore.service)

        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(30.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.ChatBubble, null); Spacer(Modifier.width(9.dp)); Text("iumrah Care", style = MaterialTheme.typography.titleLarge) }
            Text(L10n.text("booking_care_body", language), color = MaterialTheme.colorScheme.onSurface.copy(alpha=.55f))
            IumrahPrimaryButton(L10n.text("booking_open_care", language)) { chrome.openBookingChat(bookingID) }
        }
        IumrahPressable(onClick = { chrome.openPilgrimCheckout(bookingID) }, modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp, background = MaterialTheme.colorScheme.surfaceVariant) {
            Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Luggage, null); Spacer(Modifier.width(12.dp)); Text("Pilgrim documents & payment", style = MaterialTheme.typography.titleMedium) }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(42.dp))
    }
}

@Composable private fun HotelSection(city: String, hotel: String, room: String?, onChange: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(30.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Hotel, null); Spacer(Modifier.width(9.dp)); Text("Hotel in $city", style = MaterialTheme.typography.titleLarge) }
        Text(hotel, style = MaterialTheme.typography.titleMedium)
        room?.let { Text(it, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.54f)) }
        IumrahSecondaryButton("Change hotel", onClick = onChange)
    }
}

@Composable private fun SettingsSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, modifier = Modifier.weight(1f)); Switch(checked, onChange) }
}
