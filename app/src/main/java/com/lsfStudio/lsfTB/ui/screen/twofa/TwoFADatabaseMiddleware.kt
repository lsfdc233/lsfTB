package com.lsfStudio.lsfTB.ui.screen.twofa

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.lsfStudio.lsfTB.ui.util.DataBase

/**
 * 2FA数据库中间件
 * 
 * 职责：
 * 1. 启动时检查 2FA 需要的表是否存在，若不存在则命令主模块创建
 * 2. 封装所有 2FA 业务逻辑（账户的增删改查）
 * 3. 通过 DataBase 主模块操作数据库
 * 4. UI层只与此中间件交互
 */
class TwoFADatabaseMiddleware(context: Context) {
    
    companion object {
        private const val TAG = "TwoFAMiddleware"
        
        // 2FA账户表
        const val TABLE_2FA_ACCOUNTS = "TwoFAAccounts"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_ISSUER = "issuer"
        const val COL_SECRET = "secret"
        const val COL_TYPE = "type"  // "TOTP" 或 "HOTP"
        const val COL_COUNTER = "counter"
    }
    
    // 持有 DataBase 实例，通过它操作数据库
    private val dataBase = DataBase(context)
    
    /**
     * 初始化：检查并创建 2FA 需要的表
     * 在应用启动时调用
     */
    fun initialize() {
        Log.d(TAG, "🔍 检查 2FA 数据表...")
        
        // 检查 2FA 需要的表是否存在
        val accountsExists = dataBase.tableExists(TABLE_2FA_ACCOUNTS)
        
        if (!accountsExists) {
            Log.d(TAG, "⚠️ 2FA 表不存在，开始创建...")
            // 命令主模块创建表
            dataBase.executeSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_2FA_ACCOUNTS (
                    $COL_ID TEXT PRIMARY KEY,
                    $COL_NAME TEXT NOT NULL,
                    $COL_ISSUER TEXT DEFAULT '',
                    $COL_SECRET TEXT NOT NULL,
                    $COL_TYPE TEXT NOT NULL DEFAULT 'TOTP',
                    $COL_COUNTER INTEGER DEFAULT 0
                )
            """)
            
            Log.d(TAG, "✅ 2FA 数据表创建完成")
        } else {
            Log.d(TAG, "✅ 2FA 数据表已存在")
        }
    }
    
    // ==================== 账户操作 ====================
    
    /**
     * 添加账户
     * @return 账户ID
     */
    fun addAccount(
        id: String,
        name: String,
        issuer: String,
        secret: String,
        type: String = "TOTP",
        counter: Long = 0L
    ): Boolean {
        return try {
            val db = dataBase.writableDatabase
            val values = ContentValues().apply {
                put(COL_ID, id)
                put(COL_NAME, name)
                put(COL_ISSUER, issuer)
                put(COL_SECRET, secret)
                put(COL_TYPE, type)
                put(COL_COUNTER, counter)
            }
            db.insertWithOnConflict(TABLE_2FA_ACCOUNTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            Log.d(TAG, "✅ 添加2FA账户: id=$id, name=$name, type=$type")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 添加2FA账户失败: id=$id", e)
            false
        }
    }
    
    /**
     * 删除账户
     */
    fun deleteAccount(id: String): Boolean {
        return try {
            val db = dataBase.writableDatabase
            val rows = db.delete(TABLE_2FA_ACCOUNTS, "$COL_ID = ?", arrayOf(id))
            Log.d(TAG, "🗑️ 删除2FA账户: id=$id, rows=$rows")
            rows > 0
        } catch (e: Exception) {
            Log.e(TAG, "❌ 删除2FA账户失败: id=$id", e)
            false
        }
    }
    
    /**
     * 批量删除账户
     */
    fun deleteAccounts(ids: List<String>): Int {
        return try {
            val db = dataBase.writableDatabase
            val placeholders = ids.joinToString(",") { "?" }
            val rows = db.delete(
                TABLE_2FA_ACCOUNTS,
                "$COL_ID IN ($placeholders)",
                ids.toTypedArray()
            )
            Log.d(TAG, "🗑️ 批量删除2FA账户: count=${ids.size}, rows=$rows")
            rows
        } catch (e: Exception) {
            Log.e(TAG, "❌ 批量删除2FA账户失败", e)
            0
        }
    }
    
    /**
     * 更新账户计数器（用于HOTP）
     */
    fun updateCounter(id: String, counter: Long): Boolean {
        return try {
            val db = dataBase.writableDatabase
            val values = ContentValues().apply {
                put(COL_COUNTER, counter)
            }
            val rows = db.update(TABLE_2FA_ACCOUNTS, values, "$COL_ID = ?", arrayOf(id))
            Log.d(TAG, "✅ 更新计数器: id=$id, counter=$counter")
            rows > 0
        } catch (e: Exception) {
            Log.e(TAG, "❌ 更新计数器失败: id=$id", e)
            false
        }
    }
    
    /**
     * 获取所有账户
     */
    fun getAllAccounts(): List<Map<String, Any?>> {
        return try {
            val db = dataBase.readableDatabase
            val cursor = db.query(
                TABLE_2FA_ACCOUNTS,
                null,
                null, null, null, null,
                "$COL_NAME ASC"
            )
            
            val results = mutableListOf<Map<String, Any?>>()
            while (cursor.moveToNext()) {
                results.add(mapOf(
                    "id" to cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)),
                    "name" to cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                    "issuer" to cursor.getString(cursor.getColumnIndexOrThrow(COL_ISSUER)),
                    "secret" to cursor.getString(cursor.getColumnIndexOrThrow(COL_SECRET)),
                    "type" to cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE)),
                    "counter" to cursor.getLong(cursor.getColumnIndexOrThrow(COL_COUNTER))
                ))
            }
            cursor.close()
            
            Log.d(TAG, "📋 获取2FA账户列表: ${results.size} 个账户")
            results
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取2FA账户列表失败", e)
            emptyList()
        }
    }
    
    /**
     * 根据ID获取账户
     */
    fun getAccountById(id: String): Map<String, Any?>? {
        return try {
            val db = dataBase.readableDatabase
            val cursor = db.query(
                TABLE_2FA_ACCOUNTS,
                null,
                "$COL_ID = ?",
                arrayOf(id),
                null, null, null
            )
            
            val result = if (cursor.moveToFirst()) {
                mapOf(
                    "id" to cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)),
                    "name" to cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                    "issuer" to cursor.getString(cursor.getColumnIndexOrThrow(COL_ISSUER)),
                    "secret" to cursor.getString(cursor.getColumnIndexOrThrow(COL_SECRET)),
                    "type" to cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE)),
                    "counter" to cursor.getLong(cursor.getColumnIndexOrThrow(COL_COUNTER))
                )
            } else {
                null
            }
            
            cursor.close()
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取2FA账户失败: id=$id", e)
            null
        }
    }
    
    /**
     * 清空所有账户
     */
    fun clearAllAccounts(): Boolean {
        return try {
            val db = dataBase.writableDatabase
            db.execSQL("DELETE FROM $TABLE_2FA_ACCOUNTS")
            Log.d(TAG, "🗑️ 清空所有2FA账户")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清空2FA账户失败", e)
            false
        }
    }
    
    /**
     * 获取账户数量
     */
    fun getAccountCount(): Int {
        return try {
            val db = dataBase.readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_2FA_ACCOUNTS", null)
            val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
            cursor.close()
            count
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取账户数量失败", e)
            0
        }
    }
}
