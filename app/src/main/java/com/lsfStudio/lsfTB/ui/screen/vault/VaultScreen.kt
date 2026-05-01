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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
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
    
    // 创建 CategoryManager（用于持久化分类数据）
    val categoryManager = remember { com.lsfStudio.lsfTB.ui.viewmodel.CategoryManager(context) }
    
    // 创建数据库中间件（用于资源管理）
    val dbMiddleware = remember { VaultDatabaseMiddleware(context) }
    
    // 文件列表状态
    var vaultFiles by remember { mutableStateOf<List<VaultFile>>(loadVaultFiles(context)) }
    var refreshKey by remember { mutableStateOf(0) }
    
    // 当前选中的分类
    var selectedCategory by remember { mutableStateOf("all") }
    
    // 自定义分类列表（从 CategoryManager 获取，响应式）
    val customCategories by categoryManager.customCategories.collectAsState()
    
    // 每次进入页面时重新加载数据
    LaunchedEffect(refreshKey) {
        vaultFiles = loadVaultFiles(context)
    }
    
    // 根据分类过滤文件
    val filteredFiles = when {
        selectedCategory == "all" -> vaultFiles
        selectedCategory == "photos" -> vaultFiles.filter { it.fileType == FileType.IMAGE }
        selectedCategory == "videos" -> vaultFiles.filter { it.fileType == FileType.VIDEO }
        selectedCategory.startsWith("custom_") -> {
            // 自定义分类：筛选包含该分类的文件
            val categoryName = selectedCategory.removePrefix("custom_")
            vaultFiles.filter { categoryName in it.categories }
        }
        else -> vaultFiles
    }
    
    // 监听ImageViewer的重命名结果（使用refreshKey触发重新订阅）
    LaunchedEffect(refreshKey) {
        vaultFiles.forEach { file ->
            val renameKey = "image_viewer_rename_${file.id}"
            launch {
                navigator.observeResult<Unit>(renameKey).collect {
                    // 重命名后重新加载文件列表
                    vaultFiles = loadVaultFiles(context)
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
    
    // 滑动选择状态
    var isDraggingToSelect by remember { mutableStateOf(false) }
    var dragSelectionStartIndex by remember { mutableStateOf<Int?>(null) }
    var lastDraggedIndex by remember { mutableStateOf<Int?>(null) }
    
    // 对话框状态
    var showTagDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddToCategoryDialog by remember { mutableStateOf(false) }
    var fileToTag by remember { mutableStateOf<VaultFile?>(null) }
    var filesToExport by remember { mutableStateOf<List<VaultFile>>(emptyList()) }
    var filesToDelete by remember { mutableStateOf<List<VaultFile>>(emptyList()) }
    var filesToRename by remember { mutableStateOf<List<VaultFile>>(emptyList()) }
    var renamePreviewList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) } // 源文件名 -> 新文件名
    var tagInput by remember { mutableStateOf("") }
    var renameInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("") }
    var selectedCategoriesForAdd by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // 分类管理状态
    var showCategoryManagePage by remember { mutableStateOf(false) }
    
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
                    handleSelectedFile(context, uri, dbMiddleware) { newFile ->
                        vaultFiles = vaultFiles + newFile
                        // 无需保存，数据已由数据库管理
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
                exportSelectedFiles(context, filesToExport, it, dbMiddleware)
                
                // 从数据库中删除已导出的文件
                filesToExport.forEach { file ->
                    val resourceUid = dbMiddleware.getResourceByPath(file.filePath)
                    if (resourceUid != null) {
                        dbMiddleware.deleteResource(resourceUid)
                        android.util.Log.d("VaultScreen", "✅ 已从数据库删除: $resourceUid")
                    }
                }
                
                // 更新UI列表
                vaultFiles = vaultFiles.filter { file ->
                    !filesToExport.any { exported -> exported.id == file.id }
                }
                // 无需保存，数据已由数据库管理
                
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
            Column(
                modifier = Modifier
                    .background(colorScheme.surface)
                    .statusBarsPadding()
            ) {
                // 标题栏
                TopAppBar(
                    color = Color.Transparent,
                    title = if (isMultiSelectMode) "已选择${selectedFiles.size}项" else "私密保险箱",
                    scrollBehavior = scrollBehavior,
                    actions = {
                        if (isMultiSelectMode) {
                            // 多选模式：显示全选按钮
                            IconButton(
                                onClick = {
                                    HapticFeedbackUtil.lightClick(context)
                                    // 全选/取消全选
                                    if (selectedFiles.size == filteredFiles.size) {
                                        selectedFiles = emptySet()
                                    } else {
                                        selectedFiles = filteredFiles.map { it.id }.toSet()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Rounded.SelectAll,
                                    "全选",
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        // 添加分类按钮（右侧）
                        IconButton(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                categoryInput = ""
                                showAddCategoryDialog = true
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                "添加分类",
                                tint = colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
                    
                // 胶囊分类标签（横向滚动）- 多选模式下也显示
                val categoryScrollState = rememberScrollState()
                
                // 使用snapshotFlow检测滚动到边缘时震动（不影响点击）
                LaunchedEffect(Unit) {
                    snapshotFlow { categoryScrollState.value }
                        .collect { value ->
                            if (value == 0 || value == categoryScrollState.maxValue) {
                                HapticFeedbackUtil.lightClick(context)
                            }
                        }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .horizontalScroll(categoryScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryChip(
                        label = "全部",
                        count = vaultFiles.size,
                        isSelected = selectedCategory == "all",
                        onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            selectedCategory = "all"
                        },
                        onLongClick = {
                            HapticFeedbackUtil.longPress(context)
                            showCategoryManagePage = true
                        }
                    )
                    CategoryChip(
                        label = "照片",
                        count = vaultFiles.count { it.fileType == FileType.IMAGE },
                        isSelected = selectedCategory == "photos",
                        onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            selectedCategory = "photos"
                        },
                        onLongClick = {
                            HapticFeedbackUtil.longPress(context)
                            showCategoryManagePage = true
                        }
                    )
                    CategoryChip(
                        label = "视频",
                        count = vaultFiles.count { it.fileType == FileType.VIDEO },
                        isSelected = selectedCategory == "videos",
                        onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            selectedCategory = "videos"
                        },
                        onLongClick = {
                            HapticFeedbackUtil.longPress(context)
                            showCategoryManagePage = true
                        }
                    )
                    
                    // 显示自定义分类
                    customCategories.forEach { category ->
                        CategoryChip(
                            label = category,
                            count = vaultFiles.count { category in it.categories },
                            isSelected = selectedCategory == "custom_$category",
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                selectedCategory = "custom_$category"
                            },
                            onLongClick = {
                                HapticFeedbackUtil.longPress(context)
                                showCategoryManagePage = true
                            }
                        )
                    }
                }
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
                                    
                                    // 提取扩展名（兼容旧数据）
                                    val extension = firstFile.originalName.substringAfterLast('.', "")
                                    val extWithDot = if (extension.isNotEmpty()) ".${extension}" else ""
                                    
                                    // 分享文件（按元数据重命名）
                                    ShareUtil.shareFile(
                                        context = context,
                                        filePath = firstFile.filePath,
                                        mimeType = mimeType,
                                        originalFileName = firstFile.originalName.substringBeforeLast('.'),
                                        extension = extWithDot
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
                                val selectedFileList = vaultFiles.filter { it.id in selectedFiles }
                                if (selectedFileList.isNotEmpty()) {
                                    // 计算所有选中文件的分类交集（已属于的分类）
                                    val commonCategories = if (selectedFileList.size == 1) {
                                        // 只有一个文件，直接使用它的分类
                                        selectedFileList.first().categories.toSet()
                                    } else {
                                        // 多个文件，取交集
                                        selectedFileList
                                            .map { it.categories.toSet() }
                                            .reduce { acc, set -> acc.intersect(set) }
                                    }
                                    
                                    android.util.Log.d("VaultScreen", "📊 选中${selectedFileList.size}个文件，共同分类: $commonCategories")
                                    selectedCategoriesForAdd = commonCategories
                                    showAddToCategoryDialog = true
                                }
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
            if (filteredFiles.isEmpty()) {
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
                items(filteredFiles, key = { it.id }) { file ->
                    val fileIndex = filteredFiles.indexOf(file)
                    
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
                            // 长按：进入多选模式并选中当前项
                            if (!isMultiSelectMode) {
                                isMultiSelectMode = true
                                selectedFiles = setOf(file.id)
                                dragSelectionStartIndex = fileIndex
                                lastDraggedIndex = fileIndex
                                // 震动反馈
                                HapticFeedbackUtil.longPress(context)
                            } else {
                                // 已在多选模式下长按：重新设置起始点（不改变选择状态）
                                dragSelectionStartIndex = fileIndex
                                lastDraggedIndex = fileIndex
                                // 震动反馈
                                HapticFeedbackUtil.longPress(context)
                            }
                        },
                        onDragEnter = {
                            // 拖动进入：如果正在拖动选择，则进行智能选择
                            if (isMultiSelectMode && isDraggingToSelect && dragSelectionStartIndex != null) {
                                val startIndex = dragSelectionStartIndex!!
                                val currentIndex = fileIndex
                                val lastIndex = lastDraggedIndex ?: startIndex
                                
                                // 计算列数（用于判断是否换行）
                                // GridCells.Adaptive(minSize = 120.dp)，假设屏幕宽度约 360dp，大约 3 列
                                val columns = 3
                                
                                // 判断移动方向
                                val startRow = startIndex / columns
                                val currentRow = currentIndex / columns
                                val lastRow = lastIndex / columns
                                
                                val startCol = startIndex % columns
                                val currentCol = currentIndex % columns
                                val lastCol = lastIndex % columns
                                
                                // 检测是否跨行（纵向移动）
                                val isVerticalMove = currentRow != lastRow
                                
                                if (isVerticalMove) {
                                    // 纵向移动：选择/反选整行
                                    val newRowStart = currentRow * columns
                                    val newRowEnd = minOf(newRowStart + columns - 1, filteredFiles.size - 1)
                                    
                                    // 检查这一行是否已经被完全选中
                                    val rowFiles = filteredFiles.slice(newRowStart..newRowEnd).map { it.id }.toSet()
                                    val isRowFullySelected = rowFiles.all { it in selectedFiles }
                                    
                                    if (isRowFullySelected) {
                                        // 如果整行已选中，则取消选中这一行
                                        selectedFiles = selectedFiles - rowFiles
                                    } else {
                                        // 否则选中这一行
                                        selectedFiles = selectedFiles + rowFiles
                                    }
                                    
                                    // 震动反馈（每行一次）
                                    HapticFeedbackUtil.lightClick(context)
                                } else {
                                    // 横向移动：逐个选择/取消选择
                                    if (currentIndex != lastIndex) {
                                        // 确定移动方向
                                        val step = if (currentIndex > lastIndex) 1 else -1
                                        var idx = lastIndex + step
                                        
                                        while (if (step > 0) idx <= currentIndex else idx >= currentIndex) {
                                            val fileId = filteredFiles[idx].id
                                            
                                            // 检查这个文件是否在起始点到当前点的范围内
                                            val minIdx = minOf(startIndex, currentIndex)
                                            val maxIdx = maxOf(startIndex, currentIndex)
                                            
                                            if (idx in minIdx..maxIdx) {
                                                // 在范围内：选中
                                                selectedFiles = selectedFiles + fileId
                                            } else {
                                                // 不在范围内：取消选中
                                                selectedFiles = selectedFiles - fileId
                                            }
                                            
                                            idx += step
                                        }
                                        
                                        // 震动反馈（每个文件一次）
                                        HapticFeedbackUtil.lightClick(context)
                                    }
                                }
                                
                                lastDraggedIndex = currentIndex
                            }
                        },
                        onDragStart = {
                            // 开始拖动：标记为拖动选择模式
                            if (isMultiSelectMode) {
                                isDraggingToSelect = true
                            }
                        },
                        onDragEnd = {
                            // 结束拖动：清除拖动状态
                            isDraggingToSelect = false
                            dragSelectionStartIndex = null
                            lastDraggedIndex = null
                        },
                        onDelete = {
                            scope.launch {
                                deleteFile(context, file, dbMiddleware)
                                vaultFiles = vaultFiles.filter { it.id != file.id }
                                // 无需保存，数据已由数据库管理
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
                                    // 无需保存，数据已由数据库管理
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
                                moveFilesToPublic(context, filesToDelete, dbMiddleware)
                                
                                // 从列表中删除
                                vaultFiles = vaultFiles.filter { file ->
                                    !filesToDelete.any { deleted -> deleted.id == file.id }
                                }
                                // 无需保存，数据已由数据库管理
                                
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
                                        val renamedFile = renameSingleFile(context, filesToRename.first(), renameInput, dbMiddleware)
                                        if (renamedFile != null) {
                                            vaultFiles = vaultFiles.map { 
                                                if (it.id == renamedFile.id) renamedFile else it 
                                            }
                                            // 无需保存，数据已由数据库管理
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
                                val renamedFiles = executeBatchRename(context, filesToRename, renameInput, dbMiddleware)
                                // 更新 vaultFiles 中的对应项
                                vaultFiles = vaultFiles.map { file ->
                                    val renamed = renamedFiles.find { it.id == file.id }
                                    if (renamed != null) renamed else file
                                }
                                // 无需保存，数据已由数据库管理
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
        
        // 添加分类对话框
        if (showAddCategoryDialog) {
            WindowDialog(
                show = showAddCategoryDialog,
                title = "添加分类",
                onDismissRequest = { 
                    showAddCategoryDialog = false
                    categoryInput = ""
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = categoryInput,
                        onValueChange = { 
                            // 限制最多20个字符
                            if (it.length <= 20) {
                                categoryInput = it 
                            }
                        },
                        label = "输入分类名称（最多20个字符）",
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
                                showAddCategoryDialog = false
                                categoryInput = ""
                            }
                        )
                        Spacer(Modifier.width(12.dp))
                        TextButton(
                            text = "添加",
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                val categoryName = categoryInput.trim()
                                if (categoryName.isNotEmpty() && !customCategories.contains(categoryName)) {
                                    categoryManager.addCategory(categoryName)
                                    showAddCategoryDialog = false
                                    categoryInput = ""
                                }
                            },
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        }
        
        // 添加到分类对话框
        if (showAddToCategoryDialog) {
            WindowDialog(
                show = showAddToCategoryDialog,
                title = "添加到分类",
                onDismissRequest = { 
                    showAddToCategoryDialog = false
                    selectedCategoriesForAdd = emptySet()
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 显示现有分类列表（可多选，支持滚动）
                    if (customCategories.isEmpty()) {
                        Text(
                            text = "暂无自定义分类，请先创建分类",
                            color = colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        // 使用LazyColumn支持滚动，选项更紧密
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)
                        ) {
                            items(customCategories.size) { index ->
                                val category = customCategories[index]
                                val isSelected = category in selectedCategoriesForAdd
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            // 处理分类切换
                                            if (isSelected) {
                                                // 取消勾选：从数据库中移除关联
                                                val selectedFileList = vaultFiles.filter { it.id in selectedFiles }
                                                selectedFileList.forEach { file ->
                                                    val resourceUid = dbMiddleware.getResourceByPath(file.filePath)
                                                    if (resourceUid != null) {
                                                        val allCategories = dbMiddleware.getAllCategories()
                                                        val categoryObj = allCategories.find { it["name"] == category && !(it["isSystem"] as Boolean) }
                                                        if (categoryObj != null) {
                                                            val categoryUid = categoryObj["uid"] as String
                                                            dbMiddleware.removeResourceFromCategory(resourceUid, categoryUid)
                                                            android.util.Log.d("VaultScreen", "❌ 移除关联: resource=$resourceUid, category=$categoryUid ($category)")
                                                        }
                                                    }
                                                }
                                                
                                                // 更新内存状态
                                                vaultFiles = vaultFiles.map { file ->
                                                    if (file.id in selectedFiles) {
                                                        file.copy(categories = file.categories - category)
                                                    } else {
                                                        file
                                                    }
                                                }
                                                // 无需保存，数据已由数据库管理
                                                
                                                selectedCategoriesForAdd = selectedCategoriesForAdd - category
                                            } else {
                                                // 勾选：添加到选中集合（实际添加在“添加”按钮中处理）
                                                selectedCategoriesForAdd = selectedCategoriesForAdd + category
                                            }
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            // 处理分类切换
                                            if (isSelected) {
                                                // 取消勾选：从数据库中移除关联
                                                val selectedFileList = vaultFiles.filter { it.id in selectedFiles }
                                                selectedFileList.forEach { file ->
                                                    val resourceUid = dbMiddleware.getResourceByPath(file.filePath)
                                                    if (resourceUid != null) {
                                                        val allCategories = dbMiddleware.getAllCategories()
                                                        val categoryObj = allCategories.find { it["name"] == category && !(it["isSystem"] as Boolean) }
                                                        if (categoryObj != null) {
                                                            val categoryUid = categoryObj["uid"] as String
                                                            dbMiddleware.removeResourceFromCategory(resourceUid, categoryUid)
                                                            android.util.Log.d("VaultScreen", "❌ 移除关联: resource=$resourceUid, category=$categoryUid ($category)")
                                                        }
                                                    }
                                                }
                                                
                                                // 更新内存状态
                                                vaultFiles = vaultFiles.map { file ->
                                                    if (file.id in selectedFiles) {
                                                        file.copy(categories = file.categories - category)
                                                    } else {
                                                        file
                                                    }
                                                }
                                                // 无需保存，数据已由数据库管理
                                                
                                                selectedCategoriesForAdd = selectedCategoriesForAdd - category
                                            } else {
                                                // 勾选：添加到选中集合（实际添加在“添加”按钮中处理）
                                                selectedCategoriesForAdd = selectedCategoriesForAdd + category
                                            }
                                        },
                                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                                            checkedColor = colorScheme.primary,
                                            checkmarkColor = colorScheme.onPrimary
                                        )
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = category,
                                        fontSize = 15.sp,
                                        color = colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            text = "取消",
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                showAddToCategoryDialog = false
                                selectedCategoriesForAdd = emptySet()
                            }
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                text = "新建分类",
                                onClick = {
                                    HapticFeedbackUtil.lightClick(context)
                                    // 不关闭当前对话框，叠加显示新建分类对话框
                                    categoryInput = ""
                                    showAddCategoryDialog = true
                                }
                            )
                            TextButton(
                                text = "确认修改",
                                onClick = {
                                    HapticFeedbackUtil.lightClick(context)
                                    val selectedFileList = vaultFiles.filter { it.id in selectedFiles }
                                    val fileCount = selectedFileList.size
                                    
                                    // 计算需要添加的分类（当前勾选 - 原本就有的）
                                    val originalCommonCategories = if (selectedFileList.size == 1) {
                                        selectedFileList.first().categories.toSet()
                                    } else {
                                        selectedFileList
                                            .map { it.categories.toSet() }
                                            .reduce { acc, set -> acc.intersect(set) }
                                    }
                                    val categoriesToAdd = selectedCategoriesForAdd - originalCommonCategories
                                    val categoriesToRemove = originalCommonCategories - selectedCategoriesForAdd
                                    
                                    android.util.Log.d("VaultScreen", "📊 原始共同分类: $originalCommonCategories")
                                    android.util.Log.d("VaultScreen", "📊 需要添加: $categoriesToAdd, 需要移除: $categoriesToRemove")
                                    
                                    // 为选中的文件处理分类变更（保存到数据库）
                                    selectedFileList.forEach { file ->
                                        // 获取或创建资源记录
                                        var resourceUid = dbMiddleware.getResourceByPath(file.filePath)
                                        if (resourceUid == null) {
                                            // 如果资源不存在，创建新记录
                                            val extension = file.originalName.substringAfterLast('.', "")
                                            val nameWithoutExt = file.originalName.substringBeforeLast('.')
                                            resourceUid = dbMiddleware.addResource(
                                                originalName = nameWithoutExt,
                                                extension = if (extension.isNotEmpty()) ".${extension}" else "",
                                                filePath = file.filePath,
                                                fileType = file.fileType.name,
                                                fileSize = File(file.filePath).length()
                                            )
                                        }
                                        
                                        // 添加新分类
                                        categoriesToAdd.forEach { categoryName ->
                                            val allCategories = dbMiddleware.getAllCategories()
                                            val category = allCategories.find { it["name"] == categoryName && !(it["isSystem"] as Boolean) }
                                            if (category != null) {
                                                val categoryUid = category["uid"] as String
                                                dbMiddleware.addResourceToCategory(resourceUid, categoryUid)
                                                android.util.Log.d("VaultScreen", "✅ 添加关联: resource=$resourceUid, category=$categoryUid ($categoryName)")
                                            }
                                        }
                                        
                                        // 移除已取消的分类
                                        categoriesToRemove.forEach { categoryName ->
                                            val allCategories = dbMiddleware.getAllCategories()
                                            val category = allCategories.find { it["name"] == categoryName && !(it["isSystem"] as Boolean) }
                                            if (category != null) {
                                                val categoryUid = category["uid"] as String
                                                dbMiddleware.removeResourceFromCategory(resourceUid, categoryUid)
                                                android.util.Log.d("VaultScreen", "❌ 移除关联: resource=$resourceUid, category=$categoryUid ($categoryName)")
                                            }
                                        }
                                    }
                                    
                                    // 更新内存中的状态
                                    vaultFiles = vaultFiles.map { file ->
                                        if (file.id in selectedFiles) {
                                            // 使用当前勾选的分类集合
                                            file.copy(categories = selectedCategoriesForAdd.toList())
                                        } else {
                                            file
                                        }
                                    }
                                    // 无需保存，数据已由数据库管理
                                    
                                    // 显示成功提示
                                    when {
                                        categoriesToAdd.isNotEmpty() && categoriesToRemove.isNotEmpty() -> {
                                            MessageManager.showToast(context, "$fileCount 个文件分类已更新")
                                        }
                                        categoriesToAdd.isNotEmpty() -> {
                                            MessageManager.showToast(context, "$fileCount 个文件已添加分类")
                                        }
                                        categoriesToRemove.isNotEmpty() -> {
                                            MessageManager.showToast(context, "$fileCount 个文件已移出分类")
                                        }
                                        else -> {
                                            MessageManager.showToast(context, "分类未变化")
                                        }
                                    }
                                    
                                    // 跳转到第一个选中的分类
                                    val firstCategory = selectedCategoriesForAdd.firstOrNull()
                                    if (firstCategory != null) {
                                        selectedCategory = "custom_$firstCategory"
                                    }
                                    
                                    // 退出多选状态
                                    isMultiSelectMode = false
                                    selectedFiles = emptySet()
                                    
                                    showAddToCategoryDialog = false
                                    selectedCategoriesForAdd = emptySet()
                                },
                                colors = ButtonDefaults.textButtonColorsPrimary()
                            )
                        }
                    }
                }
            }
        }
        
        // 分类管理页面
        if (showCategoryManagePage) {
            // 系统分类 + 自定义分类
            val systemCategories = listOf("全部", "照片", "视频")
            var reorderList by remember { mutableStateOf((systemCategories + customCategories).toMutableList()) }
            var draggedIndex by remember { mutableStateOf<Int?>(null) }
            var dragOffsetY by remember { mutableStateOf(0f) } // 拖拽偏移量（相对于起始位置）
            var dragStartY by remember { mutableStateOf(0f) } // 拖拽起始Y坐标
            val listState = rememberLazyListState() // 列表状态，用于控制滚动
            val coroutineScope = rememberCoroutineScope()
            
            WindowDialog(
                show = showCategoryManagePage,
                title = "管理分类",
                onDismissRequest = { showCategoryManagePage = false }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "长按拖动图标可调整自定义分类顺序",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp) // 添加内边距，方便滚动
                    ) {
                        items(
                            count = reorderList.size,
                            key = { index -> reorderList[index] } // 使用分类名称作为key，提高性能
                        ) { index ->
                            val category = reorderList[index]
                            val isDragging = draggedIndex == index
                            val isSystemCategory = index < 3 // 前三个是系统分类
                            
                            // 计算分类数量
                            val categoryCount = when (category) {
                                "全部" -> vaultFiles.size
                                "照片" -> vaultFiles.count { it.fileType == FileType.IMAGE }
                                "视频" -> vaultFiles.count { it.fileType == FileType.VIDEO }
                                else -> vaultFiles.count { category in it.categories }
                            }
                            
                            // 如果是正在拖拽的项，添加偏移和放大效果
                            val dragModifier = if (isDragging) {
                                Modifier
                                    .background(
                                        color = colorScheme.surface,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    )
                                    .graphicsLayer {
                                        translationY = dragOffsetY
                                        scaleX = 1.05f  // 轻微放大
                                        scaleY = 1.05f  // 轻微放大
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                        clip = true
                                    }
                                    .alpha(0.98f)  // 轻微半透明
                            } else {
                                Modifier
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = if (isDragging) 8.dp else 0.dp)
                                    .then(dragModifier),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    category, 
                                    fontSize = 16.sp, 
                                    color = if (isSystemCategory) colorScheme.onSurface.copy(alpha = 0.4f) else colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "($categoryCount)", 
                                    fontSize = 14.sp, 
                                    color = if (isSystemCategory) colorScheme.onSurface.copy(alpha = 0.3f) else colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                
                                // 只有自定义分类才显示删除按钮
                                if (!isSystemCategory) {
                                    IconButton(onClick = {
                                        HapticFeedbackUtil.lightClick(context)
                                        // 使用 CategoryManager 删除分类（会自动持久化）
                                        categoryManager.removeCategory(category)
                                        
                                        // 从文件中移除该分类标签
                                        vaultFiles = vaultFiles.map { it.copy(categories = it.categories - category) }
                                        // 无需保存，数据已由数据库管理
                                        
                                        val newList = reorderList.toMutableList()
                                        newList.removeAt(index)
                                        reorderList = newList
                                        if (selectedCategory == "custom_$category") selectedCategory = "all"
                                        MessageManager.showToast(context, "分类已删除")
                                    }) {
                                        Icon(Icons.Rounded.RemoveCircleOutline, "删除", tint = Color(0xFFFF4757), modifier = Modifier.size(24.dp))
                                    }
                                    
                                    // 拖拽手柄 - 改进版
                                    IconButton(
                                        onClick = {},
                                        modifier = Modifier.pointerInput(category) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { offset ->
                                                    HapticFeedbackUtil.longPress(context)
                                                    draggedIndex = index
                                                    dragStartY = offset.y
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    if (draggedIndex == null) return@detectDragGesturesAfterLongPress
                                                    
                                                    // 累积拖拽偏移（不重置）
                                                    dragOffsetY += dragAmount.y
                                                    
                                                    // 计算目标位置（基于累积偏移）
                                                    val itemHeight = 50.dp.toPx() // 估算每项高度
                                                    val offsetItems = (dragOffsetY / itemHeight).roundToInt()
                                                    var newIndex = (draggedIndex!! + offsetItems)
                                                        .coerceIn(0, reorderList.size - 1)
                                                    
                                                    // 防止拖拽到系统分类区域（前3个）
                                                    if (draggedIndex!! >= 3 && newIndex < 3) {
                                                        newIndex = 3 // 最多只能拖到第4个位置
                                                    }
                                                    
                                                    if (newIndex != draggedIndex!!) {
                                                        val newList = reorderList.toMutableList()
                                                        val item = newList.removeAt(draggedIndex!!)
                                                        newList.add(newIndex, item)
                                                        reorderList = newList
                                                        draggedIndex = newIndex
                                                        // 关键：减去已交换的偏移量，而不是重置为0
                                                        dragOffsetY -= offsetItems * itemHeight
                                                        
                                                        // 每次交换触发震动
                                                        HapticFeedbackUtil.lightClick(context)
                                                    }
                                                    
                                                    // 边缘检测和自动滚动
                                                    val viewportHeight = 400.dp.toPx() // LazyColumn的高度
                                                    val firstVisibleIndex = listState.firstVisibleItemIndex
                                                    
                                                    // 检测是否在顶部边缘（前20%区域）
                                                    if (change.position.y < viewportHeight * 0.2 && firstVisibleIndex > 3) {
                                                        coroutineScope.launch {
                                                            listState.animateScrollToItem(firstVisibleIndex - 1)
                                                        }
                                                    }
                                                    // 检测是否在底部边缘（后20%区域）
                                                    else if (change.position.y > viewportHeight * 0.8 && 
                                                             firstVisibleIndex < reorderList.size - 1) {
                                                        coroutineScope.launch {
                                                            listState.animateScrollToItem(firstVisibleIndex + 1)
                                                        }
                                                    }
                                                },
                                                onDragEnd = { 
                                                    draggedIndex = null
                                                    dragOffsetY = 0f
                                                },
                                                onDragCancel = { 
                                                    draggedIndex = null
                                                    dragOffsetY = 0f
                                                }
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Rounded.DragHandle, "拖动", tint = if (isDragging) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(text = "取消", onClick = { HapticFeedbackUtil.lightClick(context); showCategoryManagePage = false })
                        Spacer(Modifier.width(12.dp))
                        TextButton(
                            text = "保存",
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                // 只保存自定义分类（排除前三个系统分类）
                                val customOnly = reorderList.drop(3)
                                categoryManager.reorderCategories(customOnly)
                                showCategoryManagePage = false
                                MessageManager.showToast(context, "排序已保存")
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
    dbMiddleware: VaultDatabaseMiddleware,
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
        
        // 提取逻辑文件名和扩展名
        val extension = originalName.substringAfterLast('.', "")
        val nameWithoutExt = originalName.substringBeforeLast('.')
        
        // 生成UID（作为物理文件名）
        val uid = if (fileType == FileType.IMAGE) {
            dbMiddleware.generatePictureUid()
        } else {
            dbMiddleware.generateVideoUid()
        }
        
        // 物理文件名：uid.tb
        val physicalFileName = "$uid.tb"
        
        // 获取私有目录
        val vaultDir = File(context.filesDir, "vault")
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
        }
        
        // 目标文件路径（使用物理文件名）
        val destFile = File(vaultDir, physicalFileName)
        
        // ✅ 第一步：复制文件到临时位置
        val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        // ✅ 第二步：加密文件
        android.util.Log.d("VaultScreen", "🔐 开始加密文件...")
        val encryptSuccess = com.lsfStudio.lsfTB.ui.util.VaultEncryptionManager.encryptFile(tempFile, destFile)
        
        // 删除临时文件
        tempFile.delete()
        
        if (!encryptSuccess) {
            android.util.Log.e("VaultScreen", "❌ 文件加密失败")
            return
        }
        
        // 验证文件是否成功加密
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
            
            // 保存资源到数据库
            val resourceUid = dbMiddleware.addResource(
                originalName = nameWithoutExt,
                extension = if (extension.isNotEmpty()) ".${extension}" else "",
                filePath = destFile.absolutePath,
                fileType = fileType.name,
                fileSize = destFile.length()
            )
            android.util.Log.d("VaultScreen", "✅ 新资源已保存到数据库: uid=$resourceUid, originalName=$nameWithoutExt$extension, path=${destFile.absolutePath}")
            
            // 创建 VaultFile 对象
            val vaultFile = VaultFile(
                id = System.currentTimeMillis(),
                originalName = originalName,  // 用户看到的完整文件名
                encryptedName = physicalFileName,  // 物理文件名（.tb格式）
                filePath = destFile.absolutePath,
                fileType = fileType,
                tags = emptyList(),
                categories = emptyList()
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
    targetDirUri: Uri,
    dbMiddleware: VaultDatabaseMiddleware? = null
) {
    for (file in files) {
        try {
            // 按元数据恢复完整文件名
            val fullFileName = if (dbMiddleware != null) {
                val resourceUid = dbMiddleware.getResourceByPath(file.filePath)
                if (resourceUid != null) {
                    val resources = dbMiddleware.getAllResources()
                    val resource = resources.find { it["uid"] == resourceUid }
                    if (resource != null) {
                        val originalName = resource["originalName"] as String
                        val extension = resource["extension"] as String
                        "$originalName$extension"
                    } else {
                        file.originalName  // 降级处理
                    }
                } else {
                    file.originalName  // 降级处理
                }
            } else {
                file.originalName  // 兼容旧逻辑
            }
            
            // ✅ 第一步：解密文件到临时位置
            val tempFile = com.lsfStudio.lsfTB.ui.util.VaultEncryptionManager.decryptToTempFile(
                context = context,
                encryptedFilePath = file.filePath,
                originalFileName = fullFileName
            )
            
            if (tempFile == null) {
                android.util.Log.e("VaultScreen", "❌ 解密失败: ${file.originalName}")
                continue
            }
            
            // ✅ 第二步：将解密后的文件复制到目标目录
            val targetDocUri = DocumentFile.fromTreeUri(context, targetDirUri)
            val mimeType = if (file.fileType == FileType.IMAGE) "image/*" else "video/*"
            val newFile = targetDocUri?.createFile(mimeType, fullFileName)
            
            if (newFile != null) {
                // 复制解密后的文件内容
                tempFile.inputStream().use { input ->
                    context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                
                // ✅ 第三步：删除保险箱中的加密文件
                val encryptedFile = File(file.filePath)
                if (encryptedFile.exists()) {
                    encryptedFile.delete()
                }
                
                // ✅ 第四步：删除临时文件
                tempFile.delete()
                
                android.util.Log.d("VaultScreen", "✅ 文件已导出: $fullFileName")
            }
        } catch (e: Exception) {
            android.util.Log.e("VaultScreen", "❌ 导出文件失败: ${e.message}")
            e.printStackTrace()
        }
    }
}

/**
 * 删除文件（新架构：同时删除物理文件和数据库记录）
 */
private fun deleteFile(context: Context, file: VaultFile, dbMiddleware: VaultDatabaseMiddleware? = null) {
    try {
        // 1. 删除物理文件
        val destFile = File(file.filePath)
        if (destFile.exists()) {
            destFile.delete()
            android.util.Log.d("VaultScreen", "✅ 已删除物理文件: ${file.filePath}")
        }
        
        // 2. 从数据库中删除记录
        if (dbMiddleware != null) {
            val resourceUid = dbMiddleware.getResourceByPath(file.filePath)
            if (resourceUid != null) {
                dbMiddleware.deleteResource(resourceUid)
                android.util.Log.d("VaultScreen", "✅ 已从数据库删除记录: uid=$resourceUid")
            } else {
                android.util.Log.w("VaultScreen", "⚠️ 未找到数据库记录: ${file.filePath}")
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("VaultScreen", "❌ 删除文件失败: ${e.message}")
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
 * 保存文件列表到 SharedPreferences（已废弃，数据现在由数据库管理）
 * @deprecated 此函数不再执行任何操作，仅为兼容性保留
 */
@Deprecated("数据现在由数据库管理，无需手动保存", level = DeprecationLevel.HIDDEN)
private fun saveVaultFiles(context: Context, files: List<VaultFile>) {
    // 空操作：数据已由 VaultDatabaseMiddleware 管理
}
private fun loadVaultFiles(context: Context): List<VaultFile> {
    val dbMiddleware = VaultDatabaseMiddleware(context)
    
    // 迁移旧数据（.zip文件和SharedPreferences）
    migrateOldZipFiles(context, dbMiddleware)
    migrateFromSharedPreferences(context, dbMiddleware)
    
    // 从数据库获取所有资源
    val resources = dbMiddleware.getAllResources()
    val files = mutableListOf<VaultFile>()
    
    resources.forEach { resource ->
        val uid = resource["uid"] as String
        val originalName = resource["originalName"] as String
        val extension = resource["extension"] as String
        val filePath = resource["filePath"] as String
        val fileTypeStr = resource["fileType"] as String
        val addedTime = resource["addedTime"] as Long
        
        // 确定文件类型
        val fileType = if (fileTypeStr == "IMAGE") FileType.IMAGE else FileType.VIDEO
        
        // 构建完整文件名
        val fullFileName = "$originalName$extension"
        
        // 从数据库加载该资源的分类
        val categories = try {
            dbMiddleware.getCategoryNamesByResource(uid)
        } catch (e: Exception) {
            android.util.Log.e("VaultScreen", "❌ 加载分类失败: ${e.message}")
            emptyList()
        }
        
        // 生成ID（使用时间戳作为唯一标识）
        val id = addedTime
        
        files.add(
            VaultFile(
                id = id,
                originalName = fullFileName,
                encryptedName = "$fullFileName.tb",  // 兼容旧字段
                filePath = filePath,
                fileType = fileType,
                tags = emptyList(),  // Tags功能已废弃，使用categories
                categories = categories,
                addedTime = addedTime
            )
        )
    }
    
    android.util.Log.d("VaultScreen", "📋 从数据库加载${files.size}个文件")
    return files
}

/**
 * 迁移 SharedPreferences 中的旧数据到数据库
 */
private fun migrateFromSharedPreferences(context: Context, dbMiddleware: VaultDatabaseMiddleware) {
    val prefs = context.getSharedPreferences("vault_files", Context.MODE_PRIVATE)
    val count = prefs.getInt("file_count", 0)
    
    if (count == 0) {
        android.util.Log.d("VaultScreen", "✅ 没有需要迁移的SharedPreferences数据")
        return
    }
    
    android.util.Log.d("VaultScreen", "🔄 开始迁移${count}个SharedPreferences记录...")
    
    var migratedCount = 0
    for (i in 0 until count) {
        try {
            val originalName = prefs.getString("file_${i}_original", "") ?: ""
            val filePath = prefs.getString("file_${i}_path", "") ?: ""
            val fileTypeStr = prefs.getString("file_${i}_type", "IMAGE") ?: "IMAGE"
            val addedTime = prefs.getLong("file_${i}_time", System.currentTimeMillis())
            
            if (originalName.isEmpty() || filePath.isEmpty()) {
                continue
            }
            
            // 检查文件是否已存在于数据库中
            val existingUid = dbMiddleware.getResourceByPath(filePath)
            if (existingUid != null) {
                android.util.Log.d("VaultScreen", "⏭️ 跳过已存在的文件: $originalName")
                continue
            }
            
            // 提取逻辑文件名和扩展名
            val extension = originalName.substringAfterLast('.', "")
            val nameWithoutExt = originalName.substringBeforeLast('.')
            
            // 确定文件类型
            val fileType = if (fileTypeStr == "IMAGE") "IMAGE" else "VIDEO"
            
            // 添加到数据库
            dbMiddleware.addResource(
                originalName = nameWithoutExt,
                extension = if (extension.isNotEmpty()) ".${extension}" else "",
                filePath = filePath,
                fileType = fileType,
                addedTime = addedTime,
                fileSize = File(filePath).length()
            )
            
            migratedCount++
            android.util.Log.d("VaultScreen", "✅ 迁移成功: $originalName")
        } catch (e: Exception) {
            android.util.Log.e("VaultScreen", "❌ 迁移异常: ${e.message}")
            e.printStackTrace()
        }
    }
    
    // 清除 SharedPreferences 数据
    prefs.edit().clear().apply()
    
    android.util.Log.d("VaultScreen", "✅ SharedPreferences迁移完成: ${migratedCount}/${count}个文件")
}

/**
 * 迁移旧的.zip文件到新架构（uid.tb）
 */
private fun migrateOldZipFiles(context: Context, dbMiddleware: VaultDatabaseMiddleware) {
    val vaultDir = File(context.filesDir, "vault")
    if (!vaultDir.exists()) return
    
    val zipFiles = vaultDir.listFiles { file -> file.name.endsWith(".zip") }
    if (zipFiles.isNullOrEmpty()) {
        android.util.Log.d("VaultScreen", "✅ 没有需要迁移的.zip文件")
        return
    }
    
    android.util.Log.d("VaultScreen", "🔄 开始迁移${zipFiles.size}个.zip文件...")
    
    var migratedCount = 0
    zipFiles.forEach { zipFile ->
        try {
            // 提取原始文件名（去掉.zip后缀）
            val originalName = zipFile.name.removeSuffix(".zip")
            
            // 确定文件类型
            val mimeType = context.contentResolver.getType(android.net.Uri.fromFile(zipFile))
            val fileType = if (mimeType?.startsWith("image/") == true) "IMAGE" else "VIDEO"
            
            // 生成新UID
            val uid = if (fileType == "IMAGE") {
                dbMiddleware.generatePictureUid()
            } else {
                dbMiddleware.generateVideoUid()
            }
            
            // 新物理文件名
            val newFileName = "$uid.tb"
            val newFile = File(vaultDir, newFileName)
            
            // 重命名文件
            if (zipFile.renameTo(newFile)) {
                // 添加到数据库
                val extension = originalName.substringAfterLast('.', "")
                val nameWithoutExt = originalName.substringBeforeLast('.')
                
                dbMiddleware.addResource(
                    originalName = nameWithoutExt,
                    extension = if (extension.isNotEmpty()) ".${extension}" else "",
                    filePath = newFile.absolutePath,
                    fileType = fileType,
                    fileSize = newFile.length()
                )
                
                android.util.Log.d("VaultScreen", "✅ 迁移成功: $originalName.zip -> $newFileName")
                migratedCount++
            } else {
                android.util.Log.e("VaultScreen", "❌ 迁移失败: ${zipFile.name}")
            }
        } catch (e: Exception) {
            android.util.Log.e("VaultScreen", "❌ 迁移异常: ${zipFile.name}, ${e.message}")
            e.printStackTrace()
        }
    }
    
    android.util.Log.d("VaultScreen", "✅ 迁移完成: $migratedCount/${zipFiles.size}个文件")
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
    onDragEnter: () -> Unit = {},
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
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
            // 添加拖动手势检测（用于滑动选择）
            .pointerInput(isMultiSelectMode) {
                if (isMultiSelectMode) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            onDragStart()
                        },
                        onDragEnd = {
                            onDragEnd()
                        },
                        onDragCancel = {
                            onDragEnd()
                        },
                        onDrag = { change, dragAmount ->
                            // 拖动过程中检测是否进入其他项目
                            onDragEnter()
                        }
                    )
                }
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
    files: List<VaultFile>,
    dbMiddleware: VaultDatabaseMiddleware? = null
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
                    
                    // 从数据库中删除记录
                    if (dbMiddleware != null) {
                        val resourceUid = dbMiddleware.getResourceByPath(file.filePath)
                        if (resourceUid != null) {
                            dbMiddleware.deleteResource(resourceUid)
                            android.util.Log.d("VaultScreen", "✅ 已从数据库删除: $resourceUid")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * 重命名单个文件（新架构：只修改数据库，不移动文件）
 */
private suspend fun renameSingleFile(
    context: Context,
    file: VaultFile,
    newFullName: String,
    dbMiddleware: VaultDatabaseMiddleware? = null
): VaultFile? {
    return try {
        // 提取逻辑名和扩展名
        val extension = newFullName.substringAfterLast('.', "")
        val nameWithoutExt = newFullName.substringBeforeLast('.')
        
        // 更新数据库中的资源记录（只改逻辑名）
        if (dbMiddleware != null) {
            val resourceUid = dbMiddleware.getResourceByPath(file.filePath)
            if (resourceUid != null) {
                dbMiddleware.renameResource(resourceUid, nameWithoutExt)
                android.util.Log.d("VaultScreen", "✅ 重命名成功: $nameWithoutExt.$extension")
            }
        }
        
        // 返回新的VaultFile对象（物理路径不变）
        file.copy(
            originalName = newFullName
        )
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
 * 执行批量重命名（新架构：只修改数据库，不移动文件）
 */
private suspend fun executeBatchRename(
    context: Context,
    files: List<VaultFile>,
    template: String,
    dbMiddleware: VaultDatabaseMiddleware? = null
): List<VaultFile> {
    val renamedFiles = mutableListOf<VaultFile>()
    
    for ((index, file) in files.withIndex()) {
        try {
            val newFullName = applyRenameTemplate(file.originalName, template, index)
            
            // 提取逻辑名和扩展名
            val extension = newFullName.substringAfterLast('.', "")
            val nameWithoutExt = newFullName.substringBeforeLast('.')
            
            // 更新数据库中的资源记录
            if (dbMiddleware != null) {
                val resourceUid = dbMiddleware.getResourceByPath(file.filePath)
                if (resourceUid != null) {
                    dbMiddleware.renameResource(resourceUid, nameWithoutExt)
                    android.util.Log.d("VaultScreen", "✅ 批量重命名: $nameWithoutExt.$extension")
                }
            }
            
            // 创建新的VaultFile对象（物理路径不变）
            renamedFiles.add(
                file.copy(
                    originalName = newFullName
                )
            )
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

/**
 * 胶囊分类标签组件
 */
@Composable
fun CategoryChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val backgroundColor = if (isSelected) {
        colorScheme.primary
    } else {
        colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    
    val contentColor = if (isSelected) {
        colorScheme.onPrimary
    } else {
        colorScheme.onSurface.copy(alpha = 0.7f)
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick?.invoke() }
                )
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = contentColor
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "($count)",
                fontSize = 12.sp,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 保存自定义分类到 SharedPreferences
 */
private fun saveCustomCategories(context: Context, categories: List<String>) {
    val prefs = context.getSharedPreferences("vault_categories", Context.MODE_PRIVATE)
    val editor = prefs.edit()
    editor.putInt("category_count", categories.size)
    categories.forEachIndexed { index, category ->
        editor.putString("category_$index", category)
    }
    editor.apply()
}

/**
 * 从 SharedPreferences 加载自定义分类
 */
private fun loadCustomCategories(context: Context): List<String> {
    val prefs = context.getSharedPreferences("vault_categories", Context.MODE_PRIVATE)
    val count = prefs.getInt("category_count", 0)
    val categories = mutableListOf<String>()
    for (i in 0 until count) {
        prefs.getString("category_$i", null)?.let {
            categories.add(it)
        }
    }
    return categories
}
