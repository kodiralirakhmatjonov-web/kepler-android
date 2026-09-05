package com.iumrah.beta.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.booking.BookingStore
import com.iumrah.beta.ui.components.IumrahPrimaryButton
import com.iumrah.beta.ui.components.IumrahRootPageHeader

@Composable
fun BookingsHomeScreen(language: AppLanguage, bookingStore: BookingStore, chrome: AppChromeStore) {
    val state by bookingStore.state.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { IumrahRootPageHeader(L10n.text("tab_booking", language), chrome) }
        item { Text(L10n.text("booking_home_subtitle", language), color = MaterialTheme.colorScheme.onBackground.copy(alpha=.55f), style = MaterialTheme.typography.bodyLarge) }
        if (state.sessions.isEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Text(L10n.text("booking_empty_title", language), style = MaterialTheme.typography.headlineSmall)
                Text(L10n.text("booking_empty_body", language), color = MaterialTheme.colorScheme.onBackground.copy(alpha=.54f))
                Spacer(Modifier.height(16.dp))
                IumrahPrimaryButton(L10n.text("booking_hero_cta", language), onClick = chrome::startNewTrip)
            }
        } else {
            items(state.sessions, key = { it.id }) { session ->
                IumrahBookingDomeCard(session) { chrome.openBookingDetail(session.id) }
            }
            item { IumrahPrimaryButton(L10n.text("booking_hero_cta", language), onClick = chrome::startNewTrip) }
        }
    }
}
