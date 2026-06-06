package com.klaustracker.app.tracking

import android.content.Context
import android.location.Geocoder
import android.os.Build
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GeocoderLocationEnricher(
    private val context: Context,
) {

    suspend fun enrich(latitude: Double, longitude: Double): EnrichmentDraft {
        return withContext(Dispatchers.IO) {
            val remotePoi = fetchNominatimReverse(latitude, longitude)
            if (remotePoi != null) {
                return@withContext EnrichmentHeuristics.buildDraft(
                    formattedAddress = remotePoi.formattedAddress,
                    featureName = remotePoi.poiName,
                    explicitPoiType = remotePoi.poiType,
                    explicitIsHotel = remotePoi.isHotel,
                    provider = "nominatim",
                    confidence = 0.85f,
                )
            }

            if (!Geocoder.isPresent()) {
                return@withContext EnrichmentHeuristics.buildDraft(
                    formattedAddress = null,
                    featureName = null,
                    confidence = null,
                )
            }

            val geocoder = Geocoder(context)
            val address = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                }
            } catch (_: Exception) {
                null
            }

            if (address == null) {
                return@withContext EnrichmentHeuristics.buildDraft(
                    formattedAddress = null,
                    featureName = null,
                    confidence = null,
                )
            }

            val formattedAddress = buildString {
                val lines = (0 until address.maxAddressLineIndex + 1)
                    .mapNotNull { index -> address.getAddressLine(index)?.trim() }
                    .filter { it.isNotBlank() }
                append(lines.joinToString(separator = ", "))
            }.trim().ifBlank { null }

            EnrichmentHeuristics.buildDraft(
                formattedAddress = formattedAddress,
                featureName = address.featureName,
                explicitPoiType = address.featureName?.let { "poi" },
                confidence = if (formattedAddress != null) 0.7f else 0.4f,
            )
        }
    }

    private fun fetchNominatimReverse(latitude: Double, longitude: Double): NominatimReverseResult? {
        return try {
            val endpoint = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$latitude&lon=$longitude&zoom=18&addressdetails=1&extratags=1&namedetails=1"
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.setRequestProperty("User-Agent", "KlausTracker/0.1 (local-dev)")
            connection.setRequestProperty("Accept", "application/json")

            if (connection.responseCode !in 200..299) {
                connection.disconnect()
                return null
            }

            val payload = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            parseNominatim(payload)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseNominatim(payload: String): NominatimReverseResult? {
        val json = JSONObject(payload)
        val displayName = json.optString("display_name").takeIf { it.isNotBlank() }
        val featureName = json.optString("name").takeIf { it.isNotBlank() }
            ?: json.optJSONObject("namedetails")?.optString("name")?.takeIf { it.isNotBlank() }
        val type = json.optString("type").takeIf { it.isNotBlank() }
        val category = json.optString("class").takeIf { it.isNotBlank() }
        val tourismTag = json.optJSONObject("extratags")?.optString("tourism")
            ?.takeIf { it.isNotBlank() }

        val combined = listOfNotNull(featureName, displayName, type, tourismTag)
            .joinToString(" ")
            .lowercase()
        val isHotel = tourismTag == "hotel" || combined.contains("hotel") || combined.contains("hostel") || combined.contains("resort")

        val poiType = when {
            isHotel -> "lodging"
            !type.isNullOrBlank() -> type
            !category.isNullOrBlank() -> category
            !featureName.isNullOrBlank() -> "poi"
            !displayName.isNullOrBlank() -> "address"
            else -> null
        }

        if (featureName == null && displayName == null) {
            return null
        }

        return NominatimReverseResult(
            formattedAddress = displayName,
            poiName = featureName ?: displayName,
            poiType = poiType,
            isHotel = isHotel,
        )
    }
}

private data class NominatimReverseResult(
    val formattedAddress: String?,
    val poiName: String?,
    val poiType: String?,
    val isHotel: Boolean,
)