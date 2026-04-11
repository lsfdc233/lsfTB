package com.lsfStudio.lsfTB.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.lsfStudio.lsfTB.ui.screen.home.HomeUiState

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            appName = "lsfTB",
            appVersion = "1.0.0",
            appVersionCode = 1L,
            isSafeMode = false,
            checkUpdateEnabled = true
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun refresh() {
        // 硬编码模式无需刷新
    }
}
