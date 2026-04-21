package com.lsfStudio.lsfTB.ui.util

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * 主数据库模块
 * 
 * 职责：
 * 1. 统一管理 lsfTB.db 数据库
 * 2. 提供通用的 CRUD 操作接口
 * 3. 管理数据库版本
 * 4. 接受来自各个中间件的命令执行操作
 * 
 * 架构说明：
 * - 这是唯一的 SQLiteOpenHelper 实现
 * - 所有页面中间件通过此类操作数据库
 * - 启动时自动检查并创建数据库
 */
class DataBase(context: Context) : SQLiteOpenHelper(
    context,
    DatabaseConfig.DATABASE_NAME,
    null,
    DatabaseConfig.DATABASE_VERSION
) {
    
    companion object {
        private const val TAG = "DataBase"
        
        // ==================== MetaData 表（通用键值对存储）====================
        const val TABLE_METADATA = "MetaData"
        const val COL_META_KEY = "key"
        const val COL_META_VALUE = "value"  // BLOB类型，支持二进制数据
        
        /**
         * 创建 MetaData 表
         * 用于存储应用的元数据（如设备ID、初始化状态等）
         */
        fun createMetaTable(db: SQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_METADATA (
                    $COL_META_KEY TEXT PRIMARY KEY,
                    $COL_META_VALUE BLOB
                )
            """)
            Log.d(TAG, "✅ MetaData 表创建成功")
        }
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        Log.d(TAG, "📦 创建数据库: ${DatabaseConfig.DATABASE_NAME}, 版本: ${DatabaseConfig.DATABASE_VERSION}")
        
        // 创建 MetaData 表
        createMetaTable(db)
        
        // 注意：其他表由各页面中间件负责创建
        // Vault 表由 VaultDatabaseMiddleware 创建
        // 2FA 表由 TwoFADatabaseMiddleware 创建
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "🔄 数据库升级: $oldVersion -> $newVersion")
        
        // 根据版本进行升级
        if (oldVersion < 2) {
            // 版本2的升级逻辑
        }
        
        if (oldVersion < 3) {
            // 版本3的升级逻辑
        }
        
        Log.d(TAG, "✅ 数据库升级完成")
    }
    
    // ==================== 通用 CRUD 操作 ====================
    
    /**
     * 插入或替换元数据（二进制）
     * @param key 键
     * @param value 值（二进制）
     * @return 是否成功
     */
    fun insertOrReplaceMetadata(key: String, value: ByteArray): Boolean {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_META_KEY, key)
                put(COL_META_VALUE, value)
            }
            db.insertWithOnConflict(TABLE_METADATA, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            Log.d(TAG, "✅ 写入元数据: key=$key, size=${value.size} bytes")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 写入元数据失败: key=$key", e)
            false
        }
    }
    
    /**
     * 插入或替换元数据（文本）
     * @param key 键
     * @param value 值（文本）
     * @return 是否成功
     */
    fun insertOrReplaceMetadataText(key: String, value: String): Boolean {
        return insertOrReplaceMetadata(key, value.toByteArray())
    }
    
    /**
     * 获取元数据（二进制）
     * @param key 键
     * @return 二进制数据，不存在返回 null
     */
    fun getMetadataBinary(key: String): ByteArray? {
        return try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_METADATA,
                arrayOf(COL_META_VALUE),
                "$COL_META_KEY = ?",
                arrayOf(key),
                null, null, null
            )
            
            val result = if (cursor.moveToFirst()) {
                cursor.getBlob(cursor.getColumnIndexOrThrow(COL_META_VALUE))
            } else {
                null
            }
            
            cursor.close()
            Log.d(TAG, "📖 读取元数据: key=$key, found=${result != null}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ 读取元数据失败: key=$key", e)
            null
        }
    }
    
    /**
     * 获取元数据（文本）
     * @param key 键
     * @return 文本数据，不存在返回 null
     */
    fun getMetadataText(key: String): String? {
        val binary = getMetadataBinary(key)
        return binary?.let { 
            try {
                String(it, Charsets.UTF_8)  // 明确指定 UTF-8 编码
            } catch (e: Exception) {
                Log.e(TAG, "❌ 解码元数据失败: key=$key", e)
                null
            }
        }
    }
    
    /**
     * 删除元数据
     * @param key 键
     * @return 是否成功
     */
    fun deleteMetadata(key: String): Boolean {
        return try {
            val db = writableDatabase
            val rows = db.delete(TABLE_METADATA, "$COL_META_KEY = ?", arrayOf(key))
            Log.d(TAG, "🗑️ 删除元数据: key=$key, rows=$rows")
            rows > 0
        } catch (e: Exception) {
            Log.e(TAG, "❌ 删除元数据失败: key=$key", e)
            false
        }
    }
    
    /**
     * 执行原始 SQL 语句
     * @param sql SQL 语句
     */
    fun executeSQL(sql: String) {
        try {
            writableDatabase.execSQL(sql)
            Log.d(TAG, "✅ 执行 SQL: $sql")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 执行 SQL 失败: $sql", e)
            throw e
        }
    }
    
    /**
     * 检查表是否存在
     * @param tableName 表名
     * @return 是否存在
     */
    fun tableExists(tableName: String): Boolean {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(tableName)
            )
            val exists = cursor.count > 0
            cursor.close()
            Log.d(TAG, "🔍 检查表: $tableName, exists=$exists")
            exists
        } catch (e: Exception) {
            Log.e(TAG, "❌ 检查表失败: $tableName", e)
            false
        }
    }
}

/**
 * 数据库配置
 */
object DatabaseConfig {
    const val DATABASE_NAME = "lsfTB.db"
    const val DATABASE_VERSION = 3  // 当前数据库版本
}
