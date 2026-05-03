package com.example.petmeds.ui.pets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petmeds.domain.model.Species
import com.example.petmeds.ui.common.PawPillWordmark
import com.example.petmeds.ui.common.PetAvatar
import com.example.petmeds.ui.theme.Brand
import com.example.petmeds.ui.theme.BrandGradients

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetProfileScreen(
    onNavigateToSettings: () -> Unit,
    onEditPet: () -> Unit,
    viewModel: PetProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            val pet = state.pet
            if (pet != null) {
                PetHeroCard(pet = pet, onEdit = onEditPet)
                StatsRow(
                    activeMeds = state.activeMeds,
                    daysOnApp = state.daysOnApp,
                )
                QuickActionsCard(onSettings = onNavigateToSettings)
            } else {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("No pet found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun PetHeroCard(pet: com.example.petmeds.domain.model.Pet, onEdit: () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .background(BrandGradients.petHero())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box {
                PetAvatar(
                    name = pet.name,
                    photoPath = pet.photoPath,
                    species = pet.species,
                    sizeDp = 96,
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp),
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit pet",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            Text(
                pet.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Brand.DarkBlue,
            )
            val (chipBg, chipFg) = speciesChipColors(pet.species)
            Surface(
                shape = RoundedCornerShape(50),
                color = chipBg,
            ) {
                Text(
                    speciesLabel(pet.species),
                    style = MaterialTheme.typography.labelMedium,
                    color = chipFg,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            // Additional details row
            val details = buildList {
                pet.breed?.takeIf { it.isNotBlank() }?.let { add(it) }
                pet.weightKg?.let { w ->
                    val label = if (w == w.toLong().toFloat()) "${w.toLong()} kg" else "$w kg"
                    add(label)
                }
                pet.birthDate?.let { add("Born $it") }
            }
            if (details.isNotEmpty()) {
                Text(
                    details.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatsRow(activeMeds: Int, daysOnApp: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(
            label = "Active meds",
            value = "$activeMeds",
            icon = Icons.Filled.Medication,
            iconBg = Brand.Coral.copy(alpha = 0.18f),
            iconTint = Brand.Coral,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "Days tracked",
            value = "$daysOnApp",
            icon = Icons.Filled.Pets,
            iconBg = Brand.Sage.copy(alpha = 0.32f),
            iconTint = Brand.SageDark,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier,
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Brand.DarkBlue,
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickActionsCard(onSettings: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            QuickActionRow(
                icon = Icons.Filled.Settings,
                label = "Settings & permissions",
                onClick = onSettings,
            )
        }
    }
}

@Composable
private fun QuickActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun speciesLabel(species: Species): String = when (species) {
    Species.DOG    -> "🐶 Dog"
    Species.CAT    -> "🐱 Cat"
    Species.RABBIT -> "🐰 Rabbit"
    Species.BIRD   -> "🐦 Bird"
    Species.OTHER  -> "🐾 Other"
}

private fun speciesChipColors(species: Species): Pair<Color, Color> = when (species) {
    Species.DOG    -> Brand.Sage     to Brand.SageOn
    Species.CAT    -> Brand.Lavender to Brand.LavenderOn
    Species.RABBIT -> Brand.Apricot  to Brand.CoralOn
    Species.BIRD   -> Brand.SkyBlue  to Brand.DarkBlue
    Species.OTHER  -> Brand.Cream    to Brand.DarkBlue
}
