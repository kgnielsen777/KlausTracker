package com.klaustracker.app.data

import androidx.room.withTransaction
import com.klaustracker.app.data.local.TrackerDatabase
import com.klaustracker.app.data.local.entity.CapturePointEntity
import com.klaustracker.app.data.local.entity.EnrichmentEntity
import com.klaustracker.app.data.local.entity.PlaceEntity
import com.klaustracker.app.data.local.entity.PlaceSuggestionEntity
import com.klaustracker.app.data.local.entity.StaySegmentEntity
import com.klaustracker.app.data.local.entity.VisitEntity
import com.klaustracker.app.data.local.model.PlaceDurationSummaryRow
import com.klaustracker.app.data.local.model.PlaceSuggestionRow
import com.klaustracker.app.data.local.model.VisitDetailRow
import com.klaustracker.app.tracking.EnrichmentDraft
import com.klaustracker.app.tracking.PlaceSuggestionHeuristics
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

    fun observePlaceDurationSummaries(sinceUtc: String): Flow<List<PlaceDurationSummaryRow>> =
        database.placeDao().observePlaceDurationSummaries(sinceUtc)

    fun observeVisitDetailsForPlace(placeId: String): Flow<List<VisitDetailRow>> =
        database.visitDao().observeVisitDetailsForPlace(placeId)

    fun observePendingPlaceSuggestions(): Flow<List<PlaceSuggestionRow>> =
        database.placeSuggestionDao().observePendingSuggestions()

    suspend fun refreshRecurringSuggestions(summaries: List<PlaceDurationSummaryRow>) {
        val now = Instant.now().toString()
        summaries.forEach { summary ->
            if (summary.labelType != "detected") {
                return@forEach
            }
            if (summary.visitCount < RECURRING_MIN_VISITS || summary.totalDurationMinutes < RECURRING_MIN_DURATION_MINUTES) {
                return@forEach
            }

            val latestSuggestion = database.placeSuggestionDao().latestForPlace(summary.placeId)
            if (latestSuggestion?.status == "pending" || latestSuggestion?.status == "accepted") {
                return@forEach
            }

            val visits = database.visitDao().visitsForPlace(summary.placeId)
            val suggestion = PlaceSuggestionHeuristics.suggest(summary, visits)
            database.placeSuggestionDao().upsert(
                PlaceSuggestionEntity(
                    id = UUID.randomUUID().toString(),
                    placeId = summary.placeId,
                    suggestedLabelType = suggestion.labelType,
                    reason = suggestion.reason,
                    confidence = suggestion.confidence,
                    status = "pending",
                    createdUtc = now,
                    updatedUtc = now,
                )
            )
        }
    }

    suspend fun acceptSuggestion(suggestionId: String): Boolean {
        val suggestion = database.placeSuggestionDao().byId(suggestionId) ?: return false
        val place = database.placeDao().byId(suggestion.placeId) ?: return false
        val now = Instant.now().toString()

        database.withTransaction {
            database.placeDao().updateLabel(
                placeId = place.id,
                labelType = suggestion.suggestedLabelType,
                customLabel = place.customLabel,
                canonicalName = canonicalNameForLabel(place.canonicalName, suggestion.suggestedLabelType, place.customLabel),
                updatedUtc = now,
            )
            database.placeSuggestionDao().updateStatus(suggestionId, "accepted", now)
        }

        return true
    }

    suspend fun dismissSuggestion(suggestionId: String): Boolean {
        val suggestion = database.placeSuggestionDao().byId(suggestionId) ?: return false
        database.placeSuggestionDao().updateStatus(suggestion.id, "dismissed", Instant.now().toString())
        return true
    }

    suspend fun updatePlaceLabel(
        placeId: String,
        labelType: String,
        customLabel: String?,
    ): Boolean {
        val place = database.placeDao().byId(placeId) ?: return false
        val normalizedCustom = customLabel?.trim().takeIf { !it.isNullOrBlank() }
        val canonicalName = canonicalNameForLabel(place.canonicalName, labelType, normalizedCustom)

        database.placeDao().updateLabel(
            placeId = placeId,
            labelType = labelType,
            customLabel = normalizedCustom,
            canonicalName = canonicalName,
            updatedUtc = Instant.now().toString(),
        )
        return true
    }

    suspend fun mergePlaces(sourcePlaceId: String, targetPlaceId: String): Boolean {
        if (sourcePlaceId == targetPlaceId) {
            return false
        }

        val source = database.placeDao().byId(sourcePlaceId) ?: return false
        val target = database.placeDao().byId(targetPlaceId) ?: return false
        val now = Instant.now().toString()

        database.withTransaction {
            database.visitDao().reassignPlace(sourcePlaceId = source.id, targetPlaceId = target.id)

            val mergedLabelType = if (target.labelType == "detected" && source.labelType != "detected") {
                source.labelType
            } else {
                target.labelType
            }
            val mergedCustomLabel = target.customLabel ?: source.customLabel
            val mergedName = when {
                mergedLabelType == "custom" && !mergedCustomLabel.isNullOrBlank() -> mergedCustomLabel
                target.canonicalName == "Detected place" && source.canonicalName != "Detected place" -> source.canonicalName
                else -> target.canonicalName
            }
            val mergedAddress = target.defaultAddress ?: source.defaultAddress

            database.placeDao().updateAfterMerge(
                placeId = target.id,
                canonicalName = mergedName,
                labelType = mergedLabelType,
                customLabel = mergedCustomLabel,
                defaultAddress = mergedAddress,
                updatedUtc = now,
            )
            database.placeDao().deactivate(source.id, now)
        }

        return true
    }

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
    ): String {
        val captureId = UUID.randomUUID().toString()
        database.capturePointDao().upsert(
            CapturePointEntity(
                id = captureId,
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
        return captureId
    }

    suspend fun persistCaptureEnrichment(capturePointId: String, enrichmentDraft: EnrichmentDraft) {
        database.enrichmentDao().upsert(
            EnrichmentEntity(
                id = UUID.randomUUID().toString(),
                capturePointId = capturePointId,
                formattedAddress = enrichmentDraft.formattedAddress,
                poiName = enrichmentDraft.poiName,
                poiType = enrichmentDraft.poiType,
                isHotel = enrichmentDraft.isHotel,
                confidence = enrichmentDraft.confidence,
                provider = enrichmentDraft.provider,
                providerTimestampUtc = enrichmentDraft.providerTimestampUtc,
                status = enrichmentDraft.status,
            )
        )

        database.capturePointDao().updateEnrichmentStatus(capturePointId, enrichmentDraft.captureStatus)
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

    private fun canonicalNameForLabel(currentName: String, labelType: String, customLabel: String?): String {
        return when (labelType) {
            "home" -> "Home"
            "work" -> "Work"
            "friend" -> "Friend"
            "family" -> "Family"
            "custom" -> customLabel ?: currentName
            else -> currentName
        }
    }

    companion object {
        private const val RECURRING_MIN_VISITS = 3
        private const val RECURRING_MIN_DURATION_MINUTES = 180
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
