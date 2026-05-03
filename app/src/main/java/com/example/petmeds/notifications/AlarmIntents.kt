package com.example.petmeds.notifications

object AlarmIntents {
    const val ACTION_DOSE_DUE = "com.example.petmeds.ACTION_DOSE_DUE"
    const val ACTION_DOSE_TAKEN = "com.example.petmeds.ACTION_DOSE_TAKEN"
    const val ACTION_DOSE_SKIP = "com.example.petmeds.ACTION_DOSE_SKIP"

    const val EXTRA_MED_ID = "extra_med_id"
    const val EXTRA_SCHEDULED_AT_EPOCH_MS = "extra_scheduled_at_ms"
    const val EXTRA_DOSE_LOG_ID = "extra_dose_log_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
}
