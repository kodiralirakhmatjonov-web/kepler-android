package com.iumrah.beta.ui.booking

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.iumrah.beta.core.media.AndroidImageCodec
import com.iumrah.beta.data.booking.BookingService
import com.iumrah.beta.models.account.IumrahSecurityConfirmation
import com.iumrah.beta.models.booking.StoredBookingSession
import com.iumrah.beta.ui.components.IumrahPrimaryButton
import com.iumrah.beta.ui.components.IumrahSecondaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Passport number is transient Compose state only; it is never written to the booking vault. */
@Composable
fun IumrahSecurityConfirmationPanel(session: StoredBookingSession, service: BookingService) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember(session.id) { mutableStateOf<IumrahSecurityConfirmation?>(null) }
    var firstName by remember(session.id) { mutableStateOf(session.booking.pilgrimProfile?.firstName.orEmpty()) }
    var lastName by remember(session.id) { mutableStateOf(session.booking.pilgrimProfile?.lastName.orEmpty()) }
    var passportNumber by remember(session.id) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true; error = null
        scope.launch {
            runCatching {
                val bytes = withContext(Dispatchers.IO) { AndroidImageCodec.jpeg(context.contentResolver, uri, 2048, 88) }
                service.uploadSecurityPassport(session.id, session.accessToken, bytes, "image/jpeg")
            }.onSuccess { result -> status = result.confirmation; busy = false }
                .onFailure { cause -> busy = false; error = cause.message }
        }
    }

    LaunchedEffect(session.id) {
        runCatching { service.securityConfirmation(session.id, session.accessToken) }.getOrNull()?.let { status = it.confirmation }
    }

    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(30.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Lock, null) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("iumrah Security", style = MaterialTheme.typography.titleLarge)
                Text(securityStatusText(status), color = MaterialTheme.colorScheme.onSurface.copy(alpha=.55f), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (status?.isConfirmed == true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null)
                Spacer(Modifier.width(8.dp))
                Text("Identity confirmed")
            }
        } else if (status?.isPendingReview == true) {
            Text("Your data is under secure review. Editing is temporarily locked.", color = MaterialTheme.colorScheme.onSurface.copy(alpha=.58f))
        } else {
            OutlinedTextField(firstName, { firstName = it }, label = { Text("First name") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(22.dp))
            OutlinedTextField(lastName, { lastName = it }, label = { Text("Last name") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(22.dp))
            OutlinedTextField(passportNumber, { passportNumber = it.uppercase().take(20) }, label = { Text("Passport number") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(22.dp))
            IumrahSecondaryButton(if (status?.hasPassportPhoto == true) "Replace passport photo" else "Add passport photo") { launcher.launch("image/*") }
            IumrahPrimaryButton(
                if (busy) "Submitting…" else "Submit securely",
                enabled = !busy && firstName.isNotBlank() && lastName.isNotBlank() && passportNumber.length >= 4 && status?.hasPassportPhoto == true,
                onClick = {
                    busy = true; error = null
                    val ephemeralPassport = passportNumber
                    scope.launch {
                        runCatching { service.submitSecurityConfirmation(session.id, session.accessToken, firstName.trim(), lastName.trim(), ephemeralPassport.trim()) }
                            .onSuccess { result ->
                                status = result.confirmation
                                passportNumber = "" // explicit destruction from UI state after successful submission
                                busy = false
                            }.onFailure { cause -> busy = false; error = cause.message }
                    }
                },
            )
        }
        status?.reviewNote?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Passport details are sent to the booking backend and are not stored in the Android booking vault.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.42f))
    }
}

private fun securityStatusText(status: IumrahSecurityConfirmation?): String = when {
    status == null -> "Protected booking verification"
    status.isConfirmed -> "Confirmed"
    status.isPendingReview -> "Under review"
    status.needsResubmission -> "Needs resubmission"
    else -> "Ready for secure verification"
}
