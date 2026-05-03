package com.example.petmeds.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.petmeds.domain.model.DoseStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface DoseLogDao {

    @Query(
        """
        SELECT * FROM dose_logs
        WHERE (scheduledAt BETWEEN :from AND :to) OR (status = 'PENDING' AND scheduledAt < :from)
        ORDER BY COALESCE(scheduledAt, createdAt) ASC
        """
    )
    fun observeWindow(from: Instant, to: Instant): Flow<List<DoseLogEntity>>

    @Query(
        """
        SELECT * FROM dose_logs
        WHERE medicationId = :medicationId
        ORDER BY COALESCE(scheduledAt, takenAt, createdAt) DESC
        """
    )
    fun observeForMedication(medicationId: Long): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs ORDER BY COALESCE(scheduledAt, takenAt, createdAt) DESC")
    fun observeAll(): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE medicationId = :medId AND scheduledAt = :at LIMIT 1")
    suspend fun findScheduled(medId: Long, at: Instant): DoseLogEntity?

    @Query(
        """
        SELECT * FROM dose_logs
        WHERE medicationId = :medId AND status = 'TAKEN'
        ORDER BY takenAt DESC
        LIMIT 1
        """
    )
    suspend fun lastTaken(medId: Long): DoseLogEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: DoseLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DoseLogEntity): Long

    @Query("UPDATE dose_logs SET status = :status, takenAt = :takenAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DoseStatus, takenAt: Instant?)

    @Query(
        """
        UPDATE dose_logs SET status = 'MISSED'
        WHERE status = 'PENDING' AND scheduledAt IS NOT NULL AND scheduledAt < :before
        """
    )
    suspend fun markMissedBefore(before: Instant): Int
}
