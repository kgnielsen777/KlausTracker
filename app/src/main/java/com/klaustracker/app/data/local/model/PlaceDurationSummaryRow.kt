package com.klaustracker.app.data.local.model

data class PlaceDurationSummaryRow(
    val placeId: String,
    val placeName: String,
    val labelType: String,
    val customLabel: String?,
    val defaultAddress: String?,
    val visitCount: Int,
    val totalDurationMinutes: Int,
    val lastVisitUtc: String?,
)