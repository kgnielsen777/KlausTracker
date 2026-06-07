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
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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

        val repository = TrackerRepository(TrackerDatabaseProvider.database(applicationContext))
        val estimatedSpeedKmh = if (speedKmh == null) {
            estimateSpeedFromPreviousCapture(
                repository = repository,
                latitude = location.latitude,
                longitude = location.longitude,
                timestampUtc = Instant.now().toString(),
            )
        } else {
            null
        }

        val effectiveSpeedKmh = speedKmh ?: estimatedSpeedKmh
        val motionState = if (effectiveSpeedKmh == null) {
            "stay_candidate"
        } else {
            TransitStayClassifier.classifyMotionState(effectiveSpeedKmh)
        }

        val captureId = repository.insertCapture(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            speedKmh = effectiveSpeedKmh,
            motionState = motionState,
            source = "fused",
            enrichmentStatus = "pending",
        )

        val enrichmentDraft = GeocoderLocationEnricher(applicationContext)
            .enrich(location.latitude, location.longitude)
        repository.persistCaptureEnrichment(captureId, enrichmentDraft)
        repository.retryPendingEnrichments(
            enricher = GeocoderLocationEnricher(applicationContext),
            limit = 5,
        )

        return Result.success()
    }

    companion object {
        const val UNIQUE_PERIODIC_WORK_NAME = "location_capture_periodic"
        const val UNIQUE_NOW_WORK_NAME = "location_capture_now"
        const val PERIODIC_MINUTES = 30L
    }
}

private suspend fun estimateSpeedFromPreviousCapture(
    repository: TrackerRepository,
    latitude: Double,
    longitude: Double,
    timestampUtc: String,
): Float? {
    val previous = repository.observeRecentCaptures(limit = 1).first().firstOrNull() ?: return null

    val currentInstant = runCatching { Instant.parse(timestampUtc) }.getOrNull() ?: return null
    val previousInstant = runCatching { Instant.parse(previous.timestampUtc) }.getOrNull() ?: return null
    val deltaSeconds = (currentInstant.epochSecond - previousInstant.epochSecond).coerceAtLeast(1L)

    val distanceMeters = distanceMeters(
        lat1 = previous.latitude,
        lng1 = previous.longitude,
        lat2 = latitude,
        lng2 = longitude,
    )
    val speedMetersPerSecond = distanceMeters / deltaSeconds
    return (speedMetersPerSecond * 3.6).toFloat()
}

private fun distanceMeters(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double,
): Double {
    val earthRadiusMeters = 6_371_000.0
    val latRad1 = Math.toRadians(lat1)
    val latRad2 = Math.toRadians(lat2)
    val latDelta = Math.toRadians(lat2 - lat1)
    val lngDelta = Math.toRadians(lng2 - lng1)

    val a = sin(latDelta / 2).pow(2) +
        cos(latRad1) * cos(latRad2) * sin(lngDelta / 2).pow(2)
    val c = 2 * asin(sqrt(a))
    return earthRadiusMeters * c
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
