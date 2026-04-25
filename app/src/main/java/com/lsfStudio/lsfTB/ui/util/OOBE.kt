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
        
        val dataBase = DataBase(context)
        
        // 检查 MetaData 表是否存在
        if (!dataBase.tableExists(DataBase.TABLE_METADATA)) {
            Log.w(TAG, "⚠️ MetaData 表不存在，等待 DataBase 创建...")
            // DataBase 会在 onCreate 中自动创建 MetaData 表
        }
        
        // 检查是否已初始化
        val initialized = dataBase.getMetadataText(KEY_INITIALIZED)
        
        return if (initialized == "true") {
            // 已初始化，直接通过
            Log.d(TAG, "✅ OOBE 已初始化")
            true
        } else {
            // 未初始化，采集并保存设备信息
            Log.d(TAG, "📝 OOBE 未初始化，开始采集设备信息...")
            val result = initializeDeviceInfo(context, dataBase)
            
            // 🔧 容错处理：尝试生成设备标识符，如果失败则使用 "dev" 模式
            if (result) {
                try {
                    // 检查 OOBESecurity 类是否存在
                    Class.forName("com.lsfStudio.lsfTB.ui.util.OOBESecurity")
                    
                    // 类存在，调用正常流程
                    val securityResult = com.lsfStudio.lsfTB.ui.util.OOBESecurity.generateAndStoreDeviceIdentifier(context)
                    
                    if (securityResult) {
                        Log.d(TAG, "✅ 设备标识符生成成功")
                    } else {
                        Log.w(TAG, "⚠️ 设备标识符生成失败，降级到 dev 模式")
                        fallbackToDevMode(dataBase)
                    }
                } catch (e: ClassNotFoundException) {
                    // 类不存在（文件被 .gitignore），直接使用 dev 模式
                    Log.w(TAG, "⚠️ OOBESecurity 模块不可用，降级到 dev 模式")
                    fallbackToDevMode(dataBase)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 调用 OOBESecurity 失败", e)
                    fallbackToDevMode(dataBase)
                }
            }
            
            Log.d(TAG, "📝 OOBE 初始化结果: $result")
            result
        }
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
                        // 发现新版本，保持 latestVersionInfo 不变，由 UI 层弹窗显示
                        Log.d(TAG, "发现新版本: ${latestVersion.versionName}")
                        // 可以考虑使用 LiveData、Flow 或者广播
                    } else {
                        // 未发现新版本，显示 Toast
                        Log.d(TAG, "当前已是最新版本")
                        
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
