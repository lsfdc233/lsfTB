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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.lsfStudio.lsfTB.ui.LocalBottomBarVisibility
import com.lsfStudio.lsfTB.data.model.VaultFile
import com.lsfStudio.lsfTB.data.model.FileType
import com.lsfStudio.lsfTB.ui.util.HapticFeedbackUtil
import com.lsfStudio.lsfTB.ui.util.MessageManager
import com.lsfStudio.lsfTB.ui.util.ShareUtil
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
    var refreshKey by remember { mutableStateOf(0) }
    
    // 监听ImageViewer的重命名结果（使用refreshKey触发重新订阅）
    LaunchedEffect(refreshKey) {
        vaultFiles.forEach { file ->
            val renameKey = "image_viewer_rename_${file.id}"
            launch {
                navigator.observeResult<Unit>(renameKey).collect {
                    // 重命名后重新加载文件列表
                    vaultFiles = loadVaultFiles(context)
                    saveVaultFiles(context, vaultFiles)
                    navigator.clearResult(renameKey)
                    // 增加refreshKey以重新订阅
                    refreshKey++
                }
            }
            
            // 监听ImageViewer的删除结果
            val deleteKey = "image_viewer_delete_${file.id}"
            launch {
                navigator.observeResult<Long>(deleteKey).collect { deletedFileId ->
                    // 删除后重新加载文件列表
                    vaultFiles = loadVaultFiles(context)
                    saveVaultFiles(context, vaultFiles)
                    navigator.clearResult(deleteKey)
                    // 增加refreshKey以重新订阅
                    refreshKey++
                }
            }
        }
    }
    
    // 多选模式状态
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedFiles by remember { mutableStateOf<Set<Long>>(emptySet()) }
    
    // 对话框状态
    var showTagDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var fileToTag by remember { mutableStateOf<VaultFile?>(null) }
    var filesToExport by remember { mutableStateOf<List<VaultFile>>(emptyList()) }
    var filesToDelete by remember { mutableStateOf<List<VaultFile>>(emptyList()) }
    var filesToRename by remember { mutableStateOf<List<VaultFile>>(emptyList()) }
    var renamePreviewList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) } // 源文件名 -> 新文件名
    var tagInput by remember { mutableStateOf("") }
    var renameInput by remember { mutableStateOf("") }
    
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
    
    // 文件选择器启动器 - 使用相册选择器（支持图片和视频）
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = Int.MAX_VALUE)
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
    
    // 控制底部导航栏显示/隐藏
    val (_, setBottomBarVisibility) = LocalBottomBarVisibility.current
    LaunchedEffect(isMultiSelectMode) {
        if (isMultiSelectMode) {
            // 多选模式：隐藏底部导航栏
            setBottomBarVisibility(false)
        } else {
            // 普通模式：显示底部导航栏
            setBottomBarVisibility(true)
        }
    }
    
    // 页面退出时恢复底部导航栏显示
    DisposableEffect(Unit) {
        onDispose {
            setBottomBarVisibility(true)
        }
    }
    
    Scaffold(
        topBar = {
            if (isMultiSelectMode) {
                // 多选模式顶部栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surface)
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 左侧关闭按钮
                        IconButton(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                isMultiSelectMode = false
                                selectedFiles = emptySet()
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                "关闭",
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        // 中间标题
                        Text(
                            text = "已选择${selectedFiles.size}项",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface
                        )
                        
                        // 右侧菜单按钮
                        IconButton(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                // TODO: 显示更多选项菜单
                            }
                        ) {
                            Icon(
                                Icons.Rounded.SelectAll,
                                "菜单",
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            } else {
                // 普通模式顶部栏
                TopAppBar(
                    color = colorScheme.surface,
                    title = "私密保险箱",
                    scrollBehavior = scrollBehavior
                )
            }
        },
        bottomBar = {
            if (isMultiSelectMode) {
                // 多选模式底部操作栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surface)
                        .padding(
                            top = 2.dp,
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 发送
                        MultiSelectBottomItem(
                            icon = Icons.AutoMirrored.Rounded.Send,
                            label = "发送",
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                // 分享选中的文件
                                val selectedFileList = vaultFiles.filter { it.id in selectedFiles }
                                if (selectedFileList.isNotEmpty()) {
                                    // 获取第一个选中文件的类型
                                    val firstFile = selectedFileList.first()
                                    val mimeType = if (firstFile.fileType == FileType.VIDEO) "video/*" else "image/*"
                                    
                                    // 分享文件（不带.zip后缀）
                                    ShareUtil.shareFile(
                                        context = context,
                                        filePath = firstFile.filePath,
                                        mimeType = mimeType,
                                        originalFileName = firstFile.originalName
                                    )
                                }
                            }
                        )
                        
                        // 重命名
                        MultiSelectBottomItem(
                            icon = Icons.Rounded.Edit,
                            label = "重命名",
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                val selectedFileList = vaultFiles.filter { it.id in selectedFiles }
                                if (selectedFileList.isNotEmpty()) {
                                    filesToRename = selectedFileList
                                    // 如果只选择一个文件，预设文件名
                                    if (selectedFileList.size == 1) {
                                        renameInput = selectedFileList.first().originalName
                                    } else {
                                        renameInput = "{P}{S}"
                                    }
                                    showRenameDialog = true
                                }
                            }
                        )
                        
                        // 添加到
                        MultiSelectBottomItem(
                            icon = Icons.Rounded.AddCircle,
                            label = "添加到",
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                // TODO: 添加到功能
                            }
                        )
                        
                        // 移出
                        MultiSelectBottomItem(
                            icon = Icons.Rounded.Delete,
                            label = "移出",
                            isDanger = true,
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                // 显示二次确认弹窗
                                val selectedFileList = vaultFiles.filter { it.id in selectedFiles }
                                if (selectedFileList.isNotEmpty()) {
                                    filesToDelete = selectedFileList
                                    showDeleteConfirmDialog = true
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isMultiSelectMode) {
                // 蓝色圆形加号按钮（仅在非多选模式显示）
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
                                    // 单击：根据文件类型跳转到不同的查看器
                                    val currentIndex = vaultFiles.indexOfFirst { it.id == file.id }
                                    if (file.fileType == FileType.VIDEO) {
                                        // 视频：跳转到视频播放器
                                        navigator.navigateForResult(
                                            Route.VideoPlayer(
                                                filePath = file.filePath,
                                                fileName = file.originalName,
                                                fileId = file.id,
                                                allFilePaths = vaultFiles.map { it.filePath },
                                                allFileNames = vaultFiles.map { it.originalName },
                                                allAddedTimes = vaultFiles.map { it.addedTime },
                                                currentIndex = if (currentIndex >= 0) currentIndex else 0
                                            ),
                                            "image_viewer_rename_${file.id}"
                                        )
                                    } else {
                                        // 图片：跳转到图片查看器
                                        navigator.navigateForResult(
                                            Route.ImageViewer(
                                                filePath = file.filePath,
                                                fileName = file.originalName,
                                                addedTime = file.addedTime,
                                                fileId = file.id,
                                                allFilePaths = vaultFiles.map { it.filePath },
                                                allFileNames = vaultFiles.map { it.originalName },
                                                allAddedTimes = vaultFiles.map { it.addedTime },
                                                currentIndex = if (currentIndex >= 0) currentIndex else 0
                                            ),
                                            "image_viewer_rename_${file.id}"
                                        )
                                    }
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
        
        // 移出确认对话框
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmDialog = false
                    filesToDelete = emptyList()
                },
                title = { Text("确认移出") },
                text = { Text("确定要将${filesToDelete.size}个文件移出保险箱吗？\n文件将移动到 /sdcard/Pictures/lsfTB") },
                confirmButton = {
                    TextButton(
                        text = "移出",
                        onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            scope.launch {
                                // 执行移出操作
                                moveFilesToPublic(context, filesToDelete)
                                
                                // 从列表中删除
                                vaultFiles = vaultFiles.filter { file ->
                                    !filesToDelete.any { deleted -> deleted.id == file.id }
                                }
                                saveVaultFiles(context, vaultFiles)
                                
                                // 强制刷新UI
                                refreshKey++
                                
                                // 退出多选模式
                                isMultiSelectMode = false
                                selectedFiles = emptySet()
                                
                                // 显示Toast通知（使用MessageManager自动适配超级岛）
                                MessageManager.showToast(
                                    context,
                                    "已移出到 /sdcard/Pictures/lsfTB",
                                    android.widget.Toast.LENGTH_LONG
                                )
                                
                                showDeleteConfirmDialog = false
                                filesToDelete = emptyList()
                            }
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                },
                dismissButton = {
                    TextButton(
                        text = "取消",
                        onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            showDeleteConfirmDialog = false
                            filesToDelete = emptyList()
                        }
                    )
                }
            )
        }
        
        // 重命名对话框
        if (showRenameDialog && filesToRename.isNotEmpty()) {
            val isSingleFile = filesToRename.size == 1
            
            WindowDialog(
                show = showRenameDialog,
                title = if (isSingleFile) "重命名" else "批量重命名",
                onDismissRequest = { 
                    showRenameDialog = false
                    renameInput = ""
                    filesToRename = emptyList()
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isSingleFile) {
                        // 单文件重命名
                        TextField(
                            value = renameInput,
                            onValueChange = { renameInput = it },
                            label = "输入新文件名（含扩展名）",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // 批量重命名
                        TextField(
                            value = renameInput,
                            onValueChange = { renameInput = it },
                            label = "重命名模板",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "{P}: 文件名前缀（如 abc.png 中的 abc）\n{S}: 文件扩展名（如 abc.png 中的 .png）\n{N}: 序号（如 {0}、{1} 从指定数字递增）",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            text = "取消",
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                showRenameDialog = false
                                renameInput = ""
                                filesToRename = emptyList()
                            }
                        )
                        Spacer(Modifier.width(12.dp))
                        TextButton(
                            text = if (isSingleFile) "确定" else "预览",
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                if (isSingleFile) {
                                    // 单文件直接重命名
                                    scope.launch {
                                        val renamedFile = renameSingleFile(context, filesToRename.first(), renameInput)
                                        if (renamedFile != null) {
                                            vaultFiles = vaultFiles.map { 
                                                if (it.id == renamedFile.id) renamedFile else it 
                                            }
                                            saveVaultFiles(context, vaultFiles)
                                            showRenameDialog = false
                                            renameInput = ""
                                            filesToRename = emptyList()
                                            // 显示成功提示（使用MessageManager自动适配超级岛）
                                            MessageManager.showToast(
                                                context,
                                                "重命名成功",
                                                android.widget.Toast.LENGTH_SHORT
                                            )
                                        }
                                    }
                                } else {
                                    // 批量重命名显示预览
                                    val previewList = generateRenamePreview(filesToRename, renameInput)
                                    renamePreviewList = previewList
                                    showRenameDialog = false
                                    showPreviewDialog = true
                                }
                            },
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        }
        
        // 批量重命名预览对话框
        if (showPreviewDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPreviewDialog = false
                    renamePreviewList = emptyList()
                },
                title = { Text("预览") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        renamePreviewList.forEach { (oldName, newName) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = oldName,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = " > ",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = newName,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E88E5),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        text = "重命名",
                        onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            scope.launch {
                                val renamedFiles = executeBatchRename(context, filesToRename, renameInput)
                                // 更新 vaultFiles 中的对应项
                                vaultFiles = vaultFiles.map { file ->
                                    val renamed = renamedFiles.find { it.id == file.id }
                                    if (renamed != null) renamed else file
                                }
                                saveVaultFiles(context, vaultFiles)
                                showPreviewDialog = false
                                renamePreviewList = emptyList()
                                filesToRename = emptyList()
                                renameInput = ""
                                // 显示成功提示（使用MessageManager自动适配超级岛）
                                MessageManager.showToast(
                                    context,
                                    "批量重命名成功",
                                    android.widget.Toast.LENGTH_SHORT
                                )
                            }
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                },
                dismissButton = {
                    TextButton(
                        text = "取消",
                        onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            showPreviewDialog = false
                            renamePreviewList = emptyList()
                        }
                    )
                }
            )
        }
    }
}

/**
 * 多选模式底部操作项
 */
@Composable
private fun MultiSelectBottomItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isDanger: Boolean = false
) {
    val context = LocalContext.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable(
                onClick = {
                    HapticFeedbackUtil.lightClick(context)
                    onClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isDanger) Color(0xFFFF4757) else colorScheme.onSurface,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isDanger) Color(0xFFFF4757) else colorScheme.onSurface
        )
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
        // 显示错误提示（使用MessageManager自动适配超级岛）
        MessageManager.showToast(context, "无法打开文件", android.widget.Toast.LENGTH_SHORT)
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
 * 将文件移出到公共目录 /sdcard/Pictures/lsfTB
 */
private suspend fun moveFilesToPublic(
    context: Context,
    files: List<VaultFile>
) {
    // 创建目标目录
    val targetDir = File("/sdcard/Pictures/lsfTB")
    if (!targetDir.exists()) {
        targetDir.mkdirs()
    }
    
    for (file in files) {
        try {
            // 恢复原始文件名（去掉 .zip 后缀）
            val originalFileName = file.originalName
            
            // 目标文件路径
            val targetFile = File(targetDir, originalFileName)
            
            // 复制文件内容
            val sourceFile = File(file.filePath)
            if (sourceFile.exists()) {
                sourceFile.copyTo(targetFile, overwrite = true)
                
                // 确保复制成功后再删除原文件
                if (targetFile.exists() && targetFile.length() > 0) {
                    sourceFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * 重命名单个文件
 */
private suspend fun renameSingleFile(
    context: Context,
    file: VaultFile,
    newFullName: String
): VaultFile? {
    return try {
        val vaultDir = File(context.filesDir, "vault")
        val oldFilePath = file.filePath
        
        // 新文件名（添加.zip后缀）
        val newEncryptedName = "$newFullName.zip"
        val newFilePath = File(vaultDir, newEncryptedName).absolutePath
        
        // 重命名文件
        val oldFile = File(oldFilePath)
        val newFile = File(newFilePath)
        if (oldFile.exists()) {
            oldFile.renameTo(newFile)
            
            // 返回新的VaultFile对象
            file.copy(
                originalName = newFullName,
                encryptedName = newEncryptedName,
                filePath = newFilePath
            )
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 生成批量重命名预览列表
 */
private fun generateRenamePreview(
    files: List<VaultFile>,
    template: String
): List<Pair<String, String>> {
    return files.mapIndexed { index, file ->
        val oldName = file.originalName
        val newName = applyRenameTemplate(oldName, template, index)
        oldName to newName
    }
}

/**
 * 应用重命名模板
 */
private fun applyRenameTemplate(
    originalName: String,
    template: String,
    index: Int
): String {
    var result = template
    
    // 分离文件名和扩展名
    val lastDotIndex = originalName.lastIndexOf('.')
    val prefix = if (lastDotIndex > 0) originalName.substring(0, lastDotIndex) else originalName
    val suffix = if (lastDotIndex > 0) originalName.substring(lastDotIndex) else ""
    
    // 替换 {P} - 文件名前缀
    result = result.replace("{P}", prefix)
    
    // 替换 {S} - 文件扩展名
    result = result.replace("{S}", suffix)
    
    // 替换 {N} - 序号，支持 {0}, {1}, {2} 等
    val regex = Regex("\\{(\\d+)\\}")
    val matchResult = regex.find(result)
    if (matchResult != null) {
        val startNum = matchResult.groupValues[1].toInt()
        val num = startNum + index
        result = result.replace(matchResult.value, num.toString())
    }
    
    return result
}

/**
 * 执行批量重命名
 */
private suspend fun executeBatchRename(
    context: Context,
    files: List<VaultFile>,
    template: String
): List<VaultFile> {
    val vaultDir = File(context.filesDir, "vault")
    val renamedFiles = mutableListOf<VaultFile>()
    
    for ((index, file) in files.withIndex()) {
        try {
            val oldFilePath = file.filePath
            val newFullName = applyRenameTemplate(file.originalName, template, index)
            val newEncryptedName = "$newFullName.zip"
            val newFilePath = File(vaultDir, newEncryptedName).absolutePath
            
            // 重命名文件
            val oldFile = File(oldFilePath)
            val newFile = File(newFilePath)
            if (oldFile.exists()) {
                oldFile.renameTo(newFile)
                
                // 创建新的VaultFile对象
                renamedFiles.add(
                    file.copy(
                        originalName = newFullName,
                        encryptedName = newEncryptedName,
                        filePath = newFilePath
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            renamedFiles.add(file) // 失败时保留原文件
        }
    }
    
    return renamedFiles
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
