package com.klaustracker.app.data.local.model

data class VisitDetailRow(
    val visitId: String,
    val placeId: String,
    val placeName: String,
    val labelType: String,
    val customLabel: String?,
    val defaultAddress: String?,
    val staySegmentId: String,
    val startUtc: String,
    val endUtc: String,
    val durationMinutes: Int,
    val centroidLat: Double,
    val centroidLng: Double,
    val radiusMeters: Float,
    val classification: String,
)