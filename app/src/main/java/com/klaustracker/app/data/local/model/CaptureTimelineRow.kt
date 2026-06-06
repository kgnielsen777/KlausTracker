package com.klaustracker.app.data.local.model

data class CaptureTimelineRow(
    val id: String,
    val timestampUtc: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val speedKmh: Float?,
    val motionState: String,
    val source: String,
    val enrichmentStatus: String,
    val enrichedAddress: String?,
    val poiName: String?,
    val poiType: String?,
    val isHotel: Boolean?,
)