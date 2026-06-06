package com.klaustracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "place_suggestions",
    foreignKeys = [
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["place_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["place_id"]), Index(value = ["status"])],
)
data class PlaceSuggestionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "place_id") val placeId: String,
    @ColumnInfo(name = "suggested_label_type") val suggestedLabelType: String,
    val reason: String,
    val confidence: Float,
    val status: String,
    @ColumnInfo(name = "created_utc") val createdUtc: String,
    @ColumnInfo(name = "updated_utc") val updatedUtc: String,
)
