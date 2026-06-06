package com.klaustracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.klaustracker.app.data.local.entity.PlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Upsert
    suspend fun upsert(place: PlaceEntity)

    @Query("SELECT * FROM places WHERE active = 1 ORDER BY updated_utc DESC")
    suspend fun activePlaces(): List<PlaceEntity>

    @Query("SELECT * FROM places WHERE active = 1 ORDER BY updated_utc DESC")
    fun observeActivePlaces(): Flow<List<PlaceEntity>>

    @Query("SELECT COUNT(*) FROM places")
    suspend fun count(): Int
}
