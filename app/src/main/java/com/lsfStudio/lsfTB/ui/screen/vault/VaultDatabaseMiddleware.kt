package com.lsfStudio.lsfTB.ui.screen.vault

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.lsfStudio.lsfTB.ui.util.DataBase

/**
 * Vault数据库中间件
 * 
 * 职责：
 * 1. 启动时检查 Vault 需要的表是否存在，若不存在则命令主模块创建
 * 2. 封装所有 Vault 业务逻辑
 * 3. 通过 DataBase 主模块操作数据库
 * 4. UI层只与此中间件交互
 */
class VaultDatabaseMiddleware(context: Context) {
    
    companion object {
        private const val TAG = "VaultMiddleware"
    }
    
    // 持有 DataBase 实例，通过它操作数据库
    private val dataBase = DataBase(context)
    
    // UID计数器（内存中，重启后重置）
    private var pictureCounter = 0
    private var videoCounter = 0
    private var categoryCounter = 0
    
    /**
     * 初始化：检查并创建 Vault 需要的表
     * 在应用启动时调用
     */
    fun initialize() {
        Log.d(TAG, "🔍 检查 Vault 数据表...")
        
        // 检查 Vault 需要的表是否存在
        val resourcesExists = dataBase.tableExists(VaultTableDefinitions.TABLE_RESOURCES)
        val categoriesExists = dataBase.tableExists(VaultTableDefinitions.TABLE_CATEGORIES)
        val mapExists = dataBase.tableExists(VaultTableDefinitions.TABLE_MAP)
        
        if (!resourcesExists || !categoriesExists || !mapExists) {
            Log.d(TAG, "⚠️ Vault 表不存在，开始创建...")
            // 命令主模块创建表
            dataBase.executeSQL("""
                CREATE TABLE IF NOT EXISTS ${VaultTableDefinitions.TABLE_RESOURCES} (
                    ${VaultTableDefinitions.COL_UID} TEXT PRIMARY KEY,
                    ${VaultTableDefinitions.COL_ORIGINAL_NAME} TEXT NOT NULL,
                    ${VaultTableDefinitions.COL_EXTENSION} TEXT NOT NULL,
                    ${VaultTableDefinitions.COL_FILE_PATH} TEXT NOT NULL,
                    ${VaultTableDefinitions.COL_FILE_TYPE} TEXT NOT NULL,
                    ${VaultTableDefinitions.COL_ADDED_TIME} INTEGER NOT NULL,
                    ${VaultTableDefinitions.COL_FILE_SIZE} INTEGER DEFAULT 0,
                    ${VaultTableDefinitions.COL_THUMBNAIL_PATH} TEXT,
                    ${VaultTableDefinitions.COL_DURATION} INTEGER
                )
            """)
            
            dataBase.executeSQL("""
                CREATE TABLE IF NOT EXISTS ${VaultTableDefinitions.TABLE_CATEGORIES} (
                    ${VaultTableDefinitions.COL_CAT_UID} TEXT PRIMARY KEY,
                    ${VaultTableDefinitions.COL_CATEGORY_NAME} TEXT NOT NULL,
                    ${VaultTableDefinitions.COL_SORT_ORDER} INTEGER DEFAULT 0,
                    ${VaultTableDefinitions.COL_IS_SYSTEM} INTEGER DEFAULT 0,
                    ${VaultTableDefinitions.COL_CREATED_TIME} INTEGER DEFAULT 0
                )
            """)
            
            dataBase.executeSQL("""
                CREATE TABLE IF NOT EXISTS ${VaultTableDefinitions.TABLE_MAP} (
                    ${VaultTableDefinitions.COL_RESOURCE_UID} TEXT NOT NULL,
                    ${VaultTableDefinitions.COL_MAP_CATEGORY_UID} TEXT NOT NULL,
                    PRIMARY KEY (${VaultTableDefinitions.COL_RESOURCE_UID}, ${VaultTableDefinitions.COL_MAP_CATEGORY_UID}),
                    FOREIGN KEY (${VaultTableDefinitions.COL_RESOURCE_UID}) REFERENCES ${VaultTableDefinitions.TABLE_RESOURCES}(${VaultTableDefinitions.COL_UID}) ON DELETE CASCADE,
                    FOREIGN KEY (${VaultTableDefinitions.COL_MAP_CATEGORY_UID}) REFERENCES ${VaultTableDefinitions.TABLE_CATEGORIES}(${VaultTableDefinitions.COL_CAT_UID}) ON DELETE CASCADE
                )
            """)
            
            dataBase.executeSQL("CREATE INDEX IF NOT EXISTS idx_map_resource ON ${VaultTableDefinitions.TABLE_MAP}(${VaultTableDefinitions.COL_RESOURCE_UID})")
            dataBase.executeSQL("CREATE INDEX IF NOT EXISTS idx_map_category ON ${VaultTableDefinitions.TABLE_MAP}(${VaultTableDefinitions.COL_MAP_CATEGORY_UID})")
            
            Log.d(TAG, "✅ Vault 数据表创建完成")
        } else {
            Log.d(TAG, "✅ Vault 数据表已存在")
        }
    }
    
    /**
     * 生成图片UID
     * 格式：picture{timestamp}{counter}
     */
    fun generatePictureUid(): String {
        pictureCounter++
        return "picture${System.currentTimeMillis()}${pictureCounter}"
    }
    
    /**
     * 生成视频UID
     * 格式：video{timestamp}{counter}
     */
    fun generateVideoUid(): String {
        videoCounter++
        return "video${System.currentTimeMillis()}${videoCounter}"
    }
    
    /**
     * 生成分类UID
     * 格式：Category{timestamp}{counter}
     */
    fun generateCategoryUid(): String {
        categoryCounter++
        return "Category${System.currentTimeMillis()}${categoryCounter}"
    }
    
    // ==================== 资源操作 ====================
    
    /**
     * 添加资源
     * @return 资源UID
     */
    fun addResource(
        originalName: String,  // 逻辑文件名（不含扩展名）
        extension: String,     // 文件扩展名（如 .jpg）
        filePath: String,      // 物理文件路径（uid.tb）
        fileType: String,      // "IMAGE" 或 "VIDEO"
        addedTime: Long = System.currentTimeMillis(),
        fileSize: Long = 0,
        thumbnailPath: String? = null,
        duration: Long? = null
    ): String {
        // 根据文件类型生成UID
        val uid = if (fileType == "IMAGE") {
            generatePictureUid()
        } else {
            generateVideoUid()
        }
        
        val db = dataBase.writableDatabase
        val values = ContentValues().apply {
            put(VaultTableDefinitions.COL_UID, uid)
            put(VaultTableDefinitions.COL_ORIGINAL_NAME, originalName)
            put(VaultTableDefinitions.COL_EXTENSION, extension)
            put(VaultTableDefinitions.COL_FILE_PATH, filePath)
            put(VaultTableDefinitions.COL_FILE_TYPE, fileType)
            put(VaultTableDefinitions.COL_ADDED_TIME, addedTime)
            put(VaultTableDefinitions.COL_FILE_SIZE, fileSize)
            put(VaultTableDefinitions.COL_THUMBNAIL_PATH, thumbnailPath)
            put(VaultTableDefinitions.COL_DURATION, duration)
        }
        db.insertWithOnConflict(VaultTableDefinitions.TABLE_RESOURCES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        
        Log.d(TAG, "✅ 添加资源: uid=$uid, originalName=$originalName$extension, path=$filePath")
        return uid
    }
    
    /**
     * 删除资源
     */
    fun deleteResource(uid: String) {
        val db = dataBase.writableDatabase
        // 先删除关联
        db.delete(VaultTableDefinitions.TABLE_MAP, "${VaultTableDefinitions.COL_RESOURCE_UID} = ?", arrayOf(uid))
        // 再删除资源
        db.delete(VaultTableDefinitions.TABLE_RESOURCES, "${VaultTableDefinitions.COL_UID} = ?", arrayOf(uid))
    }
    
    /**
     * 重命名资源（只修改逻辑文件名，不碰物理文件）
     */
    fun renameResource(uid: String, newOriginalName: String) {
        val db = dataBase.writableDatabase
        val values = ContentValues().apply {
            put(VaultTableDefinitions.COL_ORIGINAL_NAME, newOriginalName)
        }
        db.update(VaultTableDefinitions.TABLE_RESOURCES, values, "${VaultTableDefinitions.COL_UID} = ?", arrayOf(uid))
        Log.d(TAG, "✅ 重命名资源: uid=$uid, newOriginalName=$newOriginalName")
    }
    
    /**
     * 获取所有资源
     */
    fun getAllResources(): List<Map<String, Any?>> {
        val db = dataBase.readableDatabase
        val cursor = db.query(
            VaultTableDefinitions.TABLE_RESOURCES, 
            null, null, null, null, null, 
            "${VaultTableDefinitions.COL_ADDED_TIME} DESC"
        )
        val results = mutableListOf<Map<String, Any?>>()
        while (cursor.moveToNext()) {
            results.add(mapOf(
                "uid" to cursor.getString(cursor.getColumnIndexOrThrow(VaultTableDefinitions.COL_UID)),
                "originalName" to cursor.getString(cursor.getColumnIndexOrThrow(VaultTableDefinitions.COL_ORIGINAL_NAME)),
                "extension" to cursor.getString(cursor.getColumnIndexOrThrow(VaultTableDefinitions.COL_EXTENSION)),
                "filePath" to cursor.getString(cursor.getColumnIndexOrThrow(VaultTableDefinitions.COL_FILE_PATH)),
                "fileType" to cursor.getString(cursor.getColumnIndexOrThrow(VaultTableDefinitions.COL_FILE_TYPE)),
                "addedTime" to cursor.getLong(cursor.getColumnIndexOrThrow(VaultTableDefinitions.COL_ADDED_TIME)),
                "fileSize" to cursor.getLong(cursor.getColumnIndexOrThrow(VaultTableDefinitions.COL_FILE_SIZE)),
                "thumbnailPath" to cursor.getString(cursor.getColumnIndexOrThrow(VaultTableDefinitions.COL_THUMBNAIL_PATH)),
                "duration" to if (cursor.isNull(cursor.getColumnIndexOrThrow(VaultTableDefinitions.COL_DURATION))) null else cursor.getLong(cursor.getColumnIndexOrThrow(VaultTableDefinitions.COL_DURATION))
            ))
        }
        cursor.close()
        return results
    }
    
    /**
     * 根据文件路径获取资源UID
     * @return 资源UID，如果不存在返回null
     */
    fun getResourceByPath(filePath: String): String? {
        val resources = getAllResources()
        return resources.find { it["filePath"] == filePath }?.get("uid") as? String
    }
    
    // ==================== 分类操作 ====================
    
    /**
     * 创建分类
     * @return 分类UID
     */
    fun createCategory(name: String, sortOrder: Int = 0, isSystem: Boolean = false): String {
        val uid = generateCategoryUid()
        val db = dataBase.writableDatabase
        val values = ContentValues().apply {
            put(VaultTableDefinitions.COL_CAT_UID, uid)
            put(VaultTableDefinitions.COL_CATEGORY_NAME, name)
            put(VaultTableDefinitions.COL_SORT_ORDER, sortOrder)
            put(VaultTableDefinitions.COL_IS_SYSTEM, if (isSystem) 1 else 0)
            put(VaultTableDefinitions.COL_CREATED_TIME, System.currentTimeMillis())
        }
        db.insertWithOnConflict(VaultTableDefinitions.TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        Log.d(TAG, "✅ 创建分类: uid=$uid, name=$name, isSystem=$isSystem")
        return uid
    }
    
    /**
     * 删除分类
     */
    fun deleteCategory(uid: String) {
        val db = dataBase.writableDatabase
        // 先删除关联
        db.delete(VaultTableDefinitions.TABLE_MAP, "${VaultTableDefinitions.COL_MAP_CATEGORY_UID} = ?", arrayOf(uid))
        // 再删除分类
        db.delete(VaultTableDefinitions.TABLE_CATEGORIES, "${VaultTableDefinitions.COL_CAT_UID} = ?", arrayOf(uid))
    }
    
    /**
     * 更新分类排序
     */
    fun updateCategorySortOrder(uid: String, sortOrder: Int) {
        val db = dataBase.writableDatabase
        val values = ContentValues().apply {
            put(VaultTableDefinitions.COL_SORT_ORDER, sortOrder)
        }
        db.update(VaultTableDefinitions.TABLE_CATEGORIES, values, "${VaultTableDefinitions.COL_CAT_UID} = ?", arrayOf(uid))
    }
    
    /**
     * 重命名分类
     */
    fun renameCategory(uid: String, newName: String) {
        val db = dataBase.writableDatabase
        val values = ContentValues().apply {
            put(VaultTableDefinitions.COL_CATEGORY_NAME, newName)
        }
        db.update(VaultTableDefinitions.TABLE_CATEGORIES, values, "${VaultTableDefinitions.COL_CAT_UID} = ?", arrayOf(uid))
    }
    
    /**
     * 获取所有分类
     */
    fun getAllCategories(): List<Map<String, Any?>> {
        val db = dataBase.readableDatabase
        val cursor = db.query(
            VaultTableDefinitions.TABLE_CATEGORIES, 
            null, null, null, null, null, 
            "${VaultTableDefinitions.COL_SORT_ORDER} ASC, ${VaultTableDefinitions.COL_CREATED_TIME} ASC"
        )
        val results = mutableListOf<Map<String, Any?>>()
        while (cursor.moveToNext()) {
            results.add(mapOf(
                "uid" to cursor.getString(cursor.getColumnIndexOrThrow(VaultTableDefinitions.COL_CAT_UID)),
                "name" to cursor.getString(cursor.getColumnIndexOrThrow(VaultTableDefinitions.COL_CATEGORY_NAME)),
                "sortOrder" to cursor.getInt(cursor.getColumnIndexOrThrow(VaultTableDefinitions.COL_SORT_ORDER)),
                "isSystem" to (cursor.getInt(cursor.getColumnIndexOrThrow(VaultTableDefinitions.COL_IS_SYSTEM)) == 1),
                "createdTime" to cursor.getLong(cursor.getColumnIndexOrThrow(VaultTableDefinitions.COL_CREATED_TIME))
            ))
        }
        cursor.close()
        Log.d(TAG, "📋 获取分类列表: ${results.size} 个分类")
        results.forEach { cat ->
            Log.d(TAG, "   - ${cat["name"]} (uid: ${cat["uid"]}, isSystem: ${cat["isSystem"]})")
        }
        return results
    }
    
    // ==================== 关联操作 ====================
    
    /**
     * 添加资源到分类
     */
    fun addResourceToCategory(resourceUid: String, categoryUid: String) {
        val db = dataBase.writableDatabase
        val values = ContentValues().apply {
            put(VaultTableDefinitions.COL_RESOURCE_UID, resourceUid)
            put(VaultTableDefinitions.COL_MAP_CATEGORY_UID, categoryUid)
        }
        db.insertWithOnConflict(VaultTableDefinitions.TABLE_MAP, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }
    
    /**
     * 批量添加资源到分类
     */
    fun addResourcesToCategory(resourceUids: List<String>, categoryUid: String) {
        resourceUids.forEach { resourceUid ->
            addResourceToCategory(resourceUid, categoryUid)
        }
    }
    
    /**
     * 从分类中移除资源
     */
    fun removeResourceFromCategory(resourceUid: String, categoryUid: String) {
        val db = dataBase.writableDatabase
        db.delete(
            VaultTableDefinitions.TABLE_MAP, 
            "${VaultTableDefinitions.COL_RESOURCE_UID} = ? AND ${VaultTableDefinitions.COL_MAP_CATEGORY_UID} = ?", 
            arrayOf(resourceUid, categoryUid)
        )
    }
    
    /**
     * 获取某个分类下的所有资源UID
     */
    fun getResourceUidsByCategory(categoryUid: String): List<String> {
        val db = dataBase.readableDatabase
        val cursor = db.query(
            VaultTableDefinitions.TABLE_MAP, 
            arrayOf(VaultTableDefinitions.COL_RESOURCE_UID), 
            "${VaultTableDefinitions.COL_MAP_CATEGORY_UID} = ?", 
            arrayOf(categoryUid), 
            null, null, null
        )
        val results = mutableListOf<String>()
        while (cursor.moveToNext()) {
            results.add(cursor.getString(cursor.getColumnIndexOrThrow(VaultTableDefinitions.COL_RESOURCE_UID)))
        }
        cursor.close()
        return results
    }
    
    /**
     * 获取某个资源所属的所有分类UID
     */
    fun getCategoryUidsByResource(resourceUid: String): List<String> {
        val db = dataBase.readableDatabase
        val cursor = db.query(
            VaultTableDefinitions.TABLE_MAP, 
            arrayOf(VaultTableDefinitions.COL_MAP_CATEGORY_UID), 
            "${VaultTableDefinitions.COL_RESOURCE_UID} = ?", 
            arrayOf(resourceUid), 
            null, null, null
        )
        val results = mutableListOf<String>()
        while (cursor.moveToNext()) {
            results.add(cursor.getString(cursor.getColumnIndexOrThrow(VaultTableDefinitions.COL_MAP_CATEGORY_UID)))
        }
        cursor.close()
        return results
    }
    
    /**
     * 获取某个资源所属的所有分类名称
     */
    fun getCategoryNamesByResource(resourceUid: String): List<String> {
        val categoryUids = getCategoryUidsByResource(resourceUid)
        val allCategories = getAllCategories()
        return allCategories
            .filter { it["uid"] in categoryUids }
            .map { it["name"] as String }
    }
}
