package com.iumrah.beta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iumrah.beta.core.config.AppConfig
import com.iumrah.beta.domain.pricing.LocalPackagePricingEngine

@Composable
fun IumrahApp() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.clip(RoundedCornerShape(32.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("iumrah", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                Text("Android native foundation", style = MaterialTheme.typography.titleMedium)
                Text("Backend: ${AppConfig.API_BASE_URL}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Test package markup: ${LocalPackagePricingEngine.packageMarkupRate.multiply(java.math.BigDecimal(100)).toPlainString()}%",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("Stage 1 / 10", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
