package com.lsfStudio.lsfTB.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * 用户账户管理中间件
 * 
 * 职责：
 * 1. 管理用户登录状态
 * 2. 缓存用户信息到本地数据库
 * 3. 处理头像存储（私有目录）
 * 4. 提供用户信息查询接口
 */
object AccountManager {
    
    private const val TAG = "AccountManager"
    private const val AVATAR_DIR = "user_avatars"
    
    /**
     * 用户信息数据类
     */
    data class UserInfo(
        val username: String,
        val group: String,
        val tag: String,
        val level: Int,
        val experience: Int,
        val points: Int = 0,
        val nextLevel: Int,
        val isCheckedIn: Boolean = false,
        val avatarPath: String? = null  // 本地头像路径
    )
    
    /**
     * 保存用户信息到本地数据库
     * 
     * @param context 上下文
     * @param userInfo 用户信息
     */
    fun saveUserInfo(context: Context, userInfo: UserInfo) {
        Log.d(TAG, "💾 保存用户信息: ${userInfo.username}")
        
        val db = DataBase(context)
        
        // 检查 account 表是否存在，不存在则创建
        ensureAccountTableExists(db)
        
        // 插入或更新用户信息
        db.writableDatabase.execSQL(
            """
            INSERT OR REPLACE INTO account (
                username, `group`, tag, level, experience, points, next_level, is_checked_in, avatar_path, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
            """,
            arrayOf(
                userInfo.username,
                userInfo.group,
                userInfo.tag,
                userInfo.level.toString(),
                userInfo.experience.toString(),
                userInfo.points.toString(),
                userInfo.nextLevel.toString(),
                userInfo.isCheckedIn.toString(),
                userInfo.avatarPath
            )
        )
        
        db.close()
        Log.d(TAG, "✅ 用户信息已保存")
    }
    
    /**
     * 从本地数据库获取用户信息
     * 
     * @param context 上下文
     * @return 用户信息，如果未登录返回 null
     */
    fun getUserInfo(context: Context): UserInfo? {
        val db = DataBase(context)
        
        ensureAccountTableExists(db)
        
        val cursor = db.readableDatabase.rawQuery(
            "SELECT * FROM account LIMIT 1",
            null
        )
        
        return if (cursor.moveToFirst()) {
            val userInfo = UserInfo(
                username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                group = cursor.getString(cursor.getColumnIndexOrThrow("group")),
                tag = cursor.getString(cursor.getColumnIndexOrThrow("tag")),
                level = cursor.getInt(cursor.getColumnIndexOrThrow("level")),
                experience = cursor.getInt(cursor.getColumnIndexOrThrow("experience")),
                points = cursor.getInt(cursor.getColumnIndexOrThrow("points")),
                nextLevel = cursor.getInt(cursor.getColumnIndexOrThrow("next_level")),
                isCheckedIn = cursor.getString(cursor.getColumnIndexOrThrow("is_checked_in")).toBoolean(),
                avatarPath = cursor.getString(cursor.getColumnIndexOrThrow("avatar_path"))
            )
            cursor.close()
            db.close()
            Log.d(TAG, "📖 读取用户信息: ${userInfo.username}")
            userInfo
        } else {
            cursor.close()
            db.close()
            Log.d(TAG, "ℹ️ 用户未登录")
            null
        }
    }
    
    /**
     * 保存头像到私有目录
     * 
     * @param context 上下文
     * @param base64Image Base64 编码的图片数据
     * @param username 用户名（用于文件名）
     * @return 本地文件路径
     */
    fun saveAvatar(context: Context, base64Image: String, username: String): String? {
        return try {
            // 解析 Base64 图片
            val imageData = parseBase64Image(base64Image) ?: return null
            
            // 创建头像目录
            val avatarDir = File(context.filesDir, AVATAR_DIR)
            if (!avatarDir.exists()) {
                avatarDir.mkdirs()
            }
            
            // 生成文件名
            val fileName = "${username}_avatar.png"
            val avatarFile = File(avatarDir, fileName)
            
            // 写入文件
            FileOutputStream(avatarFile).use { fos ->
                fos.write(imageData)
            }
            
            val path = avatarFile.absolutePath
            Log.d(TAG, "📷 头像已保存: $path")
            path
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 保存头像失败", e)
            null
        }
    }
    
    /**
     * 清除用户信息（登出）
     * 
     * @param context 上下文
     */
    fun clearUserInfo(context: Context) {
        Log.d(TAG, "🚪 清除用户信息")
        
        val db = DataBase(context)
        db.writableDatabase.execSQL("DELETE FROM account")
        db.close()
        
        // 删除头像文件
        val avatarDir = File(context.filesDir, AVATAR_DIR)
        if (avatarDir.exists()) {
            avatarDir.deleteRecursively()
        }
        
        Log.d(TAG, "✅ 用户信息已清除")
    }
    
    /**
     * 检查是否已登录
     * 
     * @param context 上下文
     * @return true 如果已登录
     */
    fun isLoggedIn(context: Context): Boolean {
        return getUserInfo(context) != null
    }
    
    /**
     * 确保 account 表存在
     */
    private fun ensureAccountTableExists(db: DataBase) {
        db.writableDatabase.execSQL(
            """
            CREATE TABLE IF NOT EXISTS account (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                `group` TEXT NOT NULL DEFAULT 'user',
                tag TEXT DEFAULT '',
                level INTEGER NOT NULL DEFAULT 0,
                experience INTEGER NOT NULL DEFAULT 0,
                points INTEGER NOT NULL DEFAULT 0,
                next_level INTEGER NOT NULL DEFAULT 0,
                is_checked_in TEXT NOT NULL DEFAULT 'false',
                avatar_path TEXT,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """
        )

        ensureAccountColumn(db, "points", "INTEGER NOT NULL DEFAULT 0")
        ensureAccountColumn(db, "is_checked_in", "TEXT NOT NULL DEFAULT 'false'")
    }

    private fun ensureAccountColumn(db: DataBase, columnName: String, definition: String) {
        val cursor = db.readableDatabase.rawQuery("PRAGMA table_info(account)", null)
        var exists = false
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == columnName) {
                exists = true
                break
            }
        }
        cursor.close()

        if (!exists) {
            db.writableDatabase.execSQL("ALTER TABLE account ADD COLUMN $columnName $definition")
        }
    }
    
    /**
     * 解析 Base64 图片数据
     * 
     * @param base64String Base64 字符串（可能包含 data:image/xxx;base64, 前缀）
     * @return 图片字节数组
     */
    private fun parseBase64Image(base64String: String): ByteArray? {
        return try {
            // 移除 data:image/xxx;base64, 前缀
            val base64Data = if (base64String.contains(",")) {
                base64String.substring(base64String.indexOf(",") + 1)
            } else {
                base64String
            }
            
            Base64.decode(base64Data, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 解析 Base64 图片失败", e)
            null
        }
    }
}
