package com.example.petmeds.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {

    @Query("SELECT * FROM pets ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<PetEntity>>

    @Query("SELECT COUNT(*) FROM pets")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM pets WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): PetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pet: PetEntity): Long

    @Update
    suspend fun update(pet: PetEntity)

    @Query("DELETE FROM pets WHERE id = :id")
    suspend fun delete(id: Long)
}
