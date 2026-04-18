package com.lsfStudio.lsfTB.ui.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

/**
 * 内部下载管理器
 * 支持多线程断点续传下载
 * 
 * 模块化设计，可被其他服务调用
 * 
 * 注意：由于需要多线程Range请求，此处使用HttpURLConnection而非OkHttpClient
 * OkHttpClient不直接支持多线程分段下载
 */
object DownloadManager {
    private const val TAG = "DownloadManager"
    private const val THREAD_COUNT = 4 // 下载线程数
    
    // 下载状态管理
    @Volatile
    private var isDownloading = false
    
    /**
     * 检查是否正在下载
     */
    fun isCurrentlyDownloading(): Boolean {
        return isDownloading
    }
    
    /**
     * 下载状态回调
     */
    interface DownloadCallback {
        fun onProgress(progress: Int, speed: String, remainingTime: String)
        fun onSuccess(file: File)
        fun onError(error: String)
    }
    
    /**
     * 开始下载
     */
    suspend fun download(
        context: Context,
        url: String,
        fileName: String,
        versionName: String,
        callback: DownloadCallback
    ) {
        // 检查是否已经在下载
        if (isDownloading) {
            Log.w(TAG, "已经有下载任务正在进行")
            MessageManager.showToast(context, "正在下载新版本", Toast.LENGTH_SHORT)
            return
        }
        
        isDownloading = true
        
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "========== 开始下载 ==========")
                Log.d(TAG, "URL: $url")
                Log.d(TAG, "文件名: $fileName")
                Log.d(TAG, "版本: $versionName")
                
                // 创建通知渠道
                NotificationHelper.createNotificationChannel(context)
                
                // 显示开始下载通知
                NotificationHelper.showDownloadProgress(context, versionName, 0, "准备中...", "")
                
                // 获取文件大小
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                connection.connect()
                val fileSize = connection.contentLengthLong
                connection.disconnect()
                
                if (fileSize <= 0) {
                    Log.e(TAG, "无法获取文件大小")
                    callback.onError("无法获取文件大小")
                    NotificationHelper.showErrorNotification(context, "下载失败", "无法获取文件大小")
                    return@withContext
                }
                
                Log.d(TAG, "文件大小: $fileSize bytes (${formatFileSize(fileSize)})")
                
                // 显示 Toast 提示（已注释，使用通知栏代替）
                // Handler(Looper.getMainLooper()).post {
                //     Toast.makeText(context, "开始下载 ${versionName}", Toast.LENGTH_SHORT).show()
                // }
                
                // 创建临时文件
                val cacheDir = context.cacheDir
                val outputFile = File(cacheDir, fileName)
                
                // 如果文件已存在，删除它（重新开始下载）
                if (outputFile.exists()) {
                    outputFile.delete()
                }
                
                // 预分配文件大小
                RandomAccessFile(outputFile, "rw").use { raf ->
                    raf.setLength(fileSize)
                }
                
                // 计算每个线程的下载范围
                val partSize = fileSize / THREAD_COUNT
                val downloadedBytes = AtomicLong(0)
                var lastUpdateTime = System.currentTimeMillis()
                var lastDownloadedBytes = 0L
                
                // 启动多个下载线程
                val threads = mutableListOf<Thread>()
                for (i in 0 until THREAD_COUNT) {
                    val startByte = i * partSize
                    val endByte = if (i == THREAD_COUNT - 1) fileSize - 1 else (i + 1) * partSize - 1
                    
                    val thread = Thread {
                        downloadPart(url, outputFile, startByte, endByte, downloadedBytes) { }
                    }
                    threads.add(thread)
                    thread.start()
                }
                
                // 主线程监控总体进度
                while (downloadedBytes.get() < fileSize) {
                    val currentTime = System.currentTimeMillis()
                    val timeDiff = currentTime - lastUpdateTime
                    
                    if (timeDiff >= 500) { // 每500ms更新一次
                        val currentDownloaded = downloadedBytes.get()
                        val progress = ((currentDownloaded.toDouble() / fileSize) * 100).toInt().coerceIn(0, 100)
                        val speedBytes = (currentDownloaded - lastDownloadedBytes) * 1000 / timeDiff
                        val remainingBytes = fileSize - currentDownloaded
                        val remainingTimeMs = if (speedBytes > 0) remainingBytes * 1000 / speedBytes else 0
                        
                        val speedStr = formatSpeed(speedBytes)
                        val timeStr = formatTime(remainingTimeMs)
                        
                        Log.d(TAG, "总进度: $progress% | 速度: $speedStr | 剩余: $timeStr")
                        callback.onProgress(progress, speedStr, timeStr)
                        
                        // 更新通知（使用MessageManager自动适配超级岛）
                        MessageManager.showDownloadProgress(
                            context, versionName, progress, speedStr, timeStr
                        )
                        
                        lastUpdateTime = currentTime
                        lastDownloadedBytes = currentDownloaded
                    }
                    
                    Thread.sleep(100) // 避免CPU占用过高
                }
                
                // 等待所有线程完成
                threads.forEach { it.join() }
                
                Log.d(TAG, "========== 下载完成 ==========")
                Log.d(TAG, "文件路径: ${outputFile.absolutePath}")
                Log.d(TAG, "文件大小: ${outputFile.length()} bytes")
                
                // 显示 Toast 提示（已注释，使用通知栏代替）
                // Handler(Looper.getMainLooper()).post {
                //     Toast.makeText(context, "下载完成，准备安装", Toast.LENGTH_LONG).show()
                // }
                
                // 发送完成通知（使用MessageManager自动适配超级岛）
                MessageManager.showDownloadComplete(context, versionName, outputFile)
                
                callback.onSuccess(outputFile)
                
            } catch (e: Exception) {
                Log.e(TAG, "========== 下载失败 ==========", e)
                Log.e(TAG, "错误信息: ${e.message}")
                
                // 显示 Toast 提示（已注释，使用通知栏代替）
                // Handler(Looper.getMainLooper()).post {
                //     Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_LONG).show()
                // }
                
                callback.onError("下载失败: ${e.message}")
                MessageManager.showError(context, "下载失败", e.message ?: "未知错误")
            } finally {
                // 重置下载状态
                isDownloading = false
                Log.d(TAG, "下载状态已重置")
            }
        }
    }
    
    /**
     * 下载文件的一部分
     */
    private fun downloadPart(
        url: String,
        outputFile: File,
        startByte: Long,
        endByte: Long,
        downloadedBytes: AtomicLong,
        onProgress: (Int) -> Unit
    ) {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var randomAccessFile: RandomAccessFile? = null
        
        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Range", "bytes=$startByte-$endByte")
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            
            inputStream = connection.inputStream
            randomAccessFile = RandomAccessFile(outputFile, "rw")
            randomAccessFile.seek(startByte)
            
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                randomAccessFile.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                downloadedBytes.addAndGet(bytesRead.toLong())
                
                // 计算进度
                val partSize = endByte - startByte + 1
                val progress = ((totalBytesRead.toDouble() / partSize) * 100).toInt()
                onProgress(progress.coerceIn(0, 100))
            }
            
            Log.d(TAG, "线程下载完成: $startByte-$endByte, 共 $totalBytesRead bytes")
            
        } catch (e: Exception) {
            Log.e(TAG, "线程下载失败: $startByte-$endByte", e)
        } finally {
            try {
                inputStream?.close()
                randomAccessFile?.close()
                connection?.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "关闭资源失败", e)
            }
        }
    }
    
    /**
     * 格式化速度
     */
    private fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
            bytesPerSecond < 1024 * 1024 -> "${bytesPerSecond / 1024} KB/s"
            else -> String.format("%.2f MB/s", bytesPerSecond / 1024.0 / 1024.0)
        }
    }
    
    /**
     * 格式化时间
     */
    private fun formatTime(milliseconds: Long): String {
        return when {
            milliseconds < 1000 -> "< 1秒"
            milliseconds < 60000 -> "${milliseconds / 1000}秒"
            milliseconds < 3600000 -> "${milliseconds / 60000}分钟"
            else -> "${milliseconds / 3600000}小时"
        }
    }
    
    /**
     * 格式化文件大小
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / 1024.0 / 1024.0)
            else -> String.format("%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0)
        }
    }
}
