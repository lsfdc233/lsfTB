package com.lsfStudio.lsfTB.ui.screen.home

import androidx.compose.runtime.Immutable
import com.lsfStudio.lsfTB.ui.util.LatestVersionInfo

@Immutable
data class HomeUiState(
    val appName: String = "lsfTB",
    val appVersion: String = "1.0.0",
    val appVersionCode: Long = 1,
    val isSafeMode: Boolean = false,
    val checkUpdateEnabled: Boolean = true,
    val latestVersionInfo: LatestVersionInfo = LatestVersionInfo.Empty,
    val isChecked: Boolean = false, // 是否已检查过更新
)

@Immutable
data class HomeActions(
    val onSettingsClick: () -> Unit,
    val onAboutClick: () -> Unit,
    val onOpenAbout: () -> Unit,
    val onCheckUpdate: (android.content.Context, Boolean) -> Unit, // 检查更新
    val onClearLatestVersionInfo: () -> Unit, // 清除最新版本信息
)
