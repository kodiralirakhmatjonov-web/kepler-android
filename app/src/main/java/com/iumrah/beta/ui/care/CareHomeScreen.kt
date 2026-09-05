package com.iumrah.beta.ui.care

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.booking.BookingStore
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.components.IumrahRootPageHeader

@Composable
fun CareHomeScreen(language: AppLanguage, bookingStore: BookingStore, chrome: AppChromeStore) {
    val state by bookingStore.state.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 54.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        item { IumrahRootPageHeader(L10n.text("care_title", language), chrome) }
        item { Text(L10n.text("care_subtitle", language), color = MaterialTheme.colorScheme.onBackground.copy(alpha=.55f), style = MaterialTheme.typography.bodyLarge) }
        item {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(34.dp)).padding(21.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Favorite, null, tint = MaterialTheme.colorScheme.background)
                Text(L10n.text("care_promise_title", language), color = MaterialTheme.colorScheme.background, style = MaterialTheme.typography.headlineSmall)
                Text(L10n.text("care_promise_body", language), color = MaterialTheme.colorScheme.background.copy(alpha=.68f))
            }
        }
        if (state.sessions.isEmpty()) {
            item {
                Text(L10n.text("care_locked_title", language), style = MaterialTheme.typography.titleLarge)
                Text(L10n.text("care_locked_body", language), color = MaterialTheme.colorScheme.onBackground.copy(alpha=.52f))
            }
        } else {
            item { Text(L10n.text("care_chats", language), style = MaterialTheme.typography.titleLarge) }
            items(state.sessions, key = { it.id }) { session ->
                IumrahPressable(onClick = { chrome.openBookingChat(session.id) }, modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
                    Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ChatBubble, null) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(session.displayBookingNumber, style = MaterialTheme.typography.titleMedium)
                            Text(session.travelerName ?: session.booking.hotelNames.makkah, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.52f), maxLines = 1)
                        }
                        Text("Care", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
