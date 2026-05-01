package com.lsfStudio.lsfTB.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.lsfStudio.lsfTB.ui.theme.AppSettings

@Immutable
data class MainActivityUiState(
    val appSettings: AppSettings,
    val pageScale: Float,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val enableSmoothCorner: Boolean,
    val disableAllAnimations: Boolean,  // 去掉所有动画效果
)
