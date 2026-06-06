package com.klaustracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.klaustracker.app.data.local.entity.PlaceSuggestionEntity
import com.klaustracker.app.data.local.model.PlaceSuggestionRow
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceSuggestionDao {
    @Upsert
    suspend fun upsert(suggestion: PlaceSuggestionEntity)

    @Query("SELECT * FROM place_suggestions WHERE id = :suggestionId LIMIT 1")
    suspend fun byId(suggestionId: String): PlaceSuggestionEntity?

    @Query("SELECT * FROM place_suggestions WHERE place_id = :placeId AND status = :status LIMIT 1")
    suspend fun forPlaceByStatus(placeId: String, status: String): PlaceSuggestionEntity?

    @Query("SELECT * FROM place_suggestions WHERE place_id = :placeId ORDER BY updated_utc DESC LIMIT 1")
    suspend fun latestForPlace(placeId: String): PlaceSuggestionEntity?

    @Query(
        """
        SELECT
            s.id AS suggestionId,
            p.id AS placeId,
            p.canonical_name AS placeName,
            p.label_type AS labelType,
            p.custom_label AS customLabel,
            p.default_address AS defaultAddress,
            s.suggested_label_type AS suggestedLabelType,
            s.reason AS reason,
            s.confidence AS confidence,
            s.created_utc AS createdUtc
        FROM place_suggestions s
        INNER JOIN places p ON p.id = s.place_id
        WHERE s.status = 'pending' AND p.active = 1
        ORDER BY s.confidence DESC, s.created_utc DESC
        """
    )
    fun observePendingSuggestions(): Flow<List<PlaceSuggestionRow>>

    @Query("UPDATE place_suggestions SET status = :status, updated_utc = :updatedUtc WHERE id = :suggestionId")
    suspend fun updateStatus(suggestionId: String, status: String, updatedUtc: String)
}
