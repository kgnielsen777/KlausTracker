package com.klaustracker.app.data

import com.klaustracker.app.data.local.TrackerDatabase
import com.klaustracker.app.data.local.entity.CapturePointEntity
import com.klaustracker.app.data.local.entity.PlaceEntity
import com.klaustracker.app.data.local.entity.StaySegmentEntity
import com.klaustracker.app.data.local.entity.VisitEntity
import java.time.Instant
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
        val captureId = UUID.randomUUID().toString()
        val captureCount = database.capturePointDao().count()
        val baseLat = 55.6761
        val baseLng = 12.5683
        val offset = captureCount * 0.001

        database.capturePointDao().upsert(
            CapturePointEntity(
                id = captureId,
                timestampUtc = now,
                latitude = baseLat + offset,
                longitude = baseLng + offset,
                accuracyMeters = 24f,
                speedKmh = 0f,
                motionState = "stationary",
                source = "demo",
                enrichmentStatus = "pending",
            )
        )

        ensureDemoPlaceAndVisit(now)
    }

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
