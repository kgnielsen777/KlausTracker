package com.klaustracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.klaustracker.app.data.local.entity.CapturePointEntity

@Dao
interface CapturePointDao {
    @Upsert
    suspend fun upsert(point: CapturePointEntity)

    @Query("SELECT * FROM capture_points ORDER BY timestamp_utc DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<CapturePointEntity>
}
