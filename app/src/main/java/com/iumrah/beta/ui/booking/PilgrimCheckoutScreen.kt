package com.iumrah.beta.ui.booking

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.iumrah.beta.core.media.AndroidImageCodec
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.account.IumrahAccountService
import com.iumrah.beta.data.account.IumrahAccountStore
import com.iumrah.beta.data.booking.BookingStore
import com.iumrah.beta.models.account.IumrahCheckoutResponse
import com.iumrah.beta.ui.components.IumrahPill
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.components.IumrahPrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PilgrimCheckoutScreen(
    bookingID: String,
    language: AppLanguage,
    bookingStore: BookingStore,
    accountStore: IumrahAccountStore,
    accountService: IumrahAccountService,
    chrome: AppChromeStore,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = bookingStore.booking(bookingID)
    var data by remember(bookingID) { mutableStateOf<IumrahCheckoutResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val accountToken = accountStore.bearerToken
    val receiptLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null || accountToken.isNullOrBlank()) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            runCatching {
                val bytes = withContext(Dispatchers.IO) { AndroidImageCodec.jpeg(context.contentResolver, uri, 2048, 88) }
                accountService.uploadReceipt(bookingID, "manual_card", bytes, "image/jpeg", accountToken)
            }.onSuccess {
                data = runCatching { accountService.checkout(bookingID, accountStore.authorizationHeaders(session?.accessToken)) }.getOrNull()
                busy = false
            }.onFailure { busy = false; error = it.message }
        }
    }

    LaunchedEffect(bookingID, accountToken) {
        val headers = accountStore.authorizationHeaders(session?.accessToken)
        if (headers.isNotEmpty()) runCatching { accountService.checkout(bookingID, headers) }.onSuccess { data = it }.onFailure { error = it.message }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        IumrahPressable(onClick = chrome::back, modifier = Modifier.size(44.dp), cornerRadius = 99.dp, background = MaterialTheme.colorScheme.surfaceVariant) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ArrowBack, "Back") } }
        Text("Pilgrim checkout", style = MaterialTheme.typography.headlineLarge)
        Text("Payment, pilgrim data and travel documents stay linked to this booking.", color = MaterialTheme.colorScheme.onBackground.copy(alpha=.55f))
        if (data == null && error == null) CircularProgressIndicator()
        data?.let { checkout ->
            CheckoutCard(Icons.Rounded.CreditCard, "Payment", checkout.status) {
                Text("Visa •••• ${checkout.payment.visaCardNumber.takeLast(4)}", style = MaterialTheme.typography.bodyLarge)
                checkout.payment.instructions.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.52f)) }
                if (!accountToken.isNullOrBlank()) IumrahPrimaryButton(if (busy) "Uploading…" else "Upload payment receipt", enabled = !busy) { receiptLauncher.launch("image/*") }
                else Text("Activate/sign in to your iumrah account to upload documents.", color = MaterialTheme.colorScheme.onSurface.copy(alpha=.52f))
            }
            CheckoutCard(Icons.Rounded.Person, "Pilgrims", "${checkout.travelers.count { it.completed }}/${checkout.travelers.size} complete") {
                checkout.travelers.sortedBy { it.position }.forEach { traveler ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${traveler.position + 1}. ${listOf(traveler.firstName, traveler.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { traveler.travelerType }}", modifier = Modifier.weight(1f))
                        IumrahPill(if (traveler.completed) "Ready" else "Required")
                    }
                }
            }
            CheckoutCard(Icons.Rounded.Description, "Documents", "${checkout.documents.size}") {
                checkout.documents.forEach { Text(it.title, style = MaterialTheme.typography.bodyLarge) }
                if (checkout.documents.isEmpty()) Text("Documents will appear here after preparation.", color = MaterialTheme.colorScheme.onSurface.copy(alpha=.52f))
            }
            if (checkout.receipts.isNotEmpty()) CheckoutCard(Icons.Rounded.Description, "Receipts", "${checkout.receipts.size}") {
                checkout.receipts.forEach { Row { Text(it.paymentMethod, Modifier.weight(1f)); IumrahPill(it.reviewStatus) } }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(36.dp))
    }
}

@Composable private fun CheckoutCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, status: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(30.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null); Spacer(Modifier.width(10.dp)); Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f)); IumrahPill(status) }
        content()
    }
}
