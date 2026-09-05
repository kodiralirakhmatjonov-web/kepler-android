package com.iumrah.beta.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iumrah.beta.core.settings.AppAppearance

/**
 * iumrah Galaxy visual foundation.
 *
 * The Android app deliberately uses a calmer One UI-inspired hierarchy:
 * soft neutral page surfaces, restrained contrast, rounded but not pill-shaped
 * containers, and system sans typography so Galaxy devices can use their native
 * font rendering and metrics.
 */
object IumrahColors {
    val LightPage = Color(0xFFF5F5F7)
    val LightCard = Color(0xFFFFFFFF)
    val LightRaised = Color(0xFFEEEFF2)
    val LightRaisedStrong = Color(0xFFE6E7EB)

    val DarkPage = Color(0xFF101114)
    val DarkCard = Color(0xFF191B1F)
    val DarkRaised = Color(0xFF24272C)
    val DarkRaisedStrong = Color(0xFF2C3036)

    val Graphite = Color(0xFF1A1B1E)
    val SoftGraphite = Color(0xFF2A2B2F)
    val CareDark = Color(0xFF12332F)
    val CareLight = Color(0xFF7BA991)
}

object IumrahGalaxyMetrics {
    val ScreenHorizontal = 24.dp
    val ScreenTop = 10.dp
    val SectionGap = 30.dp
    val ContentGap = 14.dp

    val RadiusSmall = 14.dp
    val RadiusControl = 18.dp
    val RadiusButton = 20.dp
    val RadiusTile = 22.dp
    val RadiusCard = 26.dp
    val RadiusLarge = 30.dp

    val TouchTarget = 48.dp
    val ControlHeight = 54.dp
    val PrimaryButtonHeight = 56.dp
}

private val LightColors = lightColorScheme(
    background = IumrahColors.LightPage,
    surface = IumrahColors.LightCard,
    surfaceVariant = IumrahColors.LightRaised,
    onBackground = IumrahColors.Graphite,
    onSurface = IumrahColors.Graphite,
    primary = IumrahColors.Graphite,
    onPrimary = Color.White,
    secondary = IumrahColors.CareDark,
    onSecondary = Color.White,
    outline = Color(0xFFD7D8DD),
    outlineVariant = Color(0xFFE5E6EA),
)

private val DarkColors = darkColorScheme(
    background = IumrahColors.DarkPage,
    surface = IumrahColors.DarkCard,
    surfaceVariant = IumrahColors.DarkRaised,
    onBackground = Color(0xFFF4F4F6),
    onSurface = Color(0xFFF4F4F6),
    primary = Color(0xFFF2F2F4),
    onPrimary = Color(0xFF111216),
    secondary = IumrahColors.CareLight,
    onSecondary = Color(0xFF0E1714),
    outline = Color(0xFF3A3D43),
    outlineVariant = Color(0xFF2C2F34),
)

private val IumrahTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.65).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 31.sp,
        lineHeight = 37.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.48).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.32).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.18).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium,
    ),
)

private val IumrahShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(IumrahGalaxyMetrics.RadiusSmall),
    medium = RoundedCornerShape(IumrahGalaxyMetrics.RadiusControl),
    large = RoundedCornerShape(IumrahGalaxyMetrics.RadiusCard),
    extraLarge = RoundedCornerShape(IumrahGalaxyMetrics.RadiusLarge),
)

@Composable
fun IumrahTheme(
    appearance: AppAppearance = AppAppearance.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (appearance) {
        AppAppearance.SYSTEM -> systemDark
        AppAppearance.LIGHT -> false
        AppAppearance.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = IumrahTypography,
        shapes = IumrahShapes,
        content = content,
    )
}
