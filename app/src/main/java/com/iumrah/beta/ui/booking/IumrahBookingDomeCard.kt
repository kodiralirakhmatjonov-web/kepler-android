package com.iumrah.beta.ui.booking

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iumrah.beta.core.design.IumrahHaptics
import com.iumrah.beta.core.design.IumrahMotion
import com.iumrah.beta.models.booking.StoredBookingSession
import kotlin.math.*

private data class DomePoint(val x: Float, val y: Float, val z: Float, val ring: Int, val index: Int)

@Composable
fun IumrahBookingDomeCard(session: StoredBookingSession, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    var flipped by remember(session.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, IumrahMotion.cardFlip, label = "booking-dome-flip")
    val view = LocalView.current
    val density = LocalDensity.current
    val interaction = remember { MutableInteractionSource() }
    val camera = with(density) { 18.dp.toPx() * density.density }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.60f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = camera * 24f
            }
            .clip(RoundedCornerShape(22.dp))
            .clickable(interactionSource = interaction, indication = null) {
                IumrahHaptics.soft(view)
                flipped = !flipped
            },
    ) {
        val backVisible = rotation > 90f
        Box(Modifier.fillMaxSize().graphicsLayer { if (backVisible) rotationY = 180f }) {
            if (backVisible) BookingDomeBack(session, onOpen) else BookingDomeFront(session)
        }
    }
}

@Composable private fun BookingDomeFront(session: StoredBookingSession) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF070708), Color(0xFF17171A), Color(0xFF050506))),
        ),
    ) {
        SpectralDome(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("iumrah", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(session.displayBookingNumber, color = Color.White.copy(alpha=.58f), style = MaterialTheme.typography.labelLarge)
            }
            Column {
                Text(session.travelerName?.takeIf { it.isNotBlank() } ?: "Your Umrah", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(session.effectiveStatus.replace('_', ' '), color = Color.White.copy(alpha=.55f), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(12.dp))
                ActivityDots()
            }
        }
    }
}

@Composable private fun BookingDomeBack(session: StoredBookingSession, onOpen: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0A0A0C), Color(0xFF202026), Color.Black)))) {
        Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("BOOKING", color = Color.White.copy(alpha=.45f), style = MaterialTheme.typography.labelLarge)
                Text(session.displayBookingNumber, color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Text(session.booking.hotelNames.makkah, color = Color.White.copy(alpha=.70f))
                session.booking.hotelNames.madinah.takeIf { it.isNotBlank() }?.let { Text(it, color = Color.White.copy(alpha=.70f)) }
                Text(session.booking.flight, color = Color.White.copy(alpha=.52f), maxLines = 2)
            }
            androidx.compose.material3.Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text("Open booking") }
        }
    }
}

@Composable private fun ActivityDots() {
    val transition = rememberInfiniteTransition(label = "booking-activity")
    val phase by transition.animateFloat(0f, (2f * Math.PI).toFloat(), infiniteRepeatable(tween(1365, easing = LinearEasing)), label = "dots-phase")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(7) { index ->
            val wave = ((sin(phase * 1.0f - index * 1.45f) + 1f) / 2f)
            Box(Modifier.size((5f + wave * 2f).dp).graphicsLayer { alpha = .24f + wave * .76f }.background(Color.White, RoundedCornerShape(99.dp)))
        }
    }
}

@Composable private fun SpectralDome(modifier: Modifier) {
    val points = remember {
        buildList {
            val rings = 18
            for (r in 0 until rings) {
                val v = r / (rings - 1f)
                val theta = v * (Math.PI / 2).toFloat()
                val radius = sin(theta)
                val z = cos(theta)
                val count = (8 + radius * 28).roundToInt()
                repeat(count) { i ->
                    val a = (i / count.toFloat()) * (Math.PI * 2).toFloat() + (r % 2) * .10f
                    add(DomePoint(cos(a) * radius, sin(a) * radius, z, r, i))
                }
            }
        }
    }
    val transition = rememberInfiniteTransition(label = "spectral-cycle")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(4600, easing = LinearEasing)), label = "spectral-phase")
    Canvas(modifier) {
        val center = Offset(size.width * .70f, size.height * .59f)
        val radius = min(size.width, size.height) * .36f
        drawCircle(Brush.radialGradient(listOf(Color(0x3348B8FF), Color.Transparent), center, radius * 1.35f), radius * 1.35f, center)
        points.forEach { point ->
            val spin = phase * (Math.PI * 2).toFloat()
            val rx = point.x * cos(spin) - point.y * sin(spin)
            val ry = point.x * sin(spin) + point.y * cos(spin)
            val front = ((ry + 1f) * .5f).coerceIn(0f, 1f)
            val p = Offset(center.x + rx * radius, center.y - point.z * radius * .92f + ry * radius * .14f)
            val hue = (phase * 360f + point.ring * 11f + point.index * 2.4f) % 360f
            val color = Color.hsv(hue, .52f, 1f, .24f + front * .58f)
            val dot = 0.9f + front * 2.2f
            drawCircle(color, dot, p)
        }
        drawCircle(Brush.radialGradient(listOf(Color(0x22FFFFFF), Color.Transparent), center - Offset(radius*.20f, radius*.46f), radius*.55f), radius*.58f, center - Offset(radius*.20f, radius*.46f))
    }
}
