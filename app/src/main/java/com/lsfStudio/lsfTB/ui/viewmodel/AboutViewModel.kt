package com.lsfStudio.lsfTB.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.lsfStudio.lsfTB.ui.screen.about.AboutUiState
import com.lsfStudio.lsfTB.ui.util.LatestVersionInfo
import com.lsfStudio.lsfTB.ui.util.checkNewVersion

/**
 * 关于页面 ViewModel
 * 
 * 管理关于页面的 UI 状态，包括版本信息和更新检查
 */
class AboutViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        AboutUiState(
            title = "关于",
            appName = "lsfTB",
            versionName = "",
            links = emptyList(),
            isCheckingUpdate = false,
            latestVersionInfo = LatestVersionInfo.Empty
        )
    )
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    /**
     * 初始化关于页面信息
     * 
     * @param appName 应用名称
     * @param versionName 版本名称
     * @param links 链接列表
     */
    fun initialize(appName: String, versionName: String, links: List<com.lsfStudio.lsfTB.ui.screen.about.LinkInfo>) {
        _uiState.value = _uiState.value.copy(
            appName = appName,
            versionName = versionName,
            links = links
        )
    }

    /**
     * 手动检查更新
     * 
     * @param context 上下文，用于网络检查和 API 调用
     */
    fun checkUpdate(context: Context) {
        viewModelScope.launch {
            // 设置检查中状态
            _uiState.value = _uiState.value.copy(isCheckingUpdate = true, showUpToDateDialog = false)
            
            try {
                // 执行检查更新
                val latestVersion = checkNewVersion(context)
                
                // 获取当前版本代码
                val currentVersionCode = com.lsfStudio.lsfTB.BuildConfig.VERSION_CODE
                
                // 比较版本号
                val hasUpdate = latestVersion.versionCode > currentVersionCode
                
                // 更新状态
                _uiState.value = _uiState.value.copy(
                    isCheckingUpdate = false,
                    latestVersionInfo = if (hasUpdate) latestVersion else LatestVersionInfo.Empty,
                    showUpToDateDialog = !hasUpdate // 如果没有更新，显示“已是最新版本”对话框
                )
            } catch (e: Exception) {
                // 发生异常时重置状态
                _uiState.value = _uiState.value.copy(
                    isCheckingUpdate = false,
                    latestVersionInfo = LatestVersionInfo.Empty,
                    showUpToDateDialog = false
                )
            }
        }
    }
    
    /**
     * 关闭“已是最新版本”对话框
     */
    fun dismissUpToDateDialog() {
        _uiState.value = _uiState.value.copy(showUpToDateDialog = false)
    }
}
