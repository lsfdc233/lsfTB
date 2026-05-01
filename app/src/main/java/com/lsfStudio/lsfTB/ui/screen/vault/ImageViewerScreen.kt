package com.lsfStudio.lsfTB.ui.screen.vault

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.lsfStudio.lsfTB.data.model.VaultFile
import com.lsfStudio.lsfTB.data.model.FileType
import com.lsfStudio.lsfTB.ui.component.video.VideoPlayerFromPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import com.lsfStudio.lsfTB.ui.theme.isInDarkTheme
import java.io.File

/**
 * 图片/视频查看器页面 - 全屏沉浸式
 * 
 * 布局:
 * - 顶部栏: 返回按钮 + 文件名 + 更多操作
 * - 中间: 图片/视频内容(支持缩放和左右滑动切换)
 * - 底部栏: 发送、编辑、收藏、删除、更多
 */
@Composable
fun ImageViewerScreen(
    filePath: String,
    fileName: String,
    addedTime: Long,
    fileId: Long = 0,  // 添加文件ID参数
    onBack: () -> Unit,
    onDelete: () -> Unit = {},
    onFavorite: () -> Unit = {},
    onEdit: () -> Unit = {},
    onShare: () -> Unit = {},
    // 新增：支持文件列表和当前索引，用于左右滑动切换
    allFiles: List<VaultFile>? = null,
    currentIndex: Int = 0,
    onFileChanged: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val navigator = com.lsfStudio.lsfTB.ui.navigation3.LocalNavigator.current
    val scope = rememberCoroutineScope()
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    // 当前显示的文件信息（支持切换）
    var actualCurrentIndex by remember { mutableIntStateOf(currentIndex) }
    var currentFilePath by remember { mutableStateOf(filePath) }
    var currentFileName by remember { mutableStateOf(fileName) }
    var currentAddedTime by remember { mutableStateOf(addedTime) }
    var swipeOffsetX by remember { mutableFloatStateOf(0f) } // 用于左右滑动的偏移量
    
    // ✅ 解密后的临时文件路径（用于预览）
    var decryptedTempFilePath by remember { mutableStateOf<String?>(null) }
    
    // ✅ 加载时解密文件
    LaunchedEffect(currentFilePath, currentFileName) {
        // 清理之前的临时文件
        decryptedTempFilePath?.let { oldPath ->
            File(oldPath).delete()
        }
        
        // 解密当前文件到临时位置
        val tempFile = com.lsfStudio.lsfTB.ui.util.VaultEncryptionManager.decryptToTempFile(
            context = context,
            encryptedFilePath = currentFilePath,
            originalFileName = currentFileName
        )
        
        decryptedTempFilePath = tempFile?.absolutePath
    }
    
    // 屏幕宽度和速度追踪
    var screenWidth by remember { mutableFloatStateOf(0f) }
    var lastSwipeTime by remember { mutableLongStateOf(0L) }
    var lastSwipePosition by remember { mutableFloatStateOf(0f) }
    var swipeVelocity by remember { mutableFloatStateOf(0f) }
    
    // 获取当前是否为深色模式
    val isDarkTheme = isInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    val surfaceColor = if (isDarkTheme) Color.Black else Color.White // 纯色，不带灰度
    
    // 获取不带.zip后缀的文件名
    val displayName = remember(currentFileName) {
        if (currentFileName.endsWith(".zip")) {
            currentFileName.removeSuffix(".zip")
        } else {
            currentFileName
        }
    }
    
    // 移出文件到公共目录
    suspend fun moveFileToPublic(fileId: Long, filePath: String, originalName: String) {
        try {
            // 创建目标目录
            val targetDir = File("/sdcard/Pictures/lsfTB")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            
            // 目标文件路径
            val targetFile = File(targetDir, originalName)
            
            // 复制文件内容
            val sourceFile = File(filePath)
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
    
    // 控制系统状态栏和导航栏显示/隐藏
    LaunchedEffect(showControls) {
        val activity = context as? ComponentActivity
        activity?.window?.let { window ->
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            
            if (showControls) {
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.show(WindowInsetsCompat.Type.navigationBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.statusBars())
                controller.hide(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }
    
    // 页面退出时恢复系统栏显示
    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? ComponentActivity
            activity?.window?.let { window ->
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .onGloballyPositioned { coordinates ->
                screenWidth = coordinates.size.width.toFloat()
            }
    ) {
        // 图片/视频内容层 - 卷轴式滑动
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                        // 缩放或移动时隐藏控制栏
                        if (showControls && (zoom != 1f || pan.x != 0f || pan.y != 0f)) {
                            showControls = false
                        }
                    }
                }
                .pointerInput(allFiles, actualCurrentIndex, scale) {
                    // 只有在有文件列表且未缩放时才允许左右滑动
                    if (allFiles != null && allFiles.size > 1 && scale == 1f) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val currentTime = System.currentTimeMillis()
                                val timeDelta = currentTime - lastSwipeTime
                                
                                // 计算滑动速度（像素/毫秒）
                                if (timeDelta > 0) {
                                    swipeVelocity = (swipeOffsetX - lastSwipePosition) / timeDelta.toFloat()
                                }
                                
                                // 判断是否切换：基于位置或速度
                                val halfWidth = screenWidth / 2f
                                val velocityThreshold = 0.5f // 像素/毫秒
                                
                                val shouldSwitchPrev = swipeOffsetX > halfWidth || 
                                    (swipeOffsetX > 0 && swipeVelocity > velocityThreshold)
                                val shouldSwitchNext = swipeOffsetX < -halfWidth || 
                                    (swipeOffsetX < 0 && swipeVelocity < -velocityThreshold)
                                
                                if (shouldSwitchPrev && actualCurrentIndex > 0) {
                                    // 切换到上一张
                                    val newIndex = actualCurrentIndex - 1
                                    onFileChanged?.invoke(newIndex)
                                    allFiles.getOrNull(newIndex)?.let { prevFile ->
                                        currentFilePath = prevFile.filePath
                                        currentFileName = prevFile.originalName
                                        currentAddedTime = prevFile.addedTime
                                        actualCurrentIndex = newIndex
                                        // ✅ 新文件会在 LaunchedEffect 中自动解密
                                    }
                                    // 根据速度决定动画时长
                                    val duration = if (kotlin.math.abs(swipeVelocity) > 0.3f) 150 else 300
                                } else if (shouldSwitchNext && actualCurrentIndex < allFiles.size - 1) {
                                    // 切换到下一张
                                    val newIndex = actualCurrentIndex + 1
                                    onFileChanged?.invoke(newIndex)
                                    allFiles.getOrNull(newIndex)?.let { nextFile ->
                                        currentFilePath = nextFile.filePath
                                        currentFileName = nextFile.originalName
                                        currentAddedTime = nextFile.addedTime
                                        actualCurrentIndex = newIndex
                                        // ✅ 新文件会在 LaunchedEffect 中自动解密
                                    }
                                    // 根据速度决定动画时长
                                    val duration = if (kotlin.math.abs(swipeVelocity) > 0.3f) 150 else 300
                                } else {
                                    // 回弹动画
                                    val duration = 250
                                }
                                
                                // 重置偏移量
                                swipeOffsetX = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                lastSwipeTime = System.currentTimeMillis()
                                lastSwipePosition = swipeOffsetX
                                swipeOffsetX += dragAmount
                            }
                        )
                    }
                }
                .clickable(onClick = { showControls = !showControls })
        ) {
            // 获取当前文件类型
            val currentFileType = allFiles?.getOrNull(actualCurrentIndex)?.fileType
            
            if (currentFileType == FileType.VIDEO) {
                // 视频：使用VideoPlayer（需要解密）
                val decryptedVideoPath = decryptedTempFilePath ?: currentFilePath
                VideoPlayerFromPath(
                    videoPath = decryptedVideoPath,
                    autoPlay = true
                )
            } else {
                // 图片：使用AsyncImage（需要解密）
                val decryptedImagePath = decryptedTempFilePath ?: currentFilePath
                AsyncImage(
                    model = File(decryptedImagePath),
                    contentDescription = currentFileName,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX + swipeOffsetX
                            translationY = offsetY
                        },
                    contentScale = ContentScale.Fit
                )
                
                // 向右滑动时显示上一张（在左侧）
                if (swipeOffsetX > 0 && actualCurrentIndex > 0) {
                    allFiles?.getOrNull(actualCurrentIndex - 1)?.let { prevFile ->
                        if (prevFile.fileType == FileType.IMAGE) {
                            AsyncImage(
                                model = File(prevFile.filePath),
                                contentDescription = prevFile.originalName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationX = swipeOffsetX - size.width
                                    },
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
                
                // 向左滑动时显示下一张（在右侧）
                if (swipeOffsetX < 0 && actualCurrentIndex < (allFiles?.size ?: 0) - 1) {
                    allFiles?.getOrNull(actualCurrentIndex + 1)?.let { nextFile ->
                        if (nextFile.fileType == FileType.IMAGE) {
                            AsyncImage(
                                model = File(nextFile.filePath),
                                contentDescription = nextFile.originalName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationX = swipeOffsetX + size.width
                                    },
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
        
        // 顶部控制栏 - 使用与底部相同的背景色逻辑
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceColor)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp) // 进一步压缩
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 返回按钮 - 优化响应速度
                    IconButton(
                        onClick = {
                            // 直接返回，由 DisposableEffect 恢复系统栏
                            onBack()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            "返回",
                            tint = if (isDarkTheme) Color.White else Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 文件名
                    Column {
                        Text(
                            text = displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // 旋转按钮
                    IconButton(
                        onClick = { /* TODO: 旋转图片 */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.RotateRight,
                            "旋转",
                            tint = if (isDarkTheme) Color.White else Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    // 更多按钮
                    IconButton(
                        onClick = { /* TODO: 更多选项 */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            "更多",
                            tint = if (isDarkTheme) Color.White else Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
        
        // 底部控制栏 - 使用Box包裹AnimatedVisibility以正确定位
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceColor)
                        .navigationBarsPadding()
                        .padding(vertical = 4.dp, horizontal = 4.dp) // 进一步压缩
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 发送
                        BottomActionItem(
                            icon = Icons.AutoMirrored.Rounded.Send,
                            label = "发送",
                            onClick = {
                                // 获取当前文件类型和MIME类型
                                val currentFileType = allFiles?.getOrNull(actualCurrentIndex)?.fileType
                                val mimeType = if (currentFileType == FileType.VIDEO) "video/*" else "image/*"
                                
                                // 分享文件（不带.zip后缀）
                                com.lsfStudio.lsfTB.ui.util.ShareUtil.shareFile(
                                    context = context,
                                    filePath = currentFilePath,
                                    mimeType = mimeType,
                                    originalFileName = displayName
                                )
                            },
                            isDarkTheme = isDarkTheme
                        )
                        
                        // 移出
                        BottomActionItem(
                            icon = Icons.Rounded.Delete,
                            label = "移出",
                            onClick = {
                                showDeleteConfirmDialog = true
                            },
                            isDarkTheme = isDarkTheme,
                            dangerColor = Color(0xFFFF4757)
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
                },
                title = { Text("确认移出") },
                text = { Text("确定要将此文件移出保险箱吗？\n文件将移动到 /sdcard/Pictures/lsfTB") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            com.lsfStudio.lsfTB.ui.util.HapticFeedbackUtil.lightClick(context)
                            // 执行移出操作
                            scope.launch {
                                moveFileToPublic(fileId, currentFilePath, displayName)
                                
                                // 通知VaultScreen刷新列表
                                val requestKey = "image_viewer_delete_${fileId}"
                                navigator.setResult<Long>(requestKey, fileId)
                                
                                showDeleteConfirmDialog = false
                                onBack()
                            }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFFF4757)
                        )
                    ) {
                        Text("移出")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            com.lsfStudio.lsfTB.ui.util.HapticFeedbackUtil.lightClick(context)
                            showDeleteConfirmDialog = false
                        }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

/**
 * 底部操作项
 */
@Composable
private fun BottomActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    activeColor: Color? = null,
    dangerColor: Color? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = when {
                dangerColor != null -> dangerColor
                activeColor != null -> activeColor
                isDarkTheme -> Color.White
                else -> Color.Black
            },
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = when {
                dangerColor != null -> dangerColor
                activeColor != null -> activeColor
                isDarkTheme -> Color.White
                else -> Color.Black
            }
        )
    }
}

/**
 * 重命名查看器中的文件
 */
private fun renameFileInViewer(
    context: android.content.Context,
    oldFilePath: String,
    oldDisplayName: String,
    newFullName: String
): Boolean {
    return try {
        val vaultDir = java.io.File(context.filesDir, "vault")
        
        // 新文件名（添加.zip后缀）
        val newEncryptedName = "$newFullName.zip"
        val newFilePath = java.io.File(vaultDir, newEncryptedName).absolutePath
        
        // 重命名文件
        val oldFile = java.io.File(oldFilePath)
        val newFile = java.io.File(newFilePath)
        if (oldFile.exists()) {
            oldFile.renameTo(newFile)
        } else {
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
