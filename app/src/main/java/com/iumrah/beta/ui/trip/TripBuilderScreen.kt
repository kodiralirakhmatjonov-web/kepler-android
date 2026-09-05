package com.iumrah.beta.ui.trip

import android.app.DatePickerDialog
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iumrah.beta.core.design.IumrahGalaxyMetrics
import com.iumrah.beta.core.design.IumrahMotion
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.flight.AirportSearchService
import com.iumrah.beta.domain.journey.JourneyStore
import com.iumrah.beta.domain.trip.DateFlexibility
import com.iumrah.beta.domain.trip.JourneyScope
import com.iumrah.beta.domain.trip.PackageTier
import com.iumrah.beta.domain.trip.SaudiArrivalAirport
import com.iumrah.beta.models.flight.Airport
import com.iumrah.beta.ui.components.IumrahBackButton
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.components.IumrahPrimaryButton
import com.iumrah.beta.ui.components.IumrahSectionHeader
import java.time.LocalDate
import kotlinx.coroutines.delay

/**
 * Samsung One UI-inspired trip setup.
 *
 * Business state, routing and pricing contracts are intentionally unchanged;
 * this file only replaces the visual hierarchy and interaction layer.
 */
@Composable
fun TripBuilderScreen(
    language: AppLanguage,
    journey: JourneyStore,
    airports: AirportSearchService,
    chrome: AppChromeStore,
) {
    val initial = journey.state.value.trip
    var draft by remember { mutableStateOf(initial) }
    var originQuery by remember { mutableStateOf(initial.originAirport?.compactTitle ?: initial.originCode) }
    var suggestions by remember { mutableStateOf<List<Airport>>(emptyList()) }

    LaunchedEffect(originQuery) {
        delay(300)
        suggestions = if (originQuery.length >= 2 && originQuery != draft.originAirport?.compactTitle) {
            runCatching { airports.search(originQuery) }.getOrElse { emptyList() }
        } else {
            emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = IumrahGalaxyMetrics.ScreenHorizontal,
                end = IumrahGalaxyMetrics.ScreenHorizontal,
                bottom = 128.dp,
            ),
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = IumrahGalaxyMetrics.ScreenTop, bottom = 18.dp),
                ) {
                    IumrahBackButton(onClick = chrome::back)
                }
            }

            item {
                TripIntro(language)
                Spacer(Modifier.height(34.dp))
            }

            item {
                TripSection(title = L10n.text("trip_origin_title", language)) {
                    OriginField(
                        query = originQuery,
                        onQueryChange = {
                            originQuery = it
                            if (draft.originAirport != null) {
                                draft = draft.copy(originAirport = null, origin = it.take(3).uppercase())
                            }
                        },
                    )
                }
            }

            if (suggestions.isNotEmpty()) {
                items(suggestions, key = { it.iata }) { airport ->
                    AirportSuggestion(
                        airport = airport,
                        onClick = {
                            draft = draft.copy(origin = airport.iata, originAirport = airport)
                            originQuery = airport.compactTitle
                            suggestions = emptyList()
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                item { Spacer(Modifier.height(20.dp)) }
            } else {
                item { Spacer(Modifier.height(IumrahGalaxyMetrics.SectionGap)) }
            }

            item {
                TripSection(title = L10n.text("trip_arrival_title", language)) {
                    SelectionRow(
                        options = listOf(
                            SaudiArrivalAirport.JEDDAH to L10n.text("airport_jeddah_full", language),
                            SaudiArrivalAirport.MADINAH to L10n.text("airport_madinah", language),
                        ),
                        selected = draft.arrivalAirport,
                        onSelect = { draft = draft.copy(arrivalAirport = it) },
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = L10n.text(
                            if (draft.arrivalAirport == SaudiArrivalAirport.MADINAH) {
                                "trip_arrival_madinah_hint"
                            } else {
                                "trip_arrival_jeddah_hint"
                            },
                            language,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .54f),
                    )
                }
                Spacer(Modifier.height(IumrahGalaxyMetrics.SectionGap))
            }

            item {
                TripSection(title = L10n.text("trip_dates_title", language)) {
                    DateRow(
                        language = language,
                        departure = draft.departureDate,
                        returnDate = draft.returnDate,
                        onDeparture = {
                            draft = draft.copy(
                                departureDate = it,
                                returnDate = maxOf(draft.returnDate, it.plusDays(1)),
                            )
                        },
                        onReturn = { if (it.isAfter(draft.departureDate)) draft = draft.copy(returnDate = it) },
                    )
                    Spacer(Modifier.height(10.dp))
                    SelectionRow(
                        options = listOf(
                            DateFlexibility.EXACT to L10n.text("flex_exact", language),
                            DateFlexibility.WEEKEND to L10n.text("flex_weekend", language),
                        ),
                        selected = draft.flexibility,
                        compact = true,
                        onSelect = { draft = draft.withFlexibility(it) },
                    )
                }
                Spacer(Modifier.height(IumrahGalaxyMetrics.SectionGap))
            }

            item {
                TripSection(
                    title = L10n.text("trip_travelers_title", language),
                    subtitle = L10n.text("trip_travelers_body", language),
                ) {
                    TravelersCard(
                        adultsLabel = L10n.text("adults", language),
                        adults = draft.adults,
                        onAdults = { draft = draft.copy(adults = it) },
                        childrenLabel = L10n.text("children", language),
                        children = draft.children,
                        onChildren = { draft = draft.copy(children = it) },
                        infantsLabel = L10n.text("infants", language),
                        infants = draft.infants,
                        onInfants = { draft = draft.copy(infants = it) },
                        roomsLabel = L10n.text("rooms", language),
                        rooms = draft.rooms,
                        onRooms = { draft = draft.copy(rooms = it) },
                    )
                }
                Spacer(Modifier.height(IumrahGalaxyMetrics.SectionGap))
            }

            item {
                TripSection(title = L10n.text("trip_format_title", language)) {
                    SelectionRow(
                        options = listOf(
                            JourneyScope.MAKKAH_ONLY to L10n.text("scope_makkah", language),
                            JourneyScope.MAKKAH_AND_MADINAH to L10n.text("scope_both", language),
                        ),
                        selected = draft.scope,
                        onSelect = { draft = draft.copy(scope = it) },
                    )
                    Spacer(Modifier.height(12.dp))
                    PackageTierGrid(
                        language = language,
                        selected = draft.packageTier,
                        onSelect = { draft = draft.copy(packageTier = it) },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(
                    start = IumrahGalaxyMetrics.ScreenHorizontal,
                    end = IumrahGalaxyMetrics.ScreenHorizontal,
                    top = 12.dp,
                    bottom = 10.dp,
                ),
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .72f))
            Spacer(Modifier.height(12.dp))
            IumrahPrimaryButton(
                title = L10n.text("trip_continue_hotel", language),
                enabled = draft.canContinue,
            ) {
                journey.updateTrip(draft)
                chrome.openHotelSelection()
            }
        }
    }
}

@Composable
private fun TripIntro(language: AppLanguage) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = L10n.text("trip_intro_kicker", language),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .46f),
        )
        Text(
            text = L10n.text("trip_intro_title", language),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = L10n.text("trip_intro_body", language),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .58f),
        )
    }
}

@Composable
private fun TripSection(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        IumrahSectionHeader(title = title, subtitle = subtitle)
        Spacer(Modifier.height(13.dp))
        content()
    }
}

@Composable
private fun OriginField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f),
            )
        },
        shape = RoundedCornerShape(IumrahGalaxyMetrics.RadiusControl),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun AirportSuggestion(
    airport: Airport,
    onClick: () -> Unit,
) {
    IumrahPressable(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = IumrahGalaxyMetrics.RadiusControl,
        background = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = airport.iata,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = airport.compactTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = airport.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .52f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun <T> SelectionRow(
    options: List<Pair<T, String>>,
    selected: T,
    compact: Boolean = false,
    onSelect: (T) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.forEach { (value, label) ->
            ChoiceTile(
                label = label,
                selected = selected == value,
                compact = compact,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(value) },
            )
        }
    }
}

@Composable
private fun ChoiceTile(
    label: String,
    selected: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = IumrahMotion.fastColor,
        label = "trip-choice-background",
    )
    val foreground = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    IumrahPressable(
        onClick = onClick,
        modifier = modifier.heightIn(min = if (compact) 54.dp else 70.dp),
        cornerRadius = IumrahGalaxyMetrics.RadiusTile,
        background = background,
        pressedBackgroundAlpha = .82f,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = if (compact) 12.dp else 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = foreground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = foreground,
                )
            }
        }
    }
}

@Composable
private fun TravelersCard(
    adultsLabel: String,
    adults: Int,
    onAdults: (Int) -> Unit,
    childrenLabel: String,
    children: Int,
    onChildren: (Int) -> Unit,
    infantsLabel: String,
    infants: Int,
    onInfants: (Int) -> Unit,
    roomsLabel: String,
    rooms: Int,
    onRooms: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(IumrahGalaxyMetrics.RadiusCard))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        CounterRow(adultsLabel, adults, 1, 9, onAdults)
        CounterDivider()
        CounterRow(childrenLabel, children, 0, 8, onChildren)
        CounterDivider()
        CounterRow(infantsLabel, infants, 0, 4, onInfants)
        CounterDivider()
        CounterRow(roomsLabel, rooms, 1, 5, onRooms)
    }
}

@Composable
private fun CounterDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 18.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .70f),
    )
}

@Composable
private fun CounterRow(
    title: String,
    value: Int,
    min: Int,
    max: Int,
    onValue: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        CounterButton(Icons.Rounded.Remove, enabled = value > min) { onValue(value - 1) }
        Text(
            text = value.toString(),
            modifier = Modifier.width(46.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        CounterButton(Icons.Rounded.Add, enabled = value < max) { onValue(value + 1) }
    }
}

@Composable
private fun CounterButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IumrahPressable(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(42.dp),
        cornerRadius = 15.dp,
        background = MaterialTheme.colorScheme.surfaceVariant,
        pressedScale = .91f,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) .92f else .22f),
            )
        }
    }
}

@Composable
private fun DateRow(
    language: AppLanguage,
    departure: LocalDate,
    returnDate: LocalDate,
    onDeparture: (LocalDate) -> Unit,
    onReturn: (LocalDate) -> Unit,
) {
    val context = LocalContext.current

    fun open(date: LocalDate, callback: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, day -> callback(LocalDate.of(year, month + 1, day)) },
            date.year,
            date.monthValue - 1,
            date.dayOfMonth,
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
        }.show()
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DateTile(
            label = L10n.text("departure", language),
            date = L10n.date(departure.toString(), language),
            modifier = Modifier.weight(1f),
            onClick = { open(departure, onDeparture) },
        )
        DateTile(
            label = L10n.text("return", language),
            date = L10n.date(returnDate.toString(), language),
            modifier = Modifier.weight(1f),
            onClick = { open(returnDate, onReturn) },
        )
    }
}

@Composable
private fun DateTile(
    label: String,
    date: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    IumrahPressable(
        onClick = onClick,
        modifier = modifier.heightIn(min = 82.dp),
        cornerRadius = IumrahGalaxyMetrics.RadiusTile,
        background = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .50f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = date,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PackageTierGrid(
    language: AppLanguage,
    selected: PackageTier,
    onSelect: (PackageTier) -> Unit,
) {
    val rows = PackageTier.entries.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { rowTiers ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowTiers.forEach { tier ->
                    ChoiceTile(
                        label = L10n.text("tier_${tier.wireValue}", language),
                        selected = selected == tier,
                        compact = true,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(tier) },
                    )
                }
                if (rowTiers.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
