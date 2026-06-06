package com.klaustracker.app.tracking

import com.klaustracker.app.data.local.entity.VisitEntity
import com.klaustracker.app.data.local.model.PlaceDurationSummaryRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceSuggestionHeuristicsTest {

    @Test
    fun suggest_prefersHomeForNightHeavyVisits() {
        val summary = summaryRow(totalMinutes = 420, visitCount = 4, placeName = "Detected place")
        val visits = listOf(
            visit("2026-06-01T22:30:00Z", 120),
            visit("2026-06-02T23:00:00Z", 120),
            visit("2026-06-03T01:00:00Z", 120),
            visit("2026-06-03T14:00:00Z", 60),
        )

        val suggestion = PlaceSuggestionHeuristics.suggest(summary, visits)

        assertEquals("home", suggestion.labelType)
        assertTrue(suggestion.confidence > 0.7f)
    }

    @Test
    fun suggest_prefersWorkForWeekdayDaytimePattern() {
        val summary = summaryRow(totalMinutes = 480, visitCount = 5, placeName = "Detected place")
        val visits = listOf(
            visit("2026-06-01T09:00:00Z", 120),
            visit("2026-06-02T10:00:00Z", 120),
            visit("2026-06-03T11:00:00Z", 120),
            visit("2026-06-04T14:00:00Z", 60),
            visit("2026-06-06T23:00:00Z", 60),
        )

        val suggestion = PlaceSuggestionHeuristics.suggest(summary, visits)

        assertEquals("work", suggestion.labelType)
        assertTrue(suggestion.confidence > 0.7f)
    }

    @Test
    fun suggest_fallsBackToTextWhenPatternWeak() {
        val summary = summaryRow(totalMinutes = 120, visitCount = 2, placeName = "Acme Office Building")
        val visits = listOf(
            visit("2026-06-01T19:00:00Z", 60),
            visit("2026-06-02T20:00:00Z", 60),
        )

        val suggestion = PlaceSuggestionHeuristics.suggest(summary, visits)

        assertEquals("work", suggestion.labelType)
        assertEquals("Fallback from place text pattern", suggestion.reason)
    }

    private fun summaryRow(totalMinutes: Int, visitCount: Int, placeName: String): PlaceDurationSummaryRow {
        return PlaceDurationSummaryRow(
            placeId = "p1",
            placeName = placeName,
            labelType = "detected",
            customLabel = null,
            defaultAddress = null,
            visitCount = visitCount,
            totalDurationMinutes = totalMinutes,
            lastVisitUtc = "2026-06-06T00:00:00Z",
        )
    }

    private fun visit(startUtc: String, durationMinutes: Int): VisitEntity {
        return VisitEntity(
            id = "v-$startUtc",
            placeId = "p1",
            staySegmentId = "s-$startUtc",
            startUtc = startUtc,
            endUtc = startUtc,
            durationMinutes = durationMinutes,
        )
    }
}
