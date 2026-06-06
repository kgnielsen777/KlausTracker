package com.klaustracker.app.data

import com.klaustracker.app.data.local.TrackerDatabase
import com.klaustracker.app.data.local.entity.CapturePointEntity
import com.klaustracker.app.data.local.entity.PlaceEntity
import com.klaustracker.app.data.local.entity.StaySegmentEntity
import com.klaustracker.app.data.local.entity.VisitEntity
import com.klaustracker.app.tracking.CaptureSample
import com.klaustracker.app.tracking.TransitStayClassifier
import java.time.Instant
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class TrackerRepository(
    private val database: TrackerDatabase,
) {
    fun observeRecentCaptures(limit: Int = 20): Flow<List<CapturePointEntity>> =
        database.capturePointDao().observeRecent(limit)

    fun observeActivePlaces(): Flow<List<PlaceEntity>> =
        database.placeDao().observeActivePlaces()

    suspend fun addDemoCapture() {
        val now = Instant.now().toString()
        val captureCount = database.capturePointDao().count()
        val baseLat = 55.6761
        val baseLng = 12.5683
        val offset = captureCount * 0.001

        insertCapture(
            latitude = baseLat + offset,
            longitude = baseLng + offset,
            accuracyMeters = 24f,
            speedKmh = 0f,
            motionState = "stationary",
            source = "demo",
            enrichmentStatus = "pending",
            timestampUtc = now,
        )

        ensureDemoPlaceAndVisit(now)
    }

    suspend fun insertCapture(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        speedKmh: Float?,
        motionState: String,
        source: String,
        enrichmentStatus: String,
        timestampUtc: String = Instant.now().toString(),
    ) {
        database.capturePointDao().upsert(
            CapturePointEntity(
                id = UUID.randomUUID().toString(),
                timestampUtc = timestampUtc,
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = accuracyMeters,
                speedKmh = speedKmh,
                motionState = motionState,
                source = source,
                enrichmentStatus = enrichmentStatus,
            )
        )

        detectAndPersistStayIfNeeded()
    }

    private suspend fun detectAndPersistStayIfNeeded() {
        val samples = database.capturePointDao()
            .recent(8)
            .asReversed()
            .mapNotNull { point ->
                runCatching {
                    CaptureSample(
                        timestampUtc = Instant.parse(point.timestampUtc),
                        latitude = point.latitude,
                        longitude = point.longitude,
                        speedKmh = point.speedKmh,
                    )
                }.getOrNull()
            }

        val stayDraft = TransitStayClassifier.buildStaySegment(samples) ?: return
        persistStay(stayDraft)
    }

    private suspend fun persistStay(stayDraft: com.klaustracker.app.tracking.StaySegmentDraft) {
        val roundedLat = roundCoordinate(stayDraft.centroidLat)
        val roundedLng = roundCoordinate(stayDraft.centroidLng)

        val placeId = stableId(
            prefix = "place",
            stayDraft.startUtc.toString(),
            stayDraft.endUtc.toString(),
            roundedLat,
            roundedLng,
        )
        val stayId = stableId(
            prefix = "stay",
            stayDraft.startUtc.toString(),
            stayDraft.endUtc.toString(),
            roundedLat,
            roundedLng,
        )
        val visitId = stableId(prefix = "visit", stayId)
        val now = Instant.now().toString()

        database.placeDao().upsert(
            PlaceEntity(
                id = placeId,
                canonicalName = "Detected place",
                labelType = "detected",
                customLabel = null,
                defaultAddress = null,
                centroidLat = stayDraft.centroidLat,
                centroidLng = stayDraft.centroidLng,
                active = true,
                createdUtc = now,
                updatedUtc = now,
            )
        )

        database.staySegmentDao().upsert(
            StaySegmentEntity(
                id = stayId,
                startUtc = stayDraft.startUtc.toString(),
                endUtc = stayDraft.endUtc.toString(),
                centroidLat = stayDraft.centroidLat,
                centroidLng = stayDraft.centroidLng,
                radiusMeters = stayDraft.radiusMeters,
                durationMinutes = stayDraft.durationMinutes,
                classification = stayDraft.classification,
                confidence = stayDraft.confidence,
            )
        )

        database.visitDao().upsert(
            VisitEntity(
                id = visitId,
                placeId = placeId,
                staySegmentId = stayId,
                startUtc = stayDraft.startUtc.toString(),
                endUtc = stayDraft.endUtc.toString(),
                durationMinutes = stayDraft.durationMinutes,
            )
        )
    }

    private fun stableId(prefix: String, vararg parts: String): String {
        val raw = listOf(prefix, *parts).joinToString(separator = ":")
        return UUID.nameUUIDFromBytes(raw.toByteArray()).toString()
    }

    private fun roundCoordinate(value: Double): String =
        String.format(Locale.US, "%.4f", value)

    private suspend fun ensureDemoPlaceAndVisit(now: String) {
        if (database.placeDao().count() > 0) {
            return
        }

        val placeId = UUID.randomUUID().toString()
        val stayId = UUID.randomUUID().toString()

        database.placeDao().upsert(
            PlaceEntity(
                id = placeId,
                canonicalName = "Demo Home",
                labelType = "home",
                customLabel = null,
                defaultAddress = "Copenhagen, Denmark",
                centroidLat = 55.6761,
                centroidLng = 12.5683,
                active = true,
                createdUtc = now,
                updatedUtc = now,
            )
        )

        database.staySegmentDao().upsert(
            StaySegmentEntity(
                id = stayId,
                startUtc = now,
                endUtc = now,
                centroidLat = 55.6761,
                centroidLng = 12.5683,
                radiusMeters = 80f,
                durationMinutes = 0,
                classification = "stay",
                confidence = 0.5f,
            )
        )

        database.visitDao().upsert(
            VisitEntity(
                id = UUID.randomUUID().toString(),
                placeId = placeId,
                staySegmentId = stayId,
                startUtc = now,
                endUtc = now,
                durationMinutes = 0,
            )
        )
    }
}
