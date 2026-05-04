package com.example.petmeds.ui.meds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.petmeds.domain.model.MedicineReference
import kotlinx.coroutines.delay

@Composable
fun MedicineAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<MedicineReference>,
    onSearch: (String) -> Unit,
    onSuggestionSelected: (MedicineReference) -> Unit,
    onInfoClick: (MedicineReference) -> Unit,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // True while the user has typed something and hasn't picked a suggestion yet.
    var searchActive by remember { mutableStateOf(false) }

    // LaunchedEffect restarts every time `value` changes, so the 300 ms delay
    // naturally acts as a debounce — previous launches are cancelled automatically.
    LaunchedEffect(value) {
        if (searchActive && value.length >= 2) {
            delay(300L)
            onSearch(value)
        } else {
            onSearch("") // clear suggestions
        }
    }

    val showSuggestions = searchActive && suggestions.isNotEmpty()

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { v ->
                searchActive = true
                onValueChange(v)
            },
            label = { Text("Medication name") },
            placeholder = { Text("e.g. Bravecto, Melonex…") },
            isError = isError,
            supportingText = if (isError) {
                { Text("Required") }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        AnimatedVisibility(visible = showSuggestions) {
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp),
                ) {
                    items(suggestions, key = { it.brandName + it.composition }) { med ->
                        SuggestionRow(
                            medicine = med,
                            onClick = {
                                searchActive = false
                                onSuggestionSelected(med)
                            },
                            onInfoClick = { onInfoClick(med) },
                        )
                        if (med != suggestions.last()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    medicine: MedicineReference,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = medicine.brandName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${medicine.genericName} · ${medicine.manufacturer}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = onInfoClick,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = "More info about ${medicine.brandName}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
