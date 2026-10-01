package com.iumrah.beta.ui.generator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.cupertino.CupertinoSymbol
import com.iumrah.beta.ui.cupertino.Icon
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/** Android mirror of iOS TripProgressStage / IumrahGeneratorHeader. */
enum class GeneratorStage(val raw: Int, val key: String, val icon: CupertinoSymbol) {
    TRIP(1, "step_trip", CupertinoSymbol.CalendarClock),
    HOTEL(2, "step_hotel", CupertinoSymbol.Hotel),
    FLIGHT(3, "step_flight", CupertinoSymbol.AirplaneTakeoff),
    TRANSFER(4, "step_transfer", CupertinoSymbol.Car),
    READY(5, "step_ready", CupertinoSymbol.CheckCircle),
}

object GeneratorGeometry {
    val pagePadding = 18.dp
    val cardRadius = 28.dp
    val heroRadius = 34.dp
    val compactRadius = 19.dp
    val controlHeight = 56.dp
}

@Composable
fun generatorPageColor(): Color {
    val dark = MaterialTheme.colorScheme.background.red + MaterialTheme.colorScheme.background.green + MaterialTheme.colorScheme.background.blue < 0.75f
    return if (dark) Color(0xFF000000) else Color.White
}

@Composable
fun generatorCardColor(): Color {
    val dark = MaterialTheme.colorScheme.background.red + MaterialTheme.colorScheme.background.green + MaterialTheme.colorScheme.background.blue < 0.75f
    return if (dark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
}

@Composable
fun generatorRaisedColor(): Color {
    val dark = MaterialTheme.colorScheme.background.red + MaterialTheme.colorScheme.background.green + MaterialTheme.colorScheme.background.blue < 0.75f
    return if (dark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
}

@Composable
fun GeneratorHeader(
    stage: GeneratorStage,
    language: AppLanguage,
    chrome: AppChromeStore,
    currentPriceText: String? = null,
    modifier: Modifier = Modifier,
) {
    val page = generatorPageColor()
    val card = generatorCardColor()
    val fg = if (page == Color.White) Color.Black else Color.White
    val secondary = fg.copy(alpha = .55f)
    val shape = RoundedCornerShape(28.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(9.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = .035f), spotColor = Color.Black.copy(alpha = .055f))
            .clip(shape)
            .background(card.copy(alpha = .96f))
            .border(.7.dp, fg.copy(alpha = .07f), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(76.dp)) {
            GeneratorStageCarousel(stage = stage, modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp))
            IumrahPressable(
                onClick = chrome::back,
                modifier = Modifier.align(Alignment.CenterStart).size(40.dp),
                cornerRadius = 99.dp,
                background = page.copy(alpha = .72f),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(CupertinoSymbol.ChevronLeft, "Back", Modifier.size(20.dp), tint = fg)
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${stepPrefix(language)} ${stage.raw} / 5",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = secondary,
            )
            Spacer(Modifier.weight(1f))
            if (currentPriceText != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(currentTotalLabel(language), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = secondary, letterSpacing = .7.sp)
                    Text(currentPriceText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = fg)
                }
            } else {
                Text(L10n.text(stage.key, language), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            GeneratorStage.entries.forEach { item ->
                val active = item == stage
                val done = item.raw < stage.raw
                val fill = when {
                    done -> Color(0xFF74A187)
                    active -> fg
                    else -> secondary.copy(alpha = .28f)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(if (active) 5.dp else 3.5.dp)
                        .clip(CircleShape)
                        .background(fill),
                )
            }
        }
    }
}

@Composable
private fun GeneratorStageCarousel(stage: GeneratorStage, modifier: Modifier = Modifier) {
    val card = generatorCardColor()
    val fg = if (generatorPageColor() == Color.White) Color.Black else Color.White
    val activeIndex = stage.raw - 1
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val localDensity = LocalDensity.current
        val radiusPx = with(localDensity) { 78.dp.toPx() }
        GeneratorStage.entries.forEachIndexed { index, item ->
            val angle = (index - activeIndex) * (2.0 * Math.PI / GeneratorStage.entries.size)
            val depth = cos(angle).toFloat()
            val frontness = ((depth + 1f) / 2f).coerceIn(0f, 1f)
            val x = (sin(angle) * radiusPx).toFloat()
            val scale = .56f + (.46f * frontness)
            val alpha = .14f + (.86f * frontness.pow(.9f))
            val iconTint = when (item) {
                GeneratorStage.TRIP -> Color(0xFFFF9500)
                GeneratorStage.HOTEL -> Color(0xFF5856D6)
                GeneratorStage.FLIGHT -> Color(0xFF007AFF)
                GeneratorStage.TRANSFER -> Color(0xFF32ADE6)
                GeneratorStage.READY -> Color(0xFF34C759)
            }
            Box(
                Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        translationX = x
                        translationY = with(localDensity) { 6.dp.toPx() } * (1f - frontness)
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        rotationY = (-sin(angle) * 18.0).toFloat()
                        cameraDistance = 12f * localDensity.density
                    }
                    .shadow(if (index == activeIndex) 10.dp else 0.dp, RoundedCornerShape(20.dp), clip = false)
                    .clip(RoundedCornerShape(20.dp))
                    .background(card.copy(alpha = .96f))
                    .border(.7.dp, fg.copy(alpha = if (index == activeIndex) .07f else .025f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(item.icon, null, Modifier.size(if (index == activeIndex) 30.dp else 26.dp), tint = iconTint)
            }
        }
    }
}

@Composable
fun GeneratorCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val fg = if (generatorPageColor() == Color.White) Color.Black else Color.White
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .fillMaxWidth()
            .shadow(7.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = .025f), spotColor = Color.Black.copy(alpha = .05f))
            .clip(shape)
            .background(generatorCardColor())
            .border(.7.dp, fg.copy(alpha = .06f), shape)
            .padding(18.dp),
    ) { content() }
}

@Composable
fun GeneratorPrimaryButton(
    title: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val page = generatorPageColor()
    val bg = if (page == Color.White) Color.Black else Color(0xFFF5F5F7)
    val fg = if (page == Color.White) Color.White else Color(0xFF111216)
    IumrahPressable(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        cornerRadius = 19.dp,
        background = bg.copy(alpha = if (enabled) 1f else .42f),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = fg, textAlign = TextAlign.Center)
        }
    }
}

private fun stepPrefix(language: AppLanguage): String = when (language) {
    AppLanguage.RUSSIAN -> "Шаг"
    AppLanguage.ENGLISH -> "Step"
    AppLanguage.UZBEK -> "Bosqich"
    AppLanguage.UZBEK_CYRILLIC -> "Босқич"
}

private fun currentTotalLabel(language: AppLanguage): String = when (language) {
    AppLanguage.RUSSIAN -> "ТЕКУЩАЯ ЦЕНА"
    AppLanguage.ENGLISH -> "CURRENT TOTAL"
    AppLanguage.UZBEK -> "JORIY NARX"
    AppLanguage.UZBEK_CYRILLIC -> "ЖОРИЙ НАРХ"
}
