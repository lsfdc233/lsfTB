package com.lsfStudio.lsfTB.ui.screen.vault

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lsfStudio.lsfTB.data.model.VaultFile
import com.lsfStudio.lsfTB.ui.theme.isInDarkTheme
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.widget.VideoView
import java.io.File
import kotlinx.coroutines.launch

/**
 * 视频播放器页面 - 全屏沉浸式
 * 
 * 布局:
 * - 顶部栏: 返回按钮 + 文件名 + 更多操作（复制自ImageViewerScreen）
 * - 底部控制栏上方: 播放/暂停 + 静音按钮
 * - 底部栏: 发送、删除
 */
@Composable
fun VideoPlayerScreen(
    filePath: String,
    fileName: String,
    fileId: Long = 0,
    onBack: () -> Unit,
    onDelete: () -> Unit = {},
    onFavorite: () -> Unit = {},
    allFiles: List<VaultFile>? = null,
    currentIndex: Int = 0
) {
    val context = LocalContext.current
    val navigator = com.lsfStudio.lsfTB.ui.navigation3.LocalNavigator.current
    val scope = rememberCoroutineScope()
    var showControls by remember { mutableStateOf(true) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    // 当前文件信息
    var currentFilePath by remember { mutableStateOf(filePath) }
    var currentFileName by remember { mutableStateOf(fileName) }
    var actualCurrentIndex by remember { mutableIntStateOf(currentIndex) }
    
    // 获取当前是否为深色模式
    val isDarkTheme = isInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    val surfaceColor = if (isDarkTheme) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)
    
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
    
    // MediaPlayer 状态
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableLongStateOf(0L) }
    var previewFrameBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // 播放/暂停控制
    fun togglePlayPause() {
        if (isPlaying) {
            videoViewRef?.pause()
        } else {
            videoViewRef?.start()
        }
    }
    
    // 静音控制
    fun toggleMute() {
        isMuted = !isMuted
        mediaPlayer?.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
    }
    
    // 跳转进度
    fun seekTo(position: Long) {
        videoViewRef?.seekTo(position.toInt())
        currentPosition = position
    }
    
    // 提取指定位置的帧
    fun extractFrameAtPosition(positionMs: Long): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(currentFilePath)
            // 使用接近关键帧的时间获取帧
            val frame = retriever.getFrameAtTime(
                positionMs * 1000, // 转换为微秒
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            retriever.release()
            frame
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // 更新播放进度
    LaunchedEffect(videoViewRef, isDragging) {
        if (videoViewRef != null && !isDragging) {
            while (true) {
                currentPosition = videoViewRef?.currentPosition?.toLong() ?: 0L
                duration = videoViewRef?.duration?.toLong() ?: 0L
                kotlinx.coroutines.delay(250)
            }
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
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 视频播放器（使用Android VideoView）
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoPath(currentFilePath)
                    setOnPreparedListener { mp ->
                        mediaPlayer = mp
                        mp.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
                        mp.isLooping = false
                        mp.start()
                    }
                    setOnCompletionListener {
                        isPlaying = false
                    }
                    start()
                    videoViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { videoView ->
                // 可以在这里更新播放状态
            }
        )
        
        // 点击视频区域切换控制栏显示
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = { showControls = !showControls })
        )
        
        // 顶部控制栏（返回 + 文件名 + 更多）
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
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 返回按钮
                    IconButton(
                        onClick = {
                            showControls = false
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
                    
                    // 更多按钮
                    IconButton(
                        onClick = {
                            com.lsfStudio.lsfTB.ui.util.HapticFeedbackUtil.lightClick(context)
                            // 跳转到横屏专业播放器
                            navigator.push(
                                com.lsfStudio.lsfTB.ui.navigation3.Route.ProfessionalVideoPlayer(
                                    filePath = currentFilePath,
                                    fileName = currentFileName,
                                    fileId = fileId,
                                    allFilePaths = allFiles?.map { it.filePath } ?: emptyList(),
                                    allFileNames = allFiles?.map { it.originalName } ?: emptyList(),
                                    allAddedTimes = allFiles?.map { it.addedTime } ?: emptyList(),
                                    currentIndex = actualCurrentIndex
                                )
                            )
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Fullscreen,
                            "全屏播放",
                            tint = if (isDarkTheme) Color.White else Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
        
        // 播放/暂停 + 静音按钮（在底部控制栏上方）
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                ) {
                    // 拖拽位置帧预览（拖拽时显示）
                    if (isDragging && previewFrameBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = previewFrameBitmap!!.asImageBitmap(),
                                contentDescription = "预览帧",
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(68.dp)
                                    .background(Color.Black, RoundedCornerShape(4.dp))
                            )
                        }
                    }
                    
                    // 播放/暂停 + 时间进度 + 静音按钮
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceColor)
                            .padding(vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 播放/暂停按钮
                            IconButton(
                                onClick = {
                                    com.lsfStudio.lsfTB.ui.util.HapticFeedbackUtil.lightClick(context)
                                    togglePlayPause()
                                    isPlaying = !isPlaying
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "暂停" else "播放",
                                    tint = if (isDarkTheme) Color.White else Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            // 时间进度显示（中间）
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatDuration(if (isDragging) dragPosition else currentPosition),
                                    fontSize = 12.sp,
                                    color = if (isDarkTheme) Color(0xFFBBBBBB) else Color(0xFF666666)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "/",
                                    fontSize = 12.sp,
                                    color = if (isDarkTheme) Color(0xFF999999) else Color(0xFF999999)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatDuration(duration),
                                    fontSize = 12.sp,
                                    color = if (isDarkTheme) Color(0xFFBBBBBB) else Color(0xFF666666)
                                )
                            }
                            
                            // 静音按钮
                            IconButton(
                                onClick = {
                                    com.lsfStudio.lsfTB.ui.util.HapticFeedbackUtil.lightClick(context)
                                    toggleMute()
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = if (isMuted) "取消静音" else "静音",
                                    tint = if (isDarkTheme) Color.White else Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    
                    // 可拖拽进度条
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    ) {
                        var boxWidth by remember { mutableFloatStateOf(0f) }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { it ->
                                    boxWidth = it.width.toFloat()
                                }
                                .pointerInput(boxWidth, duration) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            if (boxWidth > 0 && duration > 0) {
                                                isDragging = true
                                                val fraction = (offset.x / boxWidth).coerceIn(0f, 1f)
                                                dragPosition = (duration * fraction).toLong()
                                                // 提取拖拽位置的预览帧
                                                previewFrameBitmap = extractFrameAtPosition(dragPosition)
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            if (boxWidth > 0 && duration > 0) {
                                                val fraction = (change.position.x / boxWidth).coerceIn(0f, 1f)
                                                dragPosition = (duration * fraction).toLong()
                                                // 更新预览帧（每秒更新一次以避免频繁提取）
                                                if (dragPosition % 1000 < 250) {
                                                    previewFrameBitmap = extractFrameAtPosition(dragPosition)
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                            previewFrameBitmap = null
                                            seekTo(dragPosition)
                                        }
                                    )
                                }
                        ) {
                            // 进度条背景
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .align(Alignment.Center)
                            ) {
                                val progress = if (duration > 0 && !isDragging) {
                                    currentPosition.toFloat() / duration
                                } else if (duration > 0 && isDragging) {
                                    dragPosition.toFloat() / duration
                                } else {
                                    0f
                                }
                                
                                // 背景轨道
                                drawRoundRect(
                                    color = if (isDarkTheme) Color(0xFF666666) else Color(0xFFCCCCCC),
                                    topLeft = Offset.Zero,
                                    size = Size(size.width, 4.dp.toPx()),
                                    cornerRadius = CornerRadius(2.dp.toPx())
                                )
                                
                                // 已播放进度
                                if (progress > 0) {
                                    drawRoundRect(
                                        color = if (isDarkTheme) Color(0xFF3F51B5) else Color(0xFF2196F3),
                                        topLeft = Offset.Zero,
                                        size = Size(size.width * progress, 4.dp.toPx()),
                                        cornerRadius = CornerRadius(2.dp.toPx())
                                    )
                                }
                                
                                // 进度点
                                val dotX = size.width * progress
                                drawCircle(
                                    color = if (isDarkTheme) Color(0xFF3F51B5) else Color(0xFF2196F3),
                                    radius = 8.dp.toPx(),
                                    center = Offset(dotX, size.height / 2)
                                )
                            }
                        }
                    }
                    
                    // 发送和删除按钮
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceColor)
                            .padding(vertical = 4.dp, horizontal = 4.dp)
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
                                    val mimeType = "video/*"
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
        }
        
        // 移出确认对话框
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmDialog = false
                },
                title = { Text("确认移出") },
                text = { Text("确定要将此视频移出保险箱吗？\n文件将移动到 /sdcard/Pictures/lsfTB") },
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
    dangerColor: Color? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (dangerColor != null) dangerColor else if (isDarkTheme) Color.White else Color.Black,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (dangerColor != null) dangerColor else if (isDarkTheme) Color.White else Color.Black
        )
    }
}

/**
 * 格式化时间长度为 mm:ss 或 h:mm:ss 格式
 */
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
