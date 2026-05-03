package com.example.petmeds.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScheduledAlarmDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScheduledAlarmEntity): Long

    @Query("SELECT * FROM scheduled_alarms WHERE medicationId = :medicationId")
    suspend fun listForMedication(medicationId: Long): List<ScheduledAlarmEntity>

    @Query("SELECT * FROM scheduled_alarms")
    suspend fun listAll(): List<ScheduledAlarmEntity>

    @Query("DELETE FROM scheduled_alarms WHERE medicationId = :medicationId")
    suspend fun deleteForMedication(medicationId: Long)

    @Query("DELETE FROM scheduled_alarms")
    suspend fun deleteAll()
}
