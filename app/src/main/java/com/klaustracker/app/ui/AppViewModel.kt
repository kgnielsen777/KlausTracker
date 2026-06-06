package com.klaustracker.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.klaustracker.app.data.TrackerRepository
import com.klaustracker.app.data.local.TrackerDatabaseProvider
import com.klaustracker.app.data.local.entity.CapturePointEntity
import com.klaustracker.app.data.local.entity.PlaceEntity
import com.klaustracker.app.tracking.TrackingScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val captures: List<CapturePointEntity> = emptyList(),
    val places: List<PlaceEntity> = emptyList(),
    val isLoading: Boolean = true,
    val periodicCaptureEnabled: Boolean = false,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TrackerRepository(TrackerDatabaseProvider.database(application))
    private val trackingScheduler = TrackingScheduler(application)

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeRecentCaptures().collect { captures ->
                _uiState.update {
                    it.copy(captures = captures, isLoading = false)
                }
            }
        }

        viewModelScope.launch {
            repository.observeActivePlaces().collect { places ->
                _uiState.update { it.copy(places = places) }
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
}
