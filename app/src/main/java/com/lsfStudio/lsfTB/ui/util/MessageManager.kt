package com.lsfStudio.lsfTB.ui.util

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.Keep

/**
 * 统一消息管理器 - 模块化设计
 * 支持传统 Toast/Notification 和小米超级岛（HyperOS 3.0+）
 */
object MessageManager {
    
    /**
     * 消息类型枚举
     */
    enum class MessageType {
        TOAST,          // 短暂提示
        NOTIFICATION,   // 系统通知
        SUPER_ISLAND    // 小米超级岛
    }
    
    /**
     * 消息优先级
     */
    enum class Priority {
        LOW,      // 低优先级
        NORMAL,   // 普通优先级
        HIGH      // 高优先级
    }
    
    /**
     * 显示Toast消息（自动适配超级岛）
     * 
     * @param context 上下文
     * @param message 消息内容
     * @param duration 显示时长（Toast.LENGTH_SHORT 或 Toast.LENGTH_LONG）
     * @param priority 优先级
     */
    fun showToast(
        context: Context,
        message: String,
        duration: Int = Toast.LENGTH_SHORT,
        priority: Priority = Priority.NORMAL
    ) {
        // 检查是否为 HyperOS 3.0+
        if (isHyperOS3OrAbove()) {
            // 在 HyperOS 上，尝试使用通知来触发超级岛
            // 超级岛通常由短暂的高优先级通知触发
            showSuperIslandStyleNotification(context, message, priority)
        } else {
            // 非 HyperOS，使用传统 Toast
            showTraditionalToast(context, message, duration)
        }
    }
    
    /**
     * 显示下载进度（自动适配超级岛）
     * 
     * @param context 上下文
     * @param versionName 版本号
     * @param progress 进度百分比 (0-100)
     * @param speed 下载速度
     * @param remainingTime 剩余时间
     */
    fun showDownloadProgress(
        context: Context,
        versionName: String,
        progress: Int,
        speed: String,
        remainingTime: String
    ) {
        val message = "正在下载 $versionName\n进度: $progress% | 速度: $speed | 剩余: $remainingTime"
        
        if (isHyperOS3OrAbove()) {
            // 在 HyperOS 上，使用通知来触发超级岛
            showSuperIslandStyleNotification(context, message, Priority.NORMAL)
        } else {
            // 非 HyperOS，使用传统通知
            NotificationHelper.showDownloadProgress(context, versionName, progress, speed, remainingTime)
        }
    }
    
    /**
     * 显示下载完成通知（自动适配超级岛）
     * 
     * @param context 上下文
     * @param versionName 版本号
     * @param file APK文件
     */
    fun showDownloadComplete(
        context: Context,
        versionName: String,
        file: java.io.File
    ) {
        val message = "新版本 $versionName 下载完成，点击安装"
        
        if (isHyperOS3OrAbove()) {
            // 在 HyperOS 上，使用通知来触发超级岛
            showSuperIslandStyleNotification(context, message, Priority.HIGH)
        } else {
            // 非 HyperOS，使用传统通知
            NotificationHelper.showDownloadComplete(context, versionName, file)
        }
    }
    
    /**
     * 显示错误消息（自动适配超级岛）
     * 
     * @param context 上下文
     * @param title 标题
     * @param message 错误信息
     */
    fun showError(
        context: Context,
        title: String,
        message: String
    ) {
        val fullMessage = "$title: $message"
        
        if (isHyperOS3OrAbove()) {
            // 在 HyperOS 上，使用通知来触发超级岛
            showSuperIslandStyleNotification(context, fullMessage, Priority.HIGH)
        } else {
            // 非 HyperOS，使用传统通知
            NotificationHelper.showErrorNotification(context, title, message)
        }
    }
    
    /**
     * 显示传统Toast
     */
    private fun showTraditionalToast(
        context: Context,
        message: String,
        duration: Int
    ) {
        Toast.makeText(context, message, duration).show()
    }
    
    /**
     * 显示超级岛风格的通知
     * HyperOS会自动将短暂的高优先级通知显示为超级岛样式
     */
    private fun showSuperIslandStyleNotification(
        context: Context,
        message: String,
        priority: Priority
    ) {
        try {
            // 创建专门用于超级岛的通知渠道
            val channelId = "super_island_channel"
            val channelName = "超级岛通知"
            
            // 创建通知渠道
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val channel = android.app.NotificationChannel(
                    channelId,
                    channelName,
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "用于显示超级岛样式的通知"
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            // 构建通知
            val notificationId = System.currentTimeMillis().toInt() // 使用时间戳作为ID，确保每次都是新通知
            
            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setContentTitle("lsfTB")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(
                    when (priority) {
                        Priority.LOW -> androidx.core.app.NotificationCompat.PRIORITY_LOW
                        Priority.NORMAL -> androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
                        Priority.HIGH -> androidx.core.app.NotificationCompat.PRIORITY_HIGH
                    }
                )
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setTimeoutAfter(2000) // 2秒后自动消失，模拟Toast
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.notify(notificationId, builder.build())
            
            android.util.Log.d("MessageManager", "已发送超级岛风格通知: $message")
        } catch (e: Exception) {
            android.util.Log.e("MessageManager", "发送超级岛通知失败，降级为Toast: ${e.message}")
            // 降级为传统Toast
            showTraditionalToast(context, message, Toast.LENGTH_SHORT)
        }
    }
    
    /**
     * 尝试显示小米超级岛
     * 
     * @return true 如果成功使用超级岛，false 如果失败需要降级
     */
    private fun tryShowSuperIsland(
        context: Context,
        message: String,
        type: MessageType,
        priority: Priority
    ): Boolean {
        // 检查是否为 HyperOS 3.0+
        if (!isHyperOS3OrAbove()) {
            return false
        }
        
        return try {
            // 调用超级岛API
            SuperIslandHelper.showMessage(context, message, type, priority)
            true
        } catch (e: Exception) {
            // 超级岛调用失败，降级到传统方式
            android.util.Log.w("MessageManager", "超级岛显示失败，降级到传统方式: ${e.message}")
            false
        }
    }
    
    /**
     * 检查是否为 HyperOS 3.0 及以上版本
     */
    fun isHyperOS3OrAbove(): Boolean {
        return try {
            // 获取 MIUI 版本
            val miuiVersion = getMIUIVersion()
            
            // HyperOS 3.0 对应 MIUI 版本号为 15.0+
            // 格式可能是 "15.0.1" 或 "V15.0.1.0"
            if (miuiVersion.isNotEmpty()) {
                val version = parseVersion(miuiVersion)
                version >= 15.0
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("MessageManager", "检查系统版本失败: ${e.message}")
            false
        }
    }
    
    /**
     * 获取 MIUI/HyperOS 版本号
     */
    private fun getMIUIVersion(): String {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java)
            
            // 尝试多个可能的属性名
            val properties = listOf(
                "ro.miui.ui.version.name",
                "ro.build.version.incremental",
                "ro.product.mod_device"
            )
            
            for (prop in properties) {
                val value = method.invoke(null, prop) as? String
                if (!value.isNullOrEmpty()) {
                    return value
                }
            }
            
            ""
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 解析版本号字符串为数字
     */
    private fun parseVersion(versionString: String): Double {
        return try {
            // 提取第一个数字部分（例如 "V15.0.1.0" -> "15.0"）
            val regex = Regex("[Vv]?([\\d]+\\.[\\d]+)")
            val matchResult = regex.find(versionString)
            
            if (matchResult != null) {
                matchResult.groupValues[1].toDouble()
            } else {
                0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }
}
