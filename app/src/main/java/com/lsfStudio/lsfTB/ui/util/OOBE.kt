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
    const val KEY_DEVICE_ID = "device_id"             // 旧版本兼容键，客户端不再写入
    const val KEY_UID = "device_uid"                  // 服务端分配的唯一 UID
    
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
        
        // 🔒 第二步：初始化完整性校验器（获取当前 APK 签名哈希）
        try {
            com.lsfStudio.lsfTB.security.IntegrityChecker.initialize(context)
            Log.d(TAG, "✅ 完整性校验器初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 完整性校验器初始化失败", e)
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
        
        // 🌐 第二步：测试服务器连接并上报设备信息（已移至 HomeViewModel.checkUpdate 后执行）
        // if (result) {
        //     testServerAndReport(context, dataBase)
        // }
        
        Log.d(TAG, "📝 OOBE 初始化结果: $result")
        return result
    }
    
    /**
     * 降级到开发模式：设置标识符为 "dev"
     */
    private fun fallbackToDevMode(dataBase: DataBase) {
        try {
            dataBase.deleteMetadata(KEY_DEVICE_ID)
            Log.d(TAG, "✅ 已清理旧版 device_id")
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
                
                // 第一步：注册设备公钥 (Bootstrap)
                val deviceId = KeystoreManager.getDeviceId(context)
                val publicKey = KeystoreManager.exportPublicKey()
                
                Log.d(TAG, "🔑 注册设备公钥...")
                Log.d(TAG, "   Device ID: $deviceId")
                
                // 构建紧凑的 JSON
                val registerKeyJson = org.json.JSONObject().apply {
                    put("public_key", publicKey)
                }.toString()
                
                val registerKeyResponse = withContext(Dispatchers.IO) {
                    NetworkClient.send(
                        context = context,
                        method = "POST",
                        url = "$serverUrl/register-key",
                        path = "/lsfStudio/api/register-key",
                        bodyContent = registerKeyJson,
                        useChallengeResponse = false
                    )
                }
                
                if (registerKeyResponse.isSuccessful) {
                    Log.d(TAG, "✅ 设备公钥注册成功")
                } else {
                    Log.w(TAG, "⚠️ 设备公钥注册失败: ${registerKeyResponse.code}")
                    return@launch
                }
                
                // 第二步：上报设备元数据并接收 UID
                Log.d(TAG, "📤 上报设备元数据...")
                val deviceInfo = collectDeviceInfo(context)
                
                val reportJson = org.json.JSONObject().apply {
                    put("android_id", deviceInfo.androidId)
                    put("brand", deviceInfo.brand)
                    put("model", deviceInfo.model)
                    put("device", deviceInfo.device)
                    put("board", deviceInfo.board)
                    put("hardware", deviceInfo.hardware)
                }.toString()
                
                val reportResponse = withContext(Dispatchers.IO) {
                    NetworkClient.send(
                        context = context,
                        method = "POST",
                        url = "$serverUrl/register",
                        path = "/lsfStudio/api/register",
                        bodyContent = reportJson
                    )
                }
                
                if (reportResponse.isSuccessful) {
                    Log.d(TAG, "✅ 设备元数据上报成功")
                    try {
                        val jsonResponse = org.json.JSONObject(reportResponse.body ?: "{}")
                        val uid = jsonResponse.optJSONObject("data")?.optInt("uid", -1)
                        if (uid != null && uid != -1) {
                            // 存储 UID
                            dataBase.insertOrReplaceMetadataText(KEY_UID, uid.toString())
                            dataBase.deleteMetadata(KEY_DEVICE_ID)
                            Log.d(TAG, "✅ UID 已存储: $uid")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ 解析 UID 失败", e)
                    }
                } else {
                    Log.w(TAG, "⚠️ 设备元数据上报失败: ${reportResponse.code}")
                }
                
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
     * 启动时检查更新（已废弃，移至 HomeViewModel）
     * 
     * @param context 上下文
     * @param checkUpdateEnabled 用户是否启用了自动检查更新
     */
    fun checkUpdateOnStartup(context: Context, checkUpdateEnabled: Boolean) {
        // 此方法已废弃，启动时检查更新已移至 HomeViewModel
        // 保留此方法仅为兼容性
        Log.d(TAG, "⚠️ checkUpdateOnStartup 已废弃，请使用 HomeViewModel")
    }
    
    /**
     * 测试服务器连接并上报设备信息（由 HomeViewModel 在检查更新后调用）
     * 
     * @param context 上下文
     */
    fun testServerConnection(context: Context) {
        Log.d(TAG, "🌐 OOBE.testServerConnection 被调用")
        val dataBase = DataBase(context)
        testServerAndReport(context, dataBase)
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
                KEY_UID to "设备 UID",
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
