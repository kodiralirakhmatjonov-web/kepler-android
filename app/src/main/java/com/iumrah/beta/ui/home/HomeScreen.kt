package com.iumrah.beta.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.iumrah.beta.R
import com.iumrah.beta.core.design.IumrahColors
import com.iumrah.beta.core.design.IumrahHaptics
import com.iumrah.beta.core.design.IumrahMotion
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.navigation.AppTab
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.ui.components.IumrahPill
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.components.IumrahRootPageHeader
import com.iumrah.beta.ui.media.LoopingRawVideo
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(language: AppLanguage, chrome: AppChromeStore) {
    var showStory by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            IumrahRootPageHeader(
                title = L10n.text("tab_home", language),
                chrome = chrome,
                usesBrandLogo = true,
            )
        }
        item { HomeHero(language = language, onBuild = chrome::startNewTrip) }
        item { EmotionalPrompt(language = language, onOpen = { showStory = true }) }
        item { HomeVideoCarousel() }
        item {
            HomeImageCard(
                drawable = R.drawable.iumrah_flights_home_card,
                contentDescription = "iumrah Flights",
                onClick = chrome::openFlights,
            )
        }
        item {
            HomeFeatureCard(
                title = L10n.text("hotels_title", language),
                body = L10n.text("hotels_subtitle", language),
                icon = Icons.Rounded.Hotel,
                onClick = { chrome.navigate(AppTab.HOTELS) },
            )
        }
        item {
            CareCard(language = language, onClick = { chrome.navigate(AppTab.CARE) })
        }
        item {
            HomeFeatureCard(
                title = L10n.text("home_philosophy_title", language),
                body = L10n.text("home_philosophy_body", language),
                icon = Icons.Rounded.Favorite,
                onClick = {},
            )
        }
        item {
            HomeFeatureCard(
                title = L10n.text("home_connected_title", language),
                body = L10n.text("home_connected_body", language),
                icon = Icons.Rounded.Flight,
                onClick = chrome::startNewTrip,
            )
        }
    }

    if (showStory) EmotionalJourneyFullscreen(language = language, onClose = { showStory = false })
}

@Composable
private fun HomeHero(language: AppLanguage, onBuild: () -> Unit) {
    val shape = RoundedCornerShape(34.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Color(0xFF111315)).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp),
    ) {
        Text(L10n.text("home_hero_kicker", language), color = Color.White.copy(alpha = .60f), style = MaterialTheme.typography.labelLarge)
        Text(L10n.text("home_hero_title", language), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(L10n.text("home_hero_body", language), color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.bodyLarge)
        IumrahPressable(
            onClick = onBuild,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            background = Color.White,
            cornerRadius = 20.dp,
            pressedScale = IumrahMotion.PressedScale,
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(L10n.text("home_hero_cta", language), color = Color.Black, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = Color.Black)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White.copy(alpha = .74f))
            Text(L10n.text("home_hero_badge", language), color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmotionalPrompt(language: AppLanguage, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(HomeEmotionalCopy.prompt(language), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        Spacer(Modifier.padding(6.dp))
        IumrahPressable(onClick = onOpen, background = MaterialTheme.colorScheme.primary, cornerRadius = 999.dp, pressedScale = IumrahMotion.PressedScale) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(HomeEmotionalCopy.action(language), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun HomeImageCard(drawable: Int, contentDescription: String, onClick: () -> Unit) {
    IumrahPressable(onClick = onClick, cornerRadius = 30.dp, shadowElevation = 10.dp) {
        Image(
            painter = painterResource(drawable),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
    }
}

@Composable
private fun HomeFeatureCard(
    title: String,
    body: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    IumrahPressable(onClick = onClick, modifier = Modifier.fillMaxWidth(), cornerRadius = 30.dp, shadowElevation = 3.dp) {
        Row(Modifier.fillMaxWidth().padding(19.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            Box(Modifier.clip(RoundedCornerShape(19.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(13.dp)) {
                Icon(icon, contentDescription = null)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f), maxLines = 3)
            }
            Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .35f))
        }
    }
}

@Composable
private fun CareCard(language: AppLanguage, onClick: () -> Unit) {
    IumrahPressable(onClick = onClick, cornerRadius = 27.dp, shadowElevation = 8.dp, background = IumrahColors.CareDark) {
        Row(
            Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(IumrahColors.CareDark, IumrahColors.CareLight.copy(alpha = .92f)))).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.clip(RoundedCornerShape(17.dp)).background(Color.White).padding(6.dp)) {
                Image(painterResource(R.drawable.iumrah_care_mark), contentDescription = "iumrah Care", modifier = Modifier.height(43.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("iumrah Care", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(L10n.text("care_subtitle", language), color = Color.White.copy(alpha = .76f), style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            }
            Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = .75f))
        }
    }
}

private val storyResources = listOf("home_story_01", "home_story_03", "home_story_04", "home_story_05", "home_story_07", "home_story_08")

@Composable
private fun HomeVideoCarousel() {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val carouselHeight = (screenHeight * .72f).coerceIn(460.dp, 680.dp)
    val pager = rememberPagerState(pageCount = { storyResources.size })
    var muted by remember { mutableStateOf(true) }
    val hapticView = LocalView.current

    Box(Modifier.fillMaxWidth().height(carouselHeight), contentAlignment = Alignment.BottomCenter) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 4.dp),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
        ) { index ->
            val offset = ((pager.currentPage - index) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
            val cardScale = 1f - (.015f * offset)
            val cardAlpha = 1f - (.10f * offset)
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = cardScale; scaleY = cardScale; alpha = cardAlpha }
                    .clip(RoundedCornerShape(34.dp))
                    .background(Color.Black),
            ) {
                LoopingRawVideo(
                    resourceName = storyResources[index],
                    modifier = Modifier.fillMaxSize(),
                    play = pager.currentPage == index,
                    muted = muted,
                    fallback = {
                        Image(
                            painterResource(R.drawable.iumrah_makkah_background),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    },
                )
                IumrahPressable(
                    onClick = {
                        muted = !muted
                        IumrahHaptics.soft(hapticView)
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(42.dp),
                    cornerRadius = 99.dp,
                    background = Color.White.copy(alpha=.16f),
                    pressedScale = .92f,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(if (muted) Icons.Rounded.VolumeOff else Icons.Rounded.Speaker, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.padding(bottom = 14.dp).clip(RoundedCornerShape(99.dp)).background(Color.Black.copy(alpha=.28f)).padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            storyResources.indices.forEach { index ->
                val width by animateFloatAsState(if (pager.currentPage == index) 18f else 6f, IumrahMotion.selection, label = "home-carousel-dot-$index")
                Box(Modifier.width(width.dp).height(6.dp).clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha = if (pager.currentPage == index) .96f else .46f)))
            }
        }
    }
}

@Composable
private fun EmotionalJourneyFullscreen(language: AppLanguage, onClose: () -> Unit) {
    val pager = rememberPagerState(pageCount = { storyResources.size })
    var muted by remember { mutableStateOf(false) }
    var captionVisible by remember { mutableStateOf(false) }
    val hapticView = LocalView.current
    BackHandler { onClose() }

    LaunchedEffect(pager.currentPage) {
        captionVisible = false
        IumrahHaptics.selection(hapticView)
        delay(if (pager.currentPage == 0) 340 else 240)
        captionVisible = true
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { index ->
                LoopingRawVideo(
                    resourceName = storyResources[index],
                    modifier = Modifier.fillMaxSize(),
                    play = pager.currentPage == index,
                    muted = muted,
                    fallback = {
                        Image(painterResource(R.drawable.iumrah_makkah_background), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    },
                )
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.42f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha=.90f)))))

            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IumrahPill("${pager.currentPage + 1} / ${storyResources.size}", background = Color.White.copy(alpha=.14f), foreground = Color.White)
                Spacer(Modifier.weight(1f))
                CircleControl(if (muted) Icons.Rounded.VolumeOff else Icons.Rounded.Speaker) { muted = !muted }
                Spacer(Modifier.padding(4.dp))
                CircleControl(Icons.Rounded.Close, onClose)
            }

            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp),
            ) {
                AnimatedVisibility(visible = captionVisible, enter = fadeIn(IumrahMotion.vapor), exit = fadeOut(IumrahMotion.fastFade)) {
                    val scale by animateFloatAsState(if (captionVisible) 1f else .985f, IumrahMotion.softReveal, label = "caption-scale")
                    val blur by animateDpAsState(if (captionVisible) 0.dp else 13.dp, IumrahMotion.vaporDp, label = "caption-blur")
                    Column(
                        Modifier
                            .graphicsLayer { scaleX = scale; scaleY = scale; translationY = if (captionVisible) 0f else 14f }
                            .blur(blur),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(HomeEmotionalCopy.title(pager.currentPage, language), color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        HomeEmotionalCopy.subtitle(pager.currentPage, language)?.let { Text(it, color = Color.White.copy(alpha=.82f), style = MaterialTheme.typography.bodyLarge) }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    storyResources.indices.forEach { index ->
                        val indicatorWidth by animateFloatAsState(
                            targetValue = if (index == pager.currentPage) 22f else 7f,
                            animationSpec = IumrahMotion.selection,
                            label = "story-indicator-$index",
                        )
                        Box(
                            Modifier
                                .width(indicatorWidth.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Color.White.copy(alpha = if (index == pager.currentPage) .98f else .34f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleControl(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IumrahPressable(onClick = onClick, modifier = Modifier.height(44.dp).aspectRatio(1f), cornerRadius = 99.dp, background = Color.White.copy(alpha=.14f), pressedScale = .94f) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = Color.White) }
    }
}
