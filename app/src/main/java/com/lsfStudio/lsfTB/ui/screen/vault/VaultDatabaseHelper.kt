package com.lsfStudio.lsfTB.ui.screen.vault

import android.database.sqlite.SQLiteDatabase
import android.util.Log

/**
 * Vault数据库表结构定义
 * 
 * 职责：
 * 1. 定义 Vault 需要的表结构
 * 2. 提供建表 SQL 语句
 * 3. 不负责数据库操作，只负责表结构
 * 
 * 注意：此类不继承 SQLiteOpenHelper
 */
object VaultTableDefinitions {
    
    private const val TAG = "VaultTables"
    
    // 资源表
    const val TABLE_RESOURCES = "VaultResources"
    const val COL_UID = "uid"
    const val COL_ORIGINAL_NAME = "originalName"  // 逻辑文件名（不含扩展名）
    const val COL_EXTENSION = "extension"          // 文件扩展名（如 .jpg）
    const val COL_FILE_PATH = "filePath"           // 物理文件路径（uid.tb）
    const val COL_FILE_TYPE = "fileType"
    const val COL_ADDED_TIME = "addedTime"
    const val COL_FILE_SIZE = "fileSize"
    const val COL_THUMBNAIL_PATH = "thumbnailPath"
    const val COL_DURATION = "duration"
    
    // 分类表
    const val TABLE_CATEGORIES = "VaultCategories"
    const val COL_CAT_UID = "uid"
    const val COL_CATEGORY_NAME = "name"
    const val COL_SORT_ORDER = "sortOrder"
    const val COL_IS_SYSTEM = "isSystem"
    const val COL_CREATED_TIME = "createdTime"
    
    // 关联表
    const val TABLE_MAP = "VaultResourceCategoryMap"
    const val COL_RESOURCE_UID = "resourceUid"
    const val COL_MAP_CATEGORY_UID = "categoryUid"
    
    /**
     * 创建 Vault 相关表
     * @param db SQLiteDatabase 实例
     */
    fun createTables(db: SQLiteDatabase) {
        Log.d(TAG, "📦 创建 Vault 数据表...")
        
        // 创建资源表（支持逻辑文件名分离）
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_RESOURCES (
                $COL_UID TEXT PRIMARY KEY,
                $COL_ORIGINAL_NAME TEXT NOT NULL,
                $COL_EXTENSION TEXT NOT NULL,
                $COL_FILE_PATH TEXT NOT NULL,
                $COL_FILE_TYPE TEXT NOT NULL,
                $COL_ADDED_TIME INTEGER NOT NULL,
                $COL_FILE_SIZE INTEGER DEFAULT 0,
                $COL_THUMBNAIL_PATH TEXT,
                $COL_DURATION INTEGER
            )
        """)
        Log.d(TAG, "✅ 创建表: $TABLE_RESOURCES")
        
        // 创建分类表
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIES (
                $COL_CAT_UID TEXT PRIMARY KEY,
                $COL_CATEGORY_NAME TEXT NOT NULL,
                $COL_SORT_ORDER INTEGER DEFAULT 0,
                $COL_IS_SYSTEM INTEGER DEFAULT 0,
                $COL_CREATED_TIME INTEGER DEFAULT 0
            )
        """)
        Log.d(TAG, "✅ 创建表: $TABLE_CATEGORIES")
        
        // 创建关联表
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_MAP (
                $COL_RESOURCE_UID TEXT NOT NULL,
                $COL_MAP_CATEGORY_UID TEXT NOT NULL,
                PRIMARY KEY ($COL_RESOURCE_UID, $COL_MAP_CATEGORY_UID),
                FOREIGN KEY ($COL_RESOURCE_UID) REFERENCES $TABLE_RESOURCES($COL_UID) ON DELETE CASCADE,
                FOREIGN KEY ($COL_MAP_CATEGORY_UID) REFERENCES $TABLE_CATEGORIES($COL_CAT_UID) ON DELETE CASCADE
            )
        """)
        Log.d(TAG, "✅ 创建表: $TABLE_MAP")
        
        // 创建索引
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_map_resource ON $TABLE_MAP($COL_RESOURCE_UID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_map_category ON $TABLE_MAP($COL_MAP_CATEGORY_UID)")
        Log.d(TAG, "✅ 创建索引完成")
        
        Log.d(TAG, "✅ Vault 数据表创建完成")
    }
}
