package com.lsfStudio.lsfTB.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ShareUtil {
    
    fun shareFile(context: Context, filePath: String, mimeType: String, originalFileName: String) {
        try {
            val sourceFile = File(filePath)
            
            if (!sourceFile.exists()) {
                android.widget.Toast.makeText(context, "文件不存在", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            // 创建临时文件（去掉.zip后缀）
            val tempFileName = if (originalFileName.endsWith(".zip")) {
                originalFileName.removeSuffix(".zip")
            } else {
                originalFileName
            }
            
            val tempDir = File(context.cacheDir, "share_temp")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            
            val tempFile = File(tempDir, tempFileName)
            
            // 如果临时文件已存在且大小相同，直接复用
            if (!tempFile.exists() || tempFile.length() != sourceFile.length()) {
                // 复制文件到临时目录
                sourceFile.inputStream().use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            
            // 获取临时文件的URI
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, tempFileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = Intent.createChooser(shareIntent, "分享到").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(chooser)
            
            // 不再自动删除临时文件，让系统管理
            // 临时文件会在下次分享同文件时被覆盖，或用户清理缓存时删除
            
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "分享失败: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    fun shareMultipleFiles(context: Context, filePaths: List<String>, mimeType: String) {
        try {
            val uris = mutableListOf<Uri>()
            
            for (filePath in filePaths) {
                val file = File(filePath)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    uris.add(uri)
                }
            }
            
            if (uris.isEmpty()) {
                android.widget.Toast.makeText(context, "没有可分享的文件", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = Intent.createChooser(shareIntent, "分享到").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "分享失败: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
