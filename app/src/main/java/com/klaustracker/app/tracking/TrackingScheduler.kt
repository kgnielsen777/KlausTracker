package com.klaustracker.app.tracking

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class TrackingScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun startPeriodicCapture() {
        val request = PeriodicWorkRequestBuilder<LocationCaptureWorker>(
            LocationCaptureWorker.PERIODIC_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(defaultConstraints())
            .build()

        workManager.enqueueUniquePeriodicWork(
            LocationCaptureWorker.UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun stopPeriodicCapture() {
        workManager.cancelUniqueWork(LocationCaptureWorker.UNIQUE_PERIODIC_WORK_NAME)
    }

    fun captureNow() {
        val request = OneTimeWorkRequestBuilder<LocationCaptureWorker>()
            .setConstraints(defaultConstraints())
            .build()

        workManager.enqueueUniqueWork(
            LocationCaptureWorker.UNIQUE_NOW_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun defaultConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
    }
}
