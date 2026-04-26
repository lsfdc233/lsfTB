package com.lsfStudio.lsfTB.ui.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.lsfStudio.lsfTB.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * OOBE (Out-of-Box Experience) 模块
 * 
 * 职责：
 * 1. 检查 MetaData 表是否存在
 * 2. 首次启动时采集设备信息并写入数据库
 * 3. 调用 OOBESecurity 生成并存储设备标识符
 * 
 * 架构说明：
 * - 这是 util 层的通用模块，不是页面专属中间件
 * - 通过 DataBase 主模块操作数据库
 * - 在应用启动时由 MainActivity 调用
 */
object OOBE {
    
    private const val TAG = "OOBE"
    
    // MetaData 表键名
    private const val KEY_INITIALIZED = "oobe_initialized"
    const val KEY_ANDROID_ID = "android_id"           // ANDROID_ID
    const val KEY_DEVICE_BRAND = "device_brand"       // 品牌
    const val KEY_DEVICE_MODEL = "device_model"       // 型号
    const val KEY_DEVICE_DEVICE = "device_device"     // 设备代号
    const val KEY_DEVICE_BOARD = "device_board"       // 主板
    const val KEY_DEVICE_HARDWARE = "device_hardware" // 硬件平台
    const val KEY_DEVICE_ID = "device_id"  // 预留的设备ID键
    
    /**
     * 设备信息数据类
     */
    data class DeviceInfo(
        val androidId: String,
        val brand: String,
        val model: String,
        val device: String,
        val board: String,
        val hardware: String
    )
    
    /**
     * 初始化 OOBE
     * 在应用启动时调用
     * 
     * @param context 上下文
     * @return 验证是否通过
     */
    fun initialize(context: Context): Boolean {
        Log.d(TAG, "🔍 开始 OOBE 初始化...")
        
        // 🛡️ 第一步：初始化 Keystore
        try {
            KeystoreManager.initialize(context)
            Log.d(TAG, "✅ Keystore 初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Keystore 初始化失败", e)
        }
        
        val dataBase = DataBase(context)
        
        // 检查 MetaData 表是否存在
        if (!dataBase.tableExists(DataBase.TABLE_METADATA)) {
            Log.w(TAG, "⚠️ MetaData 表不存在，等待 DataBase 创建...")
            // DataBase 会在 onCreate 中自动创建 MetaData 表
        }
        
        // 采集并保存设备信息（每次都执行）
        Log.d(TAG, "📝 采集设备信息...")
        val result = initializeDeviceInfo(context, dataBase)
        
        // 🌐 第二步：测试服务器连接并上报设备信息（每次都执行，重新生成标识符）
        if (result) {
            testServerAndReport(context, dataBase)
        }
        
        Log.d(TAG, "📝 OOBE 初始化结果: $result")
        return result
    }
    
    /**
     * 降级到开发模式：设置标识符为 "dev"
     */
    private fun fallbackToDevMode(dataBase: DataBase) {
        try {
            // 将 device_id 设置为 "dev" 的二进制形式（简单存储文本）
            dataBase.insertOrReplaceMetadataText(KEY_DEVICE_ID, "dev")
            Log.d(TAG, "✅ 已设置开发版标识符: dev")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 设置开发版标识符失败", e)
        }
    }
    
    /**
     * 测试服务器连接并上报设备信息
     * 
     * @param context 上下文
     * @param dataBase 数据库实例
     */
    private fun testServerAndReport(context: Context, dataBase: DataBase) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 获取服务器 URL（从 BuildConfig 或配置文件）
                val serverUrl = getServerUrl()
                
                Log.d(TAG, "🌐 服务器 URL: $serverUrl")
                
                // 第一步：注册设备公钥（不签名）
                val deviceId = KeystoreManager.getDeviceId(context)
                val publicKey = KeystoreManager.exportPublicKey()
                
                Log.d(TAG, "🔑 注册设备公钥...")
                Log.d(TAG, "   Device ID: $deviceId")
                Log.d(TAG, "   Public Key Length: ${publicKey.length} chars")
                
                // 构建紧凑的 JSON（无空格，确保签名一致）
                val registerJson = org.json.JSONObject().apply {
                    put("deviceId", deviceId)
                    put("publicKey", publicKey)
                }.toString()
                
                Log.d(TAG, "📦 注册请求体: $registerJson")
                
                val jsonMediaType = "application/json; charset=utf-8".toMediaType()
                val registerBody = okhttp3.RequestBody.create(jsonMediaType, registerJson)
                
                val registerRequest = NetworkClient.buildPostRequest(
                    context = context,
                    url = "$serverUrl/register",
                    path = "/lsfStudio/api/register",
                    body = registerBody,
                    bodyContent = registerJson,  // 传递原始 JSON 字符串用于签名
                    autoSign = false  // 注册接口不签名
                )
                
                val registerResponse = withContext(Dispatchers.IO) {
                    NetworkClient.execute(registerRequest)
                }
                
                val registerResponseBody = registerResponse.body?.string()
                
                if (registerResponse.isSuccessful) {
                    Log.d(TAG, "✅ 设备公钥注册成功")
                    Log.d(TAG, "   响应: $registerResponseBody")
                } else {
                    Log.w(TAG, "⚠️ 设备公钥注册失败: ${registerResponse.code}")
                    Log.w(TAG, "   响应: $registerResponseBody")
                    // 继续，可能已经注册过了
                }
                
                registerResponse.close()
                
                // 第二步：测试服务器连通性（自动签名）
                Log.d(TAG, "📡 发送测试请求...")
                
                val testRequest = NetworkClient.buildGetRequest(
                    context = context,
                    url = "$serverUrl/test",
                    path = "/lsfStudio/api/test"
                )
                
                val testResponse = withContext(Dispatchers.IO) {
                    NetworkClient.execute(testRequest)
                }
                
                val testResponseBody = testResponse.body?.string()
                
                if (testResponse.isSuccessful) {
                    Log.d(TAG, "✅ 服务器连接测试成功: ${testResponse.code}")
                    Log.d(TAG, "   响应: $testResponseBody")
                    
                    // 在主线程显示 Toast
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        try {
                            MessageManager.showToast(
                                context,
                                "测试服务器连通性：${testResponse.code}",
                                Toast.LENGTH_SHORT
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "显示 Toast 失败", e)
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ 服务器返回错误: ${testResponse.code} ${testResponse.message}")
                    Log.w(TAG, "   响应: $testResponseBody")
                    
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        try {
                            MessageManager.showToast(
                                context,
                                "测试服务器连通性：${testResponse.code}",
                                Toast.LENGTH_LONG
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "显示 Toast 失败", e)
                        }
                    }
                }
                
                testResponse.close()
                
                // 第三步：上报设备信息并接收标识符（自动签名）
                Log.d(TAG, "📤 上报设备信息...")
                val deviceInfo = collectDeviceInfo(context)
                
                // 构建紧凑的 JSON（无空格，确保签名一致）
                val reportJson = org.json.JSONObject().apply {
                    put("deviceId", deviceId)
                    put("androidId", deviceInfo.androidId)
                    put("brand", deviceInfo.brand)
                    put("model", deviceInfo.model)
                    put("device", deviceInfo.device)
                    put("board", deviceInfo.board)
                    put("hardware", deviceInfo.hardware)
                }.toString()
                
                Log.d(TAG, "📦 请求体: $reportJson")
                
                val reportBody = okhttp3.RequestBody.create(
                    "application/json; charset=utf-8".toMediaType(),
                    reportJson
                )
                
                val reportRequest = NetworkClient.buildPostRequest(
                    context = context,
                    url = "$serverUrl/report",
                    path = "/lsfStudio/api/report",
                    body = reportBody,
                    bodyContent = reportJson  // 传递原始 JSON 字符串用于签名
                )
                
                val reportResponse = withContext(Dispatchers.IO) {
                    NetworkClient.execute(reportRequest)
                }
                
                val reportResponseBody = reportResponse.body?.string()
                
                if (reportResponse.isSuccessful) {
                    Log.d(TAG, "✅ 设备信息上报成功")
                    Log.d(TAG, "   响应: $reportResponseBody")
                    
                    // 解析响应，获取二进制标识符
                    try {
                        val jsonResponse = org.json.JSONObject(reportResponseBody ?: "{}")
                        val binaryIdentifierBase64 = jsonResponse.optString("binaryIdentifier", "")
                        
                        if (binaryIdentifierBase64.isNotEmpty()) {
                            Log.d(TAG, "🔑 接收到二进制标识符")
                            
                            // 解码 Base64
                            val binaryIdentifier = android.util.Base64.decode(binaryIdentifierBase64, android.util.Base64.DEFAULT)
                            
                            // 通过 DataBase 存储到数据库
                            dataBase.insertOrReplaceMetadata(KEY_DEVICE_ID, binaryIdentifier)
                            
                            Log.d(TAG, "✅ 标识符已存储到数据库")
                            Log.d(TAG, "   长度: ${binaryIdentifier.size} bytes")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ 解析标识符失败", e)
                    }
                } else {
                    Log.w(TAG, "⚠️ 设备信息上报失败: ${reportResponse.code}")
                    Log.w(TAG, "   响应: $reportResponseBody")
                }
                
                reportResponse.close()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 服务器通信失败", e)
                
                // 在主线程显示 Toast
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    try {
                        MessageManager.showToast(
                            context,
                            "服务器通信失败",
                            Toast.LENGTH_LONG
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "显示 Toast 失败", e)
                    }
                }
            }
        }
    }
    
    /**
     * 获取服务器 URL
     * 从 BuildConfig 读取，避免硬编码和反射
     */
    private fun getServerUrl(): String {
        return try {
            // 尝试从 BuildConfig 读取
            val field = BuildConfig::class.java.getDeclaredField("SERVER_URL")
            field.get(null) as String
        } catch (e: Exception) {
            // 降级到默认值
            Log.w(TAG, "⚠️ 未配置 SERVER_URL，使用默认值")
            "https://www.lsfstudio.top/lsfStudio/api"
        }
    }
    
    /**
     * 启动时检查更新（在 OOBE 完成后调用）
     * 
     * @param context 上下文
     * @param checkUpdateEnabled 用户是否启用了自动检查更新
     */
    fun checkUpdateOnStartup(context: Context, checkUpdateEnabled: Boolean) {
        if (!checkUpdateEnabled) {
            Log.d(TAG, "⚙️ 用户禁用了自动检查更新")
            return
        }
        
        Log.d(TAG, "🔄 启动时检查更新...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 执行检查更新
                val latestVersion = checkNewVersion(context)
                
                Log.d(TAG, "获取到最新版本: ${latestVersion.versionName}, versionCode: ${latestVersion.versionCode}")
                
                // 获取当前版本代码
                val currentVersionCode = BuildConfig.VERSION_CODE
                
                Log.d(TAG, "当前版本 code: $currentVersionCode")
                
                // 比较版本号
                val hasUpdate = latestVersion.versionCode > currentVersionCode
                
                Log.d(TAG, "是否有更新: $hasUpdate")
                
                // 检查是否有错误信息
                if (latestVersion.errorCode != 0 || latestVersion.errorMessage.isNotEmpty()) {
                    // 有错误，显示错误 Toast
                    Log.e(TAG, "检查更新错误: ${latestVersion.errorCode} - ${latestVersion.errorMessage}")
                    
                    val toastMessage = if (latestVersion.errorCode > 0) {
                        "${latestVersion.errorCode}: ${latestVersion.errorMessage}"
                    } else {
                        latestVersion.errorMessage
                    }
                    
                    // 在主线程显示 Toast（使用MessageManager自动适配超级岛）
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        try {
                            MessageManager.showToast(
                                context,
                                toastMessage,
                                Toast.LENGTH_LONG
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "显示 Toast 失败", e)
                        }
                    }
                } else {
                    // 无错误，正常处理
                    if (hasUpdate) {
                        // 发现新版本，存储到全局状态
                        Log.d(TAG, "发现新版本: ${latestVersion.versionName}")
                        
                        // 将最新版本信息存储到 SharedPreferences，供 About 页面读取
                        val prefs = context.getSharedPreferences("update_info", Context.MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("version_name", latestVersion.versionName)
                            putInt("version_code", latestVersion.versionCode)
                            putString("download_url", latestVersion.downloadUrl)
                            putString("changelog", latestVersion.changelog)
                            putBoolean("has_update", true)
                            apply()
                        }
                        
                        Log.d(TAG, "✅ 更新信息已存储到 SharedPreferences")
                        
                        // 发送广播通知 MainActivity 显示更新对话框
                        val intent = android.content.Intent("com.lsfstudio.lsfTB.ACTION_SHOW_UPDATE_DIALOG")
                        context.sendBroadcast(intent)
                        
                    } else {
                        // 未发现新版本，显示 Toast
                        Log.d(TAG, "当前已是最新版本")
                        
                        // 清除之前的更新信息
                        val prefs = context.getSharedPreferences("update_info", Context.MODE_PRIVATE)
                        prefs.edit().clear().apply()
                        
                        // 在主线程显示 Toast
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            try {
                                MessageManager.showToast(
                                    context,
                                    "当前已是最新版本",
                                    Toast.LENGTH_SHORT
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "显示 Toast 失败", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // 发生异常时打印错误
                Log.e(TAG, "检查更新失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 初始化设备信息（首次启动）
     */
    private fun initializeDeviceInfo(context: Context, dataBase: DataBase): Boolean {
        return try {
            // 采集设备信息
            val deviceInfo = collectDeviceInfo(context)
            
            Log.d(TAG, "📱 采集到设备信息:")
            Log.d(TAG, "   ANDROID_ID: ${deviceInfo.androidId.take(8)}...")
            Log.d(TAG, "   Brand: ${deviceInfo.brand}")
            Log.d(TAG, "   Model: ${deviceInfo.model}")
            Log.d(TAG, "   Device: ${deviceInfo.device}")
            Log.d(TAG, "   Board: ${deviceInfo.board}")
            Log.d(TAG, "   Hardware: ${deviceInfo.hardware}")
            
            // 存储原始设备信息到数据库
            dataBase.insertOrReplaceMetadataText(KEY_ANDROID_ID, deviceInfo.androidId)
            dataBase.insertOrReplaceMetadataText(KEY_DEVICE_BRAND, deviceInfo.brand)
            dataBase.insertOrReplaceMetadataText(KEY_DEVICE_MODEL, deviceInfo.model)
            dataBase.insertOrReplaceMetadataText(KEY_DEVICE_DEVICE, deviceInfo.device)
            dataBase.insertOrReplaceMetadataText(KEY_DEVICE_BOARD, deviceInfo.board)
            dataBase.insertOrReplaceMetadataText(KEY_DEVICE_HARDWARE, deviceInfo.hardware)
            
            Log.d(TAG, "✅ 设备信息已存储到数据库")
            
            // 标记为已初始化
            dataBase.insertOrReplaceMetadataText(KEY_INITIALIZED, "true")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 初始化设备信息失败", e)
            false
        }
    }
    
    /**
     * 采集设备信息
     */
    @SuppressLint("HardwareIds")
    private fun collectDeviceInfo(context: Context): DeviceInfo {
        // 获取 ANDROID_ID
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "UNKNOWN"
        
        // 获取硬件信息
        val brand = Build.BRAND
        val model = Build.MODEL
        val device = Build.DEVICE
        val board = Build.BOARD
        val hardware = Build.HARDWARE
        
        Log.d(TAG, "📱 设备信息采集完成:")
        Log.d(TAG, "   ANDROID_ID: ${androidId.take(8)}...")
        Log.d(TAG, "   Brand: $brand")
        Log.d(TAG, "   Model: $model")
        Log.d(TAG, "   Device: $device")
        Log.d(TAG, "   Board: $board")
        Log.d(TAG, "   Hardware: $hardware")
        
        return DeviceInfo(
            androidId = androidId,
            brand = brand,
            model = model,
            device = device,
            board = board,
            hardware = hardware
        )
    }
    
    /**
     * 获取所有 MetaData（排除 device_id）
     * 
     * @param context 上下文
     * @return MetaData 键值对列表（按 key 排序）
     */
    fun getAllMetadata(context: Context): List<Pair<String, String>> {
        val dataBase = DataBase(context)
        val metadataList = mutableListOf<Pair<String, String>>()
        
        try {
            // 定义需要显示的键名映射
            val keyDisplayNames = mapOf(
                KEY_ANDROID_ID to "Android ID",
                KEY_DEVICE_BRAND to "品牌",
                KEY_DEVICE_MODEL to "型号",
                KEY_DEVICE_DEVICE to "设备代号",
                KEY_DEVICE_BOARD to "主板",
                KEY_DEVICE_HARDWARE to "硬件平台"
            )
            
            // 遍历所有定义的键
            keyDisplayNames.forEach { (key, displayName) ->
                val value = dataBase.getMetadataText(key)
                if (value != null) {
                    metadataList.add(Pair(displayName, value))
                }
            }
            
            Log.d(TAG, "📖 读取 MetaData: ${metadataList.size} 条记录")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 读取 MetaData 失败", e)
        }
        
        return metadataList
    }
    
}
