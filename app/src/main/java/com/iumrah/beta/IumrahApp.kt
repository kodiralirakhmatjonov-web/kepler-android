package com.iumrah.beta

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.iumrah.beta.core.design.IumrahMotion
import com.iumrah.beta.core.design.IumrahTheme
import com.iumrah.beta.ui.onboarding.OnboardingFlow
import com.iumrah.beta.ui.shell.AppShell

@Composable
fun IumrahApp() {
    val app = LocalContext.current.applicationContext as IumrahApplication
    val container = remember(app) { app.container }
    val settings by container.settingsStore.state.collectAsState()
    val chrome by container.chromeStore.state.collectAsState()

    LaunchedEffect(settings.hasCompletedOnboarding) {
        if (settings.hasCompletedOnboarding) {
            container.accountStore.restore()
        }
    }

    IumrahTheme(appearance = settings.appearance) {
        if (!settings.isLoaded) {
            LaunchSurface()
            return@IumrahTheme
        }

        AnimatedContent(
            targetState = settings.hasCompletedOnboarding,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (targetState) {
                    (fadeIn(IumrahMotion.rootFade) + scaleIn(IumrahMotion.content, initialScale = .985f))
                        .togetherWith(fadeOut(IumrahMotion.rootFade) + scaleOut(IumrahMotion.content, targetScale = 1.015f))
                } else {
                    (fadeIn(IumrahMotion.rootFade) + scaleIn(IumrahMotion.content, initialScale = 1.015f))
                        .togetherWith(fadeOut(IumrahMotion.fastFade) + scaleOut(IumrahMotion.content, targetScale = .985f))
                }
            },
            label = "onboarding-root-transition",
        ) { completed ->
            if (completed) {
                AppShell(
                    language = settings.language,
                    chrome = container.chromeStore,
                    chromeState = chrome,
                    accountStore = container.accountStore,
                )
            } else {
                OnboardingFlow(
                    language = settings.language,
                    onFinished = container.settingsStore::completeOnboarding,
                )
            }
        }
    }
}

@Composable
private fun LaunchSurface() {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(if (androidx.compose.foundation.isSystemInDarkTheme()) R.drawable.iumrah_header_wordmark_light else R.drawable.iumrah_header_wordmark_dark),
            contentDescription = "iumrah",
            modifier = Modifier.height(34.dp),
            contentScale = ContentScale.Fit,
        )
    }
}
