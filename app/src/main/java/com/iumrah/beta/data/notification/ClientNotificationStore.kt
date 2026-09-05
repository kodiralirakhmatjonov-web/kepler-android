package com.iumrah.beta.data.notification

import android.content.Context
import com.iumrah.beta.core.network.APIClient
import com.iumrah.beta.models.notification.*
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json


data class ClientNotificationState(
    val notifications: List<ClientSystemNotification> = emptyList(),
    val dismissedHomeIDs: Set<String> = emptySet(),
    val isSyncing: Boolean = false,
    val lastError: String? = null,
) {
    val inbox: List<ClientSystemNotification> get() = notifications.sortedWith(
        compareBy<ClientSystemNotification> { it.isRead }
            .thenByDescending { it.sentAt ?: it.createdAt }
            .thenByDescending { it.id },
    )
    val home: List<ClientSystemNotification> get() = inbox.filterNot { dismissedHomeIDs.contains(it.id) }
    val unreadCount: Int get() = notifications.count { !it.isRead }
}

class ClientNotificationStore(context: Context, private val api: APIClient) {
    private val prefs = context.applicationContext.getSharedPreferences("iumrah.notifications.v1", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }
    val installationID: String = prefs.getString(KEY_INSTALLATION, null)?.takeIf { it.length >= 16 }
        ?: UUID.randomUUID().toString().lowercase().also { prefs.edit().putString(KEY_INSTALLATION, it).apply() }

    private val initialNotifications = runCatching {
        prefs.getString(KEY_CACHE, null)?.let { json.decodeFromString(ListSerializer(ClientSystemNotification.serializer()), it) }
    }.getOrNull().orEmpty()
    private val initialDismissed = prefs.getStringSet(KEY_DISMISSED, emptySet()).orEmpty()
    private val _state = MutableStateFlow(ClientNotificationState(initialNotifications, initialDismissed))
    val state: StateFlow<ClientNotificationState> = _state.asStateFlow()

    suspend fun sync(deviceToken: String?, accountToken: String?, hasTrip: Boolean, locale: String) {
        _state.update { it.copy(isSyncing = true, lastError = null) }
        val headers = authorizationHeaders(accountToken)
        runCatching {
            api.post<ClientNotificationDeviceResponse, ClientNotificationDeviceRegistration>(
                "/api/catalog/hotels/client/notifications/devices",
                ClientNotificationDeviceRegistration(
                    installationID = installationID,
                    deviceToken = deviceToken?.trim()?.takeIf(String::isNotEmpty)?.lowercase(),
                    locale = locale,
                    hasTrip = hasTrip,
                ),
                headers,
            )
            refresh(accountToken)
        }.onFailure { error -> _state.update { it.copy(lastError = error.message, isSyncing = false) } }
        if (_state.value.isSyncing) _state.update { it.copy(isSyncing = false) }
    }

    suspend fun refresh(accountToken: String?) {
        runCatching {
            api.get<ClientNotificationFeedResponse>(
                "/api/catalog/hotels/client/notifications/feed",
                query = mapOf("installationID" to installationID),
                headers = authorizationHeaders(accountToken),
            )
        }.onSuccess { response ->
            _state.update { it.copy(notifications = response.notifications, lastError = null, isSyncing = false) }
            persist()
        }.onFailure { error -> _state.update { it.copy(lastError = error.message, isSyncing = false) } }
    }

    suspend fun markOpened(notification: ClientSystemNotification, accountToken: String?) {
        _state.update { current -> current.copy(notifications = current.notifications.map { if (it.id == notification.id) it.copy(isRead = true) else it }) }
        persist()
        runCatching {
            api.post<ClientNotificationReadResponse, ClientNotificationReadRequest>(
                "/api/catalog/hotels/client/notifications/feed/${notification.id}/read",
                ClientNotificationReadRequest(installationID),
                authorizationHeaders(accountToken),
            )
        }
    }

    fun dismissFromHome(id: String) {
        _state.update { it.copy(dismissedHomeIDs = it.dismissedHomeIDs + id) }
        persist()
    }

    fun restoreToHome(id: String) {
        _state.update { it.copy(dismissedHomeIDs = it.dismissedHomeIDs - id) }
        persist()
    }

    private fun authorizationHeaders(token: String?): Map<String, String> = token?.trim()?.takeIf { it.isNotEmpty() }
        ?.let { mapOf("Authorization" to "Bearer $it") }.orEmpty()

    private fun persist() {
        val snapshot = _state.value
        prefs.edit()
            .putString(KEY_CACHE, json.encodeToString(ListSerializer(ClientSystemNotification.serializer()), snapshot.notifications.take(20)))
            .putStringSet(KEY_DISMISSED, snapshot.dismissedHomeIDs)
            .apply()
    }

    companion object {
        private const val KEY_INSTALLATION = "installation"
        private const val KEY_CACHE = "cache"
        private const val KEY_DISMISSED = "dismissed"
    }
}
