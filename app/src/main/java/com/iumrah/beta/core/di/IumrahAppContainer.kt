package com.iumrah.beta.core.di

import android.content.Context
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.network.APIClient
import com.iumrah.beta.core.security.IumrahAccountDeviceIdentity
import com.iumrah.beta.core.security.SecureJsonStore
import com.iumrah.beta.core.settings.AppSettingsStore
import com.iumrah.beta.data.account.IumrahAccountService
import com.iumrah.beta.data.account.IumrahAccountStore
import com.iumrah.beta.data.booking.BookingService
import com.iumrah.beta.data.booking.BookingStore
import com.iumrah.beta.data.chat.ChatService
import com.iumrah.beta.data.flight.*
import com.iumrah.beta.data.hotel.HotelCatalogService
import com.iumrah.beta.data.hotel.RemotePackageEngineClient
import com.iumrah.beta.data.notification.ClientNotificationStore
import com.iumrah.beta.data.pricing.LocalFXRateService
import com.iumrah.beta.domain.journey.JourneyStore
import com.iumrah.beta.domain.pricing.PackageGenerator

class IumrahAppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val secureStore = SecureJsonStore(appContext)

    val apiClient = APIClient()
    val deviceIdentity = IumrahAccountDeviceIdentity(appContext, secureStore)
    val accountService = IumrahAccountService(apiClient, deviceIdentity)
    val accountStore = IumrahAccountStore(accountService, secureStore)
    val hotelCatalogService = HotelCatalogService(apiClient)
    val packageEngine = RemotePackageEngineClient(apiClient)
    val airportSearchService = AirportSearchService(apiClient)
    val flightFareCalendarService = FlightFareCalendarService(apiClient)
    val flightInventoryProvider = IgnavFlightInventoryProvider(apiClient)
    val journeyStore = JourneyStore()
    val fxRateService = LocalFXRateService()
    val packageGenerator = PackageGenerator(fxRateService)
    val bookingService = BookingService(apiClient)
    val bookingStore = BookingStore(bookingService, accountStore, secureStore)
    val chatService = ChatService(apiClient)
    val notificationStore = ClientNotificationStore(appContext, apiClient)
    val settingsStore = AppSettingsStore(appContext)
    val chromeStore = AppChromeStore()
}
