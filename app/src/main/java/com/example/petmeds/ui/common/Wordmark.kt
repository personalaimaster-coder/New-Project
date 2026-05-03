package com.example.petmeds.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petmeds.ui.theme.Brand
import com.example.petmeds.ui.theme.BrandGradients

/**
 * Branded PawPill wordmark — a circular gradient paw badge plus a two-tone
 * "Paw" + "Pill" lockup. Used in top bars across the app to give the brand
 * presence without leaning on the launcher icon.
 */
@Composable
fun PawPillWordmark(
    modifier: Modifier = Modifier,
    badgeSize: Dp = 34.dp,
    textSize: Int = 22,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(badgeSize)
                .clip(CircleShape)
                .background(BrandGradients.Badge),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Pets,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(badgeSize * 0.55f),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Paw",
                color = Brand.DarkBlue,
                fontWeight = FontWeight.ExtraBold,
                fontSize = textSize.sp,
                letterSpacing = (-0.5).sp,
            )
            Text(
                "Pill",
                color = Brand.Coral,
                fontWeight = FontWeight.ExtraBold,
                fontSize = textSize.sp,
                letterSpacing = (-0.5).sp,
            )
        }
    }
}
