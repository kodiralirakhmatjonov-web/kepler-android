package com.iumrah.beta.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.iumrah.beta.R
import com.iumrah.beta.core.navigation.AppChromeStore

/** Native Compose counterpart of IumrahRootPageTitle.swift. */
@Composable
fun IumrahRootPageHeader(
    title: String,
    chrome: AppChromeStore,
    modifier: Modifier = Modifier,
    usesBrandLogo: Boolean = false,
) {
    Row(modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        if (usesBrandLogo) {
            Image(
                painter = painterResource(if (isSystemInDarkTheme()) R.drawable.iumrah_header_wordmark_light else R.drawable.iumrah_header_wordmark_dark),
                contentDescription = "iumrah",
                modifier = Modifier.fillMaxWidth(.48f).height(38.dp),
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
            )
        } else {
            Text(title, style = MaterialTheme.typography.headlineLarge, maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        IumrahPressable(
            onClick = chrome::openSidebar,
            modifier = Modifier.size(46.dp),
            cornerRadius = 99.dp,
            background = MaterialTheme.colorScheme.surface,
            pressedScale = .92f,
            shadowElevation = 2.dp,
        ) {
            Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Menu, contentDescription = "Menu", modifier = Modifier.size(20.dp))
            }
        }
    }
}
