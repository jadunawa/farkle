package com.orangezest.farkle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orangezest.farkle.update.ApkDownloader
import com.orangezest.farkle.update.UpdateAvailable
import com.orangezest.farkle.update.UpdateChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data class Available(val info: UpdateAvailable) : UpdateUiState
    data object Downloading : UpdateUiState
    data class ReadyToInstall(val apkFile: File) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateChecker: UpdateChecker,
    private val apkDownloader: ApkDownloader,
) : ViewModel() {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    init {
        checkForUpdate()
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            val update = updateChecker.check()
            if (update != null) {
                _state.value = UpdateUiState.Available(update)
            }
        }
    }

    fun onUserAcceptsUpdate(info: UpdateAvailable) {
        _state.value = UpdateUiState.Downloading
        viewModelScope.launch {
            val file = apkDownloader.download(info.downloadUrl)
            _state.value = if (file != null) {
                UpdateUiState.ReadyToInstall(file)
            } else {
                UpdateUiState.Error("Download failed")
            }
        }
    }

    fun dismiss() {
        _state.value = UpdateUiState.Idle
    }
}
