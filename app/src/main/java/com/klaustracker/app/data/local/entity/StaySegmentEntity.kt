package com.klaustracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stay_segments")
data class StaySegmentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "start_utc") val startUtc: String,
    @ColumnInfo(name = "end_utc") val endUtc: String,
    @ColumnInfo(name = "centroid_lat") val centroidLat: Double,
    @ColumnInfo(name = "centroid_lng") val centroidLng: Double,
    @ColumnInfo(name = "radius_meters") val radiusMeters: Float,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int,
    val classification: String,
    val confidence: Float?,
)