package com.klaustracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "enrichments",
    foreignKeys = [
        ForeignKey(
            entity = CapturePointEntity::class,
            parentColumns = ["id"],
            childColumns = ["capture_point_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["capture_point_id"])],
)
data class EnrichmentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "capture_point_id") val capturePointId: String,
    @ColumnInfo(name = "formatted_address") val formattedAddress: String?,
    @ColumnInfo(name = "poi_name") val poiName: String?,
    @ColumnInfo(name = "poi_type") val poiType: String?,
    @ColumnInfo(name = "is_hotel") val isHotel: Boolean,
    val confidence: Float?,
    val provider: String,
    @ColumnInfo(name = "provider_timestamp_utc") val providerTimestampUtc: String,
    val status: String,
)