package com.lsfStudio.lsfTB.ui.screen.home

import androidx.compose.runtime.Immutable

@Immutable
data class HomeUiState(
    val appName: String = "lsfTB",
    val appVersion: String = "1.0.0",
    val appVersionCode: Long = 1,
    val isSafeMode: Boolean = false,
    val checkUpdateEnabled: Boolean = true,
)

@Immutable
data class HomeActions(
    val onSettingsClick: () -> Unit,
    val onAboutClick: () -> Unit,
    val onOpenAbout: () -> Unit,
)
