package com.klaustracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.klaustracker.app.data.local.entity.EnrichmentEntity

@Dao
interface EnrichmentDao {
    @Upsert
    suspend fun upsert(enrichment: EnrichmentEntity)

    @Query("SELECT * FROM enrichments WHERE capture_point_id = :capturePointId LIMIT 1")
    suspend fun forCapturePoint(capturePointId: String): EnrichmentEntity?
}
