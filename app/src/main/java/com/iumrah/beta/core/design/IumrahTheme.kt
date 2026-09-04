package com.iumrah.beta.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.iumrah.beta.core.settings.AppAppearance

object IumrahColors {
    val LightPage = Color(0xFFF6F7F8)
    val LightCard = Color(0xFFFFFFFF)
    val LightRaised = Color(0xFFECEDF1)
    val DarkPage = Color(0xFF1B1D20)
    val DarkCard = Color(0xFF25272C)
    val DarkRaised = Color(0xFF2F3237)
    val Graphite = Color(0xFF17191D)
    val CareDark = Color(0xFF0E2422)
    val CareLight = Color(0xFF74A187)
}

private val LightColors = lightColorScheme(
    background = IumrahColors.LightPage,
    surface = IumrahColors.LightCard,
    surfaceVariant = IumrahColors.LightRaised,
    onBackground = Color(0xFF17181A),
    onSurface = Color(0xFF17181A),
    primary = Color(0xFF111111),
    onPrimary = Color.White,
    secondary = IumrahColors.CareDark,
)

private val DarkColors = darkColorScheme(
    background = IumrahColors.DarkPage,
    surface = IumrahColors.DarkCard,
    surfaceVariant = IumrahColors.DarkRaised,
    onBackground = Color(0xFFF4F4F5),
    onSurface = Color(0xFFF4F4F5),
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF121316),
    secondary = IumrahColors.CareLight,
)

private val IumrahTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.55).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
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
        content = content,
    )
}
