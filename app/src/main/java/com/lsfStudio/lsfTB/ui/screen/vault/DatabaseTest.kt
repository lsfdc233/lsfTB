package com.lsfStudio.lsfTB.ui.screen.vault

import android.content.Context
import android.util.Log

/**
 * 数据库测试工具
 * 用于验证SQLite数据库是否正常工作
 */
object DatabaseTest {
    
    private const val TAG = "DBTest"
    
    /**
     * 执行数据库测试
     */
    fun runTest(context: Context) {
        Log.d(TAG, "🧪 开始数据库测试...")
        
        try {
            val middleware = VaultDatabaseMiddleware(context)
            
            // 测试1: 创建分类
            Log.d(TAG, "\n--- 测试1: 创建分类 ---")
            val cat1Uid = middleware.createCategory("测试分类1", 0, false)
            val cat2Uid = middleware.createCategory("测试分类2", 1, false)
            val systemCatUid = middleware.createCategory("系统分类", 0, true)
            
            // 测试2: 添加资源
            Log.d(TAG, "\n--- 测试2: 添加资源 ---")
            val pic1Uid = middleware.addResource(
                originalName = "test_photo",
                extension = ".jpg",
                filePath = "/storage/test/picture${System.currentTimeMillis()}.tb",
                fileType = "IMAGE",
                fileSize = 1024000
            )
            
            val vid1Uid = middleware.addResource(
                originalName = "test_video",
                extension = ".mp4",
                filePath = "/storage/test/video${System.currentTimeMillis()}.tb",
                fileType = "VIDEO",
                fileSize = 10240000,
                duration = 60000
            )
            
            // 测试3: 添加关联
            Log.d(TAG, "\n--- 测试3: 添加关联 ---")
            middleware.addResourceToCategory(pic1Uid, cat1Uid)
            middleware.addResourceToCategory(pic1Uid, cat2Uid)
            middleware.addResourceToCategory(vid1Uid, cat1Uid)
            
            // 测试4: 查询数据
            Log.d(TAG, "\n--- 测试4: 查询所有分类 ---")
            val categories = middleware.getAllCategories()
            Log.d(TAG, "分类数量: ${categories.size}")
            
            Log.d(TAG, "\n--- 测试5: 查询所有资源 ---")
            val resources = middleware.getAllResources()
            Log.d(TAG, "资源数量: ${resources.size}")
            
            Log.d(TAG, "\n--- 测试6: 查询分类下的资源 ---")
            val cat1Resources = middleware.getResourceUidsByCategory(cat1Uid)
            Log.d(TAG, "分类1下的资源UIDs: $cat1Resources")
            
            Log.d(TAG, "\n--- 测试7: 查询资源所属分类 ---")
            val pic1Categories = middleware.getCategoryUidsByResource(pic1Uid)
            Log.d(TAG, "图片1所属的分类UIDs: $pic1Categories")
            
            // 测试8: 重命名
            Log.d(TAG, "\n--- 测试8: 重命名测试 ---")
            middleware.renameResource(pic1Uid, "renamed_photo")  // 只改逻辑名，不改扩展名
            middleware.renameCategory(cat1Uid, "重命名后的分类1")
            
            Log.d(TAG, "\n✅ 所有测试通过！")
            Log.d(TAG, "📁 数据库文件位置: /data/data/com.lsfStudio.lsfTB/databases/lsfTB.db")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 测试失败: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    /**
     * 清空测试数据
     */
    fun clearTestData(context: Context) {
        Log.d(TAG, "🗑️ 清空测试数据...")
        try {
            val dataBase = com.lsfStudio.lsfTB.ui.util.DataBase(context)
            val db = dataBase.writableDatabase
            
            db.execSQL("DELETE FROM VaultResourceCategoryMap")
            db.execSQL("DELETE FROM VaultResources")
            db.execSQL("DELETE FROM VaultCategories")
            
            db.close()
            Log.d(TAG, "✅ 测试数据已清空")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清空失败: ${e.message}", e)
        }
    }
}
