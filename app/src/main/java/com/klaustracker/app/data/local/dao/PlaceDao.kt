package com.klaustracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.klaustracker.app.data.local.entity.PlaceEntity
import com.klaustracker.app.data.local.model.PlaceDurationSummaryRow
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Upsert
    suspend fun upsert(place: PlaceEntity)

    @Query("SELECT * FROM places WHERE id = :placeId LIMIT 1")
    suspend fun byId(placeId: String): PlaceEntity?

    @Query("SELECT * FROM places WHERE active = 1 ORDER BY updated_utc DESC")
    suspend fun activePlaces(): List<PlaceEntity>

    @Query("SELECT * FROM places WHERE active = 1 ORDER BY updated_utc DESC")
    fun observeActivePlaces(): Flow<List<PlaceEntity>>

    @Query(
        """
        SELECT
            p.id AS placeId,
            p.canonical_name AS placeName,
            p.label_type AS labelType,
            p.custom_label AS customLabel,
            p.default_address AS defaultAddress,
            COUNT(v.id) AS visitCount,
            COALESCE(SUM(v.duration_minutes), 0) AS totalDurationMinutes,
            MAX(v.start_utc) AS lastVisitUtc
        FROM places p
        LEFT JOIN visits v ON v.place_id = p.id AND v.start_utc >= :sinceUtc
        WHERE p.active = 1
        GROUP BY p.id, p.canonical_name, p.label_type, p.custom_label, p.default_address
        ORDER BY totalDurationMinutes DESC, lastVisitUtc DESC
        """
    )
    fun observePlaceDurationSummaries(sinceUtc: String): Flow<List<PlaceDurationSummaryRow>>

    @Query(
        """
        UPDATE places
        SET label_type = :labelType,
            custom_label = :customLabel,
            canonical_name = :canonicalName,
            updated_utc = :updatedUtc
        WHERE id = :placeId
        """
    )
    suspend fun updateLabel(
        placeId: String,
        labelType: String,
        customLabel: String?,
        canonicalName: String,
        updatedUtc: String,
    )

    @Query(
        """
        UPDATE places
        SET canonical_name = :canonicalName,
            label_type = :labelType,
            custom_label = :customLabel,
            default_address = :defaultAddress,
            updated_utc = :updatedUtc
        WHERE id = :placeId
        """
    )
    suspend fun updateAfterMerge(
        placeId: String,
        canonicalName: String,
        labelType: String,
        customLabel: String?,
        defaultAddress: String?,
        updatedUtc: String,
    )

    @Query("UPDATE places SET active = 0, updated_utc = :updatedUtc WHERE id = :placeId")
    suspend fun deactivate(placeId: String, updatedUtc: String)

    @Query("SELECT COUNT(*) FROM places")
    suspend fun count(): Int

        @Query(
                """
                UPDATE places
                SET default_address = (
                        SELECT e.formatted_address
                        FROM visits v
                        INNER JOIN capture_points c ON c.timestamp_utc >= v.start_utc AND c.timestamp_utc <= v.end_utc
                        INNER JOIN enrichments e ON e.capture_point_id = c.id
                        WHERE v.place_id = places.id
                            AND e.formatted_address IS NOT NULL
                            AND e.formatted_address != ''
                        ORDER BY c.timestamp_utc DESC
                        LIMIT 1
                ),
                updated_utc = :updatedUtc
                WHERE active = 1
                    AND (default_address IS NULL OR default_address = '')
                    AND EXISTS (
                            SELECT 1
                            FROM visits v
                            INNER JOIN capture_points c ON c.timestamp_utc >= v.start_utc AND c.timestamp_utc <= v.end_utc
                            INNER JOIN enrichments e ON e.capture_point_id = c.id
                            WHERE v.place_id = places.id
                                AND e.formatted_address IS NOT NULL
                                AND e.formatted_address != ''
                    )
                """
        )
        suspend fun backfillMissingAddresses(updatedUtc: String): Int
}
