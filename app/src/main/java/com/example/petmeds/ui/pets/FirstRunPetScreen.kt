package com.example.petmeds.ui.pets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petmeds.R
import com.example.petmeds.domain.model.Species

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstRunPetScreen(
    onSaved: () -> Unit,
    viewModel: FirstRunPetViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        FirstRunBody(
            padding = padding,
            name = state.name,
            species = state.species,
            saving = state.saving,
            nameError = state.nameError,
            onNameChanged = viewModel::onNameChanged,
            onSpeciesChanged = viewModel::onSpeciesChanged,
            onSubmit = viewModel::submit,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FirstRunBody(
    padding: PaddingValues,
    name: String,
    species: Species,
    saving: Boolean,
    nameError: Boolean,
    onNameChanged: (String) -> Unit,
    onSpeciesChanged: (Species) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            label = { Text(stringResource(R.string.pet_name_label)) },
            placeholder = { Text(stringResource(R.string.pet_name_placeholder)) },
            isError = nameError,
            supportingText = if (nameError) { { Text(stringResource(R.string.error_required)) } } else null,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Pet name" },
        )

        Text(stringResource(R.string.species_label), style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            speciesOptions().forEach { (sp, labelRes) ->
                FilterChip(
                    selected = species == sp,
                    onClick = { onSpeciesChanged(sp) },
                    label = { Text(stringResource(labelRes)) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onSubmit,
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            if (saving) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
            else Text(stringResource(R.string.continue_action))
        }
    }
}

private fun speciesOptions(): List<Pair<Species, Int>> = listOf(
    Species.DOG to R.string.species_dog,
    Species.CAT to R.string.species_cat,
    Species.RABBIT to R.string.species_rabbit,
    Species.BIRD to R.string.species_bird,
    Species.OTHER to R.string.species_other,
)
