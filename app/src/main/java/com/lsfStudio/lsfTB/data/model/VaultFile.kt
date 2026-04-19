package com.lsfStudio.lsfTB.data.model

/**
 * 私密保险箱文件数据类
 */
data class VaultFile(
    val id: Long = 0,
    val originalName: String,      // 原始文件名（例如：photo.jpg）
    val encryptedName: String,     // 加密后文件名（例如：photo.jpg.zip）
    val filePath: String,          // 文件在私有目录的路径
    val fileType: FileType,        // 文件类型（图片/视频）
    val tags: List<String>,        // 标签列表
    val categories: List<String> = emptyList(),  // 分类列表（自定义分类名称）
    val addedTime: Long = System.currentTimeMillis()  // 添加时间
)

/**
 * 文件类型枚举
 */
enum class FileType {
    IMAGE,   // 图片
    VIDEO    // 视频
}

/**
 * 分类筛选类型
 */
enum class FilterType {
    ALL,      // 全部
    IMAGES,   // 仅图片
    VIDEOS    // 仅视频
}
