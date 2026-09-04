package com.iumrah.beta.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class AppTab { HOME, HOTELS, BOOKING, CARE, ACCOUNT }

sealed interface AppRoute {
    data object Root : AppRoute
    data object TripBuilder : AppRoute
    data class HotelDetail(val hotelId: String) : AppRoute
    data object Flights : AppRoute
}

data class AppChromeState(
    val currentTab: AppTab = AppTab.HOME,
    val route: AppRoute = AppRoute.Root,
    val isImmersive: Boolean = false,
)

class AppChromeStore {
    private val _state = MutableStateFlow(AppChromeState())
    val state: StateFlow<AppChromeState> = _state

    fun navigate(tab: AppTab) {
        _state.update { it.copy(currentTab = tab, route = AppRoute.Root, isImmersive = false) }
    }

    fun startNewTrip() {
        _state.update { it.copy(currentTab = AppTab.BOOKING, route = AppRoute.TripBuilder) }
    }

    fun openHotel(id: String) {
        _state.update { it.copy(currentTab = AppTab.HOTELS, route = AppRoute.HotelDetail(id)) }
    }

    fun openFlights() {
        _state.update { it.copy(route = AppRoute.Flights) }
    }

    fun back(): Boolean {
        val current = _state.value
        if (current.route == AppRoute.Root) return false
        _state.value = current.copy(route = AppRoute.Root, isImmersive = false)
        return true
    }

    fun setImmersive(value: Boolean) {
        _state.update { it.copy(isImmersive = value) }
    }
}
