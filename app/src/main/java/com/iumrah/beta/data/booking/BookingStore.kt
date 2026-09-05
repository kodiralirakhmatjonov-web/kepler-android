package com.iumrah.beta.data.booking

import com.iumrah.beta.core.network.APIException
import com.iumrah.beta.core.security.SecureJsonStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.account.IumrahAccountStore
import com.iumrah.beta.domain.booking.BookingDraftBuilder
import com.iumrah.beta.domain.journey.JourneyState
import com.iumrah.beta.domain.pricing.PackageQuote
import com.iumrah.beta.models.booking.*
import com.iumrah.beta.models.hotel.HotelRoom
import com.iumrah.beta.models.hotel.HotelSummary
import com.iumrah.beta.models.hotel.IumrahRoomCategoryOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.builtins.ListSerializer

data class BookingStoreState(
    val sessions: List<StoredBookingSession> = emptyList(),
    val isMutating: Boolean = false,
    val lastError: String? = null,
)

class BookingStore(
    val service: BookingService,
    private val accountStore: IumrahAccountStore,
    private val vault: SecureJsonStore,
) {
    private val serializer = ListSerializer(StoredBookingSession.serializer())
    private val _state = MutableStateFlow(BookingStoreState(load()))
    val state: StateFlow<BookingStoreState> = _state.asStateFlow()

    fun booking(id: String): StoredBookingSession? = _state.value.sessions.firstOrNull { it.id == id }

    fun headersFor(session: StoredBookingSession): Map<String, String> =
        accountStore.authorizationHeaders(session.accessToken).ifEmpty {
            session.accessToken.takeIf { it.isNotBlank() }?.let { mapOf("x-booking-token" to it) }.orEmpty()
        }

    suspend fun create(
        journey: JourneyState,
        quote: PackageQuote,
        language: AppLanguage,
        pilgrimProfile: BookingPilgrimProfile?,
    ): StoredBookingSession {
        _state.update { it.copy(isMutating = true, lastError = null) }
        try {
            val payload = BookingDraftBuilder.make(journey, quote, language, pilgrimProfile)
            val response = service.createBooking(payload)
            val token = response.accessToken?.trim().orEmpty()
            if (token.isBlank()) throw APIException.MissingBookingToken
            val serverProfile = response.booking.pilgrimProfile ?: pilgrimProfile
            var session = StoredBookingSession(
                id = response.booking.id,
                accessToken = token,
                booking = response.booking,
                travelerName = serverProfile?.displayName,
                telegram = serverProfile?.telegram,
                whatsapp = serverProfile?.whatsapp,
                hotelSelection = journey.makkahHotel?.let { BookingHotelSelectionSnapshot.from(it, journey.makkahRoom, journey.makkahRoomCategory) },
                madinahHotelSelection = journey.madinahHotel?.let { BookingHotelSelectionSnapshot.from(it, journey.madinahRoom, journey.madinahRoomCategory) },
            )

            accountStore.bearerToken?.takeIf { it.isNotBlank() }?.let {
                runCatching { accountStore.linkBooking(session.id, session.accessToken) }.getOrNull()?.let { linked ->
                    session = session.copy(
                        pilgrimID = linked.pilgrimID,
                        bookingNumber = linked.bookingNumber,
                        bookingDisplayNumber = linked.bookingDisplayNumber,
                    )
                }
            }

            val operational = syncGeneratorReportWithRetry(
                id = session.id,
                accessToken = session.accessToken,
                trace = payload.booking.generatorTrace,
                snapshot = payload.booking.pricingSnapshot,
            )
            if (operational != null) {
                session = session.copy(
                    pilgrimID = operational.trip.pilgrimID ?: session.pilgrimID,
                    bookingNumber = operational.trip.bookingNumber ?: session.bookingNumber,
                    bookingDisplayNumber = operational.trip.bookingDisplayNumber ?: session.bookingDisplayNumber,
                    operationStatus = operational.trip.status,
                    guide = operational.assignment?.guide,
                )
            }

            if (serverProfile != null && serverProfile.firstName.isNotBlank() && serverProfile.lastName.isNotBlank()) {
                runCatching {
                    service.syncBookingProfile(
                        session.id,
                        session.accessToken,
                        serverProfile,
                        payload.booking.generatorTrace,
                        payload.booking.pricingSnapshot,
                    )
                }.getOrNull()?.let { synced ->
                    session = session.copy(
                        pilgrimID = synced.trip.pilgrimID ?: session.pilgrimID,
                        operationStatus = synced.trip.status,
                        guide = synced.assignment?.guide ?: session.guide,
                    )
                }
            }

            upsert(session)
            _state.update { it.copy(isMutating = false) }
            return session
        } catch (error: Throwable) {
            _state.update { it.copy(isMutating = false, lastError = error.message) }
            throw error
        }
    }

    suspend fun refresh(id: String): StoredBookingSession? {
        val current = booking(id) ?: return null
        val remote = runCatching { service.fetchBooking(id, current.accessToken) }.getOrNull()
        val operational = runCatching { service.fetchOperationalTrip(id, headersFor(current)) }.getOrNull()
        var next = current
        if (remote != null) {
            next = next.copy(
                booking = remote,
                travelerName = remote.pilgrimProfile?.displayName ?: next.travelerName,
                telegram = remote.pilgrimProfile?.telegram ?: next.telegram,
                whatsapp = remote.pilgrimProfile?.whatsapp ?: next.whatsapp,
                hotelSelection = remote.hotelSelection ?: next.hotelSelection,
                madinahHotelSelection = remote.madinahHotelSelection ?: next.madinahHotelSelection,
            )
        }
        if (operational != null) {
            next = next.copy(
                pilgrimID = operational.trip.pilgrimID ?: next.pilgrimID,
                bookingNumber = operational.trip.bookingNumber ?: next.bookingNumber,
                bookingDisplayNumber = operational.trip.bookingDisplayNumber ?: next.bookingDisplayNumber,
                operationStatus = operational.trip.status,
                guide = operational.assignment?.guide ?: next.guide,
            )
        }
        upsert(next)
        return next
    }

    suspend fun updateHotel(
        bookingID: String,
        role: String,
        hotel: HotelSummary,
        room: HotelRoom?,
        category: IumrahRoomCategoryOption?,
    ) {
        val session = booking(bookingID) ?: throw APIException.MissingBookingToken
        val headers = headersFor(session)
        if (headers.isEmpty()) throw APIException.MissingBookingToken
        service.updateHotelSelection(bookingID, headers, role, hotel, room, category)
        val snapshot = BookingHotelSelectionSnapshot.from(hotel, room, category)
        val next = if (role == "madinah") session.copy(madinahHotelSelection = snapshot, pendingChangeConfirmation = true)
        else session.copy(hotelSelection = snapshot, pendingChangeConfirmation = true)
        upsert(next)
    }

    suspend fun updateContacts(bookingID: String, telegram: String, whatsapp: String) {
        val session = booking(bookingID) ?: throw APIException.MissingBookingToken
        val headers = headersFor(session)
        service.updateContacts(bookingID, headers, telegram.trim(), whatsapp.trim())
        upsert(session.copy(telegram = telegram.trim(), whatsapp = whatsapp.trim(), pendingChangeConfirmation = true))
    }

    suspend fun updateZiyarat(bookingID: String, makkah: Boolean, madinah: Boolean) {
        val session = booking(bookingID) ?: throw APIException.MissingBookingToken
        service.updateZiyarat(bookingID, headersFor(session), makkah, madinah)
        upsert(session.copy(ziyaratMakkahOverride = makkah, ziyaratMadinahOverride = madinah, pendingChangeConfirmation = true))
    }

    suspend fun updateESIM(bookingID: String, enabled: Boolean) {
        val session = booking(bookingID) ?: throw APIException.MissingBookingToken
        service.updateESIM(bookingID, headersFor(session), enabled)
        upsert(session.copy(esimOverride = enabled, pendingChangeConfirmation = true))
    }

    fun clearPendingConfirmation(bookingID: String) {
        booking(bookingID)?.let { upsert(it.copy(pendingChangeConfirmation = false)) }
    }

    suspend fun deleteBooking(bookingID: String) {
        val session = booking(bookingID) ?: return purge(bookingID)
        service.deleteBooking(bookingID, headersFor(session))
        purge(bookingID)
    }

    private suspend fun syncGeneratorReportWithRetry(
        id: String,
        accessToken: String,
        trace: BookingGeneratorTrace?,
        snapshot: com.iumrah.beta.domain.pricing.GeneratorPricingSnapshot?,
    ): ClientTripResponse? {
        val waits = listOf(0L, 350L, 800L)
        for (wait in waits) {
            if (wait > 0) delay(wait)
            runCatching { service.syncGeneratorReport(id, accessToken, trace, snapshot) }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun upsert(session: StoredBookingSession) {
        val values = _state.value.sessions.toMutableList()
        val index = values.indexOfFirst { it.id == session.id }
        if (index >= 0) values[index] = session else values.add(0, session)
        _state.update { it.copy(sessions = values) }
        persist(values)
    }

    private fun purge(id: String) {
        val values = _state.value.sessions.filterNot { it.id == id }
        _state.update { it.copy(sessions = values) }
        persist(values)
    }

    private fun persist(values: List<StoredBookingSession>) = vault.write(VAULT_KEY, values, serializer)
    private fun load(): List<StoredBookingSession> = vault.read(VAULT_KEY, serializer).orEmpty()

    companion object { private const val VAULT_KEY = "iumrah-booking-sessions-v2" }
}
