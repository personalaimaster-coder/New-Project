package com.example.petmeds.data.repo

import com.example.petmeds.data.db.PetDao
import com.example.petmeds.data.db.PetEntity
import com.example.petmeds.domain.model.Pet
import com.example.petmeds.domain.model.Species
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface PetRepository {
    fun observeAll(): Flow<List<Pet>>
    fun observeCount(): Flow<Int>
    suspend fun create(
        name: String,
        species: Species,
        photoPath: String? = null,
        breed: String? = null,
        weightKg: Float? = null,
        birthDate: LocalDate? = null,
    ): Long
    suspend fun update(pet: Pet)
    suspend fun delete(id: Long)
    suspend fun findById(id: Long): Pet?
}

@Singleton
class PetRepositoryImpl @Inject constructor(
    private val dao: PetDao,
    private val clock: Clock,
) : PetRepository {

    override fun observeAll(): Flow<List<Pet>> =
        dao.observeAll().map { list -> list.map(PetEntity::toDomain) }

    override fun observeCount(): Flow<Int> = dao.observeCount()

    override suspend fun create(
        name: String,
        species: Species,
        photoPath: String?,
        breed: String?,
        weightKg: Float?,
        birthDate: LocalDate?,
    ): Long = dao.insert(
        PetEntity(
            name = name,
            species = species,
            photoPath = photoPath,
            breed = breed,
            weightKg = weightKg,
            birthDate = birthDate,
            createdAt = clock.now(),
        )
    )

    override suspend fun update(pet: Pet) = dao.update(PetEntity.fromDomain(pet))
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun findById(id: Long): Pet? = dao.findById(id)?.toDomain()
}
