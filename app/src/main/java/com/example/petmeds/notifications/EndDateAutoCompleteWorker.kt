package com.example.petmeds.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.petmeds.data.repo.MedicationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@HiltWorker
class EndDateAutoCompleteWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: DoseAlarmScheduler,
    private val clock: Clock,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        val today = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        medicationRepository.listActive()
            .filter { med -> med.endDate?.let { it < today } == true }
            .forEach { med ->
                medicationRepository.markCompleted(med.id, clock.now())
                alarmScheduler.cancelFor(med.id)
            }
        Result.success()
    } catch (t: Throwable) {
        Result.retry()
    }
}
