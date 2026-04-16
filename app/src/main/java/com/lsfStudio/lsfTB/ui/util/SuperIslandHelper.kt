package com.lsfStudio.lsfTB.ui.util

import android.content.Context
import android.os.Build
import android.view.Gravity
import android.widget.Toast

/**
 * 小米超级岛助手
 * 用于在 HyperOS 3.0+ 上显示超级岛通知
 */
object SuperIslandHelper {
    
    private const val TAG = "SuperIslandHelper"
    
    /**
     * 显示超级岛消息
     * 
     * @param context 上下文
     * @param message 消息内容
     * @param type 消息类型
     * @param priority 优先级
     * @return true 如果成功显示
     */
    fun showMessage(
        context: Context,
        message: String,
        type: MessageManager.MessageType,
        priority: MessageManager.Priority
    ): Boolean {
        return try {
            when (type) {
                MessageManager.MessageType.TOAST -> {
                    showSuperIslandToast(context, message, priority)
                }
                MessageManager.MessageType.NOTIFICATION -> {
                    showSuperIslandNotification(context, message, priority)
                }
                MessageManager.MessageType.SUPER_ISLAND -> {
                    showSuperIslandNotification(context, message, priority)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "显示超级岛失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 显示超级岛Toast样式
     */
    private fun showSuperIslandToast(
        context: Context,
        message: String,
        priority: MessageManager.Priority
    ): Boolean {
        return try {
            // 尝试多种可能的超级岛 API
            val apiClasses = listOf(
                "miuix.appcompat.app.SuperIsland",
                "com.miui.superisland.SuperIslandManager",
                "android.app.SuperIslandManager",
                "com.hyperos.island.SuperIsland"
            )
            
            for (className in apiClasses) {
                try {
                    val clazz = Class.forName(className)
                    
                    // 尝试不同的方法名
                    val methodNames = listOf("showToast", "show", "displayToast")
                    for (methodName in methodNames) {
                        try {
                            val method = clazz.getMethod(methodName, Context::class.java, String::class.java, Int::class.java)
                            val islandPriority = convertPriority(priority)
                            method.invoke(null, context, message, islandPriority)
                            android.util.Log.d(TAG, "成功使用超级岛 API: $className.$methodName")
                            return true
                        } catch (e: NoSuchMethodException) {
                            // 尝试其他方法名
                            continue
                        }
                    }
                } catch (e: ClassNotFoundException) {
                    // 尝试下一个类
                    continue
                }
            }
            
            android.util.Log.w(TAG, "未找到可用的超级岛 API")
            false
        } catch (e: Exception) {
            android.util.Log.w(TAG, "超级岛Toast不可用: ${e.message}")
            false
        }
    }
    
    /**
     * 显示超级岛通知样式
     */
    private fun showSuperIslandNotification(
        context: Context,
        message: String,
        priority: MessageManager.Priority
    ): Boolean {
        return try {
            // 尝试多种可能的超级岛 API
            val apiClasses = listOf(
                "miuix.appcompat.app.SuperIsland",
                "com.miui.superisland.SuperIslandManager",
                "android.app.SuperIslandManager",
                "com.hyperos.island.SuperIsland"
            )
            
            for (className in apiClasses) {
                try {
                    val clazz = Class.forName(className)
                    
                    // 尝试不同的方法名
                    val methodNames = listOf("showNotification", "show", "displayNotification")
                    for (methodName in methodNames) {
                        try {
                            val method = clazz.getMethod(methodName, Context::class.java, String::class.java, Int::class.java)
                            val islandPriority = convertPriority(priority)
                            method.invoke(null, context, message, islandPriority)
                            android.util.Log.d(TAG, "成功使用超级岛 API: $className.$methodName")
                            return true
                        } catch (e: NoSuchMethodException) {
                            // 尝试其他方法名
                            continue
                        }
                    }
                } catch (e: ClassNotFoundException) {
                    // 尝试下一个类
                    continue
                }
            }
            
            android.util.Log.w(TAG, "未找到可用的超级岛 API")
            false
        } catch (e: Exception) {
            android.util.Log.w(TAG, "超级岛通知不可用: ${e.message}")
            false
        }
    }
    
    /**
     * 转换优先级到超级岛优先级
     */
    private fun convertPriority(priority: MessageManager.Priority): Int {
        return when (priority) {
            MessageManager.Priority.LOW -> 0      // PRIORITY_LOW
            MessageManager.Priority.NORMAL -> 1   // PRIORITY_NORMAL
            MessageManager.Priority.HIGH -> 2     // PRIORITY_HIGH
        }
    }
}
