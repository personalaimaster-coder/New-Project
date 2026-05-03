package com.example.petmeds.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.petmeds.domain.model.Species
import com.example.petmeds.ui.theme.Brand
import com.example.petmeds.ui.theme.MedicationAccents

/** Circular pet avatar — photo if available, species silhouette otherwise. */
@Composable
fun PetAvatar(
    name: String,
    photoPath: String? = null,
    species: Species? = null,
    sizeDp: Int = 40,
    modifier: Modifier = Modifier,
) {
    val size = sizeDp.dp
    if (photoPath != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoPath)
                .crossfade(true)
                .build(),
            contentDescription = "$name photo",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
        )
    } else {
        SpeciesFallback(name = name, species = species, size = size, modifier = modifier)
    }
}

@Composable
private fun SpeciesFallback(
    name: String,
    species: Species?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val bgColor = when (species) {
        Species.DOG    -> Brand.Sage
        Species.CAT    -> Brand.Lavender
        Species.RABBIT -> Brand.Apricot
        Species.BIRD   -> Brand.SkyBlue
        Species.OTHER  -> Brand.Cream
        null           -> MedicationAccents[(name.hashCode() and 0x7FFFFFFF) % MedicationAccents.size]
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        if (size >= 36.dp) {
            val emoji = when (species) {
                Species.DOG    -> "🐶"
                Species.CAT    -> "🐱"
                Species.RABBIT -> "🐰"
                Species.BIRD   -> "🐦"
                Species.OTHER  -> "🐾"
                null           -> name.firstOrNull()?.uppercase() ?: "?"
            }
            val fontSize = (size.value * 0.42f).sp
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Pets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.5f),
            )
        }
    }
}
