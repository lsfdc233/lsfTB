package com.lsfStudio.lsfTB.ui.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局错误上报与日志中心
 * 
 * 职责：
 * 1. 统一捕获并格式化应用内异常
 * 2. 将严重错误上报至服务端（可选）
 * 3. 本地持久化存储错误日志
 */
object ErrorReporter {
    
    private const val TAG = "ErrorReporter"
    private const val LOG_FILE_NAME = "error_log.txt"
    private const val MAX_LOG_SIZE = 1024 * 1024 // 1MB
    
    /**
     * 上报非致命错误
     */
    fun reportNonFatal(context: Context, throwable: Throwable, tag: String = TAG) {
        Log.e(tag, "Non-fatal error", throwable)
        saveToLocalLog(context, "NON-FATAL", tag, throwable)
    }
    
    /**
     * 上报致命错误
     */
    suspend fun reportFatal(context: Context, throwable: Throwable, tag: String = TAG) {
        Log.e(tag, "FATAL error", throwable)
        saveToLocalLog(context, "FATAL", tag, throwable)
        
        // 尝试上报到服务端
        withContext(Dispatchers.IO) {
            try {
                val errorData = JSONObject().apply {
                    put("type", "FATAL")
                    put("tag", tag)
                    put("message", throwable.message ?: "Unknown error")
                    put("stackTrace", getStackTraceString(throwable))
                    put("timestamp", System.currentTimeMillis())
                    put("deviceInfo", collectDeviceInfo(context))
                }
                
                // 这里可以调用 NetworkClient.send 上报到服务端
                // NetworkClient.send(context, "POST", "...", "/api/error/report", errorData.toString())
                Log.d(TAG, "Fatal error logged locally and prepared for server upload")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to prepare fatal error report", e)
            }
        }
    }
    
    /**
     * 记录业务逻辑警告
     */
    fun logWarning(context: Context, message: String, tag: String = TAG) {
        Log.w(tag, message)
        saveToLocalLog(context, "WARNING", tag, RuntimeException(message))
    }
    
    private fun saveToLocalLog(context: Context, level: String, tag: String, throwable: Throwable) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val logEntry = "[$timestamp] [$level] [$tag]\n${getStackTraceString(throwable)}\n\n"
            
            // 简单的文件追加逻辑（实际项目中建议使用更健壮的文件管理工具）
            context.openFileOutput(LOG_FILE_NAME, Context.MODE_APPEND).use {
                it.write(logEntry.toByteArray(Charsets.UTF_8))
            }
            
            // 检查文件大小并轮转
            checkAndRotateLog(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save log to local file", e)
        }
    }
    
    private fun checkAndRotateLog(context: Context) {
        try {
            val file = context.getFileStreamPath(LOG_FILE_NAME)
            if (file.exists() && file.length() > MAX_LOG_SIZE) {
                // 如果超过限制，删除旧日志（简单策略）
                file.delete()
                Log.d(TAG, "Log file rotated due to size limit")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log file", e)
        }
    }
    
    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }
    
    private fun collectDeviceInfo(context: Context): String {
        return try {
            val pm = context.packageManager
            val pi = pm.getPackageInfo(context.packageName, 0)
            "AppVersion: ${pi.versionName} (${pi.versionCode}), Model: ${android.os.Build.MODEL}"
        } catch (e: Exception) {
            "Unknown Device Info"
        }
    }
}
