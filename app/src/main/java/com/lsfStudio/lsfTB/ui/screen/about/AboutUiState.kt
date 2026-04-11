package com.lsfStudio.lsfTB.ui.screen.about

import androidx.compose.runtime.Immutable
import com.lsfStudio.lsfTB.ui.util.LatestVersionInfo

@Immutable
data class AboutUiState(
    val title: String,
    val appName: String,
    val versionName: String,
    val links: List<LinkInfo>,
    val isCheckingUpdate: Boolean = false,
    val latestVersionInfo: LatestVersionInfo = LatestVersionInfo.Empty,
    val showUpToDateDialog: Boolean = false, // 显示“已是最新版本”对话框
)

@Immutable
data class AboutScreenActions(
    val onBack: () -> Unit,
    val onOpenLink: (String) -> Unit,
    val onCheckUpdate: () -> Unit,
    val onDismissUpToDateDialog: () -> Unit, // 关闭“已是最新版本”对话框
)
