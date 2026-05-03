package com.example.petmeds.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.petmeds.domain.model.Pet
import com.example.petmeds.domain.model.Species
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val species: Species,
    val photoPath: String?,
    val breed: String? = null,
    val weightKg: Float? = null,
    val birthDate: LocalDate? = null,
    val createdAt: Instant,
) {
    fun toDomain(): Pet = Pet(
        id = id,
        name = name,
        species = species,
        photoPath = photoPath,
        breed = breed,
        weightKg = weightKg,
        birthDate = birthDate,
        createdAt = createdAt,
    )

    companion object {
        fun fromDomain(p: Pet): PetEntity = PetEntity(
            id = p.id,
            name = p.name,
            species = p.species,
            photoPath = p.photoPath,
            breed = p.breed,
            weightKg = p.weightKg,
            birthDate = p.birthDate,
            createdAt = p.createdAt,
        )
    }
}
