package com.example.petmeds.di

import android.content.Context
import androidx.room.Room
import com.example.petmeds.data.db.AppDatabase
import com.example.petmeds.data.db.CourseDao
import com.example.petmeds.data.db.CourseNoteDao
import com.example.petmeds.data.db.DoseLogDao
import com.example.petmeds.data.db.MedicationDao
import com.example.petmeds.data.db.PetDao
import com.example.petmeds.data.db.ScheduledAlarmDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
            )
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun providePetDao(db: AppDatabase): PetDao = db.petDao()
    @Provides fun provideCourseDao(db: AppDatabase): CourseDao = db.courseDao()
    @Provides fun provideCourseNoteDao(db: AppDatabase): CourseNoteDao = db.courseNoteDao()
    @Provides fun provideMedicationDao(db: AppDatabase): MedicationDao = db.medicationDao()
    @Provides fun provideDoseLogDao(db: AppDatabase): DoseLogDao = db.doseLogDao()
    @Provides fun provideScheduledAlarmDao(db: AppDatabase): ScheduledAlarmDao = db.scheduledAlarmDao()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.System
}
