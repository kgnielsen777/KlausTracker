package com.klaustracker.app.tracking

import java.time.Instant

data class EnrichmentDraft(
    val formattedAddress: String?,
    val poiName: String?,
    val poiType: String?,
    val isHotel: Boolean,
    val confidence: Float?,
    val provider: String,
    val providerTimestampUtc: String,
    val status: String,
    val captureStatus: String,
)

object EnrichmentHeuristics {

    fun buildDraft(
        formattedAddress: String?,
        featureName: String?,
        explicitPoiType: String? = null,
        explicitIsHotel: Boolean? = null,
        provider: String = "android-geocoder",
        providerTimestampUtc: String = Instant.now().toString(),
        confidence: Float? = null,
    ): EnrichmentDraft {
        val normalizedFeature = featureName?.trim().orEmpty().takeIf { it.isNotBlank() }
        val normalizedAddress = formattedAddress?.trim().orEmpty().takeIf { it.isNotBlank() }
        val combinedText = listOfNotNull(normalizedFeature, normalizedAddress).joinToString(" ").lowercase()
        val inferredHotel = combinedText.contains("hotel") || combinedText.contains("inn") || combinedText.contains("hostel") || combinedText.contains("resort")
        val isHotel = explicitIsHotel ?: inferredHotel
        val poiType = when {
            !explicitPoiType.isNullOrBlank() -> explicitPoiType
            isHotel -> "lodging"
            normalizedFeature != null -> "poi"
            normalizedAddress != null -> "address"
            else -> null
        }
        val poiName = normalizedFeature ?: normalizedAddress
        val status = when {
            poiName != null || normalizedAddress != null -> "ok"
            else -> "unavailable"
        }

        return EnrichmentDraft(
            formattedAddress = normalizedAddress,
            poiName = poiName,
            poiType = poiType,
            isHotel = isHotel,
            confidence = confidence,
            provider = provider,
            providerTimestampUtc = providerTimestampUtc,
            status = status,
            captureStatus = if (status == "ok") "enriched" else "enrichment_failed",
        )
    }
}