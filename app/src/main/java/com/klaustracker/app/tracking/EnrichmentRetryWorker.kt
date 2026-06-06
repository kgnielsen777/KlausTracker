package com.klaustracker.app.tracking

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.klaustracker.app.data.TrackerRepository
import com.klaustracker.app.data.local.TrackerDatabaseProvider

class EnrichmentRetryWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = TrackerRepository(TrackerDatabaseProvider.database(applicationContext))
        repository.retryPendingEnrichments(
            enricher = GeocoderLocationEnricher(applicationContext),
        )
        return Result.success()
    }

    companion object {
        const val UNIQUE_PERIODIC_WORK_NAME = "enrichment_retry_periodic"
        const val PERIODIC_MINUTES = 60L
    }
}
