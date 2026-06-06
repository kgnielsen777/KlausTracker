package com.klaustracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.klaustracker.app.data.local.entity.StaySegmentEntity

@Dao
interface StaySegmentDao {
    @Upsert
    suspend fun upsert(segment: StaySegmentEntity)

    @Query("SELECT * FROM stay_segments ORDER BY start_utc DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<StaySegmentEntity>
}
