package com.lsfStudio.lsfTB.security

import android.util.Base64
import android.util.Log
import com.lsfStudio.lsfTB.ui.util.KeystoreManager
import java.security.Signature

/**
 * 挑战-响应签名器
 * 
 * 功能：
 * 1. 接收服务端生成的随机挑战
 * 2. 使用设备私钥对挑战进行签名
 * 3. 返回签名结果供服务端验证
 */
object ChallengeResponseSigner {
    
    private const val TAG = "ChallengeResponse"
    
    /**
     * 对挑战进行签名
     * 
     * @param challenge 服务端生成的随机挑战字符串
     * @return Base64编码的签名结果
     */
    fun signChallenge(challenge: String): String? {
        return try {
            Log.d(TAG, "🔐 开始签名挑战")
            Log.d(TAG, "   挑战长度: ${challenge.length} chars")
            
            // 使用 KeystoreManager 对挑战进行签名
            val signature = KeystoreManager.sign(challenge)
            
            if (signature.isEmpty()) {
                Log.e(TAG, "❌ 签名结果为空")
                return null
            }
            
            Log.d(TAG, "✅ 挑战签名成功")
            Log.d(TAG, "   签名长度: ${signature.length} chars")
            
            signature
        } catch (e: Exception) {
            Log.e(TAG, "❌ 挑战签名失败", e)
            null
        }
    }
    
    /**
     * 验证挑战格式
     * 
     * @param challenge 挑战字符串
     * @return true 如果格式有效
     */
    fun isValidChallenge(challenge: String): Boolean {
        // 挑战应该是十六进制字符串，长度为64（32字节）
        if (challenge.length != 64) {
            Log.w(TAG, "⚠️ 挑战长度不正确: ${challenge.length}")
            return false
        }
        
        // 检查是否为有效的十六进制字符串
        val hexPattern = Regex("^[0-9a-fA-F]+$")
        if (!hexPattern.matches(challenge)) {
            Log.w(TAG, "⚠️ 挑战包含非十六进制字符")
            return false
        }
        
        return true
    }
}
