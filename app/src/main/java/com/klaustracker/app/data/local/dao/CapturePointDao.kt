package com.klaustracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.klaustracker.app.data.local.entity.CapturePointEntity
import com.klaustracker.app.data.local.model.CaptureTimelineRow
import kotlinx.coroutines.flow.Flow

@Dao
interface CapturePointDao {
    @Upsert
    suspend fun upsert(point: CapturePointEntity)

    @Query("SELECT * FROM capture_points WHERE id = :capturePointId LIMIT 1")
    suspend fun byId(capturePointId: String): CapturePointEntity?

    @Query("SELECT * FROM capture_points ORDER BY timestamp_utc DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<CapturePointEntity>

    @Query("SELECT * FROM capture_points ORDER BY timestamp_utc DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<CapturePointEntity>>

    @Query(
        """
        SELECT
            c.id AS id,
            c.timestamp_utc AS timestampUtc,
            c.latitude AS latitude,
            c.longitude AS longitude,
            c.accuracy_meters AS accuracyMeters,
            c.speed_kmh AS speedKmh,
            c.motion_state AS motionState,
            c.source AS source,
            c.enrichment_status AS enrichmentStatus,
            e.formatted_address AS enrichedAddress,
            e.poi_name AS poiName,
            e.poi_type AS poiType,
            e.is_hotel AS isHotel
        FROM capture_points c
        LEFT JOIN enrichments e ON e.capture_point_id = c.id
        ORDER BY c.timestamp_utc DESC
        LIMIT :limit
        """
    )
    fun observeRecentWithEnrichment(limit: Int): Flow<List<CaptureTimelineRow>>

    @Query("SELECT COUNT(*) FROM capture_points")
    suspend fun count(): Int

    @Query("SELECT * FROM capture_points WHERE enrichment_status IN (:statuses) ORDER BY timestamp_utc ASC LIMIT :limit")
    suspend fun byEnrichmentStatuses(statuses: List<String>, limit: Int): List<CapturePointEntity>

    @Query("UPDATE capture_points SET enrichment_status = :status WHERE id = :capturePointId")
    suspend fun updateEnrichmentStatus(capturePointId: String, status: String)

        @Query(
                """
                UPDATE capture_points
                SET motion_state = 'stay_candidate'
                WHERE motion_state = 'unknown'
                    AND speed_kmh IS NULL
                    AND source = 'fused'
                """
        )
        suspend fun backfillUnknownMotionForMissingSpeed(): Int

    @Query("DELETE FROM capture_points WHERE id = :capturePointId")
    suspend fun deleteById(capturePointId: String)
}
