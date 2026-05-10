package com.lsfStudio.lsfTB.ui.util

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * Debug 模式检测工具
 * 
 * 检查用户权限配置中是否启用了 Debugger 模式
 */
object Debug {
    private const val TAG = "Debug"
    
    /**
     * 检查当前用户是否启用了 Debug 模式
     * 
     * @param context 上下文
     * @return true 如果启用了 Debug 模式，否则 false
     */
    fun isDebugEnabled(context: Context): Boolean {
        try {
            // 从 UserManager 获取用户信息
            val userInfo = UserManager.getUserInfo(context) ?: run {
                Log.d(TAG, "ℹ️ No user info found")
                return false
            }
            
            // 检查 permissions 字段
            val permissionsBlob = userInfo.permissions
            
            if (permissionsBlob == null || permissionsBlob.isEmpty()) {
                Log.d(TAG, "ℹ️ No permissions data found")
                return false
            }
            
            // 解析 Blob 数据
            val permissionsJson = String(permissionsBlob, Charsets.UTF_8)
            Log.d(TAG, "📋 Permissions JSON: $permissionsJson")
            
            val jsonObject = JSONObject(permissionsJson)
            val debuggerEnabled = jsonObject.optBoolean("Debugger", false)
            
            Log.d(TAG, "🔍 Debugger mode: $debuggerEnabled")
            return debuggerEnabled
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to check debug mode", e)
            return false
        }
    }
}
