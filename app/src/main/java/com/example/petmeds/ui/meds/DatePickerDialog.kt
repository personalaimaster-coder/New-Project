package com.example.petmeds.ui.meds

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.petmeds.R
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    allowClear: Boolean = false,
    onClear: (() -> Unit)? = null,
) {
    val tz = TimeZone.UTC
    val initialMs = LocalDateTime(initial, LocalTime(12, 0)).toInstant(tz).toEpochMilliseconds()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMs)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val ms = state.selectedDateMillis ?: initialMs
                val date = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC).date
                onConfirm(date)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            if (allowClear && onClear != null) {
                TextButton(onClick = onClear) { Text(stringResource(R.string.delete)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    ) {
        DatePicker(state = state, showModeToggle = false)
    }
}
