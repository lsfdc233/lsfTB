package com.lsfStudio.lsfTB.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lsfStudio.lsfTB.data.model.VaultFile
import com.lsfStudio.lsfTB.data.repository.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * 分类管理ViewModel（业务逻辑层）
 * 连接UI层和数据层，提供分类管理的业务逻辑
 */
class CategoryViewModel(context: Context) : ViewModel() {
    
    private val repository = CategoryRepository(context)
    
    // 暴露自定义分类列表给UI
    val customCategories: StateFlow<List<String>> = repository.customCategories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    /**
     * 获取所有分类（系统 + 自定义）
     */
    fun getAllCategories(): List<String> {
        return repository.getAllCategories()
    }
    
    /**
     * 更新分类列表（用于拖拽排序后保存）
     */
    fun updateCategories(categories: List<String>) {
        repository.updateCategories(categories)
    }
    
    /**
     * 添加新分类
     */
    fun addCategory(category: String) {
        repository.addCategory(category)
    }
    
    /**
     * 删除分类
     */
    fun removeCategory(category: String) {
        repository.removeCategory(category)
    }
    
    /**
     * 从文件中移除某个分类标签（不删除文件本身）
     */
    fun removeCategoryFromFiles(category: String, vaultFiles: List<VaultFile>): List<VaultFile> {
        // 先从仓库中删除分类
        removeCategory(category)
        
        // 再从所有文件中移除该分类标签
        return vaultFiles.map { file ->
            file.copy(categories = file.categories - category)
        }
    }
    
    /**
     * 根据分类筛选文件
     */
    fun filterFilesByCategory(category: String, vaultFiles: List<VaultFile>): List<VaultFile> {
        return when (category) {
            "all" -> vaultFiles
            "photos" -> vaultFiles.filter { it.fileType == com.lsfStudio.lsfTB.data.model.FileType.IMAGE }
            "videos" -> vaultFiles.filter { it.fileType == com.lsfStudio.lsfTB.data.model.FileType.VIDEO }
            else -> {
                // 自定义分类
                vaultFiles.filter { category in it.categories }
            }
        }
    }
}
