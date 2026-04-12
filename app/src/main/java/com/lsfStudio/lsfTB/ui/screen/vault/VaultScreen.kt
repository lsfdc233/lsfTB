package com.lsfStudio.lsfTB.ui.screen.vault

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.documentfile.provider.DocumentFile
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.lsfStudio.lsfTB.ui.navigation3.LocalNavigator
import com.lsfStudio.lsfTB.ui.navigation3.Route
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.lsfStudio.lsfTB.data.model.VaultFile
import com.lsfStudio.lsfTB.data.model.FileType
import com.lsfStudio.lsfTB.ui.util.HapticFeedbackUtil
import java.io.File

/**
 * 私密保险箱页面
 */
@Composable
fun VaultScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    val navigator = LocalNavigator.current
    
    // 文件列表状态
    var vaultFiles by remember { mutableStateOf<List<VaultFile>>(loadVaultFiles(context)) }
    
    // 多选模式状态
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedFiles by remember { mutableStateOf<Set<Long>>(emptySet()) }
    
    // 对话框状态
    var showTagDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var fileToTag by remember { mutableStateOf<VaultFile?>(null) }
    var filesToExport by remember { mutableStateOf<List<VaultFile>>(emptyList()) }
    var tagInput by remember { mutableStateOf("") }
    
    // 权限请求启动器
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 处理权限结果
        permissions.entries.forEach {
            // 可以显示权限授予状态
        }
    }
    
    // 请求权限函数
    fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        requestPermissionLauncher.launch(permissions)
    }
    
    // 页面加载时请求权限
    LaunchedEffect(Unit) {
        requestPermissions()
    }
    
    // 文件选择器启动器 - 使用相册选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                uris.forEach { uri ->
                    handleSelectedFile(context, uri) { newFile ->
                        vaultFiles = vaultFiles + newFile
                        saveVaultFiles(context, vaultFiles)
                    }
                }
            }
        }
    }
    
    // 导出文件选择器（选择目标目录）
    val exportDirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        treeUri?.let {
            scope.launch {
                exportSelectedFiles(context, filesToExport, it)
                // 删除已导出的文件
                vaultFiles = vaultFiles.filter { file ->
                    !filesToExport.any { exported -> exported.id == file.id }
                }
                saveVaultFiles(context, vaultFiles)
                showExportDialog = false
                isMultiSelectMode = false
                selectedFiles = emptySet()
            }
        }
    }
    
    // 处理侧滑返回：多选状态下退出选择
    BackHandler(enabled = isMultiSelectMode) {
        isMultiSelectMode = false
        selectedFiles = emptySet()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                color = colorScheme.surface,
                title = if (isMultiSelectMode) "已选择 ${selectedFiles.size} 项" else "私密保险箱",
                navigationIcon = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = {
                            isMultiSelectMode = false
                            selectedFiles = emptySet()
                        }) {
                            Text("取消", fontSize = 16.sp)
                        }
                    }
                },
                actions = {
                    if (isMultiSelectMode && selectedFiles.isNotEmpty()) {
                        // 批量打标签
                        IconButton(onClick = {
                            filesToExport = vaultFiles.filter { it.id in selectedFiles }
                            showTagDialog = true
                        }) {
                            Icon(Icons.Rounded.Label, "打标签", tint = colorScheme.onSurface)
                        }
                        // 批量导出
                        IconButton(onClick = {
                            filesToExport = vaultFiles.filter { it.id in selectedFiles }
                            showExportDialog = true
                        }) {
                            Icon(Icons.Rounded.Delete, "导出", tint = colorScheme.onSurface)
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            // 蓝色圆形加号按钮
            FloatingActionButton(
                modifier = Modifier
                    .padding(bottom = 80.dp, end = 20.dp)
                    .clip(CircleShape),
                shadowElevation = 0.dp,
                onClick = {
                    // 震动反馈
                    HapticFeedbackUtil.lightClick(context)
                    // 打开系统相册选择器
                    filePickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo))
                },
                content = {
                    Icon(
                        Icons.Rounded.Add,
                        "添加文件",
                        modifier = Modifier.size(40.dp),
                        tint = colorScheme.onPrimary
                    )
                }
            )
        }
    ) { innerPadding ->
        // 网格视图（类似 Windows 资源管理器）- 始终可滚动
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .overScrollVertical()
                .scrollEndHaptic()
                .padding(innerPadding),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (vaultFiles.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "点击下方 + 按钮添加私密文件",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(vaultFiles, key = { it.id }) { file ->
                        VaultFileGridItem(
                            file = file,
                            isSelected = file.id in selectedFiles,
                            isMultiSelectMode = isMultiSelectMode,
                            onClick = {
                                if (isMultiSelectMode) {
                                    // 多选模式：切换选择状态
                                    selectedFiles = if (file.id in selectedFiles) {
                                        selectedFiles - file.id
                                    } else {
                                        selectedFiles + file.id
                                    }
                                    // 震动反馈
                                    HapticFeedbackUtil.select(context)
                                } else {
                                    // 单击：跳转到图片查看器页面
                                    val currentIndex = vaultFiles.indexOfFirst { it.id == file.id }
                                    navigator.push(Route.ImageViewer(
                                        filePath = file.filePath,
                                        fileName = file.originalName,
                                        addedTime = file.addedTime,
                                        allFilePaths = vaultFiles.map { it.filePath },
                                        allFileNames = vaultFiles.map { it.originalName },
                                        allAddedTimes = vaultFiles.map { it.addedTime },
                                        currentIndex = if (currentIndex >= 0) currentIndex else 0
                                    ))
                                }
                            },
                            onLongClick = {
                                // 长按：进入多选模式
                                if (!isMultiSelectMode) {
                                    isMultiSelectMode = true
                                    selectedFiles = setOf(file.id)
                                    // 震动反馈
                                    HapticFeedbackUtil.longPress(context)
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    deleteFile(context, file)
                                    vaultFiles = vaultFiles.filter { it.id != file.id }
                                    saveVaultFiles(context, vaultFiles)
                                }
                            }
                        )
                    }
                }
        }
        
        // 标签对话框
        if (showTagDialog) {
            WindowDialog(
                show = showTagDialog,
                title = "添加标签",
                onDismissRequest = { 
                    showTagDialog = false
                    tagInput = ""
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = "输入标签（用逗号分隔）",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            text = "取消",
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                showTagDialog = false
                                tagInput = ""
                            }
                        )
                        Spacer(Modifier.width(12.dp))
                        TextButton(
                            text = "确定",
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                // 为选中的文件添加标签
                                val newTags = tagInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                if (newTags.isNotEmpty()) {
                                    vaultFiles = vaultFiles.map { file ->
                                        if (file.id in selectedFiles) {
                                            file.copy(tags = file.tags + newTags)
                                        } else {
                                            file
                                        }
                                    }
                                    saveVaultFiles(context, vaultFiles)
                                }
                                showTagDialog = false
                                tagInput = ""
                            },
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        }
    }
}

/**
 * 处理选中的文件
 */
private suspend fun handleSelectedFile(
    context: Context,
    uri: Uri,
    onSuccess: (VaultFile) -> Unit
) {
    try {
        // 获取原始文件名
        val originalName = getFileName(context, uri) ?: "unknown"
        
        // 确定文件类型
        val mimeType = context.contentResolver.getType(uri)
        val fileType = if (mimeType?.startsWith("image/") == true) {
            FileType.IMAGE
        } else {
            FileType.VIDEO
        }
        
        // 创建加密后的文件名（添加 .zip 后缀）
        val encryptedName = "$originalName.zip"
        
        // 获取私有目录
        val vaultDir = File(context.filesDir, "vault")
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
        }
        
        // 目标文件路径
        val destFile = File(vaultDir, encryptedName)
        
        // 复制文件到私有目录
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        // 验证文件是否成功复制
        if (destFile.exists() && destFile.length() > 0) {
            // 删除原文件（需要 MANAGE_EXTERNAL_STORAGE 权限）
            try {
                val originalPath = getRealPathFromUri(context, uri)
                if (originalPath != null) {
                    val originalFile = File(originalPath)
                    if (originalFile.exists()) {
                        originalFile.delete()
                    }
                }
            } catch (e: Exception) {
                // 如果无法删除原文件，记录日志但不影响导入
                e.printStackTrace()
            }
            
            // 创建 VaultFile 对象
            val vaultFile = VaultFile(
                id = System.currentTimeMillis(),
                originalName = originalName,
                encryptedName = encryptedName,
                filePath = destFile.absolutePath,
                fileType = fileType,
                tags = emptyList()
            )
            
            onSuccess(vaultFile)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 从 URI 获取真实文件路径
 */
private fun getRealPathFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.MediaStore.Images.Media.DATA)
                if (index != -1) {
                    result = it.getString(index)
                }
            }
        }
    } else if (uri.scheme == "file") {
        result = uri.path
    }
    return result
}

/**
 * 预览文件（点击图片/视频）- 使用系统应用打开
 */
private fun previewFile(context: Context, file: VaultFile) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(file.filePath)
        )
        
        val mimeType = if (file.fileType == FileType.IMAGE) "image/*" else "video/*"
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "无法打开文件", android.widget.Toast.LENGTH_SHORT).show()
    }
}

/**
 * 导出选中的文件（移出功能）
 */
private suspend fun exportSelectedFiles(
    context: Context,
    files: List<VaultFile>,
    targetDirUri: Uri
) {
    for (file in files) {
        try {
            // 恢复原始文件名（去掉 .zip 后缀）
            val originalFileName = file.originalName
            
            // 在目标目录创建文件
            val targetDocUri = DocumentFile.fromTreeUri(context, targetDirUri)
            val mimeType = if (file.fileType == FileType.IMAGE) "image/*" else "video/*"
            val newFile = targetDocUri?.createFile(mimeType, originalFileName)
            
            if (newFile != null) {
                // 复制文件内容（从加密文件读取）
                val sourceFile = File(file.filePath)
                if (sourceFile.exists()) {
                    sourceFile.inputStream().use { input ->
                        context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    // 删除保险箱中的文件
                    sourceFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * 删除文件
 */
private fun deleteFile(context: Context, file: VaultFile) {
    try {
        val destFile = File(file.filePath)
        if (destFile.exists()) {
            destFile.delete()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 获取文件名
 */
private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = it.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

/**
 * 保存文件列表到 SharedPreferences
 */
private fun saveVaultFiles(context: Context, files: List<VaultFile>) {
    val prefs = context.getSharedPreferences("vault_files", Context.MODE_PRIVATE)
    val editor = prefs.edit()
    
    editor.clear()
    editor.putInt("file_count", files.size)
    
    files.forEachIndexed { index, file ->
        editor.putLong("file_${index}_id", file.id)
        editor.putString("file_${index}_original", file.originalName)
        editor.putString("file_${index}_encrypted", file.encryptedName)
        editor.putString("file_${index}_path", file.filePath)
        editor.putString("file_${index}_type", file.fileType.name)
        editor.putString("file_${index}_tags", file.tags.joinToString(","))
        editor.putLong("file_${index}_time", file.addedTime)
    }
    
    editor.apply()
}

/**
 * 从 SharedPreferences 加载文件列表
 */
private fun loadVaultFiles(context: Context): List<VaultFile> {
    val prefs = context.getSharedPreferences("vault_files", Context.MODE_PRIVATE)
    val count = prefs.getInt("file_count", 0)
    val files = mutableListOf<VaultFile>()
    
    for (i in 0 until count) {
        val id = prefs.getLong("file_${i}_id", 0)
        val originalName = prefs.getString("file_${i}_original", "") ?: ""
        val encryptedName = prefs.getString("file_${i}_encrypted", "") ?: ""
        val filePath = prefs.getString("file_${i}_path", "") ?: ""
        val fileTypeStr = prefs.getString("file_${i}_type", "IMAGE") ?: "IMAGE"
        val tagsStr = prefs.getString("file_${i}_tags", "") ?: ""
        val addedTime = prefs.getLong("file_${i}_time", 0)
        
        val fileType = try {
            FileType.valueOf(fileTypeStr)
        } catch (e: Exception) {
            FileType.IMAGE
        }
        
        val tags = if (tagsStr.isNotEmpty()) tagsStr.split(",") else emptyList()
        
        if (id != 0L && filePath.isNotEmpty()) {
            files.add(
                VaultFile(
                    id = id,
                    originalName = originalName,
                    encryptedName = encryptedName,
                    filePath = filePath,
                    fileType = fileType,
                    tags = tags,
                    addedTime = addedTime
                )
            )
        }
    }
    
    return files
}

/**
 * 网格文件项组件（类似 Windows 资源管理器）
 */
@Composable
fun VaultFileGridItem(
    file: VaultFile,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) colorScheme.primary.copy(alpha = 0.2f) else colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onTap = { onClick() }
                )
            }
    ) {
        // 缩略图/封面
        if (file.fileType == FileType.IMAGE) {
            // 图片：直接显示
            AsyncImage(
                model = File(file.filePath),
                contentDescription = file.originalName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // 视频：显示封面
            VideoThumbnail(filePath = file.filePath)
            
            // 播放图标
            Icon(
                Icons.Rounded.PlayArrow,
                "播放",
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp),
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
        
        // 选择指示器（多选模式）
        if (isMultiSelectMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) colorScheme.primary else Color.Gray.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // 文件名标签（底部）
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(4.dp)
        ) {
            Text(
                text = file.originalName,
                fontSize = 10.sp,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

/**
 * 视频缩略图组件
 */
@Composable
fun VideoThumbnail(filePath: String) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(filePath) {
        bitmap = getVideoThumbnail(filePath)
    }
    
    bitmap?.let {
        AsyncImage(
            model = it,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } ?: Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White.copy(alpha = 0.5f))
    }
}

/**
 * 获取视频封面
 */
private fun getVideoThumbnail(filePath: String): Bitmap? {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(filePath)
        val bitmap = retriever.getFrameAtTime(
            1000000, // 1秒处的帧
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        )
        retriever.release()
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
