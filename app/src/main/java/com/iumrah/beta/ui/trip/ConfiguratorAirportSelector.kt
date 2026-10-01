@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.iumrah.beta.ui.trip

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.flight.AirportSearchService
import com.iumrah.beta.models.flight.Airport
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.cupertino.CupertinoSymbol
import com.iumrah.beta.ui.cupertino.Icon
import com.iumrah.beta.ui.generator.generatorPageColor
import com.iumrah.beta.ui.generator.generatorRaisedColor
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** iOS AirportSelectorButton + AirportPickerView parity for the Generator. */
@Composable
fun ConfiguratorAirportSelector(
    language: AppLanguage,
    service: AirportSearchService,
    airport: Airport?,
    fallbackCode: String,
    onSelect: (Airport) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPicker by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            locating = true
            scope.launch {
                resolveNearestConfiguratorAirport(context, service)?.let(onSelect)
                locating = false
            }
        }
    }

    fun locate() {
        if (locating) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            locating = true
            scope.launch {
                resolveNearestConfiguratorAirport(context, service)?.let(onSelect)
                locating = false
            }
        } else locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(generatorRaisedColor()).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).clickable { showPicker = true }.padding(vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF007AFF).copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                Icon(CupertinoSymbol.AirplaneTakeoff, null, Modifier.size(17.dp), Color(0xFF007AFF))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(airportTitle(language), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                if (airport != null) {
                    Text(airport.compactTitle, fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(airport.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else {
                    Text(fallbackCode.ifBlank { airportSearchTitle(language) }.uppercase(), fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        IumrahPressable(onClick = ::locate, modifier = Modifier.size(42.dp), cornerRadius = 99.dp, background = Color.Transparent) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (locating) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                else Icon(CupertinoSymbol.Location, airportTitle(language), Modifier.size(15.dp))
            }
        }
    }

    if (showPicker) {
        ConfiguratorAirportPickerSheet(
            language = language,
            service = service,
            current = airport,
            fallbackCode = fallbackCode,
            onDismiss = { showPicker = false },
            onSelect = { selected -> onSelect(selected); showPicker = false },
        )
    }
}

@Composable
private fun ConfiguratorAirportPickerSheet(
    language: AppLanguage,
    service: AirportSearchService,
    current: Airport?,
    fallbackCode: String,
    onDismiss: () -> Unit,
    onSelect: (Airport) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf(current?.iata ?: fallbackCode) }
    var results by remember { mutableStateOf<List<Airport>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val clean = query.trim()
        if (clean.length >= 2) {
            loading = true
            results = runCatching { service.search(clean, 12) }.getOrDefault(emptyList())
            loading = false
        } else results = emptyList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = generatorPageColor(),
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 22.dp, bottom = 34.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(airportSearchTitle(language), fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                    Text(airportSearchHint(language), fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                }
                IumrahPressable(onClick = onDismiss, modifier = Modifier.size(42.dp), cornerRadius = 99.dp, background = generatorRaisedColor()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(CupertinoSymbol.Close, null, Modifier.size(15.dp)) }
                }
            }

            Row(
                Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(generatorRaisedColor()).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(CupertinoSymbol.Location, null, Modifier.size(18.dp), MaterialTheme.colorScheme.onSurface.copy(alpha = .5f))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium),
                    decorationBox = { inner ->
                        if (query.isBlank()) Text(airportSearchPlaceholder(language), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .38f))
                        inner()
                    },
                )
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                results.forEach { airport ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { onSelect(airport) }.padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF007AFF).copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                            Icon(CupertinoSymbol.AirplaneTakeoff, null, Modifier.size(17.dp), Color(0xFF007AFF))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(airport.compactTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(airport.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .52f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (airport.iata.equals(current?.iata ?: fallbackCode, true)) Icon(CupertinoSymbol.Checkmark, null, Modifier.size(17.dp), Color(0xFF34C759))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun resolveNearestConfiguratorAirport(context: Context, service: AirportSearchService): Airport? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val location = withContext(Dispatchers.IO) {
        val known = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
        known ?: withTimeoutOrNull(3500) { requestConfiguratorLocation(manager) }
    } ?: return null
    val queries = withContext(Dispatchers.IO) {
        runCatching {
            @Suppress("DEPRECATION")
            val place = Geocoder(context, Locale.ENGLISH).getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
            listOfNotNull(place?.locality, place?.subAdminArea, place?.adminArea, place?.countryName).map(String::trim).filter(String::isNotEmpty).distinctBy { it.lowercase(Locale.ENGLISH) }
        }.getOrDefault(emptyList())
    }
    val candidates = linkedMapOf<String, Airport>()
    for (query in queries) runCatching { service.search(query, 12) }.getOrDefault(emptyList()).forEach { candidates[it.iata.uppercase()] = it }
    return candidates.values.map { airport ->
        val distance = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, airport.lat, airport.lon, distance)
        airport to distance[0].toDouble()
    }.filter { it.second <= 250_000.0 }.minByOrNull { it.second }?.first
}

@SuppressLint("MissingPermission")
private suspend fun requestConfiguratorLocation(manager: LocationManager): Location? = suspendCancellableCoroutine { continuation ->
    val provider = when {
        runCatching { manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) -> LocationManager.NETWORK_PROVIDER
        runCatching { manager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) -> LocationManager.GPS_PROVIDER
        else -> null
    }
    if (provider == null) { continuation.resume(null); return@suspendCancellableCoroutine }
    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            manager.removeUpdates(this)
            if (continuation.isActive) continuation.resume(location)
        }
        @Deprecated("Deprecated in Android") override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }
    continuation.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }
    runCatching { manager.requestSingleUpdate(provider, listener, Looper.getMainLooper()) }
        .onFailure { if (continuation.isActive) continuation.resume(null) }
}

private fun airportTitle(language: AppLanguage) = when (language) {
    AppLanguage.RUSSIAN -> "Аэропорт вылета"; AppLanguage.ENGLISH -> "Departure airport"; AppLanguage.UZBEK -> "Jo‘nash aeroporti"; AppLanguage.UZBEK_CYRILLIC -> "Жўнаш аэропорти"
}
private fun airportSearchTitle(language: AppLanguage) = when (language) {
    AppLanguage.RUSSIAN -> "Выберите аэропорт"; AppLanguage.ENGLISH -> "Choose an airport"; AppLanguage.UZBEK -> "Aeroportni tanlang"; AppLanguage.UZBEK_CYRILLIC -> "Аэропортни танланг"
}
private fun airportSearchHint(language: AppLanguage) = when (language) {
    AppLanguage.RUSSIAN -> "Найдите город или код аэропорта"; AppLanguage.ENGLISH -> "Search by city or airport code"; AppLanguage.UZBEK -> "Shahar yoki aeroport kodi bo‘yicha qidiring"; AppLanguage.UZBEK_CYRILLIC -> "Шаҳар ёки аэропорт коди бўйича қидиринг"
}
private fun airportSearchPlaceholder(language: AppLanguage) = when (language) {
    AppLanguage.RUSSIAN -> "Например, TAS или Ташкент"; AppLanguage.ENGLISH -> "For example, TAS or Tashkent"; AppLanguage.UZBEK -> "Masalan, TAS yoki Toshkent"; AppLanguage.UZBEK_CYRILLIC -> "Масалан, TAS ёки Тошкент"
}
