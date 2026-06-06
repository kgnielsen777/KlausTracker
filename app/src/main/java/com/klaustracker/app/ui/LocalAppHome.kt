package com.klaustracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

            HomeTab.Places -> PlacesTab(uiState.places)
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
private fun PlacesTab(places: List<com.klaustracker.app.data.local.entity.PlaceEntity>) {
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

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(places) { place ->
                Card(modifier = Modifier.fillMaxWidth()) {
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
