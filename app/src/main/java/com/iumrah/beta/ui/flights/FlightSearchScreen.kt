package com.iumrah.beta.ui.flights

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.iumrah.beta.core.design.IumrahHaptics
import com.iumrah.beta.core.design.IumrahMotion
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.flight.IgnavFlightInventoryProvider
import com.iumrah.beta.domain.journey.JourneyStore
import com.iumrah.beta.domain.pricing.PackageGenerator
import com.iumrah.beta.models.flight.LiveFlightCandidate
import com.iumrah.beta.models.flight.LiveFlightJourneyCandidate
import com.iumrah.beta.ui.components.IumrahPill
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.components.IumrahPrimaryButton
import com.iumrah.beta.ui.media.LoopingRawVideo
import java.time.ZoneId
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@Composable
fun FlightSearchScreen(language: AppLanguage, journey: JourneyStore, provider: IgnavFlightInventoryProvider, generator: PackageGenerator, chrome: AppChromeStore) {
    val state by journey.state.collectAsState()
    val hapticView = LocalView.current
    val scope = rememberCoroutineScope()
    var generating by remember { mutableStateOf(false) }
    LaunchedEffect(state.trip, state.flightResults.isEmpty()) {
        if (state.flightResults.isEmpty() && !state.isSearchingFlights && state.flightError == null) journey.searchFlights(provider)
    }

    if (state.isSearchingFlights && state.flightResults.isEmpty()) {
        FlightLoading(language, chrome::back)
        return
    }

    val selected = state.selectedJourney
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { BackButton(chrome::back) }
        item {
            Text(L10n.text("flight_out_eyebrow", language), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha=.48f))
            Text(L10n.text("flight_out_title", language), style = MaterialTheme.typography.headlineLarge)
            Text(L10n.text("flight_out_body", language), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha=.58f))
        }
        if (state.flightError != null && state.flightResults.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(L10n.text("flight_search_failed", language), style = MaterialTheme.typography.titleLarge)
                    Text(state.flightError.orEmpty(), color = MaterialTheme.colorScheme.onSurface.copy(alpha=.55f))
                    IumrahPrimaryButton(L10n.text("flight_retry", language)) { journey.clearFlights() }
                }
            }
        }
        items(state.flightResults, key = { it.id }) { option ->
            FlightJourneyCard(option, selected = option.id == state.selectedJourneyId, language = language) {
                IumrahHaptics.selection(hapticView)
                journey.selectJourney(option.id)
            }
        }
        if (selected != null) {
            item { SelectedReturn(selected, language) }
            item {
                IumrahPrimaryButton(if (generating) "Building package…" else L10n.text("flight_view_package", language), enabled = !generating) {
                    generating = true
                    journey.setPackageError(null)
                    scope.launch {
                        runCatching { generator.generate(state) }
                            .onSuccess { quote -> journey.setQuote(quote); generating = false; IumrahHaptics.success(hapticView); chrome.openFinalPackage() }
                            .onFailure { error -> generating = false; journey.setPackageError(error.message ?: "PACKAGE_GENERATION_FAILED") }
                    }
                }
                state.packageError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable private fun FlightLoading(language: AppLanguage, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        BackButton(onBack, Modifier.padding(24.dp).align(Alignment.TopStart))
        Column(Modifier.align(Alignment.Center).padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.size(220.dp).clip(RoundedCornerShape(42.dp)).background(MaterialTheme.colorScheme.surface)) {
                LoopingRawVideo("flight_search", modifier = Modifier.fillMaxSize(), play = true, muted = true) {
                    Icon(Icons.Rounded.Flight, contentDescription = null, modifier = Modifier.align(Alignment.Center).size(72.dp))
                }
            }
            Text(L10n.text("flight_search_hero", language), style = MaterialTheme.typography.headlineMedium)
            Text(L10n.text("flight_search_airlines", language), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha=.56f))
            Text(L10n.text("flight_wait_average", language), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha=.42f))
        }
    }
}

@Composable private fun FlightJourneyCard(option: LiveFlightJourneyCandidate, selected: Boolean, language: AppLanguage, onClick: () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    val borderColor by animateColorAsState(if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, IumrahMotion.selectionColor, label = "flight-border-color")
    val borderWidth by animateDpAsState(if (selected) 1.5.dp else 0.dp, IumrahMotion.selectionDp, label = "flight-border-width")
    IumrahPressable(onClick = onClick, modifier = Modifier.fillMaxWidth().border(borderWidth, borderColor, shape), cornerRadius = 30.dp, shadowElevation = if (selected) 7.dp else 3.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Flight, contentDescription = null)
                Spacer(Modifier.padding(5.dp))
                Text(option.outbound.airline, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null)
            }
            FlightLegRow(option.outbound, language)
            option.inbound?.let { inbound ->
                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha=.07f)))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L10n.text("flight_return_eyebrow", language), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.46f))
                    Spacer(Modifier.weight(1f))
                    Text("${inbound.origin} → ${inbound.destination}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IumrahPill(if (option.outbound.stops == 0) L10n.text("flight_direct", language) else L10n.format("flight_stops", language, option.outbound.stops))
                option.baggage?.checked?.let { IumrahPill("$it checked") }
                if (option.requiresSelfTransfer == true) IumrahPill("Self-transfer")
            }
        }
    }
}

@Composable private fun FlightLegRow(leg: LiveFlightCandidate, language: AppLanguage) {
    val zone = leg.segments?.firstOrNull()?.origin?.timeZoneIdentifier?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
    val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column { Text(formatter.format(leg.departureAt), style = MaterialTheme.typography.titleLarge); Text(leg.origin, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.48f)) }
        Box(Modifier.weight(1f).padding(horizontal = 14.dp).height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha=.18f)))
        Column(horizontalAlignment = Alignment.End) { Text(formatter.format(leg.arrivalAt), style = MaterialTheme.typography.titleLarge); Text(leg.destination, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.48f)) }
    }
}

@Composable private fun SelectedReturn(journey: LiveFlightJourneyCandidate, language: AppLanguage) {
    val inbound = journey.inbound ?: return
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(L10n.text("flight_return_title", language), style = MaterialTheme.typography.titleLarge)
        FlightLegRow(inbound, language)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IumrahPill(journey.currency)
            IumrahPill("Complete journey fare locked as one itinerary")
        }
    }
}

@Composable private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    IumrahPressable(onClick = onBack, modifier = modifier.size(44.dp), cornerRadius = 99.dp, background = MaterialTheme.colorScheme.surfaceVariant) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } }
}
