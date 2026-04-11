package com.lsfStudio.lsfTB.ui.screen.settings

import androidx.compose.runtime.Immutable

@Immutable
data class SettingsUiState(
    val checkUpdate: Boolean = true,
    val themeMode: Int = 0,
    val miuixMonet: Boolean = false,
    val keyColor: Int = 0,
    val colorStyle: String = "TonalSpot",
    val colorSpec: String = "Default",
    val enablePredictiveBack: Boolean = false,
    val enableBlur: Boolean = true,
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
    val pageScale: Float = 1.0f,
    val enableWebDebugging: Boolean = false,
    val enableSmoothCorner: Boolean = true,
)

@Immutable
data class SettingsScreenActions(
    val onSetCheckUpdate: (Boolean) -> Unit,
    val onOpenTheme: () -> Unit,
    val onSetEnableWebDebugging: (Boolean) -> Unit,
    val onOpenAbout: () -> Unit,
)
