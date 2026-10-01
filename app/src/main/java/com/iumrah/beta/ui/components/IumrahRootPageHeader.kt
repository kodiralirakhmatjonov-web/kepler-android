package com.iumrah.beta.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iumrah.beta.R
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.ui.cupertino.CupertinoIcon
import com.iumrah.beta.ui.cupertino.CupertinoSymbol
import kotlinx.coroutines.delay

/**
 * Compose mirror of iOS IumrahRootPageTitle.
 * iOS remains the source of truth for sizes, spacing and visual hierarchy.
 */
@Composable
fun IumrahRootPageHeader(
    title: String,
    chrome: AppChromeStore,
    modifier: Modifier = Modifier,
    usesBrandLogo: Boolean = false,
    brandScale: Float = 1f,
    showsConnectivityStatus: Boolean = false,
    unreadCount: Int = 0,
    showsMakkahTime: Boolean = false,
    lightStyle: Boolean = false,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
    ) {
        if (usesBrandLogo && showsConnectivityStatus) {
            val compact = maxWidth < 370.dp
            val veryCompact = maxWidth < 340.dp
            val brandWidth = when {
                veryCompact -> 96.dp
                compact -> 110.dp
                else -> 132.dp
            }
            val controlSize = when {
                veryCompact -> 38.dp
                compact -> 40.dp
                else -> 44.dp
            }
            val spacing = when {
                veryCompact -> 6.dp
                compact -> 7.dp
                else -> 8.dp
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                BrandWordmark(brandWidth, brandScale, lightStyle)
                Spacer(Modifier.weight(1f))
                AnimatedConnectivityIndicator(
                    lightStyle = lightStyle,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(Modifier.width(spacing))
                HeaderControls(
                    chrome = chrome,
                    unreadCount = unreadCount,
                    controlSize = controlSize,
                    spacing = spacing,
                    lightStyle = lightStyle,
                    showsMakkahTime = showsMakkahTime,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                if (usesBrandLogo) {
                    BrandWordmark(180.dp * brandScale, brandScale, lightStyle)
                } else {
                    Text(
                        title,
                        fontSize = 38.sp,
                        lineHeight = 42.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.0).sp,
                        color = if (lightStyle) Color.White else MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.weight(1f))
                HeaderControls(
                    chrome = chrome,
                    unreadCount = unreadCount,
                    controlSize = 46.dp,
                    spacing = 10.dp,
                    lightStyle = lightStyle,
                    showsMakkahTime = showsMakkahTime,
                )
            }
        }
    }
}

@Composable
private fun BrandWordmark(width: Dp, scale: Float, lightStyle: Boolean) {
    Image(
        painter = painterResource(
            if (lightStyle || isSystemInDarkTheme()) R.drawable.iumrah_header_wordmark_dark
            else R.drawable.iumrah_header_wordmark_light,
        ),
        contentDescription = "iumrah",
        modifier = Modifier.width(width).height(46.dp * scale),
        contentScale = ContentScale.Fit,
        alignment = Alignment.CenterStart,
    )
}

@Composable
private fun HeaderControls(
    chrome: AppChromeStore,
    unreadCount: Int,
    controlSize: Dp,
    spacing: Dp,
    lightStyle: Boolean,
    showsMakkahTime: Boolean,
) {
    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.End) {
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(spacing)) {
            Box {
                GlassCircleButton(
                    size = controlSize,
                    lightStyle = lightStyle,
                    onClick = chrome::openNotifications,
                ) {
                    CupertinoIcon(
                        if (unreadCount > 0) CupertinoSymbol.BellBadge else CupertinoSymbol.BellSignal,
                        contentDescription = "Notifications",
                        modifier = Modifier.size(17.dp),
                        tint = if (lightStyle) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (unreadCount > 0) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .graphicsLayer { translationX = -3.dp.toPx(); translationY = 4.dp.toPx() }
                            .height(16.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color(0xFFFF3B30))
                            .border(1.dp, Color.White.copy(alpha = .95f), RoundedCornerShape(99.dp))
                            .padding(horizontal = if (unreadCount > 9) 5.dp else 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (unreadCount > 9) "9+" else unreadCount.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            lineHeight = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            GlassCircleButton(
                size = controlSize,
                lightStyle = lightStyle,
                onClick = chrome::openSidebar,
            ) {
                CupertinoIcon(
                    CupertinoSymbol.Menu,
                    contentDescription = "Menu",
                    modifier = Modifier.size(17.dp),
                    tint = if (lightStyle) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (showsMakkahTime) {
            Spacer(Modifier.height(8.dp))
            MakkahClock(lightStyle)
        }
    }
}

@Composable
private fun GlassCircleButton(
    size: Dp,
    lightStyle: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val fill = if (lightStyle) Color.Black.copy(alpha = .18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .92f)
    Surface(
        onClick = onClick,
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = fill,
        border = BorderStroke(.7.dp, if (lightStyle) Color.White.copy(alpha = .12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = .07f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun MakkahClock(lightStyle: Boolean) {
    val now by produceState(initialValue = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Riyadh"))) {
        while (true) {
            value = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Riyadh"))
            delay(30_000)
        }
    }
    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.End) {
        Text("Makkah", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (lightStyle) Color.White.copy(alpha = .96f) else MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
        Text(
            String.format("%02d:%02d", now.hour, now.minute),
            fontSize = 20.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (lightStyle) Color.White.copy(alpha = .96f) else MaterialTheme.colorScheme.onSurface.copy(alpha = .62f),
        )
    }
}

@Composable
private fun AnimatedConnectivityIndicator(
    lightStyle: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val online by produceState(initialValue = isOnline(context)) {
        while (true) {
            value = isOnline(context)
            delay(30_000)
        }
    }
    var expanded by remember(online) { mutableStateOf(false) }
    LaunchedEffect(online) {
        while (true) {
            delay(1_650)
            expanded = true
            delay(3_650)
            expanded = false
            delay(620)
        }
    }
    val infinite = rememberInfiniteTransition(label = "connectivity-rail")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1550, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "connectivity-rotation",
    )
    val width = if (expanded) 88.dp else 34.dp
    val gradientColors = if (expanded) {
        if (online) listOf(Color(0xFF16E0ED), Color(0xFF24F2B2), Color.White, Color(0xFF1DE8B8), Color(0xFF16E0ED))
        else listOf(Color(0xFFFF1457), Color(0xFFFF2B8E), Color.White, Color(0xFFF5277A), Color(0xFFFF1457))
    } else {
        listOf(Color(0xFF2EF55E), Color(0xFF14E6F0), Color(0xFF336BFF), Color(0xFF9E2EFA), Color(0xFFFF1F8A), Color(0xFFFFA81A), Color(0xFF2EF55E))
    }
    Box(
        modifier
            .width(width)
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Color.Black.copy(alpha = .985f))
            .border(
                2.15.dp,
                Brush.sweepGradient(gradientColors),
                RoundedCornerShape(17.dp),
            )
            .graphicsLayer { rotationZ = rotation * .018f },
        contentAlignment = Alignment.CenterEnd,
    ) {
        Row(
            Modifier.padding(start = if (expanded) 10.dp else 7.dp, end = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (expanded) {
                Text(
                    if (online) "Online" else "Offline",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
            }
            CupertinoIcon(CupertinoSymbol.Globe, null, Modifier.size(19.dp), Color.White)
        }
    }
}

private fun isOnline(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = manager.activeNetwork ?: return false
    val caps = manager.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
