package com.iumrah.beta.ui.trip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.iumrah.beta.core.config.AppConfig
import com.iumrah.beta.core.design.IumrahHaptics
import com.iumrah.beta.core.design.IumrahMotion
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.hotel.HotelCatalogService
import com.iumrah.beta.data.hotel.RemotePackageEngineClient
import com.iumrah.beta.domain.journey.JourneyStore
import com.iumrah.beta.domain.trip.JourneyScope
import com.iumrah.beta.models.hotel.HotelSummary
import com.iumrah.beta.models.hotel.IumrahRoomCategoryOption
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
    val makkahComplete = state.makkahHotel != null && state.hasMakkahRoomSelection
    val madinahRequired = state.trip.scope == JourneyScope.MAKKAH_AND_MADINAH
    val madinahComplete = !madinahRequired || (state.madinahHotel != null && state.hasMadinahRoomSelection)
    val city = when {
        !makkahComplete -> "Makkah"
        !madinahComplete -> "Madinah"
        else -> null
    }
    val selectedHotel = if (city == "Madinah") state.madinahHotel else state.makkahHotel
    val selectedCategory = if (city == "Madinah") state.madinahRoomCategory else state.makkahRoomCategory
    var hotels by remember(city) { mutableStateOf<List<HotelSummary>>(emptyList()) }
    var primaryId by remember(city) { mutableStateOf<String?>(null) }
    var categories by remember(selectedHotel?.id) { mutableStateOf<List<IumrahRoomCategoryOption>>(emptyList()) }
    var loading by remember(city) { mutableStateOf(city != null) }
    var roomsLoading by remember(selectedHotel?.id) { mutableStateOf(false) }

    LaunchedEffect(city) {
        if (city == null) {
            chrome.openFlights()
            return@LaunchedEffect
        }
        loading = true
        hotels = runCatching { catalog.listHotels(city) }.getOrElse { emptyList() }
        primaryId = runCatching { packageEngine.primaryHotel(state.trip.packageTier, state.trip.hotelStars, city).hotelId }.getOrNull()
        loading = false
    }
    LaunchedEffect(selectedHotel?.id) {
        val id = selectedHotel?.id ?: run { categories = emptyList(); return@LaunchedEffect }
        roomsLoading = true
        categories = runCatching { packageEngine.roomCategories(id) }.getOrElse { emptyList() }
        roomsLoading = false
    }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { BackButton(chrome::back) }
        item {
            Text(L10n.text(if (city == "Madinah") "hotels_madinah" else "hotels_makkah", language), style = MaterialTheme.typography.headlineLarge)
            Text("Choose a hotel and room category", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .56f))
        }
        if (loading) item { Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        items(hotels.sortedByDescending { it.id == primaryId }, key = { it.id }) { hotel ->
            val isSelected = hotel.id == selectedHotel?.id
            SelectHotelCard(hotel, language, recommended = hotel.id == primaryId, selected = isSelected) {
                IumrahHaptics.selection(hapticView)
                journey.selectHotel(hotel)
            }
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(IumrahMotion.rootFade) + expandVertically(),
                exit = fadeOut(IumrahMotion.fastFade) + shrinkVertically(),
            ) {
                RoomCategoryPanel(
                    language = language,
                    categories = categories,
                    selected = selectedCategory,
                    loading = roomsLoading,
                    onSelect = { option ->
                        IumrahHaptics.selection(hapticView)
                        journey.selectRoomCategory(option, forMadinah = city == "Madinah")
                    },
                )
            }
        }
        if (!loading && hotels.isEmpty()) item { Text(L10n.text("hotels_load_error", language), color = MaterialTheme.colorScheme.onBackground.copy(alpha=.55f)) }
    }
}

@Composable
private fun RoomCategoryPanel(
    language: AppLanguage,
    categories: List<IumrahRoomCategoryOption>,
    selected: IumrahRoomCategoryOption?,
    loading: Boolean,
    onSelect: (IumrahRoomCategoryOption) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Room", style = MaterialTheme.typography.titleMedium)
        if (loading) {
            Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) }
        } else if (categories.isEmpty()) {
            Text("Room categories are temporarily unavailable.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
        } else categories.forEach { category ->
            val active = selected?.id == category.id
            IumrahPressable(
                onClick = { onSelect(category) },
                modifier = Modifier.fillMaxWidth().then(if (active) Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(22.dp)) else Modifier),
                cornerRadius = 22.dp,
                background = MaterialTheme.colorScheme.surface,
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(L10n.text(category.category.titleKey, language).takeUnless { it == category.category.titleKey } ?: category.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(category.bedConfiguration, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .54f), style = MaterialTheme.typography.bodyMedium)
                    }
                    IumrahPill("${category.maxGuests} guests")
                    Spacer(Modifier.width(8.dp))
                    if (active) Icon(Icons.Rounded.CheckCircle, null)
                }
            }
        }
        if (selected != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Selected", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.ArrowForward, null)
            }
        }
    }
}

@Composable private fun BackButton(onBack: () -> Unit) {
    IumrahPressable(onClick = onBack, modifier = Modifier.size(44.dp), cornerRadius = 99.dp, background = MaterialTheme.colorScheme.surfaceVariant) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } }
}

@Composable private fun SelectHotelCard(hotel: HotelSummary, language: AppLanguage, recommended: Boolean, selected: Boolean, onSelect: () -> Unit) {
    IumrahPressable(
        onClick = onSelect,
        cornerRadius = 30.dp,
        modifier = Modifier.fillMaxWidth().then(if (selected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(30.dp)) else Modifier),
        shadowElevation = if (recommended || selected) 8.dp else 3.dp,
    ) {
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
                Text(if (selected) "Choose room" else "Select", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                if (selected) Icon(Icons.Rounded.CheckCircle, null) else Icon(Icons.Rounded.ArrowForward, null)
            }
        }
    }
}
