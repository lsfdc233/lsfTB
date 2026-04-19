package com.lsfStudio.lsfTB.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.lsfStudio.lsfTB.ui.screen.vault.VaultDatabaseMiddleware
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 分类管理器（数据层 + 业务逻辑层）
 * 负责分类数据的持久化、状态管理和业务逻辑
 * 已迁移至SQLite数据库存储
 */
class CategoryManager(private val context: android.content.Context) : ViewModel() {
    
    // 使用VaultDatabaseMiddleware进行数据库操作
    private val dbMiddleware = VaultDatabaseMiddleware(context)
    
    // 自定义分类列表的状态流
    private val _customCategories = MutableStateFlow<List<String>>(loadCategoriesFromDB())
    val customCategories: StateFlow<List<String>> = _customCategories.asStateFlow()
    
    /**
     * 获取所有分类（系统分类 + 自定义分类）
     */
    fun getAllCategories(): List<String> {
        return listOf("全部", "照片", "视频") + _customCategories.value
    }
    
    /**
     * 添加新分类
     */
    fun addCategory(category: String) {
        if (category.isNotBlank() && !_customCategories.value.contains(category)) {
            // 使用数据库中间件创建分类
            dbMiddleware.createCategory(name = category, sortOrder = _customCategories.value.size)
            // 更新UI状态
            val newList = _customCategories.value + category
            _customCategories.value = newList
        }
    }
    
    /**
     * 删除分类
     */
    fun removeCategory(category: String) {
        // 从数据库中查找并删除
        val allCategories = dbMiddleware.getAllCategories()
        val categoryToDelete = allCategories.find { it["name"] == category && !(it["isSystem"] as Boolean) }
        
        if (categoryToDelete != null) {
            val uid = categoryToDelete["uid"] as String
            dbMiddleware.deleteCategory(uid)
        }
        
        // 更新UI状态
        val newList = _customCategories.value - category
        _customCategories.value = newList
    }
    
    /**
     * 重排序分类
     */
    fun reorderCategories(newOrder: List<String>) {
        // 更新数据库中每个分类的sortOrder
        newOrder.forEachIndexed { index, categoryName ->
            val allCategories = dbMiddleware.getAllCategories()
            val category = allCategories.find { it["name"] == categoryName && !(it["isSystem"] as Boolean) }
            if (category != null) {
                val uid = category["uid"] as String
                dbMiddleware.updateCategorySortOrder(uid, index)
            }
        }
        
        // 更新UI状态
        _customCategories.value = newOrder
    }
    
    /**
     * 从数据库加载分类
     */
    private fun loadCategoriesFromDB(): List<String> {
        android.util.Log.d("CategoryManager", "🔍 开始从数据库加载分类...")
        val allCategories = dbMiddleware.getAllCategories()
        android.util.Log.d("CategoryManager", "📊 数据库中共有 ${allCategories.size} 个分类")
        
        // 过滤出非系统分类，按sortOrder排序
        val customCategories = allCategories
            .filter { !(it["isSystem"] as Boolean) }
            .sortedBy { it["sortOrder"] as Int }
            .map { it["name"] as String }
        
        android.util.Log.d("CategoryManager", "✅ 加载到 ${customCategories.size} 个自定义分类: $customCategories")
        return customCategories
    }
}
