package com.iumrah.beta.ui.components
import com.iumrah.beta.ui.cupertino.Icon

import com.iumrah.beta.ui.cupertino.CupertinoSymbol

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iumrah.beta.R

/** One UI-inspired root header with real system-bar inset handling. */
@Composable
fun IumrahRootPageHeader(
    title: String,
    chrome: com.iumrah.beta.core.navigation.AppChromeStore,
    modifier: Modifier = Modifier,
    usesBrandLogo: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (usesBrandLogo) {
            Image(
                painter = painterResource(
                    if (isSystemInDarkTheme()) R.drawable.iumrah_header_wordmark_light
                    else R.drawable.iumrah_header_wordmark_dark,
                ),
                contentDescription = "iumrah",
                modifier = Modifier.fillMaxWidth(.43f).height(31.dp),
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
            )
        } else {
            Text(
                title,
                fontSize = 38.sp,
                lineHeight = 42.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = (-1.0).sp,
                maxLines = 1,
            )
        }

        Spacer(Modifier.weight(1f))

        IumrahPressable(
            onClick = chrome::openSidebar,
            modifier = Modifier.size(46.dp),
            cornerRadius = 23.dp,
            background = MaterialTheme.colorScheme.surfaceVariant,
            pressedScale = .93f,
        ) {
            Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                Icon(CupertinoSymbol.Menu, contentDescription = "Menu", modifier = Modifier.size(21.dp))
            }
        }
    }
}
