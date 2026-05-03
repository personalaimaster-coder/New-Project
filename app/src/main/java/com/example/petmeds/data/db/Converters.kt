package com.example.petmeds.data.db

import androidx.room.TypeConverter
import com.example.petmeds.domain.model.DoseStatus
import com.example.petmeds.domain.model.LifecycleStatus
import com.example.petmeds.domain.model.MedForm
import com.example.petmeds.domain.model.Species
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

class Converters {

    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilliseconds()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun speciesToString(value: Species?): String? = value?.name

    @TypeConverter
    fun stringToSpecies(value: String?): Species? = value?.let { Species.valueOf(it) }

    @TypeConverter
    fun medFormToString(value: MedForm?): String? = value?.name

    @TypeConverter
    fun stringToMedForm(value: String?): MedForm? = value?.let { MedForm.valueOf(it) }

    @TypeConverter
    fun doseStatusToString(value: DoseStatus?): String? = value?.name

    @TypeConverter
    fun stringToDoseStatus(value: String?): DoseStatus? = value?.let { DoseStatus.valueOf(it) }

    @TypeConverter
    fun lifecycleStatusToString(value: LifecycleStatus?): String? = value?.name

    @TypeConverter
    fun stringToLifecycleStatus(value: String?): LifecycleStatus? =
        value?.let { LifecycleStatus.valueOf(it) }
}
