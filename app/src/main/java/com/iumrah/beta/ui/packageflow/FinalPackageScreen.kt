package com.iumrah.beta.ui.packageflow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.domain.journey.JourneyStore
import com.iumrah.beta.ui.components.IumrahPill
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.components.IumrahPrimaryButton
import java.text.NumberFormat
import java.util.Locale

@Composable
fun FinalPackageScreen(language: AppLanguage, journey: JourneyStore, chrome: AppChromeStore) {
    val state by journey.state.collectAsState()
    val quote = state.quote
    if (quote == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Package is not ready", style = MaterialTheme.typography.headlineSmall)
                IumrahPrimaryButton("Back to flights", modifier = Modifier.width(240.dp), onClick = chrome::back)
            }
        }
        return
    }
    val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply { currency = java.util.Currency.getInstance("USD"); maximumFractionDigits = 0 }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IumrahPressable(onClick = chrome::back, modifier = Modifier.size(44.dp), cornerRadius = 99.dp, background = MaterialTheme.colorScheme.surfaceVariant) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ArrowBack, "Back") }
        }
        Text("Your Umrah", style = MaterialTheme.typography.headlineLarge)
        Text("One package. One clear price.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f), style = MaterialTheme.typography.bodyLarge)

        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(34.dp)).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("TOTAL PACKAGE", color = MaterialTheme.colorScheme.background.copy(alpha = .58f), style = MaterialTheme.typography.labelLarge)
            Text(formatter.format(quote.totalPackagePrice), color = MaterialTheme.colorScheme.background, style = MaterialTheme.typography.displaySmall)
            Text("${formatter.format(quote.pricePerPerson)} per pilgrim", color = MaterialTheme.colorScheme.background.copy(alpha = .72f), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            IumrahPill(if (quote.isEstimated) "Live indicative package" else "Package quote")
        }

        PackageLine(Icons.Rounded.Flight, "Complete flight journey", state.selectedJourney?.let { "${it.outbound.origin} → ${it.outbound.destination}" }.orEmpty())
        PackageLine(Icons.Rounded.Hotel, "Makkah", listOfNotNull(state.makkahHotel?.name, state.makkahRoomCategory?.displayName ?: state.makkahRoom?.name).joinToString(" · "))
        state.madinahHotel?.let { PackageLine(Icons.Rounded.Hotel, "Madinah", listOfNotNull(it.name, state.madinahRoomCategory?.displayName ?: state.madinahRoom?.name).joinToString(" · ")) }
        PackageLine(Icons.Rounded.VerifiedUser, "Included", "Visa · meals · transfer · guide · ziyarat · Care · eSIM")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha=.55f))
            Spacer(Modifier.width(8.dp))
            Text("Android test package markup: 20%", color = MaterialTheme.colorScheme.onSurface.copy(alpha=.52f), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(4.dp))
        IumrahPrimaryButton("Continue to booking", onClick = chrome::openBookingCheckout)
        Spacer(Modifier.height(36.dp))
    }
}

@Composable private fun PackageLine(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp)).padding(17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) { Icon(icon, null) }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
