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
    data object Flights : AppRoute
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
    fun openFlights() = push(AppRoute.Flights)

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

