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
        LEFT JOIN visits v ON v.place_id = p.id
        WHERE p.active = 1
        GROUP BY p.id, p.canonical_name, p.label_type, p.custom_label, p.default_address
        ORDER BY totalDurationMinutes DESC, lastVisitUtc DESC
        """
    )
    fun observePlaceDurationSummaries(): Flow<List<PlaceDurationSummaryRow>>

    @Query("SELECT COUNT(*) FROM places")
    suspend fun count(): Int
}
