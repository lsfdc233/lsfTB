package com.lsfStudio.lsfTB.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 分类管理仓库（数据层）
 * 负责分类数据的持久化和状态管理
 */
class CategoryRepository(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "vault_categories"
        private const val KEY_CUSTOM_CATEGORIES = "custom_categories"
    }
    
    // 自定义分类列表的状态流
    private val _customCategories = MutableStateFlow<List<String>>(loadCategories())
    val customCategories: StateFlow<List<String>> = _customCategories.asStateFlow()
    
    /**
     * 获取所有分类（系统分类 + 自定义分类）
     */
    fun getAllCategories(): List<String> {
        return listOf("全部", "照片", "视频") + _customCategories.value
    }
    
    /**
     * 更新自定义分类列表
     */
    fun updateCategories(categories: List<String>) {
        _customCategories.value = categories
        saveCategories(categories)
    }
    
    /**
     * 添加分类
     */
    fun addCategory(category: String) {
        if (category.isNotBlank() && !_customCategories.value.contains(category)) {
            val newList = _customCategories.value + category
            updateCategories(newList)
        }
    }
    
    /**
     * 删除分类
     */
    fun removeCategory(category: String) {
        val newList = _customCategories.value - category
        updateCategories(newList)
    }
    
    /**
     * 重排序分类
     */
    fun reorderCategories(newOrder: List<String>) {
        updateCategories(newOrder)
    }
    
    /**
     * 从SharedPreferences加载分类
     */
    private fun loadCategories(): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt("category_count", 0)
        val categories = mutableListOf<String>()
        for (i in 0 until count) {
            prefs.getString("category_$i", null)?.let {
                categories.add(it)
            }
        }
        return categories
    }
    
    /**
     * 保存分类到SharedPreferences
     */
    private fun saveCategories(categories: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        // 先获取旧的数量
        val oldCount = prefs.getInt("category_count", 0)
        
        // 清除所有旧数据
        editor.clear()
        
        // 保存新的分类数据
        editor.putInt("category_count", categories.size)
        categories.forEachIndexed { index, category ->
            editor.putString("category_$index", category)
        }
        
        editor.apply()
    }
}
