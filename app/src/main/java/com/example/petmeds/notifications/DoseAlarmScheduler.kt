package com.example.petmeds.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import com.example.petmeds.MainActivity
import com.example.petmeds.data.db.ScheduledAlarmDao
import com.example.petmeds.data.db.ScheduledAlarmEntity
import com.example.petmeds.data.repo.MedicationRepository
import com.example.petmeds.domain.schedule.DoseScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Wraps AlarmManager to schedule the next horizon of doses for a medication.
 *
 * Reliability strategy:
 *  - Uses [AlarmManager.setAlarmClock] which is exempt from Doze.
 *  - Persists each PendingIntent's [requestCode] so we can cancel cleanly when
 *    the medication is edited or deleted.
 *  - Top-up is invoked after each fire so we always have HORIZON_DAYS forward.
 */
@Singleton
class DoseAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val medicationRepository: MedicationRepository,
    private val scheduledAlarmDao: ScheduledAlarmDao,
    private val doseScheduler: DoseScheduler,
    private val clock: Clock,
) {
    companion object {
        const val HORIZON_DAYS = 2
        private const val TAG = "DoseAlarmScheduler"
    }

    private val alarmManager: AlarmManager? get() = context.getSystemService()

    /** Cancels all alarms for [medicationId] and schedules the next horizon. */
    suspend fun rescheduleFor(medicationId: Long) {
        cancelFor(medicationId)
        val med = medicationRepository.findById(medicationId) ?: return
        if (!med.isActive) return
        val instants = doseScheduler.computeUpcoming(med, clock.now(), HORIZON_DAYS)
        instants.forEach { schedule(medicationId, it) }
        Log.d(TAG, "Scheduled ${instants.size} alarms for med $medicationId")
    }

    /** Re-plans all active medications (called from BootReceiver / TZ change). */
    suspend fun rescheduleAll() {
        cancelAll()
        medicationRepository.listActive().forEach { med ->
            doseScheduler.computeUpcoming(med, clock.now(), HORIZON_DAYS)
                .forEach { schedule(med.id, it) }
        }
    }

    suspend fun cancelFor(medicationId: Long) {
        scheduledAlarmDao.listForMedication(medicationId).forEach { record ->
            cancelByRequestCode(record.medicationId, record.requestCode, record.firesAt)
        }
        scheduledAlarmDao.deleteForMedication(medicationId)
    }

    suspend fun cancelAll() {
        scheduledAlarmDao.listAll().forEach { record ->
            cancelByRequestCode(record.medicationId, record.requestCode, record.firesAt)
        }
        scheduledAlarmDao.deleteAll()
    }

    private suspend fun schedule(medicationId: Long, at: Instant) {
        val am = alarmManager ?: return
        val requestCode = newRequestCode()
        val intent = doseDueIntent(medicationId, at, requestCode)
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        scheduledAlarmDao.insert(
            ScheduledAlarmEntity(medicationId = medicationId, requestCode = requestCode, firesAt = at)
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilliseconds(), pi)
                Log.w(TAG, "Exact alarms denied; falling back to inexact for med $medicationId")
                return
            }
            val showIntent = PendingIntent.getActivity(
                context, requestCode,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            am.setAlarmClock(
                AlarmManager.AlarmClockInfo(at.toEpochMilliseconds(), showIntent),
                pi,
            )
        } catch (se: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact alarm; falling back", se)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilliseconds(), pi)
        }
    }

    private fun cancelByRequestCode(medicationId: Long, requestCode: Int, firesAt: Instant) {
        val intent = doseDueIntent(medicationId, firesAt, requestCode)
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        ) ?: return
        alarmManager?.cancel(pi)
        pi.cancel()
    }

    private fun doseDueIntent(medicationId: Long, at: Instant, requestCode: Int): Intent =
        Intent(context, DoseAlarmReceiver::class.java).apply {
            action = AlarmIntents.ACTION_DOSE_DUE
            setPackage(context.packageName)
            putExtra(AlarmIntents.EXTRA_MED_ID, medicationId)
            putExtra(AlarmIntents.EXTRA_SCHEDULED_AT_EPOCH_MS, at.toEpochMilliseconds())
            putExtra(AlarmIntents.EXTRA_NOTIFICATION_ID, requestCode)
        }

    private fun newRequestCode(): Int = Random.nextInt(1, Int.MAX_VALUE)
}
