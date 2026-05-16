package com.example.petmeds.data.repo

import com.example.petmeds.data.db.CourseNoteDao
import com.example.petmeds.data.db.CourseNoteEntity
import com.example.petmeds.domain.model.CourseNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

interface CourseNoteRepository {
    fun observeForCourse(courseId: Long): Flow<List<CourseNote>>
    suspend fun listForCourse(courseId: Long): List<CourseNote>
    suspend fun findById(id: Long): CourseNote?
    suspend fun upsert(note: CourseNote): Long
    suspend fun delete(id: Long)
}

@Singleton
class CourseNoteRepositoryImpl @Inject constructor(
    private val dao: CourseNoteDao,
    private val clock: Clock,
) : CourseNoteRepository {

    override fun observeForCourse(courseId: Long): Flow<List<CourseNote>> =
        dao.observeForCourse(courseId).map { list -> list.map(CourseNoteEntity::toDomain) }

    override suspend fun listForCourse(courseId: Long): List<CourseNote> =
        dao.listForCourse(courseId).map(CourseNoteEntity::toDomain)

    override suspend fun findById(id: Long): CourseNote? =
        dao.findById(id)?.toDomain()

    override suspend fun upsert(note: CourseNote): Long {
        val now = clock.now()
        return if (note.id == 0L) {
            val withTimestamps = note.copy(createdAt = now, updatedAt = now)
            dao.insert(CourseNoteEntity.fromDomain(withTimestamps))
        } else {
            val withUpdatedAt = note.copy(updatedAt = now)
            dao.update(CourseNoteEntity.fromDomain(withUpdatedAt))
            note.id
        }
    }

    override suspend fun delete(id: Long) = dao.delete(id)
}
