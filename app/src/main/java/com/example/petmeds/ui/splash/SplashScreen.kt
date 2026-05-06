package com.example.petmeds.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.petmeds.ui.common.PawPillWordmark
import com.example.petmeds.ui.theme.Brand

/**
 * Brand-only landing surface shown while the app decides whether the user
 * needs onboarding or can go straight to the timeline. Renders only the
 * wordmark on the cream brand canvas — never a spinner — so the very first
 * frame after launch already feels like the app, not a load screen.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brand.Cream)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PawPillWordmark(badgeSize = 56.dp, textSize = 36)
        }
    }
}
