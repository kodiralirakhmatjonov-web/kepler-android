package com.iumrah.beta.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iumrah.beta.R
import com.iumrah.beta.core.design.IumrahMotion
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.ui.components.IumrahDot
import com.iumrah.beta.ui.components.IumrahPill
import com.iumrah.beta.ui.components.IumrahPrimaryButton
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.media.LoopingRawVideo
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val kicker: String,
    val title: String,
    val body: String,
    val footnote: String,
    val scene: Int,
)

@Composable
fun OnboardingFlow(
    language: AppLanguage,
    onFinished: () -> Unit,
) {
    val pages = listOf(
        OnboardingPage("onboarding_welcome_kicker", "onboarding_welcome_title", "onboarding_welcome_body", "onboarding_welcome_footnote", 0),
        OnboardingPage("onboarding_builder_kicker", "onboarding_builder_title", "onboarding_builder_body", "onboarding_builder_footnote", 1),
        OnboardingPage("onboarding_package_kicker", "onboarding_package_title", "onboarding_package_body", "onboarding_package_footnote", 2),
        OnboardingPage("onboarding_care_kicker", "onboarding_care_title", "onboarding_care_body", "onboarding_care_footnote", 3),
        OnboardingPage("onboarding_closing_kicker", "onboarding_closing_title", "onboarding_closing_body", "onboarding_closing_footnote", 4),
    )
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { index ->
            OnboardingPageContent(
                page = pages[index],
                language = language,
                active = pager.currentPage == index,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            if (pager.currentPage < pages.lastIndex) {
                IumrahPressable(
                    onClick = onFinished,
                    cornerRadius = 999.dp,
                    pressedScale = IumrahMotion.PressedScale,
                    background = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                ) {
                    Text(
                        L10n.text("onboarding_skip", language),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                pages.indices.forEach { IumrahDot(selected = pager.currentPage == it) }
            }
            Spacer(Modifier.height(15.dp))
            IumrahPrimaryButton(
                title = if (pager.currentPage == pages.lastIndex) L10n.text("onboarding_start", language) else L10n.text("onboarding_next", language),
                onClick = {
                    if (pager.currentPage == pages.lastIndex) onFinished()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1, animationSpec = IumrahMotion.pageSnap) }
                },
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    language: AppLanguage,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1f else 0.965f,
        animationSpec = IumrahMotion.cinematic,
        label = "onboarding-page-scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.60f,
        animationSpec = tween(280),
        label = "onboarding-page-alpha",
    )
    val y by animateFloatAsState(
        targetValue = if (active) 0f else 18f,
        animationSpec = IumrahMotion.softReveal,
        label = "onboarding-page-y",
    )

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha; translationY = y }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 64.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        OnboardingScene(scene = page.scene, active = active, language = language)

        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text(
                L10n.text(page.kicker, language),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
            )
            Spacer(Modifier.height(9.dp))
            Text(
                L10n.text(page.title, language),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(13.dp))
            Text(
                L10n.text(page.body, language),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.67f),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                L10n.text(page.footnote, language),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f),
            )
        }
    }
}

@Composable
private fun OnboardingScene(scene: Int, active: Boolean, language: AppLanguage) {
    val radius = RoundedCornerShape(38.dp)
    when (scene) {
        0 -> Box(
            Modifier.fillMaxWidth().height(330.dp).clip(radius).background(Color(0xFF111315)),
        ) {
            LoopingRawVideo(
                resourceName = "home_story_02",
                play = active,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    Image(
                        painter = painterResource(R.drawable.iumrah_makkah_background),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                },
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.54f))),
                ),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                Image(
                    painter = painterResource(R.drawable.iumrah_header_wordmark_light),
                    contentDescription = "iumrah",
                    modifier = Modifier.fillMaxWidth(0.38f).height(28.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(11.dp))
                Text(
                    L10n.text("onboarding_welcome_overlay", language),
                    color = Color.White.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        1 -> Box(Modifier.fillMaxWidth().height(330.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SceneIcon(Icons.Rounded.Route)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IumrahPill(L10n.text("onboarding_chip_makkah_madinah", language))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IumrahPill(L10n.text("onboarding_chip_flight", language))
                    IumrahPill(L10n.text("onboarding_chip_hotel", language))
                }
            }
        }

        2 -> Box(Modifier.fillMaxWidth().height(330.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SceneIcon(Icons.Rounded.FlightTakeoff)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SceneIcon(Icons.Rounded.Hotel, small = true)
                    SceneIcon(Icons.Rounded.Route, small = true)
                }
                IumrahPill(L10n.text("onboarding_chip_makkah_madinah", language))
            }
        }

        3 -> Box(
            Modifier.fillMaxWidth().height(330.dp).clip(radius).background(Color(0xFF0E2422)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Image(
                    painter = painterResource(R.drawable.iumrah_care_mark),
                    contentDescription = "iumrah Care",
                    modifier = Modifier.height(76.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IumrahPill(L10n.text("onboarding_chip_support", language), background = Color.White.copy(alpha = 0.12f), foreground = Color.White)
                    IumrahPill(L10n.text("onboarding_chip_status", language), background = Color.White.copy(alpha = 0.12f), foreground = Color.White)
                }
            }
        }

        else -> Box(Modifier.fillMaxWidth().height(330.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.iumrah_onboarding_brand),
                    contentDescription = "iumrah",
                    modifier = Modifier.height(128.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    L10n.text("onboarding_bismillah", language),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SceneIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, small: Boolean = false) {
    val size = if (small) 54.dp else 82.dp
    Box(
        modifier = Modifier
            .height(size)
            .fillMaxWidth(if (small) 0.22f else 0.28f)
            .clip(RoundedCornerShape(if (small) 22.dp else 30.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.height(if (small) 25.dp else 35.dp))
    }
}
