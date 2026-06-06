package com.klaustracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

enum class HomeTab {
    Timeline,
    Places,
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

            HomeTab.Places -> PlacesTab(
                places = uiState.places,
                actionMessage = uiState.placeActionMessage,
                onRelabelPlace = appViewModel::relabelPlace,
                onMergePlaces = appViewModel::mergePlaces,
                onDismissMessage = appViewModel::clearPlaceActionMessage,
            )
            HomeTab.Summary -> SummaryTab(
                summaries = uiState.placeSummaries,
                visitDetails = uiState.visitDetails,
                selectedPlaceId = uiState.selectedPlaceId,
                selectedVisitId = uiState.selectedVisitId,
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
private fun TimelineTab(
    isLoading: Boolean,
    captures: List<com.klaustracker.app.data.local.entity.CapturePointEntity>,
    onAddDemoCapture: () -> Unit,
) {
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

        if (isLoading) {
            Text("Loading local data...")
            return
        }

        if (captures.isEmpty()) {
            Text("No captures yet. Tap 'Add demo capture' to seed local timeline.")
            return
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(captures) { capture ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = capture.timestampUtc, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Lat ${capture.latitude}, Lng ${capture.longitude}",
                            style = MaterialTheme.typography.bodySmall,
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
