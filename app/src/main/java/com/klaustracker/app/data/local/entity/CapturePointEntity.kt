package com.klaustracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "capture_points")
data class CapturePointEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "timestamp_utc") val timestampUtc: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "accuracy_meters") val accuracyMeters: Float,
    @ColumnInfo(name = "speed_kmh") val speedKmh: Float?,
    @ColumnInfo(name = "motion_state") val motionState: String,
    val source: String,
    @ColumnInfo(name = "enrichment_status") val enrichmentStatus: String,
)