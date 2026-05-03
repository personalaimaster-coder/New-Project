package com.example.petmeds.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.example.petmeds.R

object NotificationChannels {
    const val MEDS_HIGH = "meds_high"
    const val MEDS_GROUP = "meds_group_today"

    fun ensure(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        if (nm.getNotificationChannel(MEDS_HIGH) == null) {
            val ch = NotificationChannel(
                MEDS_HIGH,
                context.getString(R.string.channel_meds_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_meds_description)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }
            nm.createNotificationChannel(ch)
        }
    }
}
