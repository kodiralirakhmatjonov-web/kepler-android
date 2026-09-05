package com.iumrah.beta.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.iumrah.beta.core.design.IumrahHaptics
import com.iumrah.beta.core.design.iosSpring
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.media.AndroidImageCodec
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.booking.BookingStore
import com.iumrah.beta.data.chat.ChatService
import com.iumrah.beta.data.chat.IumrahPublicProfile
import com.iumrah.beta.models.booking.ChatMessage
import com.iumrah.beta.ui.components.IumrahPressable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BookingChatScreen(bookingID: String, language: AppLanguage, bookingStore: BookingStore, chatService: ChatService, chrome: AppChromeStore) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val session = bookingStore.booking(bookingID)
    var messages by remember(bookingID) { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var care by remember { mutableStateOf<IumrahPublicProfile?>(null) }
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val launchScale by animateFloatAsState(1f, iosSpring(.42f, .84f), label = "chat-launch")
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null || session == null || busy) return@rememberLauncherForActivityResult
        busy = true; error = null
        scope.launch {
            runCatching {
                val data = withContext(Dispatchers.IO) { AndroidImageCodec.jpeg(context.contentResolver, uri, 1600, 84) }
                chatService.sendPhoto(data, bookingID, bookingStore.headersFor(session))
            }.onSuccess { message -> messages = (messages + message).distinctBy { it.id }.sortedBy { it.createdAt }; busy = false; IumrahHaptics.success(view) }
                .onFailure { busy = false; error = it.message }
        }
    }

    LaunchedEffect(bookingID) {
        care = runCatching { chatService.loadCareProfile() }.getOrNull()
        while (true) {
            if (session != null) {
                runCatching { chatService.loadChat(bookingID, bookingStore.headersFor(session)) }
                    .onSuccess { fresh -> messages = fresh; runCatching { chatService.markRead(bookingID, bookingStore.headersFor(session)) } }
                    .onFailure { error = it.message }
            }
            delay(6_000)
        }
    }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).graphicsLayer { scaleX = launchScale; scaleY = launchScale }) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IumrahPressable(onClick = chrome::back, modifier = Modifier.size(44.dp), cornerRadius = 99.dp, background = MaterialTheme.colorScheme.surfaceVariant) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ArrowBack, "Back") } }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(care?.displayName?.takeIf { it.isNotBlank() } ?: L10n.text("chat_staff", language), style = MaterialTheme.typography.titleMedium)
                Text(care?.roleTitle?.takeIf { it.isNotBlank() } ?: L10n.text("chat_online", language), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.50f))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha=.06f))
        if (messages.isEmpty() && error == null) {
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(L10n.text("chat_empty_title", language), style = MaterialTheme.typography.headlineSmall)
                Text(L10n.text("chat_empty_body", language), color = MaterialTheme.colorScheme.onBackground.copy(alpha=.52f), modifier = Modifier.padding(24.dp))
            }
        } else {
            LazyColumn(Modifier.weight(1f), state = listState, contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages, key = { it.id }) { message -> ChatBubble(message, language) }
            }
        }
        AnimatedVisibility(error != null) { Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp), verticalAlignment = Alignment.Bottom) {
            IumrahPressable(onClick = { photoLauncher.launch("image/*") }, modifier = Modifier.size(46.dp), cornerRadius = 99.dp, background = MaterialTheme.colorScheme.surfaceVariant, enabled = !busy) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.AttachFile, null) } }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(3000) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(L10n.text("chat_placeholder", language)) },
                shape = RoundedCornerShape(25.dp),
                maxLines = 5,
            )
            Spacer(Modifier.width(8.dp))
            IumrahPressable(onClick = {
                val body = text.trim(); val current = session
                if (body.isBlank() || current == null || busy) return@IumrahPressable
                busy = true; error = null; text = ""; IumrahHaptics.soft(view)
                scope.launch {
                    runCatching { chatService.send(body, bookingID, bookingStore.headersFor(current)) }
                        .onSuccess { message -> messages = (messages + message).distinctBy { it.id }.sortedBy { it.createdAt }; busy = false }
                        .onFailure { cause -> text = body; busy = false; error = cause.message }
                }
            }, modifier = Modifier.size(46.dp), cornerRadius = 99.dp, background = MaterialTheme.colorScheme.primary, enabled = text.trim().isNotEmpty() && !busy) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Send, null, tint = MaterialTheme.colorScheme.onPrimary) }
            }
        }
    }
}

@Composable private fun ChatBubble(message: ChatMessage, language: AppLanguage) {
    val own = message.senderType.equals("client", true) || message.senderType.equals("pilgrim", true)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (own) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 300.dp).background(if (own) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(23.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = if (own) Alignment.End else Alignment.Start,
        ) {
            if (message.body.isNotBlank()) Text(message.body, color = if (own) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            if (message.attachmentID != null || message.attachmentURL != null) Text("Photo", color = (if (own) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface).copy(alpha=.65f), style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(message.createdAt), color = (if (own) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface).copy(alpha=.50f), style = MaterialTheme.typography.labelSmall)
                if (own) Text(if (message.readByStaff == true) "  ✓✓" else "  ✓", color = MaterialTheme.colorScheme.onPrimary.copy(alpha=.55f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun formatTime(raw: String): String = runCatching {
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(raw))
}.getOrDefault(raw.takeLast(5))
