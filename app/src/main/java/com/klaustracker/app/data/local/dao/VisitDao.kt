package com.klaustracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.klaustracker.app.data.local.entity.VisitEntity
import com.klaustracker.app.data.local.model.VisitDetailRow
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {
    @Upsert
    suspend fun upsert(visit: VisitEntity)

    @Query("SELECT * FROM visits WHERE place_id = :placeId ORDER BY start_utc DESC")
    suspend fun visitsForPlace(placeId: String): List<VisitEntity>

    @Query(
        """
        SELECT
            v.id AS visitId,
            v.place_id AS placeId,
            p.canonical_name AS placeName,
            p.label_type AS labelType,
            p.custom_label AS customLabel,
            p.default_address AS defaultAddress,
            v.stay_segment_id AS staySegmentId,
            v.start_utc AS startUtc,
            v.end_utc AS endUtc,
            v.duration_minutes AS durationMinutes,
            s.centroid_lat AS centroidLat,
            s.centroid_lng AS centroidLng,
            s.radius_meters AS radiusMeters,
            s.classification AS classification
        FROM visits v
        INNER JOIN places p ON p.id = v.place_id
        INNER JOIN stay_segments s ON s.id = v.stay_segment_id
        WHERE v.place_id = :placeId
        ORDER BY v.start_utc DESC
        """
    )
    fun observeVisitDetailsForPlace(placeId: String): Flow<List<VisitDetailRow>>
}
