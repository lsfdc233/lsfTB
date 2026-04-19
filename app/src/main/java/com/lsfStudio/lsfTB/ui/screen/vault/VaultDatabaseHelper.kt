package com.lsfStudio.lsfTB.ui.screen.vault

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Vault数据库帮助类
 * 使用原生SQLite，不依赖Room
 */
class VaultDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    
    companion object {
        private const val DATABASE_NAME = "lsfTB.db"
        private const val DATABASE_VERSION = 2  // 升级到版本2，支持逻辑文件名分离
        
        // 资源表
        private const val TABLE_RESOURCES = "VaultResources"
        private const val COL_UID = "uid"
        private const val COL_ORIGINAL_NAME = "originalName"  // 逻辑文件名（不含扩展名）
        private const val COL_EXTENSION = "extension"          // 文件扩展名（如 .jpg）
        private const val COL_FILE_PATH = "filePath"           // 物理文件路径（uid.tb）
        private const val COL_FILE_TYPE = "fileType"
        private const val COL_ADDED_TIME = "addedTime"
        private const val COL_FILE_SIZE = "fileSize"
        private const val COL_THUMBNAIL_PATH = "thumbnailPath"
        private const val COL_DURATION = "duration"
        
        // 分类表
        private const val TABLE_CATEGORIES = "VaultCategories"
        private const val COL_CAT_UID = "uid"
        private const val COL_CATEGORY_NAME = "name"
        private const val COL_SORT_ORDER = "sortOrder"
        private const val COL_IS_SYSTEM = "isSystem"
        private const val COL_CREATED_TIME = "createdTime"
        
        // 关联表
        private const val TABLE_MAP = "VaultResourceCategoryMap"
        private const val COL_RESOURCE_UID = "resourceUid"
        private const val COL_MAP_CATEGORY_UID = "categoryUid"
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        // 创建资源表（支持逻辑文件名分离）
        db.execSQL("""
            CREATE TABLE $TABLE_RESOURCES (
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
        
        // 创建分类表
        db.execSQL("""
            CREATE TABLE $TABLE_CATEGORIES (
                $COL_CAT_UID TEXT PRIMARY KEY,
                $COL_CATEGORY_NAME TEXT NOT NULL,
                $COL_SORT_ORDER INTEGER DEFAULT 0,
                $COL_IS_SYSTEM INTEGER DEFAULT 0,
                $COL_CREATED_TIME INTEGER DEFAULT 0
            )
        """)
        
        // 创建关联表
        db.execSQL("""
            CREATE TABLE $TABLE_MAP (
                $COL_RESOURCE_UID TEXT NOT NULL,
                $COL_MAP_CATEGORY_UID TEXT NOT NULL,
                PRIMARY KEY ($COL_RESOURCE_UID, $COL_MAP_CATEGORY_UID),
                FOREIGN KEY ($COL_RESOURCE_UID) REFERENCES $TABLE_RESOURCES($COL_UID) ON DELETE CASCADE,
                FOREIGN KEY ($COL_MAP_CATEGORY_UID) REFERENCES $TABLE_CATEGORIES($COL_CAT_UID) ON DELETE CASCADE
            )
        """)
        
        // 创建索引
        db.execSQL("CREATE INDEX idx_map_resource ON $TABLE_MAP($COL_RESOURCE_UID)")
        db.execSQL("CREATE INDEX idx_map_category ON $TABLE_MAP($COL_MAP_CATEGORY_UID)")
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // 从版本1升级到版本2：重构资源表
            android.util.Log.d("VaultDB", "🔄 升级数据库从版本 $oldVersion 到 $newVersion")
            
            // 1. 创建新表结构
            db.execSQL("""
                CREATE TABLE ${TABLE_RESOURCES}_new (
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
            
            // 2. 迁移数据（将fileName拆分为originalName和extension）
            db.execSQL("""
                INSERT INTO ${TABLE_RESOURCES}_new (
                    $COL_UID, $COL_ORIGINAL_NAME, $COL_EXTENSION, $COL_FILE_PATH,
                    $COL_FILE_TYPE, $COL_ADDED_TIME, $COL_FILE_SIZE, $COL_THUMBNAIL_PATH, $COL_DURATION
                )
                SELECT 
                    uid,
                    CASE 
                        WHEN instr(fileName, '.') > 0 THEN substr(fileName, 1, instr(fileName, '.') - 1)
                        ELSE fileName
                    END,
                    CASE 
                        WHEN instr(fileName, '.') > 0 THEN substr(fileName, instr(fileName, '.'))
                        ELSE ''
                    END,
                    filePath,
                    fileType,
                    addedTime,
                    fileSize,
                    thumbnailPath,
                    duration
                FROM $TABLE_RESOURCES
            """)
            
            // 3. 删除旧表
            db.execSQL("DROP TABLE IF EXISTS $TABLE_RESOURCES")
            
            // 4. 重命名新表
            db.execSQL("ALTER TABLE ${TABLE_RESOURCES}_new RENAME TO $TABLE_RESOURCES")
            
            android.util.Log.d("VaultDB", "✅ 数据库升级完成")
        }
    }
    
    // ==================== 资源操作 ====================
    
    fun insertResource(uid: String, originalName: String, extension: String, filePath: String, fileType: String, 
                      addedTime: Long, fileSize: Long = 0, thumbnailPath: String? = null, duration: Long? = null) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_UID, uid)
            put(COL_ORIGINAL_NAME, originalName)
            put(COL_EXTENSION, extension)
            put(COL_FILE_PATH, filePath)
            put(COL_FILE_TYPE, fileType)
            put(COL_ADDED_TIME, addedTime)
            put(COL_FILE_SIZE, fileSize)
            put(COL_THUMBNAIL_PATH, thumbnailPath)
            put(COL_DURATION, duration)
        }
        db.insertWithOnConflict(TABLE_RESOURCES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    
    fun deleteResource(uid: String) {
        val db = writableDatabase
        // 先删除关联
        db.delete(TABLE_MAP, "$COL_RESOURCE_UID = ?", arrayOf(uid))
        // 再删除资源
        db.delete(TABLE_RESOURCES, "$COL_UID = ?", arrayOf(uid))
    }
    
    fun updateResourceOriginalName(uid: String, newOriginalName: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ORIGINAL_NAME, newOriginalName)
        }
        db.update(TABLE_RESOURCES, values, "$COL_UID = ?", arrayOf(uid))
    }
    
    fun getAllResources(): List<Map<String, Any?>> {
        val db = readableDatabase
        val cursor = db.query(TABLE_RESOURCES, null, null, null, null, null, "$COL_ADDED_TIME DESC")
        val results = mutableListOf<Map<String, Any?>>()
        while (cursor.moveToNext()) {
            results.add(mapOf(
                "uid" to cursor.getString(cursor.getColumnIndexOrThrow(COL_UID)),
                "originalName" to cursor.getString(cursor.getColumnIndexOrThrow(COL_ORIGINAL_NAME)),
                "extension" to cursor.getString(cursor.getColumnIndexOrThrow(COL_EXTENSION)),
                "filePath" to cursor.getString(cursor.getColumnIndexOrThrow(COL_FILE_PATH)),
                "fileType" to cursor.getString(cursor.getColumnIndexOrThrow(COL_FILE_TYPE)),
                "addedTime" to cursor.getLong(cursor.getColumnIndexOrThrow(COL_ADDED_TIME)),
                "fileSize" to cursor.getLong(cursor.getColumnIndexOrThrow(COL_FILE_SIZE)),
                "thumbnailPath" to cursor.getString(cursor.getColumnIndexOrThrow(COL_THUMBNAIL_PATH)),
                "duration" to if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_DURATION))) null else cursor.getLong(cursor.getColumnIndexOrThrow(COL_DURATION))
            ))
        }
        cursor.close()
        return results
    }
    
    // ==================== 分类操作 ====================
    
    fun insertCategory(uid: String, name: String, sortOrder: Int = 0, isSystem: Boolean = false) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CAT_UID, uid)
            put(COL_CATEGORY_NAME, name)
            put(COL_SORT_ORDER, sortOrder)
            put(COL_IS_SYSTEM, if (isSystem) 1 else 0)
            put(COL_CREATED_TIME, System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    
    fun deleteCategory(uid: String) {
        val db = writableDatabase
        // 先删除关联
        db.delete(TABLE_MAP, "$COL_MAP_CATEGORY_UID = ?", arrayOf(uid))
        // 再删除分类
        db.delete(TABLE_CATEGORIES, "$COL_CAT_UID = ?", arrayOf(uid))
    }
    
    fun updateCategorySortOrder(uid: String, sortOrder: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_SORT_ORDER, sortOrder)
        }
        db.update(TABLE_CATEGORIES, values, "$COL_CAT_UID = ?", arrayOf(uid))
    }
    
    fun updateCategoryName(uid: String, newName: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CATEGORY_NAME, newName)
        }
        db.update(TABLE_CATEGORIES, values, "$COL_CAT_UID = ?", arrayOf(uid))
    }
    
    fun getAllCategories(): List<Map<String, Any?>> {
        val db = readableDatabase
        val cursor = db.query(TABLE_CATEGORIES, null, null, null, null, null, "$COL_SORT_ORDER ASC, $COL_CREATED_TIME ASC")
        val results = mutableListOf<Map<String, Any?>>()
        while (cursor.moveToNext()) {
            results.add(mapOf(
                "uid" to cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_UID)),
                "name" to cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY_NAME)),
                "sortOrder" to cursor.getInt(cursor.getColumnIndexOrThrow(COL_SORT_ORDER)),
                "isSystem" to (cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_SYSTEM)) == 1),
                "createdTime" to cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_TIME))
            ))
        }
        cursor.close()
        return results
    }
    
    // ==================== 关联操作 ====================
    
    fun addRelation(resourceUid: String, categoryUid: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_RESOURCE_UID, resourceUid)
            put(COL_MAP_CATEGORY_UID, categoryUid)
        }
        db.insertWithOnConflict(TABLE_MAP, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }
    
    fun removeRelation(resourceUid: String, categoryUid: String) {
        val db = writableDatabase
        db.delete(TABLE_MAP, "$COL_RESOURCE_UID = ? AND $COL_MAP_CATEGORY_UID = ?", arrayOf(resourceUid, categoryUid))
    }
    
    fun getResourceUidsByCategory(categoryUid: String): List<String> {
        val db = readableDatabase
        val cursor = db.query(TABLE_MAP, arrayOf(COL_RESOURCE_UID), "$COL_MAP_CATEGORY_UID = ?", arrayOf(categoryUid), null, null, null)
        val results = mutableListOf<String>()
        while (cursor.moveToNext()) {
            results.add(cursor.getString(cursor.getColumnIndexOrThrow(COL_RESOURCE_UID)))
        }
        cursor.close()
        return results
    }
    
    fun getCategoryUidsByResource(resourceUid: String): List<String> {
        val db = readableDatabase
        val cursor = db.query(TABLE_MAP, arrayOf(COL_MAP_CATEGORY_UID), "$COL_RESOURCE_UID = ?", arrayOf(resourceUid), null, null, null)
        val results = mutableListOf<String>()
        while (cursor.moveToNext()) {
            results.add(cursor.getString(cursor.getColumnIndexOrThrow(COL_MAP_CATEGORY_UID)))
        }
        cursor.close()
        return results
    }
}
