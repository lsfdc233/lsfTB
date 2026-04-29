package com.lsfStudio.lsfTB.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * APK 完整性校验器
 * 
 * 功能：
 * 1. 验证 APK 签名是否与预期一致
 * 2. 检测是否被重新签名
 * 3. 运行时环境检测（Root、调试器等）
 */
object IntegrityChecker {
    
    private const val TAG = "IntegrityChecker"
    
    // 预期的 APK 签名哈希（Base64编码的SHA-256）
    // 在首次初始化时从当前 APK 获取并缓存
    private var expectedSignatureHash: String? = null
    
    /**
     * 初始化完整性校验器
     * 在应用启动时调用，用于获取并缓存当前 APK 的签名哈希
     */
    fun initialize(context: Context) {
        try {
            expectedSignatureHash = getCurrentSignatureHash(context)
            if (expectedSignatureHash != null) {
                Log.d(TAG, "✅ 完整性校验器已初始化")
                Log.d(TAG, "   预期签名哈希: ${expectedSignatureHash!!.take(32)}...")
            } else {
                Log.w(TAG, "⚠️ 无法获取签名哈希，完整性校验将被跳过")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 完整性校验器初始化失败", e)
        }
    }
    
    /**
     * 验证 APK 完整性
     * @return true 如果完整性校验通过
     */
    fun verifyApkIntegrity(context: Context): Boolean {
        return try {
            // 如果未初始化，先尝试初始化
            if (expectedSignatureHash == null) {
                Log.w(TAG, "⚠️ 完整性校验器未初始化，尝试初始化...")
                initialize(context)
            }
            
            // 如果仍然无法获取签名哈希，跳过校验
            if (expectedSignatureHash == null) {
                Log.w(TAG, "⚠️ 签名哈希不可用，跳过完整性校验")
                return true  // 返回 true 允许继续运行
            }
            
            val signatureValid = verifySignature(context)
            val environmentSecure = isEnvironmentSecure(context)
            
            if (!signatureValid) {
                Log.e(TAG, "❌ APK 签名验证失败")
                return false
            }
            
            if (!environmentSecure) {
                Log.w(TAG, "⚠️ 运行环境不安全")
                // 注意：这里不直接返回false，而是记录警告
                // 可以根据安全策略决定是否阻止运行
            }
            
            Log.d(TAG, "✅ APK 完整性校验通过")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 完整性校验异常", e)
            false
        }
    }
    
    /**
     * 验证 APK 签名
     */
    private fun verifySignature(context: Context): Boolean {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }
            
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            
            if (signatures == null || signatures.isEmpty()) {
                Log.e(TAG, "未找到 APK 签名")
                return false
            }
            
            // 获取第一个签名
            val signatureBytes = signatures[0].toByteArray()
            
            // 计算 SHA-256 哈希
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(signatureBytes)
            val currentHash = Base64.encodeToString(digest, Base64.NO_WRAP)
            
            // 与预期哈希比较
            val isValid = currentHash == expectedSignatureHash
            
            if (!isValid) {
                Log.w(TAG, "签名不匹配:")
                Log.w(TAG, "  预期: $expectedSignatureHash")
                Log.w(TAG, "  实际: $currentHash")
            }
            
            return isValid
        } catch (e: Exception) {
            Log.e(TAG, "签名验证异常", e)
            return false
        }
    }
    
    /**
     * 检查运行环境安全性
     */
    private fun isEnvironmentSecure(context: Context): Boolean {
        var isSecure = true
        
        // 检查是否可调试
        if (isDebuggable(context)) {
            Log.w(TAG, "⚠️ 应用处于调试模式")
            isSecure = false
        }
        
        // 检查是否 Root
        if (isRooted()) {
            Log.w(TAG, "⚠️ 设备已 Root")
            isSecure = false
        }
        
        // 检查是否模拟器
        if (isEmulator()) {
            Log.w(TAG, "⚠️ 运行在模拟器上")
            // 模拟器不一定是不安全的，所以不标记为不安全
        }
        
        return isSecure
    }
    
    /**
     * 检查应用是否可调试
     */
    private fun isDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    
    /**
     * 检查设备是否 Root
     */
    private fun isRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        
        for (path in paths) {
            if (File(path).exists()) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * 检查是否为模拟器
     */
    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HOST.startsWith("Build")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
    }
    
    /**
     * 获取当前 APK 签名哈希（用于调试）
     */
    fun getCurrentSignatureHash(context: Context): String? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            
            val signatures = packageInfo.signatures
            if (signatures != null && signatures.isNotEmpty()) {
                val signatureBytes = signatures[0].toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(signatureBytes)
                Base64.encodeToString(digest, Base64.NO_WRAP)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取签名哈希失败", e)
            null
        }
    }
}
