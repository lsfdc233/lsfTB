@file:OptIn(UnstableApi::class)

package com.lsfStudio.lsfTB.ui.component.video

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * 视频播放器组件
 * 
 * @param videoUri 视频文件URI
 * @param autoPlay 是否自动播放
 * @param onPlayerReady 播放器准备完成回调
 */
@Composable
fun VideoPlayer(
    videoUri: Uri,
    autoPlay: Boolean = true,
    onPlayerReady: ((ExoPlayer) -> Unit)? = null
) {
    val context = LocalContext.current
    
    // 创建 ExoPlayer 实例
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = autoPlay
        }
    }
    
    // 通知调用方播放器已准备好
    onPlayerReady?.invoke(exoPlayer)
    
    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    // 显示播放器
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true // 显示播放控制器
                setShowNextButton(false)
                setShowPreviousButton(false)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * 视频播放器组件（使用文件路径）
 * 
 * @param videoPath 视频文件路径
 * @param autoPlay 是否自动播放
 * @param onPlayerReady 播放器准备完成回调
 */
@Composable
fun VideoPlayerFromPath(
    videoPath: String,
    autoPlay: Boolean = true,
    onPlayerReady: ((ExoPlayer) -> Unit)? = null
) {
    val uri = remember(videoPath) {
        Uri.fromFile(java.io.File(videoPath))
    }
    
    VideoPlayer(
        videoUri = uri,
        autoPlay = autoPlay,
        onPlayerReady = onPlayerReady
    )
}
