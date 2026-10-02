package com.iumrah.beta.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class AppTab { HOME, HOTELS, BOOKING, CARE, ACCOUNT }

sealed interface AppRoute {
    data object Root : AppRoute
    data object TripBuilder : AppRoute
    data object HotelSelection : AppRoute
    data class HotelDetail(val hotelId: String) : AppRoute
    data class ConfiguratorHotelSelection(val role: String) : AppRoute
    data class ConfiguratorHotelDetail(val hotelId: String, val role: String) : AppRoute
    data class FlightPackageDetail(val packageId: String) : AppRoute
    data object Flights : AppRoute
    data object StorefrontFlights : AppRoute
    data object StorefrontSunday : AppRoute
    data object ReturnFlights : AppRoute
    data class FlightDetails(val journeyId: String, val direction: String) : AppRoute
    data object TransferSelection : AppRoute
    data object FinalPackage : AppRoute
    data object BookingCheckout : AppRoute
    data class BookingDetail(val bookingID: String) : AppRoute
    data class BookingHotelChange(val bookingID: String, val role: String) : AppRoute
    data class PilgrimCheckout(val bookingID: String) : AppRoute
    data class BookingChat(val bookingID: String) : AppRoute
    data object Notifications : AppRoute
    data object AccountTravelers : AppRoute
    data class AccountPolicy(val kind: String) : AppRoute
    data object AccountSecurity : AppRoute
    data object AccountAppearance : AppRoute
    data object AccountLanguage : AppRoute
    data object AccountSignals : AppRoute
    data object AccountProfileEditor : AppRoute
    data class AccountKyc(val bookingID: String) : AppRoute
}

data class AppChromeState(
    val currentTab: AppTab = AppTab.HOME,
    val route: AppRoute = AppRoute.Root,
    val backStack: List<AppRoute> = emptyList(),
    val isImmersive: Boolean = false,
    val isSidebarOpen: Boolean = false,
)

class AppChromeStore {
    private val _state = MutableStateFlow(AppChromeState())
    val state: StateFlow<AppChromeState> = _state

    fun navigate(tab: AppTab) {
        _state.update { it.copy(currentTab = tab, route = AppRoute.Root, backStack = emptyList(), isImmersive = false, isSidebarOpen = false) }
    }
    fun startNewTrip() = push(AppRoute.TripBuilder, tab = AppTab.BOOKING)
    fun openHotelSelection() = push(AppRoute.HotelSelection, tab = AppTab.BOOKING)
    fun openHotel(id: String) = push(AppRoute.HotelDetail(id), tab = AppTab.HOTELS)
    fun openConfiguratorHotelSelection(role: String) = push(AppRoute.ConfiguratorHotelSelection(role), tab = AppTab.BOOKING)
    fun openConfiguratorHotelDetail(id: String, role: String) = push(AppRoute.ConfiguratorHotelDetail(id, role), tab = AppTab.BOOKING)
    fun openFlightPackage(id: String) = push(AppRoute.FlightPackageDetail(id), tab = AppTab.HOTELS)
    fun openFlights() = push(AppRoute.Flights, tab = AppTab.BOOKING)
    fun openStorefrontFlights() = push(AppRoute.StorefrontFlights, tab = AppTab.HOTELS)
    fun openSundayClub() = push(AppRoute.StorefrontSunday, tab = AppTab.HOTELS)
    fun openReturnFlights() = push(AppRoute.ReturnFlights, tab = AppTab.BOOKING)
    fun openFlightDetails(id: String, direction: String) = push(AppRoute.FlightDetails(id, direction), tab = AppTab.BOOKING)
    fun openTransferSelection() = push(AppRoute.TransferSelection, tab = AppTab.BOOKING)
    fun openFinalPackage() = push(AppRoute.FinalPackage, tab = AppTab.BOOKING)
    fun openBookingCheckout() = push(AppRoute.BookingCheckout, tab = AppTab.BOOKING)
    fun openBookingDetail(id: String) = push(AppRoute.BookingDetail(id), tab = AppTab.BOOKING)
    fun openBookingHotelChange(id: String, role: String) = push(AppRoute.BookingHotelChange(id, role), tab = AppTab.BOOKING)
    fun openPilgrimCheckout(id: String) = push(AppRoute.PilgrimCheckout(id), tab = AppTab.BOOKING)
    fun openBookingChat(id: String) = push(AppRoute.BookingChat(id), tab = AppTab.CARE)
    fun openNotifications() = push(AppRoute.Notifications)
    fun openAccountTravelers() = push(AppRoute.AccountTravelers, tab = AppTab.ACCOUNT)
    fun openAccountPolicy(kind: String) = push(AppRoute.AccountPolicy(kind), tab = AppTab.ACCOUNT)
    fun openBookingPolicy(kind: String) = push(AppRoute.AccountPolicy(kind), tab = AppTab.BOOKING)
    fun openAccountSecurity() = push(AppRoute.AccountSecurity, tab = AppTab.ACCOUNT)
    fun openAccountAppearance() = push(AppRoute.AccountAppearance, tab = AppTab.ACCOUNT)
    fun openAccountLanguage() = push(AppRoute.AccountLanguage, tab = AppTab.ACCOUNT)
    fun openAccountSignals() = push(AppRoute.AccountSignals, tab = AppTab.ACCOUNT)
    fun openAccountProfileEditor() = push(AppRoute.AccountProfileEditor, tab = AppTab.ACCOUNT)
    fun openAccountKyc(id: String) = push(AppRoute.AccountKyc(id), tab = AppTab.ACCOUNT)
    fun openBookingSecurity(id: String) = push(AppRoute.AccountKyc(id), tab = AppTab.BOOKING)

    private fun push(route: AppRoute, tab: AppTab? = null) {
        _state.update { current ->
            val previous = current.route
            current.copy(
                currentTab = tab ?: current.currentTab,
                route = route,
                backStack = if (previous == route) current.backStack else current.backStack + previous,
                isImmersive = false,
                isSidebarOpen = false,
            )
        }
    }

    fun back(): Boolean {
        val current = _state.value
        val previous = current.backStack.lastOrNull() ?: return false
        _state.value = current.copy(route = previous, backStack = current.backStack.dropLast(1), isImmersive = false, isSidebarOpen = false)
        return true
    }

    fun setImmersive(value: Boolean) { _state.update { it.copy(isImmersive = value) } }
    fun openSidebar() { _state.update { it.copy(isSidebarOpen = true) } }
    fun closeSidebar() { _state.update { it.copy(isSidebarOpen = false) } }
}
