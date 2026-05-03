package com.example.petmeds.data.repo

import com.example.petmeds.data.db.CourseDao
import com.example.petmeds.data.db.CourseEntity
import com.example.petmeds.domain.model.Course
import com.example.petmeds.domain.model.LifecycleStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface CourseRepository {
    fun observeActive(): Flow<List<Course>>
    fun observeCompleted(): Flow<List<Course>>
    suspend fun findById(id: Long): Course?
    suspend fun upsert(course: Course): Long
    suspend fun markCompleted(id: Long, completedAt: Instant)
    suspend fun reactivate(id: Long)
    suspend fun softDelete(id: Long)
}

@Singleton
class CourseRepositoryImpl @Inject constructor(
    private val dao: CourseDao,
) : CourseRepository {

    override fun observeActive(): Flow<List<Course>> =
        dao.observeActive().map { list -> list.map(CourseEntity::toDomain) }

    override fun observeCompleted(): Flow<List<Course>> =
        dao.observeCompleted().map { list -> list.map(CourseEntity::toDomain) }

    override suspend fun findById(id: Long): Course? = dao.findById(id)?.toDomain()

    override suspend fun upsert(course: Course): Long {
        val entity = CourseEntity.fromDomain(course)
        return if (course.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            course.id
        }
    }

    override suspend fun markCompleted(id: Long, completedAt: Instant) =
        dao.setStatus(id, LifecycleStatus.COMPLETED, completedAt)

    override suspend fun reactivate(id: Long) =
        dao.setStatus(id, LifecycleStatus.ACTIVE, null)

    override suspend fun softDelete(id: Long) =
        dao.setStatus(id, LifecycleStatus.DELETED, null)
}
