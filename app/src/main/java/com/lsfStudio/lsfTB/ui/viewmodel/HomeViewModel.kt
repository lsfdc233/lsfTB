package com.lsfStudio.lsfTB.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.lsfStudio.lsfTB.BuildConfig
import com.lsfStudio.lsfTB.data.repository.SettingsRepository
import com.lsfStudio.lsfTB.data.repository.SettingsRepositoryImpl
import com.lsfStudio.lsfTB.ui.screen.home.HomeUiState
import com.lsfStudio.lsfTB.ui.util.LatestVersionInfo
import com.lsfStudio.lsfTB.ui.util.checkNewVersion

/**
 * 主页 ViewModel
 * 
 * 管理主页的 UI 状态，包括应用信息
 */
class HomeViewModel(
    private val context: Context,
    private val repo: SettingsRepository = SettingsRepositoryImpl(context)
) : ViewModel() {

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
     * @param context 上下文
     * @param forceCheck 是否强制检查更新
     */
    fun refresh(context: Context, forceCheck: Boolean = false) {
        // 不再在此处执行任何操作
    }
    
    /**
     * 检查更新（由 HomeMiuix 调用）
     * 
     * @param context 上下文
     * @param checkUpdateEnabled 用户是否启用了自动检查更新（已废弃，从 SettingsRepository 读取）
     */
    fun checkUpdate(context: Context, checkUpdateEnabled: Boolean = true) {
        viewModelScope.launch {
            // 从 SettingsRepository 读取用户的 checkUpdate 设置
            val enabled = repo.checkUpdate
            
            if (!enabled) {
                android.util.Log.d("HomeViewModel", "⚙️ 用户禁用了自动检查更新，直接启用所有控件")
                // Settings 开关关闭时，直接设置 isChecked = true，允许所有控件操作
                _uiState.value = _uiState.value.copy(isChecked = true)
                return@launch
            }
            
            try {
                android.util.Log.d("HomeViewModel", "🔄 开始检查更新...")
                
                // 执行检查更新
                val latestVersion = checkNewVersion(context)
                
                android.util.Log.d("HomeViewModel", "获取到最新版本: ${latestVersion.versionName}, versionCode: ${latestVersion.versionCode}")
                
                // 获取当前版本代码
                val currentVersionCode = BuildConfig.VERSION_CODE
                
                android.util.Log.d("HomeViewModel", "当前版本 code: $currentVersionCode")
                
                // 比较版本号
                val hasUpdate = latestVersion.versionCode > currentVersionCode
                
                android.util.Log.d("HomeViewModel", "是否有更新: $hasUpdate")
                
                // 检查是否有错误信息
                if (latestVersion.errorCode != 0 || latestVersion.errorMessage.isNotEmpty()) {
                    // 有错误，显示错误 Toast
                    android.util.Log.e("HomeViewModel", "检查更新错误: ${latestVersion.errorCode} - ${latestVersion.errorMessage}")
                    
                    val toastMessage = if (latestVersion.errorCode > 0) {
                        "${latestVersion.errorCode}: ${latestVersion.errorMessage}"
                    } else {
                        latestVersion.errorMessage
                    }
                    
                    // 在主线程显示 Toast
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        try {
                            com.lsfStudio.lsfTB.ui.util.MessageManager.showToast(
                                context,
                                toastMessage,
                                android.widget.Toast.LENGTH_LONG
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("HomeViewModel", "显示 Toast 失败", e)
                        }
                    }
                    
                    // 更新状态
                    _uiState.value = _uiState.value.copy(
                        latestVersionInfo = LatestVersionInfo.Empty,
                        isChecked = true // 标记为已检查
                    )
                    
                    // 🌐 检查更新完成后，执行 OOBE 服务器测试
                    com.lsfStudio.lsfTB.ui.util.OOBE.testServerConnection(context)
                } else {
                    // 无错误，正常处理
                    if (hasUpdate) {
                        // 发现新版本，更新状态以触发弹窗
                        android.util.Log.d("HomeViewModel", "✅ 发现新版本: ${latestVersion.versionName}")
                        _uiState.value = _uiState.value.copy(
                            latestVersionInfo = latestVersion,
                            isChecked = true // 标记为已检查
                        )
                        
                        // 🌐 检查更新完成后，执行 OOBE 服务器测试
                        com.lsfStudio.lsfTB.ui.util.OOBE.testServerConnection(context)
                    } else {
                        // 未发现新版本，显示 Toast
                        android.util.Log.d("HomeViewModel", "✅ 当前已是最新版本")
                        
                        // 清除之前的更新信息
                        _uiState.value = _uiState.value.copy(
                            latestVersionInfo = LatestVersionInfo.Empty,
                            isChecked = true // 标记为已检查
                        )
                        
                        // 🌐 检查更新完成后，执行 OOBE 服务器测试
                        com.lsfStudio.lsfTB.ui.util.OOBE.testServerConnection(context)
                        
                        // 在主线程显示 Toast
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            try {
                                com.lsfStudio.lsfTB.ui.util.MessageManager.showToast(
                                    context,
                                    "当前已是最新版本",
                                    android.widget.Toast.LENGTH_SHORT
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("HomeViewModel", "显示 Toast 失败", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // 发生异常时打印错误
                android.util.Log.e("HomeViewModel", "❌ 检查更新失败: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    latestVersionInfo = LatestVersionInfo.Empty,
                    isChecked = true // 标记为已检查（即使失败也允许继续）
                )
                
                // 🌐 即使检查更新失败，也执行 OOBE 服务器测试
                com.lsfStudio.lsfTB.ui.util.OOBE.testServerConnection(context)
            }
        }
    }
    
    /**
     * 关闭"已是最新版本"对话框
     */
    fun dismissUpToDateDialog() {
        _uiState.value = _uiState.value.copy(latestVersionInfo = LatestVersionInfo.Empty)
    }
    
    /**
     * 清除最新版本信息（用于关闭更新对话框后）
     */
    fun clearLatestVersionInfo() {
        _uiState.value = _uiState.value.copy(latestVersionInfo = LatestVersionInfo.Empty)
    }
    
    /**
     * 设置 isChecked 状态
     */
    fun setIsChecked(checked: Boolean) {
        _uiState.value = _uiState.value.copy(isChecked = checked)
    }
}
