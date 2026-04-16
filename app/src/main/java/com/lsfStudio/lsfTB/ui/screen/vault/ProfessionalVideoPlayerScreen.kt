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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.widget.VideoView
import java.io.File
import kotlinx.coroutines.launch

/**
 * 专业横屏视频播放器
 * 
 * 功能特性:
 * - 强制横屏播放
 * - 手势控制：左侧上下滑动调节亮度，右侧上下滑动调节音量，左右滑动快进/快退
 * - 倍速播放：支持 0.5x, 1.0x, 1.25x, 1.5x, 2.0x
 * - 帧预览：拖拽进度条时显示预览帧
 * - 自动隐藏：3秒无操作自动隐藏控制栏
 */
@Composable
fun ProfessionalVideoPlayerScreen(
    filePath: String,
    fileName: String,
    fileId: Long = 0,
    onBack: () -> Unit,
    allFiles: List<VaultFile>? = null,
    currentIndex: Int = 0
) {
    val context = LocalContext.current
    val navigator = com.lsfStudio.lsfTB.ui.navigation3.LocalNavigator.current
    val scope = rememberCoroutineScope()
    
    // UI状态
    var showControls by remember { mutableStateOf(true) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    
    // 文件信息
    var currentFilePath by remember { mutableStateOf(filePath) }
    val displayName = remember(fileName) {
        if (fileName.endsWith(".zip")) fileName.removeSuffix(".zip") else fileName
    }
    
    // MediaPlayer状态
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    
    // 专业控件状态
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var brightness by remember { mutableFloatStateOf(0.5f) }
    var volume by remember { mutableFloatStateOf(1.0f) }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var showSeekIndicator by remember { mutableStateOf(false) }
    var seekOffset by remember { mutableLongStateOf(0L) }
    
    // 进度条状态
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableLongStateOf(0L) }
    var previewFrameBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // 深色模式
    val surfaceColor = Color.Black.copy(alpha = 0.7f)
    
    // 移出文件
    suspend fun moveFileToPublic(fileId: Long, filePath: String, originalName: String) {
        try {
            val targetDir = File("/sdcard/Pictures/lsfTB")
            if (!targetDir.exists()) targetDir.mkdirs()
            
            val targetFile = File(targetDir, originalName)
            val sourceFile = File(filePath)
            if (sourceFile.exists()) {
                sourceFile.copyTo(targetFile, overwrite = true)
                if (targetFile.exists() && targetFile.length() > 0) {
                    sourceFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // 控制函数
    fun togglePlayPause() {
        if (isPlaying) {
            videoViewRef?.pause()
        } else {
            videoViewRef?.start()
        }
        isPlaying = !isPlaying
    }
    
    fun setVolume(newVolume: Float) {
        volume = newVolume.coerceIn(0f, 1f)
        isMuted = volume == 0f
        mediaPlayer?.setVolume(volume, volume)
    }
    
    fun setBrightness(newBrightness: Float) {
        brightness = newBrightness.coerceIn(0f, 1f)
        val activity = context as? ComponentActivity
        activity?.window?.attributes?.let { params ->
            params.screenBrightness = brightness
            activity.window.attributes = params
        }
    }
    
    fun setSpeed(speed: Float) {
        playbackSpeed = speed
        try {
            mediaPlayer?.setPlaybackParams(
                android.media.PlaybackParams().setSpeed(speed)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun seekTo(position: Long) {
        videoViewRef?.seekTo(position.toInt())
        currentPosition = position
    }
    
    fun fastSeek(offsetMs: Long) {
        val newPosition = (currentPosition + offsetMs).coerceIn(0L, duration)
        seekTo(newPosition)
    }
    
    // 提取预览帧
    fun extractFrameAtPosition(positionMs: Long): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(currentFilePath)
            val frame = retriever.getFrameAtTime(
                positionMs * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            retriever.release()
            frame
        } catch (e: Exception) {
            null
        }
    }
    
    // 更新进度
    LaunchedEffect(videoViewRef, isDragging) {
        if (videoViewRef != null && !isDragging) {
            while (true) {
                currentPosition = videoViewRef?.currentPosition?.toLong() ?: 0L
                duration = videoViewRef?.duration?.toLong() ?: 0L
                kotlinx.coroutines.delay(250)
            }
        }
    }
    
    // 自动隐藏控制栏
    LaunchedEffect(showControls) {
        if (showControls) {
            kotlinx.coroutines.delay(3000)
            if (!isDragging && !showSpeedMenu) {
                showControls = false
            }
        }
    }
    
    // 强制横屏
    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        // 初始化亮度
        activity?.window?.attributes?.let { params ->
            if (params.screenBrightness >= 0) {
                brightness = params.screenBrightness
            }
        }
    }
    
    // 退出时恢复
    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? ComponentActivity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }
    
    // 主布局
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 视频播放器
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoPath(currentFilePath)
                    setOnPreparedListener { mp ->
                        mediaPlayer = mp
                        mp.setVolume(volume, volume)
                        mp.start()
                    }
                    setOnCompletionListener { isPlaying = false }
                    start()
                    videoViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 手势检测层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            showControls = true
                            if (offset.x < size.width / 2) {
                                showBrightnessIndicator = true
                                showVolumeIndicator = false
                            } else {
                                showVolumeIndicator = true
                                showBrightnessIndicator = false
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val dragRatio = dragAmount / size.height
                            
                            if (showBrightnessIndicator) {
                                setBrightness(brightness - dragRatio)
                            } else if (showVolumeIndicator) {
                                setVolume(volume - dragRatio)
                            }
                        },
                        onDragEnd = {
                            showBrightnessIndicator = false
                            showVolumeIndicator = false
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            showControls = true
                            showSeekIndicator = true
                            seekOffset = 0L
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            if (duration > 0) {
                                val offsetSeconds = (dragAmount / 10).toLong() * 1000
                                seekOffset += offsetSeconds
                                showSeekIndicator = true
                            }
                        },
                        onDragEnd = {
                            if (seekOffset != 0L) {
                                fastSeek(seekOffset)
                            }
                            showSeekIndicator = false
                            seekOffset = 0L
                        }
                    )
                }
                .clickable(onClick = { showControls = !showControls })
        )
        
        // 顶部控制栏
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceColor)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = Color.White)
                }
                
                Text(
                    text = displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                
                // 退出全屏按钮
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.FullscreenExit, "退出全屏", tint = Color.White)
                }
                
                // 倍速按钮
                Box {
                    IconButton(onClick = { showSpeedMenu = !showSpeedMenu }) {
                        Icon(Icons.Rounded.Speed, "倍速", tint = Color.White)
                        Text(
                            text = "${playbackSpeed}x",
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(top = 20.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false }
                    ) {
                        listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            DropdownMenuItem(
                                text = { Text("${speed}x") },
                                onClick = {
                                    setSpeed(speed)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // 底部控制栏
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(surfaceColor)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 预览帧
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
                
                // 播放控制行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { togglePlayPause() }) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Text(
                        text = "${formatDuration(if (isDragging) dragPosition else currentPosition)} / ${formatDuration(duration)}",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    
                    IconButton(onClick = { 
                        isMuted = !isMuted
                        setVolume(if (isMuted) 0f else volume)
                    }) {
                        Icon(
                            if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            "音量",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // 进度条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    var boxWidth by remember { mutableFloatStateOf(0f) }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { boxWidth = it.width.toFloat() }
                            .pointerInput(boxWidth, duration) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        if (boxWidth > 0 && duration > 0) {
                                            isDragging = true
                                            val fraction = (offset.x / boxWidth).coerceIn(0f, 1f)
                                            dragPosition = (duration * fraction).toLong()
                                            previewFrameBitmap = extractFrameAtPosition(dragPosition)
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        if (boxWidth > 0 && duration > 0) {
                                            val fraction = (change.position.x / boxWidth).coerceIn(0f, 1f)
                                            dragPosition = (duration * fraction).toLong()
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
                            } else 0f
                            
                            drawRoundRect(
                                color = Color(0xFF666666),
                                topLeft = Offset.Zero,
                                size = Size(size.width, 4.dp.toPx()),
                                cornerRadius = CornerRadius(2.dp.toPx())
                            )
                            
                            if (progress > 0) {
                                drawRoundRect(
                                    color = Color(0xFF2196F3),
                                    topLeft = Offset.Zero,
                                    size = Size(size.width * progress, 4.dp.toPx()),
                                    cornerRadius = CornerRadius(2.dp.toPx())
                                )
                            }
                            
                            drawCircle(
                                color = Color(0xFF2196F3),
                                radius = 8.dp.toPx(),
                                center = Offset(size.width * progress, size.height / 2)
                            )
                        }
                    }
                }
                
                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        com.lsfStudio.lsfTB.ui.util.ShareUtil.shareFile(
                            context = context,
                            filePath = currentFilePath,
                            mimeType = "video/*",
                            originalFileName = displayName
                        )
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.Send, "发送", tint = Color.White)
                    }
                    
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Rounded.Delete, "移出", tint = Color(0xFFFF4757))
                    }
                }
            }
        }
        
        // 亮度指示器
        if (showBrightnessIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(120.dp)
                    .background(Color.Black.copy(0.7f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("☀️", fontSize = 32.sp)
                    Text("${(brightness * 100).toInt()}%", color = Color.White, fontSize = 14.sp)
                }
            }
        }
        
        // 音量指示器
        if (showVolumeIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(120.dp)
                    .background(Color.Black.copy(0.7f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isMuted) "🔇" else "🔊", fontSize = 32.sp)
                    Text("${(volume * 100).toInt()}%", color = Color.White, fontSize = 14.sp)
                }
            }
        }
        
        // 快进/快退指示器
        if (showSeekIndicator && seekOffset != 0L) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(120.dp)
                    .background(Color.Black.copy(0.7f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (seekOffset > 0) ">> ${seekOffset / 1000}s" else "<< ${-seekOffset / 1000}s",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // 移出确认对话框
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("确认移出", color = Color.White) },
                text = { Text("确定要将此视频移出保险箱吗？\n文件将移动到 /sdcard/Pictures/lsfTB", color = Color.White) },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            moveFileToPublic(fileId, currentFilePath, displayName)
                            val requestKey = "image_viewer_delete_${fileId}"
                            navigator.setResult<Long>(requestKey, fileId)
                            showDeleteConfirmDialog = false
                            onBack()
                        }
                    }) {
                        Text("移出", color = Color(0xFFFF4757))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("取消", color = Color.White)
                    }
                },
                containerColor = Color(0xFF2A2A2A)
            )
        }
    }
}

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
