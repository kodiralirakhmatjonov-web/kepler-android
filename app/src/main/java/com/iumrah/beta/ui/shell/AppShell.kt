package com.iumrah.beta.ui.shell
import com.iumrah.beta.ui.cupertino.Icon
import kotlin.math.roundToInt
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectHorizontalDragGestures

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iumrah.beta.core.design.IumrahHaptics
import com.iumrah.beta.core.design.IumrahMotion
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.navigation.AppChromeState
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.navigation.AppRoute
import com.iumrah.beta.core.navigation.AppTab
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.core.settings.AppSettingsStore
import com.iumrah.beta.data.account.IumrahAccountStore
import com.iumrah.beta.data.account.IumrahAccountService
import com.iumrah.beta.data.booking.BookingStore
import com.iumrah.beta.data.chat.ChatService
import com.iumrah.beta.data.notification.ClientNotificationStore
import com.iumrah.beta.data.flight.AirportSearchService
import com.iumrah.beta.data.flight.IgnavFlightInventoryProvider
import com.iumrah.beta.data.hotel.HotelCatalogService
import com.iumrah.beta.data.hotel.RemotePackageEngineClient
import com.iumrah.beta.domain.journey.JourneyStore
import com.iumrah.beta.domain.pricing.PackageGenerator
import com.iumrah.beta.ui.flights.FlightSearchScreen
import com.iumrah.beta.ui.packageflow.FinalPackageScreen
import com.iumrah.beta.ui.booking.BookingCheckoutScreen
import com.iumrah.beta.ui.booking.BookingDetailScreen
import com.iumrah.beta.ui.booking.BookingHotelChangeScreen
import com.iumrah.beta.ui.booking.BookingsHomeScreen
import com.iumrah.beta.ui.booking.PilgrimCheckoutScreen
import com.iumrah.beta.ui.care.CareHomeScreen
import com.iumrah.beta.ui.account.AccountRootScreen
import com.iumrah.beta.ui.account.AccountTravelersScreen
import com.iumrah.beta.ui.account.AccountPolicyScreen
import com.iumrah.beta.ui.account.AccountSecurityScreen
import com.iumrah.beta.ui.account.AccountAppearanceScreen
import com.iumrah.beta.ui.account.AccountLanguageScreen
import com.iumrah.beta.ui.account.AccountSignalsScreen
import com.iumrah.beta.ui.account.AccountProfileEditorScreen
import com.iumrah.beta.ui.account.AccountKycScreen
import com.iumrah.beta.ui.cupertino.CupertinoIcon
import com.iumrah.beta.ui.cupertino.CupertinoSymbol
import com.iumrah.beta.ui.chat.BookingChatScreen
import com.iumrah.beta.ui.notifications.NotificationsScreen
import com.iumrah.beta.ui.home.HomeScreen
import com.iumrah.beta.ui.hotels.HotelDetailScreen
import com.iumrah.beta.ui.hotels.HotelsScreen
import com.iumrah.beta.ui.trip.HotelSelectionScreen
import com.iumrah.beta.ui.trip.TripBuilderScreen

@Composable
fun AppShell(
    language: AppLanguage,
    chrome: AppChromeStore,
    chromeState: AppChromeState,
    accountStore: IumrahAccountStore,
    hotelCatalog: HotelCatalogService,
    packageEngine: RemotePackageEngineClient,
    journey: JourneyStore,
    airports: AirportSearchService,
    flightInventory: IgnavFlightInventoryProvider,
    packageGenerator: PackageGenerator,
    bookingStore: BookingStore,
    accountService: IumrahAccountService,
    chatService: ChatService,
    notifications: ClientNotificationStore,
    settingsStore: AppSettingsStore,
) {
    val hapticView = LocalView.current
    BackHandler(enabled = chromeState.isSidebarOpen || chromeState.route != AppRoute.Root) {
        if (chromeState.isSidebarOpen) chrome.closeSidebar() else chrome.back()
    }

    SidebarDrawerHost(open = chromeState.isSidebarOpen, language = language, chrome = chrome) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = chromeState.route to chromeState.currentTab,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { rootTransform() },
            label = "iumrah-root-navigation",
        ) { (route, tab) ->
            when (route) {
                AppRoute.Root -> when (tab) {
                    AppTab.HOME -> HomeScreen(language, chrome)
                    AppTab.HOTELS -> HotelsScreen(language, hotelCatalog, journey, airports, chrome)
                    AppTab.BOOKING -> BookingsHomeScreen(language, bookingStore, chrome)
                    AppTab.CARE -> CareHomeScreen(language, bookingStore, chatService, chrome)
                    AppTab.ACCOUNT -> AccountRootScreen(language, accountStore, bookingStore, settingsStore, notifications, chrome)
                }

                AppRoute.TripBuilder -> TripBuilderScreen(
                    language = language,
                    journey = journey,
                    airports = airports,
                    chrome = chrome,
                )

                AppRoute.HotelSelection -> HotelSelectionScreen(
                    language = language,
                    journey = journey,
                    catalog = hotelCatalog,
                    packageEngine = packageEngine,
                    chrome = chrome,
                )

                is AppRoute.HotelDetail -> HotelDetailScreen(
                    hotelId = route.hotelId,
                    language = language,
                    catalog = hotelCatalog,
                    packageEngine = packageEngine,
                    onBack = chrome::back,
                )

                AppRoute.Flights -> FlightSearchScreen(
                    language = language,
                    journey = journey,
                    provider = flightInventory,
                    generator = packageGenerator,
                    chrome = chrome,
                )

                AppRoute.FinalPackage -> FinalPackageScreen(language, journey, chrome)
                AppRoute.BookingCheckout -> BookingCheckoutScreen(language, journey, bookingStore, accountStore, chrome)
                is AppRoute.BookingDetail -> BookingDetailScreen(route.bookingID, language, bookingStore, chrome)
                is AppRoute.BookingHotelChange -> BookingHotelChangeScreen(route.bookingID, route.role, language, bookingStore, hotelCatalog, packageEngine, chrome)
                is AppRoute.PilgrimCheckout -> PilgrimCheckoutScreen(route.bookingID, language, bookingStore, accountStore, accountService, chrome)
                is AppRoute.BookingChat -> BookingChatScreen(route.bookingID, language, bookingStore, chatService, chrome)
                AppRoute.Notifications -> NotificationsScreen(language, notifications, accountStore, bookingStore, chrome)
                AppRoute.AccountTravelers -> AccountTravelersScreen(language, accountStore, bookingStore, accountService, chrome)
                is AppRoute.AccountPolicy -> AccountPolicyScreen(route.kind, language, chrome)
                AppRoute.AccountSecurity -> AccountSecurityScreen(language, accountStore, chrome)
                AppRoute.AccountAppearance -> AccountAppearanceScreen(language, settingsStore, chrome)
                AppRoute.AccountLanguage -> AccountLanguageScreen(language, settingsStore, chrome)
                AppRoute.AccountSignals -> AccountSignalsScreen(language, notifications, accountStore, chrome)
                AppRoute.AccountProfileEditor -> AccountProfileEditorScreen(language, accountStore, chrome)
                is AppRoute.AccountKyc -> AccountKycScreen(route.bookingID, language, bookingStore, chrome)
            }
        }

        if (!chromeState.isImmersive && chromeState.route == AppRoute.Root) {
            IumrahBottomBar(
                language = language,
                selected = chromeState.currentTab,
                onSelect = {
                    if (it != chromeState.currentTab) IumrahHaptics.selection(hapticView)
                    chrome.navigate(it)
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
    }
}

private fun rootTransform(): ContentTransform =
    fadeIn(IumrahMotion.rootFade).togetherWith(fadeOut(IumrahMotion.fastFade))

@Composable
private fun IumrahBottomBar(
    language: AppLanguage,
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        Triple(AppTab.HOME, CupertinoSymbol.Home, L10n.text("tab_home", language)),
        Triple(AppTab.HOTELS, CupertinoSymbol.Hotel, L10n.text("tab_hotels", language)),
        Triple(AppTab.BOOKING, CupertinoSymbol.Suitcase, L10n.text("tab_booking", language)),
        Triple(AppTab.CARE, CupertinoSymbol.HeartFill, L10n.text("tab_care", language)),
        Triple(AppTab.ACCOUNT, CupertinoSymbol.PersonCircle, "Account"),
    )

    val dark = MaterialTheme.colorScheme.background.red +
        MaterialTheme.colorScheme.background.green +
        MaterialTheme.colorScheme.background.blue < 1.5f
    val glass = if (dark) Color(0xFF2C2C2E).copy(alpha = .90f) else Color.White.copy(alpha = .92f)
    val border = if (dark) Color.White.copy(alpha = .12f) else Color.Black.copy(alpha = .10f)
    val selectedTint = if (dark) Color(0xFF40C8E0) else Color(0xFF30B0C7)
    val unselectedTint = if (dark) Color.White.copy(alpha = .62f) else Color.Black.copy(alpha = .54f)
    val selectionFill = selectedTint.copy(alpha = if (dark) .17f else .12f)
    val barShape = RoundedCornerShape(30.dp)
    val selectedIndex = items.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    var dragPx by remember(selected) { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var dragging by remember { androidx.compose.runtime.mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = barShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = .12f),
                    spotColor = Color.Black.copy(alpha = .12f),
                )
                .clip(barShape)
                .background(glass)
                .border(.7.dp, border, barShape)
                .padding(horizontal = 5.dp, vertical = 5.dp),
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val slotWidth = maxWidth / items.size
            val slotPx = with(density) { slotWidth.toPx() }
            val minDrag = -selectedIndex * slotPx
            val maxDrag = (items.lastIndex - selectedIndex) * slotPx
            val liveIndex = if (slotPx > 0f) {
                (selectedIndex + dragPx / slotPx).roundToInt().coerceIn(0, items.lastIndex)
            } else selectedIndex
            val restingOffset by animateFloatAsState(
                targetValue = selectedIndex * slotPx,
                animationSpec = IumrahMotion.tab,
                label = "tab-indicator",
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(selectedIndex, items.size) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragging = true
                                dragPx = 0f
                            },
                            onHorizontalDrag = { _, amount ->
                                dragPx = (dragPx + amount).coerceIn(minDrag, maxDrag)
                            },
                            onDragCancel = {
                                dragging = false
                                dragPx = 0f
                            },
                            onDragEnd = {
                                val target = if (slotPx > 0f) {
                                    (selectedIndex + dragPx / slotPx).roundToInt().coerceIn(0, items.lastIndex)
                                } else selectedIndex
                                dragging = false
                                dragPx = 0f
                                if (target != selectedIndex) onSelect(items[target].first)
                            },
                        )
                    },
            ) {
                val indicatorOffsetPx = if (dragging) selectedIndex * slotPx + dragPx else restingOffset
                Box(
                    Modifier
                        .offset { androidx.compose.ui.unit.IntOffset(indicatorOffsetPx.roundToInt(), 0) }
                        .width(slotWidth)
                        .height(52.dp)
                        .padding(horizontal = 1.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(selectionFill),
                )

                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    items.forEachIndexed { index, (tab, icon, label) ->
                        BottomTabItem(
                            selected = index == if (dragging) liveIndex else selectedIndex,
                            icon = icon,
                            label = label,
                            selectedTint = selectedTint,
                            unselectedTint = unselectedTint,
                        ) { onSelect(tab) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.BottomTabItem(
    selected: Boolean,
    icon: CupertinoSymbol,
    label: String,
    selectedTint: Color,
    unselectedTint: Color,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .92f else 1f,
        animationSpec = IumrahMotion.tab,
        label = "tab-scale",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .padding(horizontal = 1.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CupertinoIcon(
            symbol = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = if (selected) selectedTint else unselectedTint,
        )
        Text(
            text = label,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium,
            color = if (selected) selectedTint else unselectedTint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
