package com.lsfStudio.lsfTB.ui.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

/**
 * Shizuku 连接状态管理
 */
object ShizukuUtil {
    
    /**
     * 检查 Shizuku 是否已授权
     */
    fun isShizukuAuthorized(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查 Shizuku 服务是否可用
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查 Shizuku 是否已连接（授权且可用）
     */
    fun isConnected(): Boolean {
        return isShizukuAuthorized() && isShizukuAvailable()
    }
    
    /**
     * 请求 Shizuku 权限
     */
    fun requestShizukuPermission(requestCode: Int = 0) {
        try {
            // 先检查 Binder 是否可用
            if (!isShizukuAvailable()) {
                android.util.Log.w("ShizukuUtil", "Shizuku Binder 不可用，请确保 Shizuku 服务已启动")
                return
            }
            
            // 检查是否已授权
            if (isShizukuAuthorized()) {
                android.util.Log.i("ShizukuUtil", "Shizuku 已授权")
                return
            }
            
            // 请求权限
            android.util.Log.i("ShizukuUtil", "请求 Shizuku 权限...")
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
            android.util.Log.e("ShizukuUtil", "请求 Shizuku 权限失败: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    /**
     * 添加权限监听器
     */
    fun addPermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            Shizuku.addRequestPermissionResultListener(listener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 移除权限监听器
     */
    fun removePermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            Shizuku.removeRequestPermissionResultListener(listener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Composable 函数：获取 Shizuku 连接状态（带自动刷新）
 */
@Composable
fun rememberShizukuConnectionState(): MutableState<Boolean> {
    val isConnected = remember { mutableStateOf(ShizukuUtil.isConnected()) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // 初始检查和持续监听
    LaunchedEffect(refreshTrigger) {
        // 立即检查一次
        isConnected.value = ShizukuUtil.isConnected()
        
        // 添加绑定监听器
        val binderReceivedListener = Shizuku.OnBinderReceivedListener {
            isConnected.value = ShizukuUtil.isConnected()
        }
        
        val binderDeadListener = Shizuku.OnBinderDeadListener {
            isConnected.value = false
        }
        
        // 添加权限结果监听器
        val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            isConnected.value = grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        
        try {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 定期刷新状态（每2秒检查一次）
        while (true) {
            delay(2000)
            val newState = ShizukuUtil.isConnected()
            if (isConnected.value != newState) {
                isConnected.value = newState
            }
        }
    }
    
    return isConnected
}
