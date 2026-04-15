package com.cleanspace.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cleanspace.data.models.ScanProgress
import com.cleanspace.domain.usecases.ScanFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanFilesUseCase: ScanFilesUseCase,
) : ViewModel() {

    sealed interface ScanState {
        data object Idle : ScanState
        data object Scanning : ScanState
        data object Completed : ScanState
        data object Cancelled : ScanState
        data class Error(val message: String) : ScanState
    }

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _progress = MutableStateFlow(ScanProgress(0, "", 0, 0, false, 0))
    val progress: StateFlow<ScanProgress> = _progress.asStateFlow()

    private var currentJob: Job? = null

    fun startScan() {
        if (currentJob?.isActive == true) return
        currentJob = viewModelScope.launch {
            _scanState.value = ScanState.Scanning
            try {
                val result = scanFilesUseCase { p -> _progress.update { p } }
                ScanResultHolder.lastScan = result
                _scanState.value = ScanState.Completed
            } catch (_: CancellationException) {
                _scanState.value = ScanState.Cancelled
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun cancelScan() {
        currentJob?.cancel()
        scanFilesUseCase.cancel()
    }

    override fun onCleared() {
        cancelScan()
        super.onCleared()
    }
}
