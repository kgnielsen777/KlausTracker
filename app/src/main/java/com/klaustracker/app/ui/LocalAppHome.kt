package com.klaustracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.abs

enum class HomeTab {
    Timeline,
    Map,
    Places,
    Suggestions,
    Summary,
    Settings,
}

@Composable
fun LocalAppHome(
    onOpenSettings: () -> Unit,
    trackingEnabled: Boolean,
    appViewModel: AppViewModel = viewModel(),
) {
    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(HomeTab.Timeline) }

    LaunchedEffect(trackingEnabled) {
        appViewModel.setTrackingEnabled(trackingEnabled)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "KlausTracker",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (trackingEnabled) {
                "Tracking enabled"
            } else {
                "Tracking disabled"
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = if (uiState.periodicCaptureEnabled) {
                "Background capture: every 30 minutes"
            } else {
                "Background capture: off"
            },
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            HomeTab.entries.forEach { candidate ->
                val selected = candidate == tab
                if (selected) {
                    Button(onClick = { tab = candidate }, modifier = Modifier.weight(1f)) {
                        Text(candidate.name)
                    }
                } else {
                    OutlinedButton(onClick = { tab = candidate }, modifier = Modifier.weight(1f)) {
                        Text(candidate.name)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (tab) {
            HomeTab.Timeline -> {
                TimelineTab(
                    isLoading = uiState.isLoading,
                    captures = uiState.captures,
                    onAddDemoCapture = appViewModel::addDemoCapture,
                )
            }

            HomeTab.Map -> MapTab(
                captures = uiState.captures,
            )

            HomeTab.Places -> PlacesTab(
                places = uiState.places,
                actionMessage = uiState.placeActionMessage,
                onRelabelPlace = appViewModel::relabelPlace,
                onMergePlaces = appViewModel::mergePlaces,
                onDismissMessage = appViewModel::clearPlaceActionMessage,
            )
            HomeTab.Suggestions -> SuggestionsTab(
                suggestions = uiState.pendingSuggestions,
                actionMessage = uiState.placeActionMessage,
                onAcceptSuggestion = appViewModel::acceptSuggestion,
                onDismissSuggestion = appViewModel::dismissSuggestion,
                onDismissMessage = appViewModel::clearPlaceActionMessage,
            )
            HomeTab.Summary -> SummaryTab(
                summaries = uiState.placeSummaries,
                visitDetails = uiState.visitDetails,
                selectedPlaceId = uiState.selectedPlaceId,
                selectedVisitId = uiState.selectedVisitId,
                summaryPeriod = uiState.selectedSummaryPeriod,
                onSummaryPeriodChange = appViewModel::setSummaryPeriod,
                onPlaceSelected = appViewModel::selectPlace,
                onVisitSelected = appViewModel::selectVisit,
            )
            HomeTab.Settings -> SettingsTab(
                onOpenSettings = onOpenSettings,
                onCaptureNow = appViewModel::captureNow,
                onAddDemoCapture = appViewModel::addDemoCapture,
            )
        }
    }
}

@Composable
private fun MapTab(
    captures: List<com.klaustracker.app.data.local.entity.CapturePointEntity>,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Map",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Local projection of captured path with transit and stay overlays.",
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (captures.size < 2) {
            Text("Need at least 2 captures before rendering map overlays.")
            return
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            ) {
                val projected = projectCaptures(
                    captures = captures,
                    canvasWidth = size.width,
                    canvasHeight = size.height,
                )

                drawRect(color = Color(0xFFF3F6F8))

                for (i in 0 until projected.size - 1) {
                    val current = projected[i]
                    val next = projected[i + 1]
                    drawLine(
                        color = Color(0xFF8FA1AE),
                        start = current.point,
                        end = next.point,
                        strokeWidth = 2f,
                    )
                }

                for (i in 0 until projected.size - 1) {
                    val current = projected[i]
                    val next = projected[i + 1]
                    if (current.bucket == MotionBucket.Transit && next.bucket == MotionBucket.Transit) {
                        drawLine(
                            color = Color(0xFFE67E22),
                            start = current.point,
                            end = next.point,
                            strokeWidth = 4f,
                        )
                    }
                }

                projected.forEach { marker ->
                    val color = when (marker.bucket) {
                        MotionBucket.Transit -> Color(0xFFE67E22)
                        MotionBucket.Stay -> Color(0xFF2E7D32)
                        MotionBucket.Other -> Color(0xFF1976D2)
                    }
                    val radius = when (marker.bucket) {
                        MotionBucket.Transit -> 6f
                        MotionBucket.Stay -> 7f
                        MotionBucket.Other -> 5f
                    }
                    drawCircle(color = color, radius = radius, center = marker.point)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MapLegendItem(color = Color(0xFFE67E22), label = "Transit")
            MapLegendItem(color = Color(0xFF2E7D32), label = "Stay")
            MapLegendItem(color = Color(0xFF1976D2), label = "Other")
        }
    }
}

@Composable
private fun MapLegendItem(color: Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .padding(top = 4.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = color)
            }
        }
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

private data class ProjectedCapture(
    val point: Offset,
    val bucket: MotionBucket,
)

private fun projectCaptures(
    captures: List<com.klaustracker.app.data.local.entity.CapturePointEntity>,
    canvasWidth: Float,
    canvasHeight: Float,
): List<ProjectedCapture> {
    val minLat = captures.minOf { it.latitude }
    val maxLat = captures.maxOf { it.latitude }
    val minLng = captures.minOf { it.longitude }
    val maxLng = captures.maxOf { it.longitude }

    val latSpan = if (abs(maxLat - minLat) < 0.00001) 0.00001 else maxLat - minLat
    val lngSpan = if (abs(maxLng - minLng) < 0.00001) 0.00001 else maxLng - minLng

    val left = 24f
    val right = 24f
    val top = 24f
    val bottom = 24f
    val width = canvasWidth - left - right
    val height = canvasHeight - top - bottom

    return captures.map { capture ->
        val x = (((capture.longitude - minLng) / lngSpan).toFloat() * width) + left
        val y = (((maxLat - capture.latitude) / latSpan).toFloat() * height) + top
        ProjectedCapture(
            point = Offset(x, y),
            bucket = motionBucket(capture.motionState),
        )
    }
}

@Composable
private fun SuggestionsTab(
    suggestions: List<com.klaustracker.app.data.local.model.PlaceSuggestionRow>,
    actionMessage: String?,
    onAcceptSuggestion: (String) -> Unit,
    onDismissSuggestion: (String) -> Unit,
    onDismissMessage: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Suggestions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!actionMessage.isNullOrBlank()) {
            Text(text = actionMessage, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                onClick = onDismissMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                Text("Dismiss")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (suggestions.isEmpty()) {
            Text("No pending suggestions yet. Keep tracking to build recurring-place hints.")
            return
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestions) { suggestion ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(suggestion.placeName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = suggestion.defaultAddress ?: suggestion.customLabel ?: suggestion.labelType,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "Suggest label: ${suggestion.suggestedLabelType} (${(suggestion.confidence * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = suggestion.reason,
                            style = MaterialTheme.typography.bodySmall,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onAcceptSuggestion(suggestion.suggestionId) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Accept")
                            }
                            OutlinedButton(
                                onClick = { onDismissSuggestion(suggestion.suggestionId) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineTab(
    isLoading: Boolean,
    captures: List<com.klaustracker.app.data.local.entity.CapturePointEntity>,
    onAddDemoCapture: () -> Unit,
) {
    var filter by remember { mutableStateOf(TimelineFilter.All) }
    val filteredCaptures = captures.filter { filter.matches(it.motionState) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Timeline",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onAddDemoCapture, modifier = Modifier.fillMaxWidth()) {
            Text("Add demo capture")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TimelineFilter.entries.forEach { candidate ->
                FilterChip(
                    selected = filter == candidate,
                    onClick = { filter = candidate },
                    label = { Text(candidate.label) },
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Legend: Transit (moving) | Stay (stationary) | Other",
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Text("Loading local data...")
            return
        }

        if (filteredCaptures.isEmpty()) {
            Text("No captures yet. Tap 'Add demo capture' to seed local timeline.")
            return
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredCaptures) { capture ->
                val motionBucket = motionBucket(capture.motionState)
                val cardColor = when (motionBucket) {
                    MotionBucket.Transit -> MaterialTheme.colorScheme.secondaryContainer
                    MotionBucket.Stay -> MaterialTheme.colorScheme.tertiaryContainer
                    MotionBucket.Other -> MaterialTheme.colorScheme.surfaceVariant
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = capture.timestampUtc, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Lat ${capture.latitude}, Lng ${capture.longitude}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = motionLabel(capture.motionState),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Source: ${capture.source} | Motion: ${capture.motionState}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

private enum class MotionBucket {
    Transit,
    Stay,
    Other,
}

private enum class TimelineFilter(val label: String) {
    All("All"),
    Transit("Transit"),
    Stay("Stay"),
    Other("Other");

    fun matches(motionState: String): Boolean {
        val bucket = motionBucket(motionState)
        return when (this) {
            All -> true
            Transit -> bucket == MotionBucket.Transit
            Stay -> bucket == MotionBucket.Stay
            Other -> bucket == MotionBucket.Other
        }
    }
}

private fun motionBucket(motionState: String): MotionBucket {
    return when (motionState.lowercase()) {
        "transit", "driving" -> MotionBucket.Transit
        "stay_candidate", "stationary" -> MotionBucket.Stay
        else -> MotionBucket.Other
    }
}

private fun motionLabel(motionState: String): String {
    return when (motionBucket(motionState)) {
        MotionBucket.Transit -> "Transit segment"
        MotionBucket.Stay -> "Stay segment"
        MotionBucket.Other -> "Other / unknown"
    }
}

@Composable
private fun PlacesTab(
    places: List<com.klaustracker.app.data.local.entity.PlaceEntity>,
    actionMessage: String?,
    onRelabelPlace: (placeId: String, labelType: String, customLabel: String?) -> Unit,
    onMergePlaces: (sourcePlaceId: String, targetPlaceId: String) -> Unit,
    onDismissMessage: () -> Unit,
) {
    var selectedPlaceId by remember { mutableStateOf<String?>(null) }
    var mergeTargetPlaceId by remember { mutableStateOf<String?>(null) }
    var customLabel by remember { mutableStateOf("") }

    val selectedPlace = places.firstOrNull { it.id == selectedPlaceId }
    val mergeCandidates = places.filter { it.id != selectedPlaceId }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Places",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (places.isEmpty()) {
            Text("No places yet. A demo place is created when you add the first demo capture.")
            return
        }

        if (!actionMessage.isNullOrBlank()) {
            Text(
                text = actionMessage,
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = onDismissMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                Text("Dismiss")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (selectedPlace != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Manage selected place", style = MaterialTheme.typography.titleSmall)
                    Text(selectedPlace.canonicalName, style = MaterialTheme.typography.bodyMedium)
                    Text(selectedPlace.defaultAddress ?: "No address", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onRelabelPlace(selectedPlace.id, "home", null) },
                            modifier = Modifier.weight(1f),
                        ) { Text("Home") }
                        OutlinedButton(
                            onClick = { onRelabelPlace(selectedPlace.id, "work", null) },
                            modifier = Modifier.weight(1f),
                        ) { Text("Work") }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onRelabelPlace(selectedPlace.id, "friend", null) },
                            modifier = Modifier.weight(1f),
                        ) { Text("Friend") }
                        OutlinedButton(
                            onClick = { onRelabelPlace(selectedPlace.id, "family", null) },
                            modifier = Modifier.weight(1f),
                        ) { Text("Family") }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it },
                        label = { Text("Custom label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { onRelabelPlace(selectedPlace.id, "custom", customLabel) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Apply custom label")
                    }

                    if (mergeCandidates.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Merge this place into", style = MaterialTheme.typography.titleSmall)

                        mergeCandidates.forEach { candidate ->
                            OutlinedButton(
                                onClick = { mergeTargetPlaceId = candidate.id },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                            ) {
                                val marker = if (mergeTargetPlaceId == candidate.id) "* " else ""
                                Text("$marker${candidate.canonicalName}")
                            }
                        }

                        Button(
                            onClick = {
                                val target = mergeTargetPlaceId
                                if (target != null) {
                                    onMergePlaces(selectedPlace.id, target)
                                    selectedPlaceId = null
                                    mergeTargetPlaceId = null
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            enabled = mergeTargetPlaceId != null,
                        ) {
                            Text("Merge places")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(places) { place ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedPlaceId = place.id
                            mergeTargetPlaceId = null
                            customLabel = place.customLabel ?: ""
                        },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(place.canonicalName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Label: ${place.labelType}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = place.defaultAddress ?: "No address",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (selectedPlaceId == place.id) {
                            Text(
                                text = "Selected",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryTab(
    summaries: List<com.klaustracker.app.data.local.model.PlaceDurationSummaryRow>,
    visitDetails: List<com.klaustracker.app.data.local.model.VisitDetailRow>,
    selectedPlaceId: String?,
    selectedVisitId: String?,
    summaryPeriod: SummaryPeriod,
    onSummaryPeriodChange: (SummaryPeriod) -> Unit,
    onPlaceSelected: (String) -> Unit,
    onVisitSelected: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            SummaryPeriod.entries.forEach { candidate ->
                val selected = candidate == summaryPeriod
                if (selected) {
                    Button(onClick = { onSummaryPeriodChange(candidate) }, modifier = Modifier.weight(1f)) {
                        Text(candidate.label)
                    }
                } else {
                    OutlinedButton(onClick = { onSummaryPeriodChange(candidate) }, modifier = Modifier.weight(1f)) {
                        Text(candidate.label)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (summaries.isEmpty()) {
            Text("No place summaries yet. Add a few captures to build totals.")
            return
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(summaries) { summary ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlaceSelected(summary.placeId) },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(summary.placeName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "${summary.totalDurationMinutes} min across ${summary.visitCount} visit(s)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = summary.defaultAddress ?: summary.customLabel ?: summary.labelType,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (selectedPlaceId == summary.placeId) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Visits", style = MaterialTheme.typography.titleSmall)
                            if (visitDetails.isEmpty()) {
                                Text("No visits loaded yet.")
                            } else {
                                visitDetails.forEach { visit ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp)
                                            .clickable { onVisitSelected(visit.visitId) },
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("${visit.startUtc} → ${visit.endUtc}")
                                            Text("${visit.durationMinutes} min, radius ${visit.radiusMeters} m")
                                            if (selectedVisitId == visit.visitId) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "Detail: ${visit.placeName} | ${visit.defaultAddress ?: "No address"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                                Text(
                                                    text = "Stay ${visit.classification} at ${visit.centroidLat}, ${visit.centroidLng}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                                Text(
                                                    text = "Enriched address: ${visit.enrichedAddress ?: "Unavailable"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                                Text(
                                                    text = "POI: ${visit.poiName ?: "Unavailable"} (${visit.poiType ?: "n/a"})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                                Text(
                                                    text = if (visit.isHotel == true) {
                                                        "Hotel indicator: yes"
                                                    } else {
                                                        "Hotel indicator: no"
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    onOpenSettings: () -> Unit,
    onCaptureNow: () -> Unit,
    onAddDemoCapture: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Use app settings to manage location permissions and background behavior.")

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Open Android app settings")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onCaptureNow, modifier = Modifier.fillMaxWidth()) {
            Text("Capture current location now")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(onClick = onAddDemoCapture, modifier = Modifier.fillMaxWidth()) {
            Text("Insert demo capture")
        }
    }
}
