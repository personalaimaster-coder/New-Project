package com.example.petmeds.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PetEntity::class,
        CourseEntity::class,
        CourseNoteEntity::class,
        MedicationEntity::class,
        DoseLogEntity::class,
        ScheduledAlarmEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun courseDao(): CourseDao
    abstract fun courseNoteDao(): CourseNoteDao
    abstract fun medicationDao(): MedicationDao
    abstract fun doseLogDao(): DoseLogDao
    abstract fun scheduledAlarmDao(): ScheduledAlarmDao

    companion object {
        const val NAME = "petmeds.db"

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS course_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        courseId INTEGER NOT NULL,
                        occurredAt INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        body TEXT NOT NULL,
                        photoPath TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(courseId) REFERENCES courses(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_course_notes_courseId ON course_notes(courseId)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_course_notes_courseId_occurredAt " +
                            "ON course_notes(courseId, occurredAt)"
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pets ADD COLUMN breed TEXT")
                db.execSQL("ALTER TABLE pets ADD COLUMN weightKg REAL")
                db.execSQL("ALTER TABLE pets ADD COLUMN birthDate TEXT")
            }
        }

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS courses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        petId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        colorIndex INTEGER NOT NULL,
                        startDate TEXT NOT NULL,
                        endDate TEXT,
                        notes TEXT,
                        status TEXT NOT NULL,
                        completedAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(petId) REFERENCES pets(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_courses_petId ON courses(petId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_courses_petId_status ON courses(petId, status)")
                db.execSQL(
                    """
                    CREATE TABLE medications_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        petId INTEGER NOT NULL,
                        courseId INTEGER,
                        name TEXT NOT NULL,
                        dosageAmount REAL NOT NULL,
                        dosageUnit TEXT NOT NULL,
                        form TEXT NOT NULL,
                        scheduleJson TEXT NOT NULL,
                        startDate TEXT NOT NULL,
                        endDate TEXT,
                        notes TEXT,
                        isActive INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        completedAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(petId) REFERENCES pets(id) ON DELETE CASCADE,
                        FOREIGN KEY(courseId) REFERENCES courses(id) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO medications_new (
                        id, petId, courseId, name, dosageAmount, dosageUnit, form,
                        scheduleJson, startDate, endDate, notes, isActive, status,
                        completedAt, createdAt
                    )
                    SELECT
                        id, petId, NULL, name, dosageAmount, dosageUnit, form,
                        scheduleJson, startDate, endDate, notes, isActive,
                        CASE WHEN isActive = 1 THEN 'ACTIVE' ELSE 'DELETED' END,
                        NULL, createdAt
                    FROM medications
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE medications")
                db.execSQL("ALTER TABLE medications_new RENAME TO medications")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_medications_petId ON medications(petId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_medications_courseId ON medications(courseId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_medications_petId_status ON medications(petId, status)")
            }
        }
    }
}
