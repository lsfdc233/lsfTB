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
import com.lsfStudio.lsfTB.ui.util.DownloadManager

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
            // 设置检查中状态（不清除 latestVersionInfo，允许在下载时检查）
            _uiState.value = _uiState.value.copy(isCheckingUpdate = true, showUpToDateDialog = false)
                
            try {
                android.util.Log.d("AboutViewModel", "开始检查更新...")
                    
                // 执行检查更新
                val latestVersion = checkNewVersion(context)
                    
                android.util.Log.d("AboutViewModel", "获取到最新版本: ${latestVersion.versionName}, versionCode: ${latestVersion.versionCode}")
                    
                // 获取当前版本代码
                val currentVersionCode = com.lsfStudio.lsfTB.BuildConfig.VERSION_CODE
                    
                android.util.Log.d("AboutViewModel", "当前版本 code: $currentVersionCode")
                    
                // 比较版本号
                val hasUpdate = latestVersion.versionCode > currentVersionCode
                    
                android.util.Log.d("AboutViewModel", "是否有更新: $hasUpdate")
                                
                // 检查是否有错误信息
                if (latestVersion.errorCode != 0 || latestVersion.errorMessage.isNotEmpty()) {
                    // 有错误，显示错误 Toast
                    android.util.Log.e("AboutViewModel", "检查更新错误: ${latestVersion.errorCode} - ${latestVersion.errorMessage}")
                                    
                    val toastMessage = if (latestVersion.errorCode > 0) {
                        "${latestVersion.errorCode}: ${latestVersion.errorMessage}"
                    } else {
                        latestVersion.errorMessage
                    }
                                    
                    // 在主线程显示 Toast（使用MessageManager自动适配超级岛）
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        try {
                            com.lsfStudio.lsfTB.ui.util.MessageManager.showToast(
                                context,
                                toastMessage,
                                android.widget.Toast.LENGTH_LONG
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("AboutViewModel", "显示 Toast 失败", e)
                        }
                    }
                                    
                    // 更新状态
                    _uiState.value = _uiState.value.copy(
                        isCheckingUpdate = false,
                        latestVersionInfo = LatestVersionInfo.Empty,
                        showUpToDateDialog = false
                    )
                } else {
                    // 无错误，正常处理
                    // 更新状态（保留最新的版本信息）
                    _uiState.value = _uiState.value.copy(
                        isCheckingUpdate = false,
                        latestVersionInfo = if (hasUpdate) latestVersion else _uiState.value.latestVersionInfo,
                        showUpToDateDialog = !hasUpdate // 如果没有更新，显示“已是最新版本”对话框
                    )
                }
            } catch (e: Exception) {
                // 发生异常时重置状态
                android.util.Log.e("AboutViewModel", "检查更新失败: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isCheckingUpdate = false,
                    latestVersionInfo = LatestVersionInfo.Empty,
                    showUpToDateDialog = false
                )
            }
        }
    }
    
    /**
     * 关闭"已是最新版本"对话框
     */
    fun dismissUpToDateDialog() {
        _uiState.value = _uiState.value.copy(showUpToDateDialog = false)
    }
    
    /**
     * 清除最新版本信息（用于关闭更新对话框后）
     */
    fun clearLatestVersionInfo() {
        _uiState.value = _uiState.value.copy(latestVersionInfo = LatestVersionInfo.Empty)
    }
    
    /**
     * 开始下载更新
     */
    fun startDownload(context: Context) {
        val versionInfo = _uiState.value.latestVersionInfo
        
        android.util.Log.d("AboutViewModel", "========== 开始下载更新 ==========")
        android.util.Log.d("AboutViewModel", "版本信息: ${versionInfo.versionName}")
        android.util.Log.d("AboutViewModel", "下载地址: ${versionInfo.downloadUrl}")
        android.util.Log.d("AboutViewModel", "更新日志: ${versionInfo.changelog}")
        
        if (versionInfo.downloadUrl.isEmpty()) {
            android.util.Log.e("AboutViewModel", "错误: 下载地址为空！")
            return
        }
        
        viewModelScope.launch {
            try {
                // 生成文件名，格式：lsfTB_vx.x.x_xxx.apk
                val fileName = "lsfTB_${versionInfo.versionName}.apk"
                
                android.util.Log.d("AboutViewModel", "开始调用 DownloadManager.download()")
                android.util.Log.d("AboutViewModel", "文件名: $fileName")
                
                DownloadManager.download(
                    context = context,
                    url = versionInfo.downloadUrl,
                    fileName = fileName,
                    versionName = versionInfo.versionName,
                    callback = object : DownloadManager.DownloadCallback {
                        override fun onProgress(progress: Int, speed: String, remainingTime: String) {
                            // 进度更新已在 DownloadManager 中处理通知
                            android.util.Log.d("AboutViewModel", "下载进度: $progress%, 速度: $speed, 剩余: $remainingTime")
                        }
                        
                        override fun onSuccess(file: java.io.File) {
                            android.util.Log.d("AboutViewModel", "========== 下载成功 ==========")
                            android.util.Log.d("AboutViewModel", "文件路径: ${file.absolutePath}")
                            android.util.Log.d("AboutViewModel", "文件大小: ${file.length()} bytes")
                            // 下载成功后，可以自动安装或提示用户
                        }
                        
                        override fun onError(error: String) {
                            android.util.Log.e("AboutViewModel", "========== 下载失败 ==========")
                            android.util.Log.e("AboutViewModel", "错误信息: $error")
                            // 可以在这里显示错误提示
                        }
                    }
                )
                
                android.util.Log.d("AboutViewModel", "DownloadManager.download() 调用完成")
                
            } catch (e: Exception) {
                android.util.Log.e("AboutViewModel", "下载过程中发生异常", e)
            }
        }
    }
}
