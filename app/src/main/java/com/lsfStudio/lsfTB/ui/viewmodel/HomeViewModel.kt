package com.lsfStudio.lsfTB.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.lsfStudio.lsfTB.ui.screen.home.HomeUiState
import com.lsfStudio.lsfTB.ui.util.LatestVersionInfo
import com.lsfStudio.lsfTB.ui.util.checkNewVersion

/**
 * 主页 ViewModel
 * 
 * 管理主页的 UI 状态，包括应用信息和更新检查
 */
class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            appName = "lsfTB",
            appVersion = "1.0.0",
            appVersionCode = 1L,
            isSafeMode = false,
            checkUpdateEnabled = true,
            latestVersionInfo = LatestVersionInfo.Empty
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * 刷新主页信息
     * 
     * @param context 上下文，用于检查更新
     * @param forceCheck 是否强制检查更新（忽略用户设置）
     */
    fun refresh(context: Context, forceCheck: Boolean = false) {
        viewModelScope.launch {
            // 如果启用了检查更新或强制检查，则检查新版本
            if (_uiState.value.checkUpdateEnabled || forceCheck) {
                val latestVersion = checkNewVersion(context)
                _uiState.value = _uiState.value.copy(
                    latestVersionInfo = latestVersion
                )
            }
        }
    }
}
