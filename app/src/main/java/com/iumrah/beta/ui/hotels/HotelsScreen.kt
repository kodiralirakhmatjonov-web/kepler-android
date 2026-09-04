package com.iumrah.beta.ui.hotels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.iumrah.beta.R
import com.iumrah.beta.core.config.AppConfig
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.hotel.HotelCatalogService
import com.iumrah.beta.models.hotel.HotelSummary
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.components.IumrahRootPageHeader
import com.iumrah.beta.ui.components.IumrahSectionHeader

@Composable
fun HotelsScreen(
    language: AppLanguage,
    service: HotelCatalogService,
    chrome: AppChromeStore,
) {
    var makkah by remember { mutableStateOf<List<HotelSummary>>(emptyList()) }
    var madinah by remember { mutableStateOf<List<HotelSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        loading = true
        val a = runCatching { service.listHotels("Makkah") }
        val b = runCatching { service.listHotels("Madinah") }
        makkah = a.getOrElse { emptyList() }
        madinah = b.getOrElse { emptyList() }
        error = if (a.isFailure || b.isFailure) L10n.text("hotels_load_error", language) else null
        loading = false
    }
    LaunchedEffect(Unit) { load() }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { IumrahRootPageHeader(L10n.text("tab_hotels", language), chrome) }
        item { HotelsHero(language) }
        if (loading && makkah.isEmpty() && madinah.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else {
            item { IumrahSectionHeader(L10n.text("hotels_makkah", language), L10n.text("hotels_selected_badge", language)) }
            items(makkah.size, key = { makkah[it].id }) { HotelRow(makkah[it], language) { chrome.openHotel(makkah[it].id) } }
            item { Spacer(Modifier.height(4.dp)); IumrahSectionHeader(L10n.text("hotels_madinah", language), L10n.text("hotels_selected_badge", language)) }
            items(madinah.size, key = { madinah[it].id }) { HotelRow(madinah[it], language) { chrome.openHotel(madinah[it].id) } }
        }
        error?.let { message -> item { Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f)) } }
    }
}

@Composable
private fun HotelsHero(language: AppLanguage) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(34.dp)).background(Color(0xFF111315)).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(L10n.text("hotels_title", language), style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Text(L10n.text("hotels_subtitle", language), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = .72f))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha=.10f)).padding(14.dp)) {
            Text(L10n.text("hotels_note", language), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha=.85f))
        }
    }
}

@Composable
private fun HotelRow(hotel: HotelSummary, language: AppLanguage, onClick: () -> Unit) {
    IumrahPressable(onClick = onClick, modifier = Modifier.fillMaxWidth(), cornerRadius = 30.dp, shadowElevation = 3.dp) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val url = AppConfig.absoluteUrl(hotel.coverImageURL)
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = hotel.name,
                    modifier = Modifier.size(94.dp).clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.size(94.dp).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Hotel, contentDescription = null)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(hotel.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    hotel.stars?.let { Meta("$it★") }
                    hotel.rating?.let { Meta(String.format("%.1f", it)) }
                }
                Text(L10n.city(hotel.city, language), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.53f))
            }
            Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha=.34f))
        }
    }
}

@Composable private fun Meta(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha=.55f))
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.56f))
    }
}
