package com.iumrah.beta.ui.trip

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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.iumrah.beta.core.config.AppConfig
import com.iumrah.beta.core.design.IumrahHaptics
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.hotel.HotelCatalogService
import com.iumrah.beta.data.hotel.RemotePackageEngineClient
import com.iumrah.beta.domain.journey.JourneyStore
import com.iumrah.beta.domain.trip.JourneyScope
import com.iumrah.beta.models.hotel.HotelSummary
import com.iumrah.beta.ui.components.IumrahPill
import com.iumrah.beta.ui.components.IumrahPressable

@Composable
fun HotelSelectionScreen(
    language: AppLanguage,
    journey: JourneyStore,
    catalog: HotelCatalogService,
    packageEngine: RemotePackageEngineClient,
    chrome: AppChromeStore,
) {
    val state by journey.state.collectAsState()
    val hapticView = LocalView.current
    val needsMakkah = state.makkahHotel == null
    val needsMadinah = state.trip.scope == JourneyScope.MAKKAH_AND_MADINAH && state.madinahHotel == null
    val city = if (needsMakkah) "Makkah" else if (needsMadinah) "Madinah" else null
    var hotels by remember(city) { mutableStateOf<List<HotelSummary>>(emptyList()) }
    var primaryId by remember(city) { mutableStateOf<String?>(null) }
    var loading by remember(city) { mutableStateOf(city != null) }

    LaunchedEffect(city) {
        if (city == null) { chrome.openFlights(); return@LaunchedEffect }
        loading = true
        hotels = runCatching { catalog.listHotels(city) }.getOrElse { emptyList() }
        primaryId = runCatching { packageEngine.primaryHotel(state.trip.packageTier, state.trip.hotelStars, city).hotelId }.getOrNull()
        loading = false
    }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { BackButton(chrome::back) }
        item {
            Text(L10n.text(if (city == "Madinah") "hotels_madinah" else "hotels_makkah", language), style = MaterialTheme.typography.headlineLarge)
            Text(L10n.text("hotels_selected_badge", language), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha=.56f))
        }
        if (loading) item { Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        items(hotels.sortedByDescending { it.id == primaryId }, key = { it.id }) { hotel ->
            SelectHotelCard(hotel, language, recommended = hotel.id == primaryId) {
                IumrahHaptics.selection(hapticView)
                journey.selectHotel(hotel)
            }
        }
        if (!loading && hotels.isEmpty()) item { Text(L10n.text("hotels_load_error", language), color = MaterialTheme.colorScheme.onBackground.copy(alpha=.55f)) }
    }
}

@Composable private fun BackButton(onBack: () -> Unit) {
    IumrahPressable(onClick = onBack, modifier = Modifier.size(44.dp), cornerRadius = 99.dp, background = MaterialTheme.colorScheme.surfaceVariant) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } }
}

@Composable private fun SelectHotelCard(hotel: HotelSummary, language: AppLanguage, recommended: Boolean, onSelect: () -> Unit) {
    IumrahPressable(onClick = onSelect, cornerRadius = 30.dp, modifier = Modifier.fillMaxWidth(), shadowElevation = if (recommended) 8.dp else 3.dp) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                val url = AppConfig.absoluteUrl(hotel.coverImageURL)
                if (url != null) AsyncImage(model = url, contentDescription = hotel.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Rounded.Hotel, contentDescription = null, modifier = Modifier.align(Alignment.Center).size(40.dp))
                if (recommended) IumrahPill(L10n.text("flight_recommended", language), modifier = Modifier.padding(10.dp), background = MaterialTheme.colorScheme.surface.copy(alpha=.86f))
            }
            Text(hotel.name, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                hotel.stars?.let { IumrahPill("$it★") }
                hotel.rating?.let { IumrahPill(String.format("%.1f", it)) }
                IumrahPill(L10n.city(hotel.city, language))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(L10n.text("select", language).takeIf { it != "select" } ?: "Select", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.ArrowForward, contentDescription = null)
            }
        }
    }
}
