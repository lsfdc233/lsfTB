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
            initializeDeviceInfo(context, dataBase)
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
    
}
