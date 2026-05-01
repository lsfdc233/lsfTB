package com.lsfStudio.lsfTB.ui.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Vault文件加密管理器
 * 
 * 功能：
 * 1. 使用Android Keystore生成AES-GCM密钥（硬件保护）
 * 2. 加密文件：原始文件 → AES-256-GCM加密 → .tb文件
 * 3. 解密文件：.tb文件 → AES-256-GCM解密 → 临时文件
 * 
 * 安全特性：
 * - 密钥存储在硬件Keystore中，无法导出
 * - 使用GCM模式提供加密和完整性验证
 * - 每次加密使用随机IV（12字节）
 */
object VaultEncryptionManager {
    
    private const val TAG = "VaultEncryption"
    private const val KEY_ALIAS = "lsfTB_vault_encryption_key"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12  // 96 bits
    private const val GCM_TAG_LENGTH = 128  // 128 bits
    
    /**
     * 初始化加密密钥（如果不存在则生成）
     */
    fun initialize(context: Context) {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                Log.d(TAG, "🔑 生成新的AES加密密钥...")
                generateEncryptionKey()
                Log.d(TAG, "✅ 加密密钥生成成功")
            } else {
                Log.d(TAG, "✅ 加密密钥已存在")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 初始化加密密钥失败", e)
            throw RuntimeException("加密密钥初始化失败", e)
        }
    }
    
    /**
     * 生成AES加密密钥
     */
    private fun generateEncryptionKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }
    
    /**
     * 获取加密密钥
     */
    private fun getEncryptionKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)
        
        val secretKeyEntry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return secretKeyEntry.secretKey
    }
    
    /**
     * 加密文件
     * 
     * @param sourceFile 原始文件
     * @param destFile 目标文件（.tb格式）
     * @return true 如果加密成功
     */
    fun encryptFile(sourceFile: File, destFile: File): Boolean {
        return try {
            Log.d(TAG, "🔐 开始加密文件: ${sourceFile.name}")
            
            val key = getEncryptionKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            
            // 获取IV（初始化向量）
            val iv = cipher.iv
            
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    // 先写入IV（12字节）
                    output.write(iv)
                    
                    // 加密文件内容
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        val encryptedBytes = cipher.update(buffer, 0, bytesRead)
                        if (encryptedBytes != null) {
                            output.write(encryptedBytes)
                        }
                    }
                    
                    // 写入最后的加密块（包含认证标签）
                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null) {
                        output.write(finalBytes)
                    }
                }
            }
            
            Log.d(TAG, "✅ 文件加密成功: ${destFile.name} (${destFile.length()} bytes)")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 文件加密失败", e)
            false
        }
    }
    
    /**
     * 解密文件
     * 
     * @param sourceFile 加密文件（.tb格式）
     * @param destFile 目标文件（解密后的文件）
     * @return true 如果解密成功
     */
    fun decryptFile(sourceFile: File, destFile: File): Boolean {
        return try {
            Log.d(TAG, "🔓 开始解密文件: ${sourceFile.name}")
            
            val key = getEncryptionKey()
            
            FileInputStream(sourceFile).use { input ->
                // 读取IV（前12字节）
                val iv = ByteArray(GCM_IV_LENGTH)
                val ivBytesRead = input.read(iv)
                if (ivBytesRead != GCM_IV_LENGTH) {
                    Log.e(TAG, "❌ IV读取失败: 期望${GCM_IV_LENGTH}字节，实际${ivBytesRead}字节")
                    return false
                }
                
                // 初始化解密器
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec)
                
                FileOutputStream(destFile).use { output ->
                    // 解密文件内容
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        val decryptedBytes = cipher.update(buffer, 0, bytesRead)
                        if (decryptedBytes != null) {
                            output.write(decryptedBytes)
                        }
                    }
                    
                    // 写入最后的解密块
                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null) {
                        output.write(finalBytes)
                    }
                }
            }
            
            Log.d(TAG, "✅ 文件解密成功: ${destFile.name} (${destFile.length()} bytes)")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 文件解密失败", e)
            false
        }
    }
    
    /**
     * 解密文件到临时目录（用于预览）
     * 
     * @param context 上下文
     * @param encryptedFilePath 加密文件路径
     * @param originalFileName 原始文件名（用于确定扩展名）
     * @return 临时文件路径，失败返回null
     */
    fun decryptToTempFile(
        context: Context,
        encryptedFilePath: String,
        originalFileName: String
    ): File? {
        return try {
            val encryptedFile = File(encryptedFilePath)
            if (!encryptedFile.exists()) {
                Log.e(TAG, "❌ 加密文件不存在: $encryptedFilePath")
                return null
            }
            
            // 创建临时文件
            val tempDir = File(context.cacheDir, "vault_temp")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            
            // 提取扩展名
            val extension = originalFileName.substringAfterLast('.', "")
            val tempFileName = "preview_${System.currentTimeMillis()}.$extension"
            val tempFile = File(tempDir, tempFileName)
            
            // 解密到临时文件
            if (decryptFile(encryptedFile, tempFile)) {
                Log.d(TAG, "✅ 临时文件创建成功: ${tempFile.absolutePath}")
                tempFile
            } else {
                Log.e(TAG, "❌ 解密失败")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 创建临时文件失败", e)
            null
        }
    }
    
    /**
     * 清理临时文件
     */
    fun cleanupTempFiles(context: Context) {
        try {
            val tempDir = File(context.cacheDir, "vault_temp")
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
                Log.d(TAG, "🗑️ 临时文件已清理")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清理临时文件失败", e)
        }
    }
}
