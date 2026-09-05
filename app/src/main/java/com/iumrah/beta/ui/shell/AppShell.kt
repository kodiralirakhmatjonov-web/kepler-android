package com.iumrah.beta.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iumrah.beta.core.design.IumrahHaptics
import com.iumrah.beta.core.design.IumrahMotion
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.navigation.AppChromeState
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.navigation.AppRoute
import com.iumrah.beta.core.navigation.AppTab
import com.iumrah.beta.core.settings.AppLanguage
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
import com.iumrah.beta.ui.chat.BookingChatScreen
import com.iumrah.beta.ui.notifications.NotificationsScreen
import com.iumrah.beta.ui.components.IumrahRootPageHeader
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
                    AppTab.HOTELS -> HotelsScreen(language, hotelCatalog, chrome)
                    AppTab.BOOKING -> BookingsHomeScreen(language, bookingStore, chrome)
                    AppTab.CARE -> CareHomeScreen(language, bookingStore, chrome)
                    AppTab.ACCOUNT -> AccountRoot(accountStore, language, chrome)
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
    (fadeIn(IumrahMotion.rootFade) + scaleIn(IumrahMotion.content, initialScale = .985f))
        .togetherWith(fadeOut(IumrahMotion.fastFade) + scaleOut(IumrahMotion.content, targetScale = 1.015f))

@Composable
private fun IumrahBottomBar(
    language: AppLanguage,
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        Triple(AppTab.HOME, Icons.Rounded.Home, L10n.text("tab_home", language)),
        Triple(AppTab.HOTELS, Icons.Rounded.Hotel, L10n.text("tab_hotels", language)),
        Triple(AppTab.BOOKING, Icons.Rounded.Luggage, L10n.text("tab_booking", language)),
        Triple(AppTab.CARE, Icons.Rounded.Favorite, L10n.text("tab_care", language)),
        Triple(AppTab.ACCOUNT, Icons.Rounded.AccountCircle, L10n.text("profile_placeholder", language)),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .97f))
            .navigationBarsPadding()
            .height(74.dp)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (tab, icon, label) ->
            BottomTabItem(tab == selected, icon, label) { onSelect(tab) }
        }
    }
}

@Composable
private fun RowScope.BottomTabItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .91f else if (selected) 1f else .97f,
        animationSpec = IumrahMotion.tab,
        label = "tab-scale",
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .height(58.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(21.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) .96f else .50f),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) .90f else .48f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AccountRoot(accountStore: IumrahAccountStore, language: AppLanguage, chrome: AppChromeStore) {
    val state by accountStore.state.collectAsState()
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 20.dp, end = 20.dp, top = 54.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IumrahRootPageHeader(L10n.text("profile_placeholder", language), chrome)
        val profile = state.account
        if (profile == null) {
            Text(
                L10n.text("profile_subtitle_empty", language),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .58f),
            )
        } else {
            Text(
                listOf(profile.firstName, profile.lastName).filter { it.isNotBlank() }.joinToString(" "),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(profile.iumrahID, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f))
        }
        com.iumrah.beta.ui.components.IumrahSecondaryButton("Notifications") { chrome.openNotifications() }
    }
}
