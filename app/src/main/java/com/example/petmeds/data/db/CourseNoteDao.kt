package com.example.petmeds.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseNoteDao {

    @Query(
        """
        SELECT * FROM course_notes
        WHERE courseId = :courseId
        ORDER BY occurredAt DESC, id DESC
        """
    )
    fun observeForCourse(courseId: Long): Flow<List<CourseNoteEntity>>

    @Query("SELECT * FROM course_notes WHERE courseId = :courseId ORDER BY occurredAt DESC, id DESC")
    suspend fun listForCourse(courseId: Long): List<CourseNoteEntity>

    @Query("SELECT * FROM course_notes WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): CourseNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: CourseNoteEntity): Long

    @Update
    suspend fun update(note: CourseNoteEntity)

    @Query("DELETE FROM course_notes WHERE id = :id")
    suspend fun delete(id: Long)
}
