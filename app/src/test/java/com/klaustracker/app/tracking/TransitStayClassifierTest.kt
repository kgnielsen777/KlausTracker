package com.klaustracker.app.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class TransitStayClassifierTest {

    @Test
    fun classifyMotionState_returnsTransitForDrivingSpeed() {
        val state = TransitStayClassifier.classifyMotionState(speedKmh = 18f)

        assertEquals("transit", state)
    }

    @Test
    fun classifyMotionState_returnsStayCandidateForVeryLowSpeed() {
        val state = TransitStayClassifier.classifyMotionState(speedKmh = 0.5f)

        assertEquals("stay_candidate", state)
    }

    @Test
    fun buildStaySegment_returnsNullForTransitSamples() {
        val segment = TransitStayClassifier.buildStaySegment(
            samples = listOf(
                sample("2026-06-06T10:00:00Z", 55.6761, 12.5683, 22f),
                sample("2026-06-06T10:20:00Z", 55.6861, 12.5783, 24f),
            ),
        )

        assertNull(segment)
    }

    @Test
    fun buildStaySegment_returnsStayWhenLowMovementPersistsWithinRadius() {
        val segment = TransitStayClassifier.buildStaySegment(
            samples = listOf(
                sample("2026-06-06T10:00:00Z", 55.6761, 12.5683, 0.0f),
                sample("2026-06-06T10:10:00Z", 55.6763, 12.5684, 0.2f),
                sample("2026-06-06T10:25:00Z", 55.6762, 12.5685, 0.3f),
            ),
        )

        assertNotNull(segment)
        assertEquals(25, segment?.durationMinutes)
        assertEquals("stay", segment?.classification)
    }

    @Test
    fun buildStaySegment_rejectsLowMovementOutsideRadius() {
        val segment = TransitStayClassifier.buildStaySegment(
            samples = listOf(
                sample("2026-06-06T10:00:00Z", 55.6761, 12.5683, 0.0f),
                sample("2026-06-06T10:10:00Z", 55.6861, 12.5783, 0.0f),
                sample("2026-06-06T10:25:00Z", 55.6762, 12.5685, 0.0f),
            ),
        )

        assertNull(segment)
    }

    private fun sample(
        timestampUtc: String,
        latitude: Double,
        longitude: Double,
        speedKmh: Float?,
    ): CaptureSample = CaptureSample(
        timestampUtc = Instant.parse(timestampUtc),
        latitude = latitude,
        longitude = longitude,
        speedKmh = speedKmh,
    )
}
