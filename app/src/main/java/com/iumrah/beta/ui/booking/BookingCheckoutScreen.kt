package com.iumrah.beta.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.iumrah.beta.core.design.IumrahHaptics
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.account.IumrahAccountStore
import com.iumrah.beta.data.booking.BookingStore
import com.iumrah.beta.domain.journey.JourneyStore
import com.iumrah.beta.models.booking.BookingPilgrimProfile
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.components.IumrahPrimaryButton
import kotlinx.coroutines.launch

@Composable
fun BookingCheckoutScreen(
    language: AppLanguage,
    journey: JourneyStore,
    bookingStore: BookingStore,
    accountStore: IumrahAccountStore,
    chrome: AppChromeStore,
) {
    val journeyState by journey.state.collectAsState()
    val account by accountStore.state.collectAsState()
    val quote = journeyState.quote
    val scope = rememberCoroutineScope()
    val haptic = LocalView.current
    var firstName by remember(account.account?.iumrahID) { mutableStateOf(account.account?.firstName.orEmpty()) }
    var lastName by remember(account.account?.iumrahID) { mutableStateOf(account.account?.lastName.orEmpty()) }
    var telegram by remember(account.account?.iumrahID) { mutableStateOf(account.account?.telegram.orEmpty()) }
    var whatsapp by remember(account.account?.iumrahID) { mutableStateOf(account.account?.whatsapp.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val canSubmit = quote != null && firstName.trim().isNotEmpty() && lastName.trim().isNotEmpty() && (telegram.trim().isNotEmpty() || whatsapp.trim().isNotEmpty()) && !busy

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        IumrahPressable(onClick = chrome::back, modifier = Modifier.size(44.dp), cornerRadius = 99.dp, background = MaterialTheme.colorScheme.surfaceVariant) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ArrowBack, "Back") } }
        Text("Booking details", style = MaterialTheme.typography.headlineLarge)
        Text("Your booking will be created in the same iumrah system used by iumrah Business.", color = MaterialTheme.colorScheme.onBackground.copy(alpha=.55f))
        BookingField("First name", firstName, { firstName = it })
        BookingField("Last name", lastName, { lastName = it })
        BookingField("Telegram", telegram, { telegram = it })
        BookingField("WhatsApp", whatsapp, { whatsapp = it })
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        IumrahPrimaryButton(if (busy) "Creating booking…" else "Create booking", enabled = canSubmit, onClick = {
            val currentQuote = quote ?: return@IumrahPrimaryButton
            busy = true; error = null
            scope.launch {
                runCatching {
                    bookingStore.create(
                        journey = journeyState,
                        quote = currentQuote,
                        language = language,
                        pilgrimProfile = BookingPilgrimProfile(firstName.trim(), lastName.trim(), telegram.trim(), whatsapp.trim()),
                    )
                }.onSuccess { session ->
                    IumrahHaptics.success(haptic)
                    busy = false
                    chrome.openBookingDetail(session.id)
                }.onFailure { cause -> busy = false; error = cause.message ?: "Booking could not be created." }
            }
        })
        Text("Booking access token is stored using Android Keystore-backed encrypted storage.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.44f))
        Spacer(Modifier.height(36.dp))
    }
}

@Composable private fun BookingField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
}
