package com.lsfStudio.lsfTB.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.lsfStudio.lsfTB.BuildConfig
import com.lsfStudio.lsfTB.ui.screen.home.HomeUiState
import com.lsfStudio.lsfTB.ui.util.LatestVersionInfo

/**
 * 主页 ViewModel
 * 
 * 管理主页的 UI 状态，包括应用信息
 */
class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            appName = "lsfTB",
            appVersion = "${BuildConfig.VERSION_NAME}_${BuildConfig.VERSION_CODE}",
            appVersionCode = BuildConfig.VERSION_CODE.toLong(),
            isSafeMode = false,
            checkUpdateEnabled = true,
            latestVersionInfo = LatestVersionInfo.Empty
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * 刷新主页信息
     * 
     * @param context 上下文（保留参数以兼容旧代码）
     * @param forceCheck 是否强制检查更新（保留参数以兼容旧代码，已废弃）
     */
    fun refresh(context: Context, forceCheck: Boolean = false) {
        // 启动时检查更新已移至 OOBE 模块
        // 此方法保留仅为兼容性，不再执行任何操作
    }
}
