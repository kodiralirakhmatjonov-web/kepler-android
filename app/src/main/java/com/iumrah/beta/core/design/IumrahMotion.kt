package com.iumrah.beta.core.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Motion values are mapped from the SwiftUI source instead of Material defaults.
 * Stiffness is derived from the source spring response and tuned for Compose.
 */
object IumrahMotion {
    const val PressedScale = 0.985f
    const val CardPressedScale = 0.978f

    val press: SpringSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 685f) // iOS .24 / .86
    val tab: SpringSpec<Float> = spring(dampingRatio = 0.88f, stiffness = 360f)
    val sidebar: SpringSpec<Float> = spring(dampingRatio = 0.88f, stiffness = 342f) // .34 / .88
    val softReveal: SpringSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 116f) // .58 / .86
    val cinematic: SpringSpec<Float> = spring(dampingRatio = 0.84f, stiffness = 103f) // .62 / .84
    val cardFlip: SpringSpec<Float> = spring(dampingRatio = 0.84f, stiffness = 91f) // .66 / .84
    val selection: SpringSpec<Float> = spring(dampingRatio = 0.88f, stiffness = 584f) // .26 / .88
    val selectionDp: SpringSpec<Dp> = spring(dampingRatio = 0.88f, stiffness = 584f)
    val selectionColor: SpringSpec<Color> = spring(dampingRatio = 0.88f, stiffness = 584f)
    val content: SpringSpec<Float> = spring(dampingRatio = 0.90f, stiffness = 224f) // .42 / .90

    val fastFade = tween<Float>(durationMillis = 220)
    val fastColor: TweenSpec<Color> = tween(durationMillis = 110)
    val rootFade = tween<Float>(durationMillis = 280)
    val vapor = tween<Float>(durationMillis = 820)
    val vaporDp = tween<Dp>(durationMillis = 820)
    val pageSnap = tween<Float>(durationMillis = 240)
}

fun iosSpring(responseSeconds: Float, dampingFraction: Float): SpringSpec<Float> {
    val omega = (2.0 * Math.PI / responseSeconds.coerceAtLeast(0.08f)).toFloat()
    val stiffness = (omega * omega).coerceIn(Spring.StiffnessVeryLow, 1200f)
    return spring(dampingRatio = dampingFraction.coerceIn(0.01f, 1f), stiffness = stiffness)
}
