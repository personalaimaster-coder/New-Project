package com.example.petmeds.di

import com.example.petmeds.data.repo.CourseRepository
import com.example.petmeds.data.repo.CourseRepositoryImpl
import com.example.petmeds.data.repo.DoseLogRepository
import com.example.petmeds.data.repo.DoseLogRepositoryImpl
import com.example.petmeds.data.repo.MedicationRepository
import com.example.petmeds.data.repo.MedicationRepositoryImpl
import com.example.petmeds.data.repo.PetRepository
import com.example.petmeds.data.repo.PetRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindPetRepository(impl: PetRepositoryImpl): PetRepository
    @Binds abstract fun bindCourseRepository(impl: CourseRepositoryImpl): CourseRepository
    @Binds abstract fun bindMedicationRepository(impl: MedicationRepositoryImpl): MedicationRepository
    @Binds abstract fun bindDoseLogRepository(impl: DoseLogRepositoryImpl): DoseLogRepository
}
