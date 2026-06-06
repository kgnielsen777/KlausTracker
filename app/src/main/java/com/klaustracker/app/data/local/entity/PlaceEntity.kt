package com.klaustracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "canonical_name") val canonicalName: String,
    @ColumnInfo(name = "label_type") val labelType: String,
    @ColumnInfo(name = "custom_label") val customLabel: String?,
    @ColumnInfo(name = "default_address") val defaultAddress: String?,
    @ColumnInfo(name = "centroid_lat") val centroidLat: Double,
    @ColumnInfo(name = "centroid_lng") val centroidLng: Double,
    val active: Boolean,
    @ColumnInfo(name = "created_utc") val createdUtc: String,
    @ColumnInfo(name = "updated_utc") val updatedUtc: String,
)