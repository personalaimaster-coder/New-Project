package com.example.petmeds.data.repo

import com.example.petmeds.data.db.MedicationDao
import com.example.petmeds.data.db.MedicationEntity
import com.example.petmeds.domain.model.LifecycleStatus
import com.example.petmeds.domain.model.Medication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface MedicationRepository {
    fun observeActive(): Flow<List<Medication>>
    fun observeCompleted(): Flow<List<Medication>>
    fun observeByCourse(courseId: Long): Flow<List<Medication>>
    suspend fun listActive(): List<Medication>
    suspend fun listActiveForCourse(courseId: Long): List<Medication>
    suspend fun findById(id: Long): Medication?
    suspend fun upsert(med: Medication): Long
    suspend fun markCompleted(id: Long, completedAt: Instant)
    suspend fun reactivate(id: Long)
    suspend fun softDelete(id: Long)
}

@Singleton
class MedicationRepositoryImpl @Inject constructor(
    private val dao: MedicationDao,
    private val json: Json,
) : MedicationRepository {

    override fun observeActive(): Flow<List<Medication>> =
        dao.observeActive().map { list -> list.map { it.toDomain(json) } }

    override fun observeCompleted(): Flow<List<Medication>> =
        dao.observeCompleted().map { list -> list.map { it.toDomain(json) } }

    override fun observeByCourse(courseId: Long): Flow<List<Medication>> =
        dao.observeByCourse(courseId).map { list -> list.map { it.toDomain(json) } }

    override suspend fun listActive(): List<Medication> =
        dao.listActive().map { it.toDomain(json) }

    override suspend fun listActiveForCourse(courseId: Long): List<Medication> =
        dao.listActiveForCourse(courseId).map { it.toDomain(json) }

    override suspend fun findById(id: Long): Medication? =
        dao.findById(id)?.toDomain(json)

    override suspend fun upsert(med: Medication): Long {
        val entity = MedicationEntity.fromDomain(med, json)
        return if (med.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            med.id
        }
    }

    override suspend fun markCompleted(id: Long, completedAt: Instant) =
        dao.setStatus(id, LifecycleStatus.COMPLETED, completedAt)

    override suspend fun reactivate(id: Long) =
        dao.setStatus(id, LifecycleStatus.ACTIVE, null)

    override suspend fun softDelete(id: Long) = dao.softDelete(id)
}
