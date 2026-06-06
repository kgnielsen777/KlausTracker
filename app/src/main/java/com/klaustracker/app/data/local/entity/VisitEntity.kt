package com.klaustracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "visits",
    foreignKeys = [
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["place_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StaySegmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["stay_segment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["place_id"]), Index(value = ["stay_segment_id"])],
)
data class VisitEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "place_id") val placeId: String,
    @ColumnInfo(name = "stay_segment_id") val staySegmentId: String,
    @ColumnInfo(name = "start_utc") val startUtc: String,
    @ColumnInfo(name = "end_utc") val endUtc: String,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int,
)