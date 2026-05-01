package com.lsfStudio.lsfTB.ui.util

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.util.Base64
import android.util.Log
import com.lsfStudio.lsfTB.BuildConfig
import com.lsfStudio.lsfTB.security.ChallengeResponseSigner
import com.lsfStudio.lsfTB.security.IntegrityChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
     * - 连接超时: 30秒（增加以支持大文件上传）
     * - 读取超时: 60秒（增加以支持大文件上传）
     * - 写入超时: 60秒（增加以支持大文件上传）
     * - SSL: 信任所有证书（仅用于开发环境）
     */
    private val okHttpClient: OkHttpClient by lazy {
        // 创建信任所有证书的 TrustManager（仅用于开发/测试）
        val trustAllCertificates = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        
        // 创建 SSLContext
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustAllCertificates), SecureRandom())
        
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)  // 增加到30秒
            .readTimeout(60, TimeUnit.SECONDS)     // 增加到60秒
            .writeTimeout(60, TimeUnit.SECONDS)    // 增加到60秒
            .sslSocketFactory(sslContext.socketFactory, trustAllCertificates)
            .hostnameVerifier { _, _ -> true }  // 信任所有主机名
            .build().also {
                Log.d(TAG, "OkHttpClient实例已创建（开发模式：信任所有证书，超时时间已优化）")
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
     * 构建通用GET请求（自动添加签名和挑战-响应）
     * 
     * @param context 上下文（用于获取设备ID和签名）
     * @param url 请求URL
     * @param path API路径（用于签名，如 /lsfStudio/api/test）
     * @param headers 额外的请求头（可选）
     * @param useChallengeResponse 是否使用挑战-响应机制（默认true）
     * @return 配置好的Request对象
     */
    suspend fun buildGetRequestWithChallenge(
        context: Context,
        url: String,
        path: String,
        headers: Map<String, String> = emptyMap(),
        useChallengeResponse: Boolean = true
    ): Request {
        val builder = Request.Builder()
            .url(url)
            .get()
        
        // 添加基础签名头
        val signatureHeaders = generateSignatureHeaders(context, "GET", path, "")
        signatureHeaders.forEach { (key, value) ->
            builder.addHeader(key, value)
        }
        
        // 如果使用挑战-响应，获取并添加挑战签名
        if (useChallengeResponse) {
            val challengeData = getChallengeAndSign(context)
            if (challengeData != null) {
                challengeData.forEach { (key, value) ->
                    builder.addHeader(key, value)
                }
                Log.d(TAG, "✅ 已添加挑战-响应头")
            } else {
                Log.w(TAG, "⚠️ 挑战-响应失败，继续使用基础验证")
            }
        }
        
        // 添加自定义请求头
        headers.forEach { (key, value) ->
            builder.addHeader(key, value)
        }
        
        return builder.build()
    }
    
    /**
     * 构建通用POST请求（自动添加签名和挑战-响应）
     * 
     * @param context 上下文（用于获取设备ID和签名）
     * @param url 请求URL
     * @param path API路径（用于签名，如 /lsfStudio/api/report）
     * @param body 请求体
     * @param bodyContent 请求体内容字符串（用于签名，如果为null则从body中读取）
     * @param headers 额外的请求头（可选）
     * @param useChallengeResponse 是否使用挑战-响应机制（默认true）
     * @return 配置好的Request对象
     */
    suspend fun buildPostRequestWithChallenge(
        context: Context,
        url: String,
        path: String,
        body: RequestBody,
        bodyContent: String? = null,
        headers: Map<String, String> = emptyMap(),
        useChallengeResponse: Boolean = true
    ): Request {
        val builder = Request.Builder()
            .url(url)
            .post(body)
        
        // 添加基础签名头
        val content = bodyContent ?: readRequestBody(body)
        val signatureHeaders = generateSignatureHeaders(context, "POST", path, content)
        signatureHeaders.forEach { (key, value) ->
            builder.addHeader(key, value)
        }
        
        // 如果使用挑战-响应，获取并添加挑战签名
        if (useChallengeResponse) {
            val challengeData = getChallengeAndSign(context)
            if (challengeData != null) {
                challengeData.forEach { (key, value) ->
                    builder.addHeader(key, value)
                }
                Log.d(TAG, "✅ 已添加挑战-响应头")
            } else {
                Log.w(TAG, "⚠️ 挑战-响应失败，继续使用基础验证")
            }
        }
        
        // 添加自定义请求头
        headers.forEach { (key, value) ->
            builder.addHeader(key, value)
        }
        
        return builder.build()
    }
    
    /**
     * 构建通用GET请求（自动添加签名）- 旧版本，保持兼容
     * 
     * @param context 上下文（用于获取设备ID和签名）
     * @param url 请求URL
     * @param path API路径（用于签名，如 /lsfStudio/api/test）
     * @param headers 额外的请求头（可选）
     * @param autoSign 是否自动添加签名（默认true）
     * @return 配置好的Request对象
     */
    fun buildGetRequest(
        context: Context,
        url: String,
        path: String,
        headers: Map<String, String> = emptyMap(),
        autoSign: Boolean = true
    ): Request {
        val builder = Request.Builder()
            .url(url)
            .get()
        
        // 如果需要自动签名，添加签名头
        if (autoSign) {
            val signatureHeaders = generateSignatureHeaders(context, "GET", path, "")
            signatureHeaders.forEach { (key, value) ->
                builder.addHeader(key, value)
            }
        }
        
        // 添加自定义请求头
        headers.forEach { (key, value) ->
            builder.addHeader(key, value)
        }
        
        return builder.build()
    }
    
    /**
     * 构建通用POST请求（自动添加签名）- 旧版本，保持兼容
     * 
     * @param context 上下文（用于获取设备ID和签名）
     * @param url 请求URL
     * @param path API路径（用于签名，如 /lsfStudio/api/report）
     * @param body 请求体
     * @param bodyContent 请求体内容字符串（用于签名，如果为null则从body中读取）
     * @param headers 额外的请求头（可选）
     * @param autoSign 是否自动添加签名（默认true）
     * @return 配置好的Request对象
     */
    fun buildPostRequest(
        context: Context,
        url: String,
        path: String,
        body: RequestBody,
        bodyContent: String? = null,
        headers: Map<String, String> = emptyMap(),
        autoSign: Boolean = true
    ): Request {
        val builder = Request.Builder()
            .url(url)
            .post(body)
        
        // 如果需要自动签名，添加签名头
        if (autoSign) {
            // 使用提供的 bodyContent 或从 body 中读取
            val content = bodyContent ?: readRequestBody(body)
            val signatureHeaders = generateSignatureHeaders(context, "POST", path, content)
            signatureHeaders.forEach { (key, value) ->
                builder.addHeader(key, value)
            }
        }
        
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
    
    /**
     * 获取挑战并签名（用于增强验证）
     * 
     * @param context 上下文
     * @return 包含挑战和签名的Map，失败返回null
     */
    suspend fun getChallengeAndSign(context: Context): Map<String, String>? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔐 开始挑战-响应流程")
                
                // 1. 完整性校验（自动初始化）
                if (!IntegrityChecker.verifyApkIntegrity(context)) {
                    Log.e(TAG, "❌ APK完整性校验失败，中止挑战-响应")
                    return@withContext null
                }
                Log.d(TAG, "✅ APK完整性校验通过")
                
                // 2. 从服务器获取挑战
                val challengeUrl = BuildConfig.SERVER_URL + "/challenge"
                val challengeRequest = Request.Builder()
                    .url(challengeUrl)
                    .get()
                    .build()
                
                val challengeResponse = execute(challengeRequest)
                
                if (!challengeResponse.isSuccessful) {
                    Log.e(TAG, "❌ 获取挑战失败: ${challengeResponse.code}")
                    challengeResponse.close()
                    return@withContext null
                }
                
                val responseBody = challengeResponse.body?.string()
                challengeResponse.close()
                
                if (responseBody.isNullOrEmpty()) {
                    Log.e(TAG, "❌ 挑战响应为空")
                    return@withContext null
                }
                
                // 解析挑战（简单JSON解析）
                val challenge = extractChallengeFromJson(responseBody)
                
                if (challenge.isNullOrEmpty()) {
                    Log.e(TAG, "❌ 无法解析挑战")
                    return@withContext null
                }
                
                Log.d(TAG, "✅ 获取到挑战: ${challenge.take(16)}...")
                
                // 3. 验证挑战格式
                if (!ChallengeResponseSigner.isValidChallenge(challenge)) {
                    Log.e(TAG, "❌ 挑战格式无效")
                    return@withContext null
                }
                
                // 4. 对挑战进行签名
                val signature = ChallengeResponseSigner.signChallenge(challenge)
                
                if (signature.isNullOrEmpty()) {
                    Log.e(TAG, "❌ 挑战签名失败")
                    return@withContext null
                }
                
                Log.d(TAG, "✅ 挑战-响应完成")
                
                mapOf(
                    "X-Challenge" to challenge,
                    "X-Challenge-Signature" to signature
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 挑战-响应流程异常", e)
                null
            }
        }
    }
    
    /**
     * 从JSON响应中提取挑战
     */
    private fun extractChallengeFromJson(json: String): String? {
        return try {
            // 简单的JSON解析，提取 "challenge" 字段
            val pattern = Regex("\"challenge\"\\s*:\\s*\"([^\"]+)\"")
            val match = pattern.find(json)
            match?.groupValues?.get(1)
        } catch (e: Exception) {
            Log.e(TAG, "解析挑战JSON失败", e)
            null
        }
    }
    
    // ==================== 私有辅助函数 ====================
    
    /**
     * 生成签名请求头
     * 
     * @param context 上下文
     * @param method HTTP方法（GET/POST）
     * @param path API路径
     * @param body 请求体内容（GET为空字符串）
     * @return 签名请求头Map
     */
    private fun generateSignatureHeaders(
        context: Context,
        method: String,
        path: String,
        body: String
    ): Map<String, String> {
        val timestamp = System.currentTimeMillis().toString()
        val nonce = UUID.randomUUID().toString()
        val deviceId = KeystoreManager.getDeviceId(context)
        val apkSignature = getApkSignature(context)
        val androidId = getAndroidId(context)
        
        // 生成签名字符串
        val stringToSign = "$method\n$path\n$body\n$timestamp\n$nonce"
        val signature = KeystoreManager.sign(stringToSign)
        
        Log.d(TAG, "🔑 自动生成签名")
        Log.d(TAG, "   Method: $method")
        Log.d(TAG, "   Path: $path")
        Log.d(TAG, "   Timestamp: $timestamp")
        Log.d(TAG, "   Nonce: $nonce")
        Log.d(TAG, "   Signature: ${signature.take(32)}...")
        Log.d(TAG, "   APK Signature: $apkSignature")
        Log.d(TAG, "   Android ID: $androidId")
        
        return mapOf(
            "X-Timestamp" to timestamp,
            "X-Nonce" to nonce,
            "X-Signature" to signature,
            "X-Key-Id" to deviceId,
            "X-APK-Signature" to apkSignature,
            "X-Android-ID" to androidId
        )
    }
    
    /**
     * 读取RequestBody内容（用于签名）
     * 
     * @param body 请求体
     * @return 请求体字符串内容
     */
    private fun readRequestBody(body: RequestBody): String {
        return try {
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 读取请求体失败", e)
            ""
        }
    }
    
    /**
     * 获取APK签名哈希（SHA-256）
     * 
     * @param context 上下文
     * @return APK签名的Base64编码字符串
     */
    private fun getApkSignature(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            
            val signatures = packageInfo.signatures
            if (signatures != null && signatures.isNotEmpty()) {
                val signature = signatures[0]
                val signatureBytes = signature.toByteArray()
                
                // 计算 SHA-256 哈希
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(signatureBytes)
                
                // 转换为 Base64 字符串
                Base64.encodeToString(digest, Base64.NO_WRAP)
            } else {
                Log.w(TAG, "⚠️ 未找到APK签名")
                "unknown"
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取APK签名失败", e)
            "error"
        }
    }
    
    /**
     * 获取Android ID
     * 
     * @param context 上下文
     * @return Android ID 字符串
     */
    private fun getAndroidId(context: Context): String {
        return try {
            val androidId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            androidId ?: "unknown"
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取Android ID失败", e)
            "error"
        }
    }
}
