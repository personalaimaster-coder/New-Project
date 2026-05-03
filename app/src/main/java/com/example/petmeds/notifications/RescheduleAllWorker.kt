package com.example.petmeds.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.petmeds.data.repo.DoseLogRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.datetime.Clock

/**
 * Recomputes and schedules alarms for all active medications. Triggered after
 * boot, package replace, timezone change, and as a top-up after each fire.
 */
@HiltWorker
class RescheduleAllWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val alarmScheduler: DoseAlarmScheduler,
    private val doseLogRepository: DoseLogRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        doseLogRepository.markMissedBefore(Clock.System.now())
        alarmScheduler.rescheduleAll()
        Result.success()
    } catch (t: Throwable) {
        Result.retry()
    }
}
