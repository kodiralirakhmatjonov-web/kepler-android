package com.iumrah.beta.ui.shell

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.iumrah.beta.core.design.IumrahMotion
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.navigation.AppTab
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.ui.components.IumrahPressable

@Composable
fun SidebarDrawerHost(
    open: Boolean,
    language: AppLanguage,
    chrome: AppChromeStore,
    content: @Composable () -> Unit,
) {
    val width = (LocalConfiguration.current.screenWidthDp.dp * .74f).coerceAtMost(360.dp)
    val contentScale = animateFloatAsState(if (open) .985f else 1f, IumrahMotion.sidebar, label = "sidebar-content-scale").value
    val drawerX = animateFloatAsState(if (open) 0f else -1f, IumrahMotion.sidebar, label = "sidebar-x").value
    val scrim = animateFloatAsState(if (open) .28f else 0f, IumrahMotion.fastFade, label = "sidebar-scrim").value

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                scaleX = contentScale
                scaleY = contentScale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, .5f)
            },
        ) { content() }

        if (scrim > .001f) {
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = scrim))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = chrome::closeSidebar,
                    ),
            )
        }

        Box(
            Modifier
                .width(width)
                .fillMaxHeight()
                .graphicsLayer { translationX = drawerX * width.toPx() }
                .shadow(if (open) 30.dp else 0.dp, RoundedCornerShape(topEnd = 34.dp, bottomEnd = 34.dp)),
        ) {
            SidebarDrawer(language, chrome)
        }
    }
}

@Composable
private fun SidebarDrawer(language: AppLanguage, chrome: AppChromeStore) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).statusBarsPadding().padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("iumrah", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            IumrahPressable(
                onClick = chrome::closeSidebar,
                modifier = Modifier.size(38.dp),
                cornerRadius = 99.dp,
                background = MaterialTheme.colorScheme.surfaceVariant,
                pressedScale = .90f,
            ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, contentDescription = "Close") } }
        }
        Text(sidebarCopy(language).subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .52f))
        Spacer(Modifier.size(8.dp))
        DrawerRow(Icons.Rounded.Home, sidebarCopy(language).home) { chrome.navigate(AppTab.HOME) }
        DrawerRow(Icons.Rounded.Luggage, sidebarCopy(language).trips) { chrome.navigate(AppTab.BOOKING) }
        DrawerRow(Icons.Rounded.Favorite, "iumrah Care") { chrome.navigate(AppTab.CARE) }
        DrawerRow(Icons.Rounded.AccountCircle, sidebarCopy(language).account) { chrome.navigate(AppTab.ACCOUNT) }
        Spacer(Modifier.weight(1f))
        Text("Independent Umrah · iumrah", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .35f))
    }
}

@Composable
private fun DrawerRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    IumrahPressable(onClick = onClick, modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, background = MaterialTheme.colorScheme.surfaceVariant, pressedScale = .975f) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(title, modifier = Modifier.padding(start = 14.dp), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .35f))
        }
    }
}

private data class SidebarCopy(val subtitle: String, val home: String, val trips: String, val account: String)
private fun sidebarCopy(language: AppLanguage): SidebarCopy = when (language) {
    AppLanguage.RUSSIAN -> SidebarCopy("Ваша умра — в одном месте", "Главная", "Поездки", "Аккаунт")
    AppLanguage.ENGLISH -> SidebarCopy("Your Umrah in one place", "Home", "Trips", "Account")
    AppLanguage.UZBEK -> SidebarCopy("Umrangiz — bir joyda", "Bosh sahifa", "Safarlar", "Akkaunt")
    AppLanguage.UZBEK_CYRILLIC -> SidebarCopy("Умрангиз — бир жойда", "Бош саҳифа", "Сафарлар", "Аккаунт")
}
