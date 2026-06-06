package com.klaustracker.app.ui

import android.content.pm.ApplicationInfo
import android.preference.PreferenceManager
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

enum class HomeTab {
    Timeline,
    Map,
    Places,
    Suggestions,
    Summary,
    Settings,
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LocalAppHome(
    onOpenSettings: () -> Unit,
    trackingEnabled: Boolean,
    appViewModel: AppViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(HomeTab.Timeline) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val demoModeEnabled = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    LaunchedEffect(trackingEnabled) {
        appViewModel.setTrackingEnabled(trackingEnabled)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Navigation",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                HomeTab.entries.forEach { candidate ->
                    NavigationDrawerItem(
                        label = { Text(candidate.name) },
                        selected = tab == candidate,
                        onClick = {
                            tab = candidate
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = tab.name) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Text("Menu")
                        }
                    },
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = if (trackingEnabled) {
                            "Tracking enabled"
                        } else {
                            "Tracking disabled"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = if (uiState.periodicCaptureEnabled) {
                            "Background capture every 30 minutes"
                        } else {
                            "Background capture off"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                when (tab) {
                    HomeTab.Timeline -> {
                        TimelineTab(
                            isLoading = uiState.isLoading,
                            captures = uiState.timelineCaptures,
                            demoModeEnabled = demoModeEnabled,
                            onAddDemoCapture = appViewModel::addDemoCapture,
                            onDeleteCapture = appViewModel::deleteTimelineCapture,
                            undoAvailable = uiState.timelineUndoAvailable,
                            undoMessage = uiState.timelineUndoMessage,
                            onUndoDelete = appViewModel::undoDeleteTimelineCapture,
                            onDismissUndo = appViewModel::dismissTimelineUndo,
                        )
                    }

                    HomeTab.Map -> MapTab(
                        captures = uiState.captures,
                    )

                    HomeTab.Places -> PlacesTab(
                        places = uiState.places,
                        actionMessage = uiState.placeActionMessage,
                        undoAvailable = uiState.placeUndoAvailable,
                        undoMessage = uiState.placeUndoMessage,
                        onRelabelPlace = appViewModel::relabelPlace,
                        onMergePlaces = appViewModel::mergePlaces,
                        onDeletePlace = appViewModel::deletePlace,
                        onUndoDelete = appViewModel::undoDeletePlace,
                        onDismissUndo = appViewModel::dismissPlaceUndo,
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
                        demoModeEnabled = demoModeEnabled,
                        onAddDemoCapture = appViewModel::addDemoCapture,
                    )
                }
            }
        }
    }
}

@Composable
private fun MapTab(
    captures: List<com.klaustracker.app.data.local.entity.CapturePointEntity>,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val paddingPx = with(LocalDensity.current) { 80.dp.roundToPx() }
    val mapView = remember {
        Configuration.getInstance().load(
            appContext,
            PreferenceManager.getDefaultSharedPreferences(appContext),
        )
        Configuration.getInstance().userAgentValue = appContext.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            setTilesScaledToDpi(true)
            controller.setZoom(14.0)
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            Configuration.getInstance().save(
                appContext,
                PreferenceManager.getDefaultSharedPreferences(appContext),
            )
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Map",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "OpenStreetMap with zoom/pan, path lines, and motion markers.",
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                factory = { mapView },
                update = { view ->
                    view.overlays.clear()

                    val orderedCaptures = captures.sortedBy { it.timestampUtc }
                    val points = orderedCaptures.map { GeoPoint(it.latitude, it.longitude) }
                    val latestCaptureId = orderedCaptures.maxByOrNull { it.timestampUtc }?.id

                    if (points.size >= 2) {
                        val polyline = Polyline(view).apply {
                            setPoints(points)
                            outlinePaint.color = android.graphics.Color.parseColor("#8FA1AE")
                            outlinePaint.strokeWidth = 6f
                        }
                        view.overlays.add(polyline)
                    }

                    orderedCaptures.forEach { capture ->
                        val isLatest = capture.id == latestCaptureId
                        val marker = Marker(view).apply {
                            position = GeoPoint(capture.latitude, capture.longitude)
                            title = if (isLatest) {
                                "Latest capture"
                            } else {
                                motionLabel(capture.motionState)
                            }
                            snippet = buildString {
                                append(formatTimelineTimestamp(capture.timestampUtc))
                                append(" | ")
                                append(motionLabel(capture.motionState))
                            }
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        view.overlays.add(marker)
                    }

                    when (points.size) {
                        0 -> {
                            view.controller.setZoom(5.0)
                            view.controller.setCenter(GeoPoint(56.2639, 9.5018))
                        }
                        1 -> {
                            view.controller.setZoom(16.0)
                            view.controller.setCenter(points.first())
                        }
                        else -> {
                            val bounds = BoundingBox(
                                points.maxOf { it.latitude },
                                points.maxOf { it.longitude },
                                points.minOf { it.latitude },
                                points.minOf { it.longitude },
                            )
                            view.post {
                                if (view.width > 0 && view.height > 0) {
                                    view.zoomToBoundingBox(bounds, true, paddingPx)
                                } else {
                                    view.controller.setCenter(points.last())
                                    view.controller.setZoom(15.0)
                                }
                            }
                        }
                    }

                    view.postInvalidate()
                },
            )
        }

        if (captures.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("No captures yet. Map tiles are shown; overlays appear after live captures are recorded.")
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
    captures: List<com.klaustracker.app.data.local.model.CaptureTimelineRow>,
    demoModeEnabled: Boolean,
    onAddDemoCapture: () -> Unit,
    onDeleteCapture: (String) -> Unit,
    undoAvailable: Boolean,
    undoMessage: String?,
    onUndoDelete: () -> Unit,
    onDismissUndo: () -> Unit,
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

        if (demoModeEnabled) {
            Button(onClick = onAddDemoCapture, modifier = Modifier.fillMaxWidth()) {
                Text("Add demo capture")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (undoAvailable) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = undoMessage ?: "Location removed.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = onUndoDelete) {
                        Text("Undo")
                    }
                    OutlinedButton(onClick = onDismissUndo) {
                        Text("Dismiss")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

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
            Text(
                if (demoModeEnabled) {
                    "No captures yet. Tap 'Add demo capture' to seed local timeline."
                } else {
                    "No captures yet. Live captures will appear here after tracking records them."
                }
            )
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

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onDeleteCapture(capture.id)
                            true
                        } else {
                            false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                        ) {
                            Text(
                                text = "Delete",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd),
                            )
                        }
                    },
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = formatTimelineTimestamp(capture.timestampUtc),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "Lat ${formatCoordinate(capture.latitude)}, Lng ${formatCoordinate(capture.longitude)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = motionLabel(capture.motionState),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "Speed: ${formatSpeed(capture.speedKmh)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "Address: ${capture.enrichedAddress ?: "Unavailable"}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "POI: ${capture.poiName ?: "Unavailable"} (${capture.poiType ?: "n/a"})",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (capture.isHotel == true) {
                                Text(
                                    text = "Hotel match",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
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

private val timelineTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun formatTimelineTimestamp(timestampUtc: String): String {
    val localDateTime = runCatching {
        Instant.parse(timestampUtc)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }.getOrNull() ?: return timestampUtc

    return timelineTimestampFormatter.format(localDateTime)
}

private fun formatCoordinate(value: Double): String =
    String.format(Locale.US, "%.5f", value)

private fun formatSpeed(speedKmh: Float?): String {
    return if (speedKmh == null) {
        "Unavailable"
    } else {
        String.format(Locale.US, "%.1f km/h", speedKmh)
    }
}

@Composable
private fun PlacesTab(
    places: List<com.klaustracker.app.data.local.entity.PlaceEntity>,
    actionMessage: String?,
    undoAvailable: Boolean,
    undoMessage: String?,
    onRelabelPlace: (placeId: String, labelType: String, customLabel: String?) -> Unit,
    onMergePlaces: (sourcePlaceId: String, targetPlaceId: String) -> Unit,
    onDeletePlace: (String) -> Unit,
    onUndoDelete: () -> Unit,
    onDismissUndo: () -> Unit,
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
            Text("No places yet. Detected stays will appear here after enough location history is collected.")
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

        if (undoAvailable) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = undoMessage ?: "Place removed.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = onUndoDelete) {
                        Text("Undo")
                    }
                    OutlinedButton(onClick = onDismissUndo) {
                        Text("Dismiss")
                    }
                }
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
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onDeletePlace(place.id)
                            if (selectedPlaceId == place.id) {
                                selectedPlaceId = null
                                mergeTargetPlaceId = null
                                customLabel = ""
                            }
                            true
                        } else {
                            false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                        ) {
                            Text(
                                text = "Delete",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd),
                            )
                        }
                    },
                ) {
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
    demoModeEnabled: Boolean,
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

        if (demoModeEnabled) {
            OutlinedButton(onClick = onAddDemoCapture, modifier = Modifier.fillMaxWidth()) {
                Text("Insert demo capture")
            }
        }
    }
}
