package com.iumrah.beta.data.chat

import com.iumrah.beta.core.network.APIClient
import com.iumrah.beta.models.booking.ChatListResponse
import com.iumrah.beta.models.booking.ChatMessage
import com.iumrah.beta.models.booking.ChatMessagePostResponse
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class IumrahPublicProfile(
    val id: String,
    val firstName: String = "",
    val lastName: String = "",
    val displayName: String = "",
    val roleKind: String = "",
    val roleTitle: String = "",
    val phoneUZ: String = "",
    val phoneSA: String = "",
    val telegram: String = "",
    val whatsapp: String = "",
    val instagram: String = "",
    val bio: String = "",
    val publicSlug: String = "",
    val publicVisible: Boolean = false,
    val active: Boolean = true,
    val isOwner: Boolean = false,
    val photoURL: String? = null,
)

@Serializable private data class ChatMessageSendRequest(val body: String, val clientMessageID: String)
@Serializable private class EmptyPayload
@Serializable private data class BasicOKResponse(val ok: Boolean)
@Serializable private data class IumrahPublicProfilesResponse(val ok: Boolean, val members: List<IumrahPublicProfile>)

class ChatService(private val api: APIClient) {
    suspend fun loadChat(bookingID: String, headers: Map<String, String>): List<ChatMessage> =
        api.get<ChatListResponse>("/api/catalog/hotels/client/chats/$bookingID/messages", headers = headers).messages
            .sortedBy { it.createdAt }

    suspend fun send(message: String, bookingID: String, headers: Map<String, String>): ChatMessage =
        api.post<ChatMessagePostResponse, ChatMessageSendRequest>(
            "/api/catalog/hotels/client/chats/$bookingID/messages",
            ChatMessageSendRequest(message.trim(), UUID.randomUUID().toString()),
            headers,
        ).message

    suspend fun sendPhoto(data: ByteArray, bookingID: String, headers: Map<String, String>): ChatMessage =
        api.upload<ChatMessagePostResponse>(
            "/api/catalog/hotels/client/chats/$bookingID/attachments",
            data,
            "image/jpeg",
            headers,
            75,
        ).message

    suspend fun loadAttachment(path: String, headers: Map<String, String>): ByteArray = api.fetchData(path, headers, 45)

    suspend fun markRead(bookingID: String, headers: Map<String, String>) {
        api.post<BasicOKResponse, EmptyPayload>(
            "/api/catalog/hotels/client/chats/$bookingID/read",
            EmptyPayload(),
            headers,
        )
    }

    suspend fun loadCareProfile(): IumrahPublicProfile? {
        val value = api.get<IumrahPublicProfilesResponse>("/api/catalog/hotels/team")
        return value.members.firstOrNull { it.isOwner } ?: value.members.firstOrNull()
    }
}
