package com.klaustracker.app.data.local.model

data class PlaceSuggestionRow(
    val suggestionId: String,
    val placeId: String,
    val placeName: String,
    val labelType: String,
    val customLabel: String?,
    val defaultAddress: String?,
    val suggestedLabelType: String,
    val reason: String,
    val confidence: Float,
    val createdUtc: String,
)
