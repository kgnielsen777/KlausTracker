package com.klaustracker.app.tracking

import android.Manifest
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.klaustracker.app.data.TrackerRepository
import com.klaustracker.app.data.local.TrackerDatabaseProvider
import kotlinx.coroutines.tasks.await

class LocationCaptureWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!hasForegroundLocationPermission(applicationContext)) {
            return Result.success()
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(applicationContext)

        val location = try {
            fusedClient.lastLocation.await() ?: run {
                val tokenSource = CancellationTokenSource()
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    tokenSource.token,
                ).await()
            }
        } catch (_: Exception) {
            null
        }

        if (location == null) {
            return Result.retry()
        }

        val speedKmh = if (location.hasSpeed()) {
            location.speed * 3.6f
        } else {
            null
        }

        val motionState = TransitStayClassifier.classifyMotionState(speedKmh)

        val repository = TrackerRepository(TrackerDatabaseProvider.database(applicationContext))
        val captureId = repository.insertCapture(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            speedKmh = speedKmh,
            motionState = motionState,
            source = "fused",
            enrichmentStatus = "pending",
        )

        val enrichmentDraft = GeocoderLocationEnricher(applicationContext)
            .enrich(location.latitude, location.longitude)
        repository.persistCaptureEnrichment(captureId, enrichmentDraft)

        return Result.success()
    }

    companion object {
        const val UNIQUE_PERIODIC_WORK_NAME = "location_capture_periodic"
        const val UNIQUE_NOW_WORK_NAME = "location_capture_now"
        const val PERIODIC_MINUTES = 30L
    }
}

private fun hasForegroundLocationPermission(context: Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PermissionChecker.PERMISSION_GRANTED

    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PermissionChecker.PERMISSION_GRANTED

    return fineGranted || coarseGranted
}
