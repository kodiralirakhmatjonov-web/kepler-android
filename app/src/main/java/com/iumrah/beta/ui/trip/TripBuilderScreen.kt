package com.iumrah.beta.ui.trip

import android.app.DatePickerDialog
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.flight.AirportSearchService
import com.iumrah.beta.domain.journey.JourneyStore
import com.iumrah.beta.domain.trip.*
import com.iumrah.beta.models.flight.Airport
import com.iumrah.beta.ui.components.IumrahPill
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.components.IumrahPrimaryButton
import com.iumrah.beta.ui.components.IumrahSectionHeader
import java.time.LocalDate
import kotlinx.coroutines.delay

@Composable
fun TripBuilderScreen(language: AppLanguage, journey: JourneyStore, airports: AirportSearchService, chrome: AppChromeStore) {
    val initial = journey.state.value.trip
    var draft by remember { mutableStateOf(initial) }
    var originQuery by remember { mutableStateOf(initial.originAirport?.compactTitle ?: initial.originCode) }
    var suggestions by remember { mutableStateOf<List<Airport>>(emptyList()) }

    LaunchedEffect(originQuery) {
        delay(300)
        suggestions = if (originQuery.length >= 2 && originQuery != draft.originAirport?.compactTitle) runCatching { airports.search(originQuery) }.getOrElse { emptyList() } else emptyList()
    }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { BackHeader(chrome::back) }
        item {
            Text(L10n.text("trip_intro_kicker", language), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha=.48f))
            Text(L10n.text("trip_intro_title", language), style = MaterialTheme.typography.headlineLarge)
            Text(L10n.text("trip_intro_body", language), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha=.62f))
        }
        item {
            IumrahSectionHeader(L10n.text("trip_origin_title", language))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = originQuery,
                onValueChange = { originQuery = it; if (draft.originAirport != null) draft = draft.copy(originAirport = null, origin = it.take(3).uppercase()) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
            )
        }
        if (suggestions.isNotEmpty()) {
            items(suggestions, key = { it.iata }) { airport ->
                IumrahPressable(onClick = { draft = draft.copy(origin = airport.iata, originAirport = airport); originQuery = airport.compactTitle; suggestions = emptyList() }, cornerRadius = 22.dp) {
                    Column(Modifier.fillMaxWidth().padding(15.dp)) { Text(airport.compactTitle, style = MaterialTheme.typography.titleMedium); Text(airport.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.55f)) }
                }
            }
        }
        item {
            IumrahSectionHeader(L10n.text("trip_arrival_title", language))
            Spacer(Modifier.height(10.dp))
            SelectionRow(
                listOf(
                    SaudiArrivalAirport.JEDDAH to L10n.text("airport_jeddah_full", language),
                    SaudiArrivalAirport.MADINAH to L10n.text("airport_madinah", language),
                ), draft.arrivalAirport,
            ) { draft = draft.copy(arrivalAirport = it) }
            Text(
                L10n.text(if (draft.arrivalAirport == SaudiArrivalAirport.MADINAH) "trip_arrival_madinah_hint" else "trip_arrival_jeddah_hint", language),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha=.52f),
            )
        }
        item {
            IumrahSectionHeader(L10n.text("trip_dates_title", language))
            Spacer(Modifier.height(10.dp))
            DateRow(draft.departureDate, draft.returnDate, onDeparture = { draft = draft.copy(departureDate = it, returnDate = maxOf(draft.returnDate, it.plusDays(1))) }, onReturn = { if (it.isAfter(draft.departureDate)) draft = draft.copy(returnDate = it) })
            Spacer(Modifier.height(10.dp))
            SelectionRow(listOf(DateFlexibility.EXACT to L10n.text("flex_exact", language), DateFlexibility.WEEKEND to L10n.text("flex_weekend", language)), draft.flexibility) { draft = draft.withFlexibility(it) }
        }
        item {
            IumrahSectionHeader(L10n.text("trip_travelers_title", language), L10n.text("trip_travelers_body", language))
            Spacer(Modifier.height(10.dp))
            CounterRow(L10n.text("adults", language), draft.adults, 1, 9) { draft = draft.copy(adults = it) }
            CounterRow(L10n.text("children", language), draft.children, 0, 8) { draft = draft.copy(children = it) }
            CounterRow(L10n.text("infants", language), draft.infants, 0, 4) { draft = draft.copy(infants = it) }
            CounterRow(L10n.text("rooms", language), draft.rooms, 1, 5) { draft = draft.copy(rooms = it) }
        }
        item {
            IumrahSectionHeader(L10n.text("trip_format_title", language))
            Spacer(Modifier.height(10.dp))
            SelectionRow(listOf(JourneyScope.MAKKAH_ONLY to L10n.text("scope_makkah", language), JourneyScope.MAKKAH_AND_MADINAH to L10n.text("scope_both", language)), draft.scope) { draft = draft.copy(scope = it) }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PackageTier.entries) { tier ->
                    IumrahPressable(onClick = { draft = draft.copy(packageTier = tier) }, cornerRadius = 99.dp, background = if (draft.packageTier == tier) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) {
                        Text(L10n.text("tier_${tier.wireValue}", language), modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp), color = if (draft.packageTier == tier) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        item {
            IumrahPrimaryButton(L10n.text("trip_continue_hotel", language), enabled = draft.canContinue) {
                journey.updateTrip(draft)
                chrome.openHotelSelection()
            }
        }
    }
}

@Composable private fun BackHeader(onBack: () -> Unit) { IumrahPressable(onClick = onBack, modifier = Modifier.size(44.dp), cornerRadius = 99.dp, background = MaterialTheme.colorScheme.surfaceVariant) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } } }

@Composable private fun <T> SelectionRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            IumrahPressable(onClick = { onSelect(value) }, modifier = Modifier.weight(1f), cornerRadius = 22.dp, background = if (selected == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) {
                Text(label, modifier = Modifier.padding(14.dp), color = if (selected == value) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable private fun CounterRow(title: String, value: Int, min: Int, max: Int, onValue: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        SmallCircle(Icons.Rounded.Remove, value > min) { onValue(value - 1) }
        Text(value.toString(), modifier = Modifier.padding(horizontal = 14.dp), style = MaterialTheme.typography.titleMedium)
        SmallCircle(Icons.Rounded.Add, value < max) { onValue(value + 1) }
    }
}

@Composable private fun SmallCircle(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    IumrahPressable(onClick = onClick, enabled = enabled, modifier = Modifier.size(38.dp), cornerRadius = 99.dp, background = MaterialTheme.colorScheme.surfaceVariant) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .25f)) } }
}

@Composable private fun DateRow(departure: LocalDate, returnDate: LocalDate, onDeparture: (LocalDate) -> Unit, onReturn: (LocalDate) -> Unit) {
    val context = LocalContext.current
    fun open(date: LocalDate, callback: (LocalDate) -> Unit) {
        DatePickerDialog(context, { _, y, m, d -> callback(LocalDate.of(y, m + 1, d)) }, date.year, date.monthValue - 1, date.dayOfMonth).apply { datePicker.minDate = System.currentTimeMillis() - 1000 }.show()
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IumrahPressable(onClick = { open(departure, onDeparture) }, modifier = Modifier.weight(1f), cornerRadius = 22.dp, background = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.padding(14.dp)) { Text("Outbound", style = MaterialTheme.typography.bodyMedium); Text(departure.toString(), style = MaterialTheme.typography.titleMedium) } }
        IumrahPressable(onClick = { open(returnDate, onReturn) }, modifier = Modifier.weight(1f), cornerRadius = 22.dp, background = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.padding(14.dp)) { Text("Return", style = MaterialTheme.typography.bodyMedium); Text(returnDate.toString(), style = MaterialTheme.typography.titleMedium) } }
    }
}
