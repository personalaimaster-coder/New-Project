package com.example.petmeds

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.petmeds.notifications.EndDateAutoCompleteWorker
import com.example.petmeds.notifications.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class PetMedsApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensure(this)
        WorkManager.getInstance(this).enqueueUniqueWork(
            END_DATE_SWEEP_ON_START,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<EndDateAutoCompleteWorker>().build(),
        )
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            END_DATE_SWEEP_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<EndDateAutoCompleteWorker>(1, TimeUnit.DAYS).build(),
        )
    }

    companion object {
        private const val END_DATE_SWEEP_ON_START = "end-date-auto-complete-on-start"
        private const val END_DATE_SWEEP_PERIODIC = "end-date-auto-complete-periodic"
    }
}
