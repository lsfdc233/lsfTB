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
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
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
    private const val CHALLENGE_PATH = "/lsfStudio/api/challenge"
    private const val CHALLENGE_RESPONSE_PATH = "/lsfStudio/api/challenge/response"
    private const val ORIGINAL_EARLY_RESULT_WAIT_MS = 200L
    private const val ORIGINAL_FINAL_WAIT_SECONDS = 65L
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    data class NetworkResult(
        val isSuccessful: Boolean,
        val code: Int,
        val body: String?
    )

    private data class TwoPhaseChallengeMeta(
        val context: Context,
        val requestId: String,
        val deviceId: String,
        val androidId: String,
        val uid: String? = null
    )

    private data class ChallengePayload(
        val requestId: String,
        val challenge: String
    )

    private sealed class PendingCallResult {
        data class Success(val response: Response) : PendingCallResult()
        data class Failure(val exception: IOException) : PendingCallResult()
    }
    
    /**
     * 全局唯一的OkHttpClient实例（内部使用）
     * 配置：
     * - 连接超时: 30秒（增加以支持大文件上传）
     * - 读取超时: 60秒（增加以支持大文件上传）
     * - 写入超时: 60秒（增加以支持大文件上传）
     * - SSL: Debug模式信任所有证书，Release模式使用系统默认验证
     */
    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
        
        // 根据构建类型配置SSL
        if (BuildConfig.DEBUG) {
            // Debug模式：信任所有证书（方便开发测试）
            Log.w(TAG, "⚠️ Debug模式：信任所有SSL证书（仅用于开发）")
            configureInsecureSsl(builder)
        } else {
            // Release模式：使用系统默认SSL验证（安全）
            Log.d(TAG, "✅ Release模式：启用标准SSL证书验证")
        }
        
        builder.build().also {
            Log.d(TAG, "OkHttpClient实例已创建（超时时间已优化）")
        }
    }
    
    /**
     * 配置不安全的SSL（仅用于Debug模式）
     */
    private fun configureInsecureSsl(builder: OkHttpClient.Builder) {
        val trustAllCertificates = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustAllCertificates), SecureRandom())
        
        builder
            .sslSocketFactory(sslContext.socketFactory, trustAllCertificates)
            .hostnameVerifier { _, _ -> true }
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
        val challengeMeta = request.tag(TwoPhaseChallengeMeta::class.java)
        if (challengeMeta != null) {
            return executeWithServerChallenge(request, challengeMeta)
        }

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
     * 执行请求并统一提取状态码与响应体。
     */
    @Throws(IOException::class)
    fun executeForResult(request: Request): NetworkResult {
        execute(request).use { response ->
            return NetworkResult(
                isSuccessful = response.isSuccessful,
                code = response.code,
                body = response.body?.string()
            )
        }
    }

    /**
     * 页面/模块只提供方法、地址、路径和请求体，由 NetworkClient 完成签名、挑战和收包。
     */
    suspend fun send(
        context: Context,
        method: String,
        url: String,
        path: String,
        bodyContent: String? = null,
        headers: Map<String, String> = emptyMap(),
        useChallengeResponse: Boolean = true,
        timeoutRetries: Int = 1
    ): NetworkResult = withContext(Dispatchers.IO) {
        val normalizedMethod = method.uppercase()

        var attempt = 0
        while (true) {
            val request = when (normalizedMethod) {
                "GET" -> buildGetRequestWithChallenge(
                    context = context,
                    url = url,
                    path = path,
                    headers = headers,
                    useChallengeResponse = useChallengeResponse
                )
                "POST" -> {
                    val content = bodyContent ?: ""
                    buildPostRequestWithChallenge(
                        context = context,
                        url = url,
                        path = path,
                        body = content.toRequestBody(jsonMediaType),
                        bodyContent = content,
                        headers = headers,
                        useChallengeResponse = useChallengeResponse
                    )
                }
                else -> throw IllegalArgumentException("Unsupported method: $method")
            }

            try {
                return@withContext executeForResult(request)
            } catch (e: SocketTimeoutException) {
                val finalResponseTimeout = e.message == "等待原请求响应超时"
                if (attempt >= timeoutRetries || finalResponseTimeout) {
                    throw e
                }

                attempt += 1
                Log.w(TAG, "请求超时，准备重试: attempt=$attempt, path=$path", e)
            }
        }

        throw IOException("请求重试流程异常")
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
            .url(resolveSignedUrl(url, path))
            .get()
        
        // 添加基础签名头和请求ID。挑战在 execute() 中走第二条 HTTP 链路完成。
        val requestId = generateRequestId()
        val signatureHeaders = generateSignatureHeaders(context, "GET", path, "")
        signatureHeaders.forEach { (key, value) ->
            builder.header(key, value)
        }
        builder.header("X-Request-ID", requestId)

        if (useChallengeResponse) {
            attachChallengeMeta(context, builder, requestId, signatureHeaders)
        }
        
        // 添加自定义请求头
        headers.forEach { (key, value) ->
            builder.header(key, value)
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
            .url(resolveSignedUrl(url, path))
            .post(body)
        
        // 添加基础签名头和请求ID。挑战在 execute() 中走第二条 HTTP 链路完成。
        val requestId = generateRequestId()
        val content = bodyContent ?: readRequestBody(body)
        val signatureHeaders = generateSignatureHeaders(context, "POST", path, content)
        signatureHeaders.forEach { (key, value) ->
            builder.header(key, value)
        }
        builder.header("X-Request-ID", requestId)

        if (useChallengeResponse) {
            attachChallengeMeta(context, builder, requestId, signatureHeaders)
        }
        
        // 添加自定义请求头
        headers.forEach { (key, value) ->
            builder.header(key, value)
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
            .url(resolveSignedUrl(url, path))
            .get()
        
        // 如果需要自动签名，添加签名头
        if (autoSign) {
            val requestId = generateRequestId()
            val signatureHeaders = generateSignatureHeaders(context, "GET", path, "")
            signatureHeaders.forEach { (key, value) ->
                builder.header(key, value)
            }
            builder.header("X-Request-ID", requestId)
        }
        
        // 添加自定义请求头
        headers.forEach { (key, value) ->
            builder.header(key, value)
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
            .url(resolveSignedUrl(url, path))
            .post(body)
        
        // 如果需要自动签名，添加签名头
        if (autoSign) {
            val requestId = generateRequestId()
            // 使用提供的 bodyContent 或从 body 中读取
            val content = bodyContent ?: readRequestBody(body)
            val signatureHeaders = generateSignatureHeaders(context, "POST", path, content)
            signatureHeaders.forEach { (key, value) ->
                builder.header(key, value)
            }
            builder.header("X-Request-ID", requestId)
        }
        
        // 添加自定义请求头
        headers.forEach { (key, value) ->
            builder.header(key, value)
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

    @Throws(IOException::class)
    private fun executeWithServerChallenge(
        request: Request,
        meta: TwoPhaseChallengeMeta
    ): Response {
        if (!IntegrityChecker.verifyApkIntegrity(meta.context)) {
            throw IOException("APK完整性校验失败")
        }

        Log.d(TAG, "🔐 启动双链路挑战流程: requestId=${meta.requestId}")

        val resultQueue = ArrayBlockingQueue<PendingCallResult>(1)
        val originalCall = okHttpClient.newCall(request)

        originalCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                resultQueue.offer(PendingCallResult.Failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                resultQueue.offer(PendingCallResult.Success(response))
            }
        })

        try {
            pollOriginalResult(resultQueue, ORIGINAL_EARLY_RESULT_WAIT_MS)?.let { early ->
                Log.d(TAG, "原请求在挑战前已返回: ${early.code}")
                return early
            }

            val challengePayload = try {
                fetchPendingChallenge(request, meta)
            } catch (e: IOException) {
                pollOriginalResult(resultQueue, 1000L)?.let { return it }
                throw e
            }
            if (!ChallengeResponseSigner.isValidChallenge(challengePayload.challenge)) {
                originalCall.cancel()
                throw IOException("服务端挑战格式无效")
            }

            val signature = ChallengeResponseSigner.signChallenge(challengePayload.challenge)
                ?: throw IOException("挑战签名失败")

            val challengeAccepted = submitChallengeResponse(
                request = request,
                meta = meta,
                challenge = challengePayload.challenge,
                signature = signature
            )

            if (!challengeAccepted) {
                pollOriginalResult(resultQueue, 5000L)?.let { return it }
                originalCall.cancel()
                throw IOException("挑战响应验证失败")
            }

            return pollOriginalResult(resultQueue, TimeUnit.SECONDS.toMillis(ORIGINAL_FINAL_WAIT_SECONDS))
                ?: throw SocketTimeoutException("等待原请求响应超时")
        } catch (e: IOException) {
            originalCall.cancel()
            drainPendingResponse(resultQueue)
            throw e
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            originalCall.cancel()
            drainPendingResponse(resultQueue)
            throw IOException("挑战流程被中断", e)
        } catch (e: Exception) {
            originalCall.cancel()
            drainPendingResponse(resultQueue)
            throw IOException("挑战流程异常", e)
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun pollOriginalResult(
        resultQueue: ArrayBlockingQueue<PendingCallResult>,
        timeoutMs: Long
    ): Response? {
        val result = resultQueue.poll(timeoutMs, TimeUnit.MILLISECONDS) ?: return null
        return when (result) {
            is PendingCallResult.Success -> result.response
            is PendingCallResult.Failure -> throw result.exception
        }
    }

    private fun drainPendingResponse(resultQueue: ArrayBlockingQueue<PendingCallResult>) {
        val result = resultQueue.poll() ?: return
        if (result is PendingCallResult.Success) {
            result.response.close()
        }
    }

    @Throws(IOException::class)
    private fun fetchPendingChallenge(
        request: Request,
        meta: TwoPhaseChallengeMeta
    ): ChallengePayload {
        val challengeUrl = request.url.newBuilder()
            .encodedPath(CHALLENGE_PATH)
            .query(null)
            .addQueryParameter("requestId", meta.requestId)
            .build()

        val challengeRequest = Request.Builder()
            .url(challengeUrl)
            .get()
            .header("X-Request-ID", meta.requestId)
            .header("X-Key-Id", meta.deviceId)
            .header("X-Android-ID", meta.androidId)
            .build()

        okHttpClient.newCall(challengeRequest).execute().use { response ->
            val responseBody = response.body?.string()
            if (!response.isSuccessful) {
                throw IOException("获取挑战失败: ${response.code}, $responseBody")
            }

            val json = JSONObject(responseBody ?: "{}")
            val challenge = json.optString("challenge", "")
            val requestId = json.optString("requestId", meta.requestId)

            if (challenge.isEmpty()) {
                throw IOException("挑战响应为空")
            }

            Log.d(TAG, "✅ 收到服务端挑战: requestId=$requestId")
            return ChallengePayload(requestId, challenge)
        }
    }

    @Throws(IOException::class)
    private fun submitChallengeResponse(
        request: Request,
        meta: TwoPhaseChallengeMeta,
        challenge: String,
        signature: String
    ): Boolean {
        val responseUrl = request.url.newBuilder()
            .encodedPath(CHALLENGE_RESPONSE_PATH)
            .query(null)
            .build()

        val responseJson = JSONObject().apply {
            put("requestId", meta.requestId)
            put("androidId", meta.androidId)
            put("challenge", challenge)
            put("signature", signature)
        }.toString()

        val challengeResponseRequest = Request.Builder()
            .url(responseUrl)
            .post(responseJson.toRequestBody(jsonMediaType))
            .header("X-Request-ID", meta.requestId)
            .header("X-Key-Id", meta.deviceId)
            .header("X-Android-ID", meta.androidId)
            .build()

        okHttpClient.newCall(challengeResponseRequest).execute().use { response ->
            val responseBody = response.body?.string()
            if (!response.isSuccessful) {
                Log.e(TAG, "❌ 挑战响应提交失败: ${response.code}, $responseBody")
                return false
            }

            val json = JSONObject(responseBody ?: "{}")
            val success = json.optBoolean("success", false)
            Log.d(TAG, "✅ 挑战响应提交结果: $success")
            return success
        }
    }
    
    // ==================== 私有辅助函数 ====================

    private fun generateRequestId(): String {
        return UUID.randomUUID().toString()
    }

    private fun resolveSignedUrl(url: String, path: String): String {
        val httpUrl = url.toHttpUrl()
        if (httpUrl.encodedPath == path) {
            return url
        }

        val resolvedUrl = httpUrl.newBuilder()
            .encodedPath(path)
            .build()
            .toString()

        Log.d(TAG, "修正请求URL路径: $url -> $resolvedUrl")
        return resolvedUrl
    }

    private fun attachChallengeMeta(
        context: Context,
        builder: Request.Builder,
        requestId: String,
        signatureHeaders: Map<String, String>
    ) {
        val deviceId = signatureHeaders["X-Key-Id"].orEmpty()
        val androidId = signatureHeaders["X-Android-ID"].orEmpty()

        builder.tag(
            TwoPhaseChallengeMeta::class.java,
            TwoPhaseChallengeMeta(
                context = context.applicationContext,
                requestId = requestId,
                deviceId = deviceId,
                androidId = androidId
            )
        )

        Log.d(TAG, "已绑定双链路挑战元数据: requestId=$requestId")
    }
    
    /**
     * 生成签名请求头
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
        val uid = DataBase(context).getMetadataText(OOBE.KEY_UID)
        
        // 生成签名字符串
        val stringToSign = "$method\n$path\n$body\n$timestamp\n$nonce"
        val signature = KeystoreManager.sign(stringToSign)
        
        Log.d(TAG, "🔑 自动生成签名")
        Log.d(TAG, "   Method: $method")
        Log.d(TAG, "   Path: $path")
        Log.d(TAG, "   Body length: ${body.length} bytes")
        Log.d(TAG, "   Body preview: ${if (body.length > 100) body.substring(0, 100) + "..." else body}")
        Log.d(TAG, "   Timestamp: $timestamp")
        Log.d(TAG, "   Nonce: $nonce")
        Log.d(TAG, "   String to sign (first 100 chars): ${stringToSign.take(100)}...")
        Log.d(TAG, "   Signature: ${signature.take(32)}...")
        Log.d(TAG, "   APK Signature: $apkSignature")
        Log.d(TAG, "   Android ID: $androidId")
        Log.d(TAG, "   UID: $uid")
        
        return mapOf(
            "X-Timestamp" to timestamp,
            "X-Nonce" to nonce,
            "X-Signature" to signature,
            "X-Key-Id" to deviceId,
            "X-APK-Signature" to apkSignature,
            "X-Android-ID" to androidId,
            "X-UID" to (uid ?: "0")
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
