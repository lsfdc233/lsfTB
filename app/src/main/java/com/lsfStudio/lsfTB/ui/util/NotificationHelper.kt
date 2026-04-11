package com.lsfStudio.lsfTB.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * 通知管理器 - 模块化设计，可被其他服务复用
 */
object NotificationHelper {
    
    /**
     * 下载通知配置
     */
    data class DownloadNotificationConfig(
        val channelId: String = "download_channel",
        val channelName: String = "下载管理",
        val channelDescription: String = "应用更新下载进度",
        val notificationId: Int = 1001
    )
    
    /**
     * 创建通知渠道
     */
    fun createNotificationChannel(
        context: Context,
        config: DownloadNotificationConfig = DownloadNotificationConfig()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                config.channelId,
                config.channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = config.channelDescription
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 显示下载进度通知
     */
    fun showDownloadProgress(
        context: Context,
        versionName: String,
        progress: Int,
        speed: String,
        remainingTime: String,
        config: DownloadNotificationConfig = DownloadNotificationConfig()
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(context, config.channelId)
            .setContentTitle("正在下载更新 $versionName")
            .setContentText("速度: $speed | 剩余: $remainingTime")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW) // 设置为低优先级，确保显示
            .build()
        
        notificationManager.notify(config.notificationId, notification)
    }
    
    /**
     * 显示下载完成通知（带安装功能）
     */
    fun showDownloadComplete(
        context: Context,
        versionName: String,
        file: File,
        config: DownloadNotificationConfig = DownloadNotificationConfig()
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 创建安装 Intent
        val installIntent = createInstallIntent(context, file)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, config.channelId)
            .setContentTitle("新版本下载完成 - $versionName")
            .setContentText("点击安装")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 设置为高优先级
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 锁屏可见
            .build()
        
        notificationManager.notify(config.notificationId + 1, notification)
        
        // 同时尝试自动唤起安装界面
        try {
            context.startActivity(installIntent)
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "自动安装失败", e)
        }
    }
    
    /**
     * 显示错误通知
     */
    fun showErrorNotification(
        context: Context,
        title: String,
        message: String,
        config: DownloadNotificationConfig = DownloadNotificationConfig()
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(context, config.channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(config.notificationId + 2, notification)
    }
    
    /**
     * 取消下载通知
     */
    fun cancelDownloadNotification(
        context: Context,
        config: DownloadNotificationConfig = DownloadNotificationConfig()
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(config.notificationId)
    }
    
    /**
     * 创建安装 Intent
     */
    private fun createInstallIntent(context: Context, file: File): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            android.net.Uri.fromFile(file)
        }
        
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        
        return intent
    }
}
