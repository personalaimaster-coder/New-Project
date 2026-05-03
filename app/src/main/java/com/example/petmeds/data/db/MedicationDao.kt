package com.example.petmeds.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.petmeds.domain.model.LifecycleStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface MedicationDao {

    @Query("SELECT * FROM medications WHERE status = 'ACTIVE' ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE petId = :petId AND status = 'ACTIVE' ORDER BY createdAt ASC")
    fun observeActiveForPet(petId: Long): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun observeCompleted(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): MedicationEntity?

    @Query("SELECT * FROM medications WHERE status = 'ACTIVE'")
    suspend fun listActive(): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE courseId = :courseId AND status != 'DELETED' ORDER BY createdAt ASC")
    fun observeByCourse(courseId: Long): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE courseId = :courseId AND status = 'ACTIVE' ORDER BY createdAt ASC")
    suspend fun listActiveForCourse(courseId: Long): List<MedicationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(med: MedicationEntity): Long

    @Update
    suspend fun update(med: MedicationEntity)

    @Query("UPDATE medications SET status = :status, completedAt = :completedAt, isActive = CASE WHEN :status = 'ACTIVE' THEN 1 ELSE 0 END WHERE id = :id")
    suspend fun setStatus(id: Long, status: LifecycleStatus, completedAt: Instant?)

    @Query("UPDATE medications SET status = 'DELETED', completedAt = NULL, isActive = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)
}
