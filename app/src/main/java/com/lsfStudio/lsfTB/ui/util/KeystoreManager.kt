package com.lsfStudio.lsfTB.ui.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

/**
 * Android Keystore 密钥管理器
 * 
 * 职责：
 * 1. 生成 RSA 密钥对（私钥不可导出）
 * 2. 使用私钥进行签名
 * 3. 导出公钥用于服务端验证
 */
object KeystoreManager {
    
    private const val TAG = "KeystoreManager"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "lsfTB_api_signing_key"
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_RSA
    private const val SIGNATURE_ALGORITHM = "SHA256withRSA"
    
    /**
     * 初始化密钥对（如果不存在则生成）
     */
    fun initialize(context: Context) {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                Log.d(TAG, "🔑 生成新的 RSA 密钥对...")
                generateKeyPair()
                Log.d(TAG, "✅ 密钥对生成成功")
            } else {
                Log.d(TAG, "✅ 密钥对已存在")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 初始化密钥对失败", e)
            throw RuntimeException("Keystore 初始化失败", e)
        }
    }
    
    /**
     * 生成 RSA 密钥对
     */
    private fun generateKeyPair() {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            ALGORITHM,
            KEYSTORE_PROVIDER
        )
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setKeySize(2048)
            .build()
        
        keyPairGenerator.initialize(keyGenParameterSpec)
        keyPairGenerator.generateKeyPair()
    }
    
    /**
     * 使用私钥对数据进行签名
     * 
     * @param data 待签名的数据（stringToSign）
     * @return Base64 编码的签名
     */
    fun sign(data: String): String {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            
            val privateKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey
            
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initSign(privateKey)
            signature.update(data.toByteArray(Charsets.UTF_8))
            
            val signedBytes = signature.sign()
            Base64.encodeToString(signedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 签名失败", e)
            throw RuntimeException("签名失败", e)
        }
    }
    
    /**
     * 导出公钥（Base64 编码）
     * 
     * @return Base64 编码的公钥
     */
    fun exportPublicKey(): String {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            
            val certificate = keyStore.getCertificate(KEY_ALIAS)
            val publicKey = certificate.publicKey
            
            Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 导出公钥失败", e)
            throw RuntimeException("导出公钥失败", e)
        }
    }
    
    /**
     * 获取设备唯一标识（ANDROID_ID）
     * 
     * @param context 上下文
     * @return 设备 ID
     */
    fun getDeviceId(context: Context): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }
}
