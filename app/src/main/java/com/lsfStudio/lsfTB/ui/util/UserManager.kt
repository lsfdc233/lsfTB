package com.lsfStudio.lsfTB.ui.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log

/**
 * 用户信息管理类
 * 负责用户信息的本地持久化和管理
 */
object UserManager {
    private const val TAG = "UserManager"
    
    /**
     * 用户信息数据类
     */
    data class UserInfo(
        val username: String = "",
        val group: String = "",
        val tag: String = "",
        val level: Int = 0,
        val experience: Int = 0,
        val points: Int = 0,
        val nextLevel: Int = 1000,
        val isCheckedIn: Boolean = false,
        val avatarPath: String? = null,
        val permissions: ByteArray? = null
    ) {
        companion object {
            fun fromJson(json: org.json.JSONObject): UserInfo {
                return UserInfo(
                    username = json.optString("username", ""),
                    group = json.optString("group", ""),
                    tag = json.optString("tag", ""),
                    level = json.optInt("level", 0),
                    experience = json.optInt("experience", 0),
                    points = json.optInt("points", 0),
                    nextLevel = json.optInt("next_level", 1000),
                    isCheckedIn = json.optString("is_checked_in", "false").toBoolean(),
                    avatarPath = null
                )
            }
        }
    }
    
    /**
     * 初始化用户信息表
     * 在应用启动时调用，检查并创建 user_info 表
     */
    fun initialize(context: Context) {
        try {
            val dataBase = DataBase(context)
            val db = dataBase.writableDatabase
            
            // 检查 user_info 表是否存在
            val cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='user_info'",
                null
            )
            
            if (cursor.count == 0) {
                Log.d(TAG, "📋 Creating user_info table...")
                db.execSQL("""
                    CREATE TABLE user_info (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL DEFAULT '',
                        `group` TEXT NOT NULL DEFAULT '',
                        tag TEXT NOT NULL DEFAULT '',
                        level INTEGER NOT NULL DEFAULT 0,
                        experience INTEGER NOT NULL DEFAULT 0,
                        points INTEGER NOT NULL DEFAULT 0,
                        next_level INTEGER NOT NULL DEFAULT 1000,
                        is_checked_in INTEGER NOT NULL DEFAULT 0,
                        avatar_path TEXT,
                        permissions BLOB,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """.trimIndent())
                Log.d(TAG, "✅ user_info table created successfully")
            } else {
                Log.d(TAG, "✅ user_info table already exists")
                // 检查并添加 permissions 字段（如果不存在）
                ensurePermissionsColumn(db)
            }
            
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize user_info table", e)
        }
    }
    
    /**
     * 保存或更新用户信息
     * 登录成功后调用
     */
    fun saveUserInfo(context: Context, userInfo: UserInfo) {
        try {
            val dataBase = DataBase(context)
            val db = dataBase.writableDatabase
            
            // 先删除旧记录（只保留一条）
            db.execSQL("DELETE FROM user_info")
            
            // 插入新记录
            db.execSQL("""
                INSERT INTO user_info 
                (username, `group`, tag, level, experience, points, next_level, is_checked_in, avatar_path, permissions, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
            """.trimIndent(), arrayOf(
                userInfo.username,
                userInfo.group,
                userInfo.tag,
                userInfo.level.toString(),
                userInfo.experience.toString(),
                userInfo.points.toString(),
                userInfo.nextLevel.toString(),
                if (userInfo.isCheckedIn) "1" else "0",
                userInfo.avatarPath,
                userInfo.permissions
            ))
            
            Log.d(TAG, "✅ User info saved: ${userInfo.username}, level=${userInfo.level}, exp=${userInfo.experience}, points=${userInfo.points}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save user info", e)
        }
    }
    
    /**
     * 更新签到状态和用户统计信息
     * 签到成功后调用
     */
    fun updateCheckinStatus(context: Context, userInfo: UserInfo) {
        try {
            val dataBase = DataBase(context)
            val db = dataBase.writableDatabase
            
            db.execSQL("""
                UPDATE user_info 
                SET level = ?,
                    experience = ?,
                    points = ?,
                    next_level = ?,
                    is_checked_in = ?,
                    updated_at = datetime('now')
                WHERE username = ?
            """.trimIndent(), arrayOf(
                userInfo.level.toString(),
                userInfo.experience.toString(),
                userInfo.points.toString(),
                userInfo.nextLevel.toString(),
                if (userInfo.isCheckedIn) "1" else "0",
                userInfo.username
            ))
            
            Log.d(TAG, "✅ Checkin status updated: level=${userInfo.level}, exp=${userInfo.experience}, points=${userInfo.points}, checkedIn=${userInfo.isCheckedIn}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update checkin status", e)
        }
    }
    
    /**
     * 获取当前用户信息
     */
    fun getUserInfo(context: Context): UserInfo? {
        try {
            val dataBase = DataBase(context)
            val db = dataBase.readableDatabase
            
            val cursor = db.rawQuery("SELECT * FROM user_info LIMIT 1", null)
            
            if (cursor.moveToFirst()) {
                val userInfo = UserInfo(
                    username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    group = cursor.getString(cursor.getColumnIndexOrThrow("group")),
                    tag = cursor.getString(cursor.getColumnIndexOrThrow("tag")),
                    level = cursor.getInt(cursor.getColumnIndexOrThrow("level")),
                    experience = cursor.getInt(cursor.getColumnIndexOrThrow("experience")),
                    points = cursor.getInt(cursor.getColumnIndexOrThrow("points")),
                    nextLevel = cursor.getInt(cursor.getColumnIndexOrThrow("next_level")),
                    isCheckedIn = cursor.getInt(cursor.getColumnIndexOrThrow("is_checked_in")) == 1,
                    avatarPath = cursor.getString(cursor.getColumnIndexOrThrow("avatar_path")),
                    permissions = cursor.getBlob(cursor.getColumnIndexOrThrow("permissions"))
                )
                
                cursor.close()
                Log.d(TAG, "📖 User info retrieved: ${userInfo.username}, level=${userInfo.level}")
                return userInfo
            }
            
            cursor.close()
            Log.d(TAG, "ℹ️ No user info found in database")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get user info", e)
            return null
        }
    }
    
    /**
     * 清除用户信息（登出时调用）
     */
    fun clearUserInfo(context: Context) {
        try {
            val dataBase = DataBase(context)
            val db = dataBase.writableDatabase
            
            db.execSQL("DELETE FROM user_info")
            Log.d(TAG, "✅ User info cleared")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to clear user info", e)
        }
    }
    
    /**
     * 确保 user_info 表有 permissions 字段
     */
    private fun ensurePermissionsColumn(db: SQLiteDatabase) {
        try {
            // 检查列是否存在
            val cursor = db.rawQuery("PRAGMA table_info(user_info)", null)
            var hasPermissionsColumn = false
            
            while (cursor.moveToNext()) {
                val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                if (columnName == "permissions") {
                    hasPermissionsColumn = true
                    break
                }
            }
            cursor.close()
            
            // 如果不存在，添加列
            if (!hasPermissionsColumn) {
                Log.d(TAG, "📋 Adding permissions column to user_info table...")
                db.execSQL("ALTER TABLE user_info ADD COLUMN permissions BLOB")
                Log.d(TAG, "✅ permissions column added")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to ensure permissions column", e)
        }
    }
}
