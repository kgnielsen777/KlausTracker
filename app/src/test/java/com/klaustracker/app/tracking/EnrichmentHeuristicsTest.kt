package com.klaustracker.app.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnrichmentHeuristicsTest {

    @Test
    fun buildDraft_marksHotelWhenFeatureContainsHotelWord() {
        val draft = EnrichmentHeuristics.buildDraft(
            formattedAddress = "123 Main Street, Copenhagen",
            featureName = "Grand Hotel Copenhagen",
        )

        assertTrue(draft.isHotel)
        assertEquals("lodging", draft.poiType)
        assertEquals("ok", draft.status)
    }

    @Test
    fun buildDraft_fallsBackToAddressWhenFeatureMissing() {
        val draft = EnrichmentHeuristics.buildDraft(
            formattedAddress = "Copenhagen, Denmark",
            featureName = null,
        )

        assertFalse(draft.isHotel)
        assertEquals("address", draft.poiType)
        assertEquals("Copenhagen, Denmark", draft.poiName)
    }

    @Test
    fun buildDraft_marksUnavailableWhenNothingResolved() {
        val draft = EnrichmentHeuristics.buildDraft(
            formattedAddress = null,
            featureName = null,
        )

        assertEquals("unavailable", draft.status)
        assertEquals("enrichment_failed", draft.captureStatus)
    }

    @Test
    fun buildDraft_usesExplicitPoiAndHotelHintsWhenProvided() {
        val draft = EnrichmentHeuristics.buildDraft(
            formattedAddress = "City Center",
            featureName = "Nordic Suites",
            explicitPoiType = "lodging",
            explicitIsHotel = true,
        )

        assertTrue(draft.isHotel)
        assertEquals("lodging", draft.poiType)
        assertEquals("ok", draft.status)
    }
}