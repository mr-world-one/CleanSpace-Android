package com.cleanspace.presentation.scan

import com.cleanspace.data.models.ScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple in-memory holder for the last scan result.
 * MVP-only: later replace with SavedStateHandle/Room caching.
 */
object ScanResultHolder {
    private val _lastScanFlow = MutableStateFlow<ScanResult?>(null)
    val lastScanFlow: StateFlow<ScanResult?> = _lastScanFlow.asStateFlow()

    var lastScan: ScanResult?
        get() = _lastScanFlow.value
        set(value) {
            _lastScanFlow.value = value
        }
}
