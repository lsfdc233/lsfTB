/**
 * 主Activity的ViewModel
 * 
 * 职责：
 * 1. 管理全局UI状态（主题、模糊效果、页面缩放等）
 * 2. 监听SharedPreferences变化并自动更新UI
 * 3. 提供Lifecycle感知的StateFlow供UI层观察
 * 
 * @property context 应用上下文，用于访问SharedPreferences和Repository
 */
package com.lsfStudio.lsfTB.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.lsfStudio.lsfTB.data.repository.SettingsRepository
import com.lsfStudio.lsfTB.data.repository.SettingsRepositoryImpl
import com.lsfStudio.lsfTB.ui.theme.ThemeController
class MainActivityViewModel(
    private val context: Context
) : ViewModel() {

    // SharedPreferences实例，用于存储用户设置
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    // 设置仓库，用于读写各种配置项
    private val settingRepo: SettingsRepository = SettingsRepositoryImpl(context)
    
    // SharedPreferences变化监听器
    // 当监听的键发生变化时，自动重新读取UI状态
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        // 如果key为null（表示所有键都可能变化）或者在观察列表中，则更新UI状态
        if (key == null || key in observedKeys) {
            _uiState.value = readUiState()
        }
    }

    // 内部可变状态流
    private val _uiState = MutableStateFlow(readUiState())
    
    // 对外暴露的不可变状态流，UI层通过观察此流获取最新状态
    val uiState: StateFlow<MainActivityUiState> = _uiState.asStateFlow()

    /**
     * ViewModel初始化块
     * 注册SharedPreferences监听器，开始监听设置变化
     */
    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * ViewModel销毁时的清理工作
     * 取消注册监听器，防止内存泄漏
     */
    override fun onCleared() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
        super.onCleared()
    }

    /**
     * 读取当前UI状态
     * 
     * 从各个数据源收集最新的配置值，构建完整的UI状态对象
     * 
     * @return 包含所有UI配置的状态对象
     */
    private fun readUiState(): MainActivityUiState {
        return MainActivityUiState(
            // 应用主题设置（颜色模式、Monet等）
            appSettings = ThemeController.getAppSettings(context),
            // 页面缩放比例（0.8-1.1）
            pageScale = settingRepo.pageScale,
            // 是否启用模糊效果
            enableBlur = settingRepo.enableBlur,
            // 是否启用浮动底栏
            enableFloatingBottomBar = settingRepo.enableFloatingBottomBar,
            // 是否启用浮动底栏的模糊效果
            enableFloatingBottomBarBlur = settingRepo.enableFloatingBottomBarBlur,
            // 是否启用平滑圆角
            enableSmoothCorner = settingRepo.enableSmoothCorner,
        )
    }

    /**
     * 需要观察的SharedPreferences键列表
     * 
     * 只有这些键发生变化时才会触发UI状态更新
     * 这样可以避免不必要的重绘，提高性能
     */
    private companion object {
        val observedKeys = setOf(
            "color_mode",                      // 颜色模式
            "key_color",                       // 主题色
            "color_style",                     // 颜色样式
            "color_spec",                      // 颜色规范版本
            "page_scale",                      // 页面缩放
            "enable_blur",                     // 模糊效果
            "enable_floating_bottom_bar",      // 浮动底栏
            "enable_floating_bottom_bar_blur", // 底栏模糊
            "enable_smooth_corner",            // 平滑圆角
        )
    }
}
