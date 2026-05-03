package com.example.petmeds.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.petmeds.data.repo.DoseLogRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Handles notification action buttons: Mark taken, Skip. */
@AndroidEntryPoint
class DoseActionReceiver : BroadcastReceiver() {

    @Inject lateinit var doseLogRepository: DoseLogRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val doseLogId = intent.getLongExtra(AlarmIntents.EXTRA_DOSE_LOG_ID, -1)
        val notificationId = intent.getIntExtra(AlarmIntents.EXTRA_NOTIFICATION_ID, -1)
        if (doseLogId <= 0) return
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    AlarmIntents.ACTION_DOSE_TAKEN -> doseLogRepository.markTaken(doseLogId)
                    AlarmIntents.ACTION_DOSE_SKIP -> doseLogRepository.markSkipped(doseLogId)
                }
                if (notificationId >= 0) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
