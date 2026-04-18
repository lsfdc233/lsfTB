package com.lsfStudio.lsfTB.ui.util

import android.content.Context
import android.util.Log
import com.lsfStudio.lsfTB.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 网络通信中枢
 * 
 * 职责：
 * 1. 统一管理OkHttpClient实例（单例模式）
 * 2. 提供统一的HTTP请求接口
 * 3. 处理网络错误和重试逻辑
 * 4. 支持GitHub API认证
 * 
 * @author lsfTB Team
 */
object NetworkClient {
    
    private const val TAG = "NetworkClient"
    
    /**
     * 全局唯一的OkHttpClient实例（内部使用）
     * 配置：
     * - 连接超时: 10秒
     * - 读取超时: 10秒
     * - 写入超时: 10秒
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build().also {
                Log.d(TAG, "OkHttpClient实例已创建")
            }
    }
    
    /**
     * 执行同步HTTP请求
     * 
     * @param request HTTP请求对象
     * @return HTTP响应对象
     * @throws IOException 网络异常
     */
    @Throws(IOException::class)
    fun execute(request: Request): Response {
        return okHttpClient.newCall(request).execute()
    }
    
    /**
     * 执行异步HTTP请求
     * 
     * @param request HTTP请求对象
     * @param callback 回调函数
     */
    fun enqueue(request: Request, callback: Callback) {
        okHttpClient.newCall(request).enqueue(callback)
    }
    
    /**
     * 构建带GitHub认证的请求
     * 
     * @param url 请求URL
     * @param acceptHeader Accept头，默认为application/vnd.github.v3+json
     * @return 配置好的Request对象
     */
    fun buildGitHubRequest(
        url: String,
        acceptHeader: String = "application/vnd.github.v3+json"
    ): Request {
        val builder = Request.Builder()
            .url(url)
            .addHeader("Accept", acceptHeader)
            .addHeader("User-Agent", "lsfTB/${BuildConfig.VERSION_CODE}")
        
        // 尝试添加GitHub Token
        try {
            val tokenField = BuildConfig::class.java.getDeclaredField("GITHUB_TOKEN")
            val token = tokenField.get(null) as? String
            if (!token.isNullOrEmpty()) {
                builder.addHeader("Authorization", "token $token")
                Log.d(TAG, "使用GitHub Token进行认证")
            } else {
                Log.d(TAG, "GitHub Token为空，使用未认证请求")
            }
        } catch (e: Exception) {
            Log.d(TAG, "未配置GitHub Token，使用未认证请求")
        }
        
        return builder.build()
    }
    
    /**
     * 构建通用GET请求
     * 
     * @param url 请求URL
     * @param headers 额外的请求头（可选）
     * @return 配置好的Request对象
     */
    fun buildGetRequest(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): Request {
        val builder = Request.Builder()
            .url(url)
            .get()
        
        // 添加自定义请求头
        headers.forEach { (key, value) ->
            builder.addHeader(key, value)
        }
        
        return builder.build()
    }
    
    /**
     * 构建通用POST请求
     * 
     * @param url 请求URL
     * @param body 请求体
     * @param headers 额外的请求头（可选）
     * @return 配置好的Request对象
     */
    fun buildPostRequest(
        url: String,
        body: okhttp3.RequestBody,
        headers: Map<String, String> = emptyMap()
    ): Request {
        val builder = Request.Builder()
            .url(url)
            .post(body)
        
        // 添加自定义请求头
        headers.forEach { (key, value) ->
            builder.addHeader(key, value)
        }
        
        return builder.build()
    }
    
    /**
     * 取消所有正在进行的请求
     */
    fun cancelAllRequests() {
        okHttpClient.dispatcher.cancelAll()
        Log.d(TAG, "已取消所有请求")
    }
    
    /**
     * 检查网络客户端是否已初始化
     * 
     * @return true表示已初始化
     */
    fun isInitialized(): Boolean {
        // 由于使用lazy委托，访问okHttpClient会触发初始化
        return try {
            okHttpClient
            true
        } catch (e: Exception) {
            false
        }
    }
}
