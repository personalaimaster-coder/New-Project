package com.example.petmeds.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.petmeds.data.repo.DoseLogRepository
import com.example.petmeds.data.repo.MedicationRepository
import com.example.petmeds.data.repo.PetRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * Fires when an exact alarm goes off. Inserts a PENDING dose log row, posts the
 * notification, and enqueues a WorkManager job to top up the schedule horizon.
 */
@AndroidEntryPoint
class DoseAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var medicationRepository: MedicationRepository
    @Inject lateinit var petRepository: PetRepository
    @Inject lateinit var doseLogRepository: DoseLogRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmIntents.ACTION_DOSE_DUE) return
        val medId = intent.getLongExtra(AlarmIntents.EXTRA_MED_ID, -1)
        val scheduledMs = intent.getLongExtra(AlarmIntents.EXTRA_SCHEDULED_AT_EPOCH_MS, -1)
        val notificationId = intent.getIntExtra(AlarmIntents.EXTRA_NOTIFICATION_ID, -1)
        if (medId <= 0 || scheduledMs <= 0 || notificationId < 0) {
            Log.w(TAG, "Bad intent extras; ignoring")
            return
        }
        val scheduledAt = Instant.fromEpochMilliseconds(scheduledMs)
        val pending = goAsync()
        scope.launch {
            try {
                NotificationChannels.ensure(context)
                val med = medicationRepository.findById(medId) ?: run {
                    Log.w(TAG, "Med $medId not found"); return@launch
                }
                if (!med.isActive) return@launch
                val pet = petRepository.findById(med.petId)
                val doseLogId = doseLogRepository.ensurePending(medId, scheduledAt)

                val notification = DoseNotificationFactory.build(
                    context, notificationId, med, pet, scheduledAt, doseLogId,
                )
                if (canPostNotifications(context)) {
                    NotificationManagerCompat.from(context).notify(notificationId, notification)
                }

                WorkManager.getInstance(context)
                    .enqueue(OneTimeWorkRequestBuilder<RescheduleAllWorker>().build())
                WorkManager.getInstance(context)
                    .enqueue(OneTimeWorkRequestBuilder<EndDateAutoCompleteWorker>().build())
            } catch (t: Throwable) {
                Log.e(TAG, "Error processing dose alarm", t)
            } finally {
                pending.finish()
            }
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object { private const val TAG = "DoseAlarmReceiver" }
}
