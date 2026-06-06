package com.klaustracker.app.tracking

import java.time.Duration
import java.time.Instant
import kotlin.math.sqrt

private const val DEFAULT_DRIVING_SPEED_KMH = 15f
private const val DEFAULT_STILL_SPEED_KMH = 2f
private const val DEFAULT_STAY_RADIUS_METERS = 75f
private const val DEFAULT_DWELL_MINUTES = 20L
private const val EARTH_RADIUS_METERS = 6_371_000.0

private const val MOTION_TRANSIT = "transit"
private const val MOTION_STAY_CANDIDATE = "stay_candidate"
private const val MOTION_UNKNOWN = "unknown"

object TransitStayClassifier {

    fun classifyMotionState(
        speedKmh: Float?,
        drivingSpeedKmh: Float = DEFAULT_DRIVING_SPEED_KMH,
        stillSpeedKmh: Float = DEFAULT_STILL_SPEED_KMH,
    ): String {
        return when {
            speedKmh == null -> MOTION_UNKNOWN
            speedKmh >= drivingSpeedKmh -> MOTION_TRANSIT
            speedKmh <= stillSpeedKmh -> MOTION_STAY_CANDIDATE
            else -> MOTION_UNKNOWN
        }
    }

    fun buildStaySegment(
        samples: List<CaptureSample>,
        stayRadiusMeters: Float = DEFAULT_STAY_RADIUS_METERS,
        dwellMinutes: Long = DEFAULT_DWELL_MINUTES,
        stillSpeedKmh: Float = DEFAULT_STILL_SPEED_KMH,
    ): StaySegmentDraft? {
        val stationarySamples = samples
            .filter { sample ->
                sample.speedKmh == null ||
                    classifyMotionState(sample.speedKmh, stillSpeedKmh = stillSpeedKmh) == MOTION_STAY_CANDIDATE
            }
            .sortedBy { it.timestampUtc }

        if (stationarySamples.size < 2) {
            return null
        }

        val startUtc = stationarySamples.first().timestampUtc
        val endUtc = stationarySamples.last().timestampUtc
        val durationMinutes = Duration.between(startUtc, endUtc).toMinutes()

        if (durationMinutes < dwellMinutes) {
            return null
        }

        val centroidLat = stationarySamples.map { it.latitude }.average()
        val centroidLng = stationarySamples.map { it.longitude }.average()
        val maxRadiusMeters = stationarySamples.maxOf { sample ->
            distanceMeters(
                sample.latitude,
                sample.longitude,
                centroidLat,
                centroidLng,
            )
        }

        if (maxRadiusMeters > stayRadiusMeters) {
            return null
        }

        return StaySegmentDraft(
            startUtc = startUtc,
            endUtc = endUtc,
            centroidLat = centroidLat,
            centroidLng = centroidLng,
            radiusMeters = maxRadiusMeters.toFloat(),
            durationMinutes = durationMinutes.toInt(),
            classification = "stay",
            confidence = 0.9f,
        )
    }
}

data class CaptureSample(
    val timestampUtc: Instant,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float?,
)

data class StaySegmentDraft(
    val startUtc: Instant,
    val endUtc: Instant,
    val centroidLat: Double,
    val centroidLng: Double,
    val radiusMeters: Float,
    val durationMinutes: Int,
    val classification: String,
    val confidence: Float,
)

private fun distanceMeters(
    latitudeA: Double,
    longitudeA: Double,
    latitudeB: Double,
    longitudeB: Double,
): Double {
    val latDelta = Math.toRadians(latitudeB - latitudeA)
    val lngDelta = Math.toRadians(longitudeB - longitudeA)
    val sinLat = kotlin.math.sin(latDelta / 2)
    val sinLng = kotlin.math.sin(lngDelta / 2)
    val haversine = sinLat * sinLat + kotlin.math.cos(Math.toRadians(latitudeA)) * kotlin.math.cos(Math.toRadians(latitudeB)) * sinLng * sinLng
    return 2 * EARTH_RADIUS_METERS * kotlin.math.asin(sqrt(haversine))
}
