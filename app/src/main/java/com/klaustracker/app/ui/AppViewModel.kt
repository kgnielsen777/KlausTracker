package com.klaustracker.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.klaustracker.app.data.TrackerRepository
import com.klaustracker.app.data.local.TrackerDatabaseProvider
import com.klaustracker.app.data.local.entity.CapturePointEntity
import com.klaustracker.app.data.local.entity.PlaceEntity
import com.klaustracker.app.data.local.model.CaptureTimelineRow
import com.klaustracker.app.data.local.model.PlaceDurationSummaryRow
import com.klaustracker.app.data.local.model.PlaceSuggestionRow
import com.klaustracker.app.data.local.model.VisitDetailRow
import com.klaustracker.app.tracking.TrackingScheduler
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val captures: List<CapturePointEntity> = emptyList(),
    val timelineCaptures: List<CaptureTimelineRow> = emptyList(),
    val places: List<PlaceEntity> = emptyList(),
    val placeSummaries: List<PlaceDurationSummaryRow> = emptyList(),
    val pendingSuggestions: List<PlaceSuggestionRow> = emptyList(),
    val visitDetails: List<VisitDetailRow> = emptyList(),
    val isLoading: Boolean = true,
    val periodicCaptureEnabled: Boolean = false,
    val timelineUndoAvailable: Boolean = false,
    val timelineUndoMessage: String? = null,
    val placeUndoAvailable: Boolean = false,
    val placeUndoMessage: String? = null,
    val selectedPlaceId: String? = null,
    val selectedVisitId: String? = null,
    val placeActionMessage: String? = null,
    val selectedSummaryPeriod: SummaryPeriod = SummaryPeriod.Week,
)

enum class SummaryPeriod(val label: String, val daysBack: Long) {
    Day("Day", 1),
    Week("Week", 7),
    Month("Month", 30),
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TrackerRepository(TrackerDatabaseProvider.database(application))
    private val trackingScheduler = TrackingScheduler(application)
    private var visitDetailsJob: Job? = null
    private val summaryPeriod = MutableStateFlow(SummaryPeriod.Week)

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var lastDeletedCaptureSnapshot: TrackerRepository.DeletedCaptureSnapshot? = null
    private var lastDeletedPlaceSnapshot: TrackerRepository.DeletedPlaceSnapshot? = null

    init {
        viewModelScope.launch {
            repository.refreshDetectedPlacesFromRecentCaptures()
        }

        viewModelScope.launch {
            repository.observeRecentCaptures().collect { captures ->
                _uiState.update {
                    it.copy(captures = captures, isLoading = false)
                }
            }
        }

        viewModelScope.launch {
            repository.observeRecentTimelineCaptures().collect { captures ->
                _uiState.update {
                    it.copy(timelineCaptures = captures, isLoading = false)
                }
            }
        }

        viewModelScope.launch {
            repository.observeActivePlaces().collect { places ->
                _uiState.update { it.copy(places = places) }
            }
        }

        viewModelScope.launch {
            summaryPeriod
                .flatMapLatest { period -> repository.observePlaceDurationSummaries(period.sinceUtc()) }
                .collect { summaries ->
                _uiState.update { it.copy(placeSummaries = summaries) }
                repository.refreshRecurringSuggestions(summaries)
            }
        }

        viewModelScope.launch {
            repository.observePendingPlaceSuggestions().collect { suggestions ->
                _uiState.update { it.copy(pendingSuggestions = suggestions) }
            }
        }
    }

    fun addDemoCapture() {
        viewModelScope.launch {
            repository.addDemoCapture()
        }
    }

    fun setTrackingEnabled(enabled: Boolean) {
        if (enabled) {
            trackingScheduler.startPeriodicCapture()
        } else {
            trackingScheduler.stopPeriodicCapture()
        }
        _uiState.update { it.copy(periodicCaptureEnabled = enabled) }
    }

    fun captureNow() {
        trackingScheduler.captureNow()
    }

    fun deleteTimelineCapture(captureId: String) {
        viewModelScope.launch {
            val snapshot = repository.deleteCapture(captureId)
            lastDeletedCaptureSnapshot = snapshot
            _uiState.update {
                it.copy(
                    timelineUndoAvailable = snapshot != null,
                    timelineUndoMessage = if (snapshot != null) {
                        "Removed location from timeline."
                    } else {
                        null
                    },
                )
            }
        }
    }

    fun undoDeleteTimelineCapture() {
        viewModelScope.launch {
            val snapshot = lastDeletedCaptureSnapshot ?: return@launch
            repository.restoreDeletedCapture(snapshot)
            lastDeletedCaptureSnapshot = null
            _uiState.update {
                it.copy(
                    timelineUndoAvailable = false,
                    timelineUndoMessage = null,
                )
            }
        }
    }

    fun dismissTimelineUndo() {
        lastDeletedCaptureSnapshot = null
        _uiState.update {
            it.copy(
                timelineUndoAvailable = false,
                timelineUndoMessage = null,
            )
        }
    }

    fun selectPlace(placeId: String) {
        visitDetailsJob?.cancel()
        _uiState.update { it.copy(selectedPlaceId = placeId, selectedVisitId = null, visitDetails = emptyList()) }
        visitDetailsJob = viewModelScope.launch {
            repository.observeVisitDetailsForPlace(placeId).collect { visits ->
                _uiState.update {
                    it.copy(
                        selectedPlaceId = placeId,
                        selectedVisitId = visits.firstOrNull()?.visitId,
                        visitDetails = visits,
                    )
                }
            }
        }
    }

    fun selectVisit(visitId: String) {
        _uiState.update { it.copy(selectedVisitId = visitId) }
    }

    fun relabelPlace(placeId: String, labelType: String, customLabel: String? = null) {
        viewModelScope.launch {
            val success = repository.updatePlaceLabel(
                placeId = placeId,
                labelType = labelType,
                customLabel = customLabel,
            )
            _uiState.update {
                it.copy(
                    placeActionMessage = if (success) {
                        "Updated label for selected place."
                    } else {
                        "Could not update place label."
                    }
                )
            }
        }
    }

    fun mergePlaces(sourcePlaceId: String, targetPlaceId: String) {
        viewModelScope.launch {
            val success = repository.mergePlaces(
                sourcePlaceId = sourcePlaceId,
                targetPlaceId = targetPlaceId,
            )
            _uiState.update {
                it.copy(
                    placeActionMessage = if (success) {
                        "Merged places successfully."
                    } else {
                        "Could not merge places."
                    }
                )
            }
        }
    }

    fun deletePlace(placeId: String) {
        viewModelScope.launch {
            val snapshot = repository.deletePlace(placeId)
            lastDeletedPlaceSnapshot = snapshot
            _uiState.update {
                it.copy(
                    placeUndoAvailable = snapshot != null,
                    placeUndoMessage = if (snapshot != null) {
                        "Removed place."
                    } else {
                        null
                    },
                    placeActionMessage = if (snapshot == null) "Could not remove place." else null,
                )
            }
        }
    }

    fun undoDeletePlace() {
        viewModelScope.launch {
            val snapshot = lastDeletedPlaceSnapshot ?: return@launch
            repository.restoreDeletedPlace(snapshot)
            lastDeletedPlaceSnapshot = null
            _uiState.update {
                it.copy(
                    placeUndoAvailable = false,
                    placeUndoMessage = null,
                )
            }
        }
    }

    fun dismissPlaceUndo() {
        lastDeletedPlaceSnapshot = null
        _uiState.update {
            it.copy(
                placeUndoAvailable = false,
                placeUndoMessage = null,
            )
        }
    }

    fun clearPlaceActionMessage() {
        _uiState.update { it.copy(placeActionMessage = null) }
    }

    fun acceptSuggestion(suggestionId: String) {
        viewModelScope.launch {
            val success = repository.acceptSuggestion(suggestionId)
            _uiState.update {
                it.copy(
                    placeActionMessage = if (success) {
                        "Suggestion accepted."
                    } else {
                        "Could not accept suggestion."
                    }
                )
            }
        }
    }

    fun dismissSuggestion(suggestionId: String) {
        viewModelScope.launch {
            val success = repository.dismissSuggestion(suggestionId)
            _uiState.update {
                it.copy(
                    placeActionMessage = if (success) {
                        "Suggestion dismissed."
                    } else {
                        "Could not dismiss suggestion."
                    }
                )
            }
        }
    }

    fun setSummaryPeriod(period: SummaryPeriod) {
        summaryPeriod.value = period
        _uiState.update { it.copy(selectedSummaryPeriod = period) }
    }

    private fun SummaryPeriod.sinceUtc(): String {
        return Instant.now().minusSeconds(daysBack * 24 * 60 * 60).toString()
    }
}
