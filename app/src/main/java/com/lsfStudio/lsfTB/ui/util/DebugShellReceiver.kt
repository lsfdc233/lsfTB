package com.lsfStudio.lsfTB.ui.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.lsfStudio.lsfTB.BuildConfig

/**
 * Debug Shell 调试接口
 * 
 * 在 Debug 模式下，允许通过 ADB shell 命令动态修改应用配置
 * 
 * 使用方法：
 * ```bash
 * # 设置构建时间
 * adb shell am broadcast -a com.lsfStudio.lsfTB.DEBUG_SHELL --es command "set_build_time" --es value "2026-04-25 12:00:00"
 * 
 * # 设置构建类型
 * adb shell am broadcast -a com.lsfStudio.lsfTB.DEBUG_SHELL --es command "set_build_type" --es value "release"
 * 
 * # 获取当前配置
 * adb shell am broadcast -a com.lsfStudio.lsfTB.DEBUG_SHELL --es command "get_config"
 * 
 * # 重置所有配置
 * adb shell am broadcast -a com.lsfStudio.lsfTB.DEBUG_SHELL --es command "reset_config"
 * 
 * # 退出登录
 * adb shell am broadcast -a com.lsfStudio.lsfTB.DEBUG_SHELL --es command "logout"
 * ```
 */
class DebugShellReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "DebugShellReceiver"
        const val ACTION_DEBUG_SHELL = "com.lsfStudio.lsfTB.DEBUG_SHELL"
        const val EXTRA_COMMAND = "command"
        const val EXTRA_VALUE = "value"
        
        // SharedPreferences 文件名
        private const val PREFS_NAME = "debug_config"
        
        // 配置键名
        private const val KEY_BUILD_TIME = "build_time"
        private const val KEY_BUILD_TYPE = "build_type"
        private const val KEY_DEV_MODE_ENABLED = "dev_mode_enabled"  // 开发者模式开关
        
        /**
         * 获取动态配置的 SharedPreferences
         */
        fun getDebugPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        
        /**
         * 获取构建时间（优先使用动态配置）
         */
        fun getBuildTime(context: Context): String {
            if (!BuildConfig.DEBUG) {
                return BuildConfig.BUILD_TIME
            }
            
            val prefs = getDebugPrefs(context)
            return prefs.getString(KEY_BUILD_TIME, BuildConfig.BUILD_TIME) ?: BuildConfig.BUILD_TIME
        }
        
        /**
         * 获取构建类型（优先使用动态配置）
         */
        fun getBuildType(context: Context): String {
            if (!BuildConfig.DEBUG) {
                return BuildConfig.BUILD_TYPE
            }
            
            val prefs = getDebugPrefs(context)
            return prefs.getString(KEY_BUILD_TYPE, BuildConfig.BUILD_TYPE) ?: BuildConfig.BUILD_TYPE
        }
        
        /**
         * 检查开发者模式是否启用
         * Release 模式下可通过验证设备标识符启用
         */
        fun isDevModeEnabled(context: Context): Boolean {
            val prefs = getDebugPrefs(context)
            return prefs.getBoolean(KEY_DEV_MODE_ENABLED, false)
        }
        
        /**
         * 启用开发者模式
         */
        fun enableDevMode(context: Context) {
            val prefs = getDebugPrefs(context)
            prefs.edit().putBoolean(KEY_DEV_MODE_ENABLED, true).apply()
            Log.d(TAG, "✅ 开发者模式已启用")
        }
        
        /**
         * 禁用开发者模式
         */
        fun disableDevMode(context: Context) {
            val prefs = getDebugPrefs(context)
            prefs.edit().putBoolean(KEY_DEV_MODE_ENABLED, false).apply()
            Log.d(TAG, "✅ 开发者模式已禁用")
        }
        
        /**
         * 重置所有动态配置
         */
        fun resetConfig(context: Context) {
            val prefs = getDebugPrefs(context)
            prefs.edit().clear().apply()
            Log.d(TAG, "✅ 已重置所有动态配置")
        }
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.e(TAG, "❌ Context 或 Intent 为空")
            return
        }
        
        // 安全检查：仅允许本地 ADB 调用（检查 UID）
        // 注意：ADB 广播可能不包含 UID 信息，所以只记录不阻止
        val callingUid = intent.getIntExtra("android.intent.extra.UID", -1)
        if (callingUid != -1 && callingUid != android.os.Process.myUid()) {
            Log.w(TAG, "⚠️ 警告: 来自其他应用的调用: UID=$callingUid")
            // 不阻止，仅记录警告
        }
        
        // 检查是否启用开发者模式（Release 模式下也需要检查）
        val isDevMode = BuildConfig.DEBUG || isDevModeEnabled(context)
        if (!isDevMode) {
            Log.w(TAG, "⚠️ 未启用开发者模式，忽略调试命令")
            return
        }
        
        val command = intent.getStringExtra(EXTRA_COMMAND)
        val value = intent.getStringExtra(EXTRA_VALUE)
        
        Log.d(TAG, "📥 收到调试命令: command=$command, value=$value")
        
        when (command) {
            "set_build_time" -> {
                handleSetBuildTime(context, value)
            }
            "set_build_type" -> {
                handleSetBuildType(context, value)
            }
            "get_config" -> {
                handleGetConfig(context)
            }
            "reset_config" -> {
                handleResetConfig(context)
            }
            "logout" -> {
                handleLogout(context)
            }
            else -> {
                Log.e(TAG, "❌ 未知命令: $command")
                logUsage()
            }
        }
    }
    
    /**
     * 设置构建时间
     */
    private fun handleSetBuildTime(context: Context, value: String?) {
        if (value.isNullOrEmpty()) {
            Log.e(TAG, "❌ 构建时间不能为空")
            return
        }
        
        // 简单验证时间格式（YYYY-MM-DD HH:MM:SS）
        val timeRegex = Regex("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$")
        if (!timeRegex.matches(value)) {
            Log.e(TAG, "❌ 时间格式错误，应为: YYYY-MM-DD HH:MM:SS")
            return
        }
        
        val prefs = getDebugPrefs(context)
        prefs.edit().putString(KEY_BUILD_TIME, value).apply()
        
        Log.d(TAG, "✅ 构建时间已设置为: $value")
        Log.d(TAG, "💡 提示: 重启应用后生效")
    }
    
    /**
     * 设置构建类型
     */
    private fun handleSetBuildType(context: Context, value: String?) {
        if (value.isNullOrEmpty()) {
            Log.e(TAG, "❌ 构建类型不能为空")
            return
        }
        
        // 验证构建类型
        val validTypes = listOf("debug", "release", "beta")
        if (value.lowercase() !in validTypes) {
            Log.e(TAG, "❌ 无效的构建类型: $value (有效值: ${validTypes.joinToString(", ")})")
            return
        }
        
        val prefs = getDebugPrefs(context)
        prefs.edit().putString(KEY_BUILD_TYPE, value.lowercase()).apply()
        
        Log.d(TAG, "✅ 构建类型已设置为: $value")
        Log.d(TAG, "💡 提示: 重启应用后生效")
    }
    
    /**
     * 获取当前配置
     */
    private fun handleGetConfig(context: Context) {
        val buildTime = getBuildTime(context)
        val buildType = getBuildType(context)
        
        Log.d(TAG, "📋 当前配置:")
        Log.d(TAG, "  - 构建时间: $buildTime")
        Log.d(TAG, "  - 构建类型: $buildType")
        Log.d(TAG, "  - 应用版本: ${BuildConfig.VERSION_NAME}_${BuildConfig.VERSION_CODE}")
        Log.d(TAG, "  - Debug 模式: ${BuildConfig.DEBUG}")
    }
    
    /**
     * 重置配置
     */
    private fun handleResetConfig(context: Context) {
        resetConfig(context)
        Log.d(TAG, "✅ 所有动态配置已重置为默认值")
        Log.d(TAG, "💡 提示: 重启应用后生效")
    }
    
    /**
     * 退出登录
     */
    private fun handleLogout(context: Context) {
        if (!com.lsfStudio.lsfTB.ui.util.AccountManager.isLoggedIn(context)) {
            Log.w(TAG, "⚠️ 用户未登录，无需退出")
            return
        }
        
        com.lsfStudio.lsfTB.ui.util.AccountManager.clearUserInfo(context)
        Log.d(TAG, "✅ 已退出登录")
        Log.d(TAG, "💡 提示: 请重启应用或刷新界面以生效")
    }
    
    /**
     * 输出使用说明
     */
    private fun logUsage() {
        Log.d(TAG, """
            📖 使用说明:
            
            1. 设置构建时间:
               adb shell am broadcast -a $ACTION_DEBUG_SHELL --es command "set_build_time" --es value "2026-04-25 12:00:00"
            
            2. 设置构建类型:
               adb shell am broadcast -a $ACTION_DEBUG_SHELL --es command "set_build_type" --es value "release"
            
            3. 查看当前配置:
               adb shell am broadcast -a $ACTION_DEBUG_SHELL --es command "get_config"
            
            4. 重置所有配置:
               adb shell am broadcast -a $ACTION_DEBUG_SHELL --es command "reset_config"
            
            5. 退出登录:
               adb shell am broadcast -a $ACTION_DEBUG_SHELL --es command "logout"
            
            ⚠️ 注意:
            - Debug 模式或通过验证启用开发者模式后可用
            - 修改后需重启应用生效
            - 配置存储在 SharedPreferences 中
        """.trimIndent())
    }
}
