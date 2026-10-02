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
import androidx.compose.foundation.border
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
    var showGlobe by remember { mutableStateOf(false) }

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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IumrahPressable(onClick = { showGlobe = true }, modifier = Modifier.size(42.dp), cornerRadius = 99.dp, background = generatorRaisedColor()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(CupertinoSymbol.Globe, null, Modifier.size(17.dp)) }
                    }
                    IumrahPressable(onClick = onDismiss, modifier = Modifier.size(42.dp), cornerRadius = 99.dp, background = generatorRaisedColor()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(CupertinoSymbol.Close, null, Modifier.size(15.dp)) }
                    }
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

            if (query.isBlank() || results.isEmpty()) {
                IumrahPressable(
                    onClick = { showGlobe = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    cornerRadius = 22.dp,
                    background = generatorRaisedColor(),
                    shadowElevation = 0.dp,
                ) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(CupertinoSymbol.Globe, null, Modifier.size(17.dp), Color(0xFF007AFF))
                        Spacer(Modifier.width(8.dp))
                        Text(airportMapTitle(language), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
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

    if (showGlobe) {
        ConfiguratorAirportGlobeDialog(
            language = language,
            service = service,
            current = current,
            fallbackCode = fallbackCode,
            onDismiss = { showGlobe = false },
            onSelect = { selected ->
                showGlobe = false
                onSelect(selected)
            },
        )
    }
}

@Composable
private fun ConfiguratorAirportGlobeDialog(
    language: AppLanguage,
    service: AirportSearchService,
    current: Airport?,
    fallbackCode: String,
    onDismiss: () -> Unit,
    onSelect: (Airport) -> Unit,
) {
    val featuredCodes = remember { listOf("TAS", "SKD", "JED", "MED", "IST", "DXB", "DOH", "GYD", "ALA", "NQZ", "DMM", "TIF") }
    var airports by remember { mutableStateOf(listOfNotNull(current)) }
    var selected by remember(current) { mutableStateOf(current) }
    var loading by remember { mutableStateOf(true) }
    var rotation by remember { mutableStateOf(-(current?.lon?.toFloat() ?: 52f)) }

    LaunchedEffect(Unit) {
        val found = linkedMapOf<String, Airport>()
        current?.let { found[it.iata.uppercase()] = it }
        for (code in featuredCodes) {
            runCatching { service.search(code, 4) }.getOrDefault(emptyList())
                .firstOrNull { it.iata.equals(code, true) }
                ?.let { found[it.iata.uppercase()] = it }
        }
        airports = found.values.toList()
        if (selected == null) selected = airports.firstOrNull { it.iata.equals(fallbackCode, true) } ?: airports.firstOrNull()
        loading = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF05070C))) {
            Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IumrahPressable(onClick = onDismiss, modifier = Modifier.size(46.dp), cornerRadius = 23.dp, background = Color.White.copy(alpha = .12f), shadowElevation = 0.dp) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(CupertinoSymbol.Close, null, Modifier.size(16.dp), Color.White) }
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(airportMapTitle(language), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(airportGlobeHintTitle(language), color = Color.White.copy(alpha = .55f), fontSize = 11.sp)
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                rotation = (rotation + dragAmount.x * .32f) % 360f
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AirportComposeGlobe(airports = airports, selected = selected, rotationDegrees = rotation, modifier = Modifier.fillMaxWidth().height(470.dp))
                    if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }

                Text(airportGlobeZoomHint(language), color = Color.White.copy(alpha = .52f), fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))

                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    airports.forEach { airport ->
                        val active = selected?.iata.equals(airport.iata, true)
                        IumrahPressable(
                            onClick = { selected = airport; rotation = -airport.lon.toFloat() },
                            modifier = Modifier.height(42.dp),
                            cornerRadius = 21.dp,
                            background = if (active) Color.White else Color.White.copy(alpha = .10f),
                            shadowElevation = 0.dp,
                        ) {
                            Row(Modifier.padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(airport.iata.uppercase(), color = if (active) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(airport.city, color = if (active) Color.Black.copy(alpha = .62f) else Color.White.copy(alpha = .55f), fontSize = 11.sp)
                            }
                        }
                    }
                }

                val choice = selected
                Column(
                    Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp).clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = .08f)).border(.8.dp, Color.White.copy(alpha = .10f), RoundedCornerShape(28.dp)).padding(17.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (choice != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFF0A84FF).copy(alpha = .22f)), contentAlignment = Alignment.Center) {
                                Icon(CupertinoSymbol.AirplaneTakeoff, null, Modifier.size(19.dp), Color(0xFF4CB3FF))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(choice.compactTitle, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(choice.subtitle, color = Color.White.copy(alpha = .52f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        IumrahPressable(
                            onClick = { onSelect(choice) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            cornerRadius = 19.dp,
                            background = Color.White,
                            shadowElevation = 0.dp,
                        ) {
                            Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(airportMapChoose(language, choice.iata), modifier = Modifier.weight(1f), color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Icon(CupertinoSymbol.ArrowRight, null, Modifier.size(17.dp), Color.Black)
                            }
                        }
                    } else {
                        Text(airportGlobeHintTitle(language), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AirportComposeGlobe(
    airports: List<Airport>,
    selected: Airport?,
    rotationDegrees: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val radius = minOf(size.width, size.height) * .42f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Color(0xFF11243D), radius, center)
        drawCircle(Color(0xFF1F5E93).copy(alpha = .48f), radius * .96f, center)
        drawCircle(Color.White.copy(alpha = .14f), radius, center, style = Stroke(width = 1.2f))

        // Meridian and latitude guides approximate the clean MapKit globe silhouette without a map SDK.
        for (lat in listOf(-60, -30, 0, 30, 60)) {
            val y = center.y - (lat / 90f) * radius * .86f
            val half = radius * cos(lat * PI / 180.0).toFloat()
            drawOval(Color.White.copy(alpha = if (lat == 0) .14f else .08f), topLeft = Offset(center.x - half, y - radius * .06f), size = androidx.compose.ui.geometry.Size(half * 2, radius * .12f), style = Stroke(1f))
        }
        for (lon in listOf(-60, -30, 0, 30, 60)) {
            val x = center.x + (lon / 90f) * radius * .76f
            drawOval(Color.White.copy(alpha = .075f), topLeft = Offset(x - radius * .12f, center.y - radius), size = androidx.compose.ui.geometry.Size(radius * .24f, radius * 2), style = Stroke(1f))
        }

        val centerLat = 25.0 * PI / 180.0
        airports.forEach { airport ->
            val lat = airport.lat * PI / 180.0
            val deltaLon = (airport.lon + rotationDegrees) * PI / 180.0
            val depth = sin(centerLat) * sin(lat) + cos(centerLat) * cos(lat) * cos(deltaLon)
            if (depth > 0) {
                val x = cos(lat) * sin(deltaLon)
                val y = cos(centerLat) * sin(lat) - sin(centerLat) * cos(lat) * cos(deltaLon)
                val point = Offset(center.x + (x * radius).toFloat(), center.y - (y * radius).toFloat())
                val active = selected?.iata.equals(airport.iata, true)
                if (active) drawCircle(Color.White.copy(alpha = .20f), 13f, point)
                drawCircle(if (active) Color.White else Color(0xFF4CB3FF), if (active) 6.5f else 4.5f, point)
            }
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


private fun airportMapTitle(language: AppLanguage) = when (language) {
    AppLanguage.RUSSIAN -> "Выбрать на глобусе"; AppLanguage.ENGLISH -> "Choose on globe"; AppLanguage.UZBEK -> "Globusdan tanlash"; AppLanguage.UZBEK_CYRILLIC -> "Глобусдан танлаш"
}
private fun airportGlobeHintTitle(language: AppLanguage) = when (language) {
    AppLanguage.RUSSIAN -> "Выберите аэропорт на глобусе"; AppLanguage.ENGLISH -> "Choose an airport on the globe"; AppLanguage.UZBEK -> "Globusdan aeroport tanlang"; AppLanguage.UZBEK_CYRILLIC -> "Глобусдан аэропорт танланг"
}
private fun airportGlobeZoomHint(language: AppLanguage) = when (language) {
    AppLanguage.RUSSIAN -> "Вращайте глобус и выберите аэропорт из точек ниже."; AppLanguage.ENGLISH -> "Rotate the globe, then choose an airport from the points below."; AppLanguage.UZBEK -> "Globusni aylantiring va pastdagi aeroportlardan birini tanlang."; AppLanguage.UZBEK_CYRILLIC -> "Глобусни айлантиринг ва пастдаги аэропортлардан бирини танланг."
}
private fun airportMapChoose(language: AppLanguage, code: String) = when (language) {
    AppLanguage.RUSSIAN -> "Выбрать $code"; AppLanguage.ENGLISH -> "Choose $code"; AppLanguage.UZBEK -> "$code ni tanlash"; AppLanguage.UZBEK_CYRILLIC -> "$code ни танлаш"
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
