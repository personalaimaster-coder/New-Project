package com.example.petmeds.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.petmeds.MainActivity
import com.example.petmeds.R
import com.example.petmeds.domain.model.Medication
import com.example.petmeds.domain.model.Pet
import kotlinx.datetime.Instant

object DoseNotificationFactory {

    fun build(
        context: Context,
        notificationId: Int,
        med: Medication,
        pet: Pet?,
        scheduledAt: Instant,
        doseLogId: Long,
    ): Notification {
        val title = context.getString(R.string.notif_dose_title, pet?.name ?: med.name)
        val text = context.getString(
            R.string.notif_dose_text,
            med.name,
            "${formatDose(med.dosageAmount)} ${med.dosageUnit}",
        )
        val publicVersion = NotificationCompat.Builder(context, NotificationChannels.MEDS_HIGH)
            .setSmallIcon(R.drawable.ic_notification_dose)
            .setContentTitle(context.getString(R.string.notif_dose_text_public))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val takenPi = PendingIntent.getBroadcast(
            context, notificationId.xor(0x1001),
            actionIntent(context, AlarmIntents.ACTION_DOSE_TAKEN, med.id, scheduledAt, doseLogId, notificationId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val skipPi = PendingIntent.getBroadcast(
            context, notificationId.xor(0x1002),
            actionIntent(context, AlarmIntents.ACTION_DOSE_SKIP, med.id, scheduledAt, doseLogId, notificationId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val openPi = PendingIntent.getActivity(
            context, notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(context, NotificationChannels.MEDS_HIGH)
            .setSmallIcon(R.drawable.ic_notification_dose)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text + (med.notes?.let { "\n$it" } ?: "")))
            .setContentIntent(openPi)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setGroup(NotificationChannels.MEDS_GROUP)
            .addAction(0, context.getString(R.string.notif_action_taken), takenPi)
            .addAction(0, context.getString(R.string.notif_action_skip), skipPi)
            .build()
    }

    private fun actionIntent(
        context: Context,
        action: String,
        medId: Long,
        scheduledAt: Instant,
        doseLogId: Long,
        notificationId: Int,
    ): Intent = Intent(context, DoseActionReceiver::class.java).apply {
        this.action = action
        setPackage(context.packageName)
        putExtra(AlarmIntents.EXTRA_MED_ID, medId)
        putExtra(AlarmIntents.EXTRA_SCHEDULED_AT_EPOCH_MS, scheduledAt.toEpochMilliseconds())
        putExtra(AlarmIntents.EXTRA_DOSE_LOG_ID, doseLogId)
        putExtra(AlarmIntents.EXTRA_NOTIFICATION_ID, notificationId)
    }

    private fun formatDose(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString()
        else value.toString()
}
