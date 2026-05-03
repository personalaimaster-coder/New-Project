package com.example.petmeds.data.repo

import com.example.petmeds.data.db.DoseLogDao
import com.example.petmeds.data.db.DoseLogEntity
import com.example.petmeds.domain.model.DoseLog
import com.example.petmeds.domain.model.DoseStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface DoseLogRepository {
    fun observeWindow(from: Instant, to: Instant): Flow<List<DoseLog>>
    fun observeForMedication(medicationId: Long): Flow<List<DoseLog>>
    fun observeAll(): Flow<List<DoseLog>>
    suspend fun ensurePending(medicationId: Long, scheduledAt: Instant): Long
    suspend fun markTaken(id: Long, at: Instant = Clock.System.now())
    suspend fun markSkipped(id: Long)
    suspend fun logPrn(medicationId: Long, at: Instant = Clock.System.now()): Long
    suspend fun lastTaken(medicationId: Long): DoseLog?
    suspend fun markMissedBefore(before: Instant): Int
}

@Singleton
class DoseLogRepositoryImpl @Inject constructor(
    private val dao: DoseLogDao,
    private val clock: Clock,
) : DoseLogRepository {

    override fun observeWindow(from: Instant, to: Instant): Flow<List<DoseLog>> =
        dao.observeWindow(from, to).map { list -> list.map(DoseLogEntity::toDomain) }

    override fun observeForMedication(medicationId: Long): Flow<List<DoseLog>> =
        dao.observeForMedication(medicationId).map { list -> list.map(DoseLogEntity::toDomain) }

    override fun observeAll(): Flow<List<DoseLog>> =
        dao.observeAll().map { list -> list.map(DoseLogEntity::toDomain) }

    override suspend fun ensurePending(medicationId: Long, scheduledAt: Instant): Long {
        dao.findScheduled(medicationId, scheduledAt)?.let { return it.id }
        val id = dao.insertIgnore(
            DoseLogEntity(
                medicationId = medicationId,
                scheduledAt = scheduledAt,
                takenAt = null,
                status = DoseStatus.PENDING,
                createdAt = clock.now(),
            )
        )
        return if (id == -1L) dao.findScheduled(medicationId, scheduledAt)!!.id else id
    }

    override suspend fun markTaken(id: Long, at: Instant) {
        dao.updateStatus(id, DoseStatus.TAKEN, at)
    }

    override suspend fun markSkipped(id: Long) {
        dao.updateStatus(id, DoseStatus.SKIPPED, null)
    }

    override suspend fun logPrn(medicationId: Long, at: Instant): Long = dao.upsert(
        DoseLogEntity(
            medicationId = medicationId,
            scheduledAt = null,
            takenAt = at,
            status = DoseStatus.TAKEN,
            createdAt = clock.now(),
        )
    )

    override suspend fun lastTaken(medicationId: Long): DoseLog? =
        dao.lastTaken(medicationId)?.toDomain()

    override suspend fun markMissedBefore(before: Instant): Int =
        dao.markMissedBefore(before)
}
