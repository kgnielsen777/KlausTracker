package com.klaustracker.app.tracking

import com.klaustracker.app.data.local.entity.VisitEntity
import com.klaustracker.app.data.local.model.PlaceDurationSummaryRow
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset

data class SuggestedPlaceLabel(
    val labelType: String,
    val reason: String,
    val confidence: Float,
)

object PlaceSuggestionHeuristics {

    fun suggest(
        summary: PlaceDurationSummaryRow,
        visits: List<VisitEntity>,
    ): SuggestedPlaceLabel {
        val totalMinutes = summary.totalDurationMinutes.coerceAtLeast(1)
        val nightMinutes = visits.sumOf { visit ->
            if (isNightVisit(visit.startUtc)) visit.durationMinutes else 0
        }
        val weekdayDayMinutes = visits.sumOf { visit ->
            if (isWeekdayDayVisit(visit.startUtc)) visit.durationMinutes else 0
        }

        val nightRatio = nightMinutes.toFloat() / totalMinutes.toFloat()
        val weekdayDayRatio = weekdayDayMinutes.toFloat() / totalMinutes.toFloat()

        if (nightRatio >= 0.45f && summary.visitCount >= 3) {
            return SuggestedPlaceLabel(
                labelType = "home",
                reason = "Most time occurs at night",
                confidence = (0.55f + (nightRatio * 0.4f)).coerceAtMost(0.95f),
            )
        }

        if (weekdayDayRatio >= 0.5f && summary.visitCount >= 3) {
            return SuggestedPlaceLabel(
                labelType = "work",
                reason = "Most time occurs on weekday daytime",
                confidence = (0.55f + (weekdayDayRatio * 0.4f)).coerceAtMost(0.95f),
            )
        }

        val text = listOfNotNull(summary.placeName, summary.defaultAddress)
            .joinToString(" ")
            .lowercase()
        val label = if (
            text.contains("office") ||
            text.contains("company") ||
            text.contains("business") ||
            text.contains("workspace") ||
            text.contains("work")
        ) {
            "work"
        } else {
            "home"
        }

        return SuggestedPlaceLabel(
            labelType = label,
            reason = "Fallback from place text pattern",
            confidence = 0.58f,
        )
    }

    private fun isNightVisit(startUtc: String): Boolean {
        val hour = parseHour(startUtc) ?: return false
        return hour in 22..23 || hour in 0..5
    }

    private fun isWeekdayDayVisit(startUtc: String): Boolean {
        val instant = runCatching { Instant.parse(startUtc) }.getOrNull() ?: return false
        val dateTime = instant.atOffset(ZoneOffset.UTC)
        val hour = dateTime.hour
        val weekday = dateTime.dayOfWeek
        val isWeekday = weekday != DayOfWeek.SATURDAY && weekday != DayOfWeek.SUNDAY
        return isWeekday && hour in 9..17
    }

    private fun parseHour(startUtc: String): Int? {
        val instant = runCatching { Instant.parse(startUtc) }.getOrNull() ?: return null
        return instant.atOffset(ZoneOffset.UTC).hour
    }
}
