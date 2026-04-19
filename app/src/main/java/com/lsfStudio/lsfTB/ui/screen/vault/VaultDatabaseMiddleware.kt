package com.lsfStudio.lsfTB.ui.screen.vault

import android.content.Context
import android.util.Log

/**
 * Vault数据库中间件
 * 封装所有Vault业务逻辑，UI层只与此中间件交互
 */
class VaultDatabaseMiddleware(context: Context) {
    
    companion object {
        private const val TAG = "VaultDB"
    }
    
    private val dbHelper = VaultDatabaseHelper(context)
    
    // UID计数器（内存中，重启后重置）
    private var pictureCounter = 0
    private var videoCounter = 0
    private var categoryCounter = 0
    
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
        
        dbHelper.insertResource(uid, originalName, extension, filePath, fileType, addedTime, fileSize, thumbnailPath, duration)
        Log.d(TAG, "✅ 添加资源: uid=$uid, originalName=$originalName$extension, path=$filePath")
        return uid
    }
    
    /**
     * 删除资源
     */
    fun deleteResource(uid: String) {
        dbHelper.deleteResource(uid)
    }
    
    /**
     * 重命名资源（只修改逻辑文件名，不碰物理文件）
     */
    fun renameResource(uid: String, newOriginalName: String) {
        dbHelper.updateResourceOriginalName(uid, newOriginalName)
        Log.d(TAG, "✅ 重命名资源: uid=$uid, newOriginalName=$newOriginalName")
    }
    
    /**
     * 获取所有资源
     */
    fun getAllResources(): List<Map<String, Any?>> {
        return dbHelper.getAllResources()
    }
    
    /**
     * 根据文件路径获取资源UID
     * @return 资源UID，如果不存在返回null
     */
    fun getResourceByPath(filePath: String): String? {
        val resources = dbHelper.getAllResources()
        return resources.find { it["filePath"] == filePath }?.get("uid") as? String
    }
    
    // ==================== 分类操作 ====================
    
    /**
     * 创建分类
     * @return 分类UID
     */
    fun createCategory(name: String, sortOrder: Int = 0, isSystem: Boolean = false): String {
        val uid = generateCategoryUid()
        dbHelper.insertCategory(uid, name, sortOrder, isSystem)
        Log.d(TAG, "✅ 创建分类: uid=$uid, name=$name, isSystem=$isSystem")
        return uid
    }
    
    /**
     * 删除分类
     */
    fun deleteCategory(uid: String) {
        dbHelper.deleteCategory(uid)
    }
    
    /**
     * 更新分类排序
     */
    fun updateCategorySortOrder(uid: String, sortOrder: Int) {
        dbHelper.updateCategorySortOrder(uid, sortOrder)
    }
    
    /**
     * 重命名分类
     */
    fun renameCategory(uid: String, newName: String) {
        dbHelper.updateCategoryName(uid, newName)
    }
    
    /**
     * 获取所有分类
     */
    fun getAllCategories(): List<Map<String, Any?>> {
        val categories = dbHelper.getAllCategories()
        Log.d(TAG, "📋 获取分类列表: ${categories.size} 个分类")
        categories.forEach { cat ->
            Log.d(TAG, "   - ${cat["name"]} (uid: ${cat["uid"]}, isSystem: ${cat["isSystem"]})")
        }
        return categories
    }
    
    // ==================== 关联操作 ====================
    
    /**
     * 添加资源到分类
     */
    fun addResourceToCategory(resourceUid: String, categoryUid: String) {
        dbHelper.addRelation(resourceUid, categoryUid)
    }
    
    /**
     * 批量添加资源到分类
     */
    fun addResourcesToCategory(resourceUids: List<String>, categoryUid: String) {
        resourceUids.forEach { resourceUid ->
            dbHelper.addRelation(resourceUid, categoryUid)
        }
    }
    
    /**
     * 从分类中移除资源
     */
    fun removeResourceFromCategory(resourceUid: String, categoryUid: String) {
        dbHelper.removeRelation(resourceUid, categoryUid)
    }
    
    /**
     * 获取某个分类下的所有资源UID
     */
    fun getResourceUidsByCategory(categoryUid: String): List<String> {
        return dbHelper.getResourceUidsByCategory(categoryUid)
    }
    
    /**
     * 获取某个资源所属的所有分类UID
     */
    fun getCategoryUidsByResource(resourceUid: String): List<String> {
        return dbHelper.getCategoryUidsByResource(resourceUid)
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
