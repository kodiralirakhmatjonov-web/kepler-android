package com.iumrah.beta.ui.booking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.iumrah.beta.core.config.AppConfig
import com.iumrah.beta.core.design.IumrahHaptics
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.booking.BookingStore
import com.iumrah.beta.data.hotel.HotelCatalogService
import com.iumrah.beta.data.hotel.RemotePackageEngineClient
import com.iumrah.beta.models.hotel.HotelSummary
import com.iumrah.beta.models.hotel.IumrahRoomCategoryOption
import com.iumrah.beta.ui.components.IumrahPill
import com.iumrah.beta.ui.components.IumrahPressable
import kotlinx.coroutines.launch

@Composable
fun BookingHotelChangeScreen(
    bookingID: String,
    role: String,
    language: AppLanguage,
    bookingStore: BookingStore,
    catalog: HotelCatalogService,
    packageEngine: RemotePackageEngineClient,
    chrome: AppChromeStore,
) {
    val city = if (role.equals("madinah", true)) "Madinah" else "Makkah"
    val scope = rememberCoroutineScope()
    val haptic = LocalView.current
    var hotels by remember { mutableStateOf<List<HotelSummary>>(emptyList()) }
    var selectedHotel by remember { mutableStateOf<HotelSummary?>(null) }
    var categories by remember { mutableStateOf<List<IumrahRoomCategoryOption>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(city) { hotels = runCatching { catalog.listHotels(city) }.getOrElse { error = it.message; emptyList() } }
    LaunchedEffect(selectedHotel?.id) {
        categories = selectedHotel?.id?.let { id -> runCatching { packageEngine.roomCategories(id) }.getOrDefault(emptyList()) }.orEmpty()
    }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 44.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item {
            IumrahPressable(onClick = chrome::back, modifier = Modifier.size(44.dp), cornerRadius = 99.dp, background = MaterialTheme.colorScheme.surfaceVariant) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ArrowBack, "Back") }
            }
        }
        item {
            Text("Change hotel", style = MaterialTheme.typography.headlineLarge)
            Text("Choose another $city hotel and room category. The same booking ID is preserved.", color = MaterialTheme.colorScheme.onBackground.copy(alpha=.55f))
        }
        items(hotels, key = { it.id }) { hotel ->
            val selected = selectedHotel?.id == hotel.id
            IumrahPressable(
                onClick = { selectedHotel = hotel; IumrahHaptics.selection(haptic) },
                modifier = Modifier.fillMaxWidth().then(if (selected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(28.dp)) else Modifier),
                cornerRadius = 28.dp,
            ) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(Modifier.fillMaxWidth().height(145.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))) {
                        AppConfig.absoluteUrl(hotel.coverImageURL)?.let { AsyncImage(it, hotel.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                            ?: Icon(Icons.Rounded.Hotel, null, Modifier.align(Alignment.Center).size(40.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(hotel.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (selected) Icon(Icons.Rounded.CheckCircle, null)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { hotel.stars?.let { IumrahPill("$it★") }; hotel.rating?.let { IumrahPill(String.format("%.1f", it)) } }
                }
            }
            AnimatedVisibility(selected) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        IumrahPressable(onClick = {
                            if (busy) return@IumrahPressable
                            busy = true; error = null
                            scope.launch {
                                runCatching { bookingStore.updateHotel(bookingID, role, hotel, null, category) }
                                    .onSuccess { busy = false; IumrahHaptics.success(haptic); chrome.back() }
                                    .onFailure { busy = false; error = it.message }
                            }
                        }, modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, background = MaterialTheme.colorScheme.surfaceVariant) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(category.displayName, style = MaterialTheme.typography.titleMedium); Text(category.bedConfiguration, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.52f)) }
                                IumrahPill("${category.maxGuests} guests")
                            }
                        }
                    }
                }
            }
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
    }
}
