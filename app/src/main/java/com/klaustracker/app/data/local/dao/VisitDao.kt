package com.klaustracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.klaustracker.app.data.local.entity.VisitEntity

@Dao
interface VisitDao {
    @Upsert
    suspend fun upsert(visit: VisitEntity)

    @Query("SELECT * FROM visits WHERE place_id = :placeId ORDER BY start_utc DESC")
    suspend fun visitsForPlace(placeId: String): List<VisitEntity>
}
