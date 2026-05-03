package com.example.petmeds.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Re-schedules all medication alarms after events that invalidate them:
 *  - BOOT_COMPLETED: alarms don't survive reboot.
 *  - MY_PACKAGE_REPLACED: alarms cleared on update.
 *  - TIMEZONE_CHANGED / LOCALE_CHANGED: wall-clock semantics shift.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val supported = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
        )
        if (intent.action !in supported) return
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<RescheduleAllWorker>().build())
    }
}
