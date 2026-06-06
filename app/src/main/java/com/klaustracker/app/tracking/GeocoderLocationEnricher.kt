package com.klaustracker.app.tracking

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeocoderLocationEnricher(
    private val context: Context,
) {

    suspend fun enrich(latitude: Double, longitude: Double): EnrichmentDraft {
        return withContext(Dispatchers.IO) {
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
                confidence = if (formattedAddress != null) 0.7f else 0.4f,
            )
        }
    }
}