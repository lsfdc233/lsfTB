@file:OptIn(UnstableApi::class)

package com.lsfStudio.lsfTB.ui.component.video

import android.net.Uri
import android.util.Log
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
    showControls: Boolean = true,
    onPlayerReady: ((ExoPlayer) -> Unit)? = null
) {
    val context = LocalContext.current
    
    // 创建 ExoPlayer 实例
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = autoPlay
            
            // 添加监听器用于调试
            addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("VideoPlayer", "播放错误: ${error.message}", error)
                    Log.e("VideoPlayer", "视频URI: $videoUri")
                    Log.e("VideoPlayer", "错误代码: ${error.errorCode}")
                }
                
                override fun onPlaybackStateChanged(state: Int) {
                    Log.d("VideoPlayer", "播放状态变化: $state")
                    when (state) {
                        Player.STATE_READY -> Log.d("VideoPlayer", "播放器已就绪")
                        Player.STATE_BUFFERING -> Log.d("VideoPlayer", "缓冲中...")
                        Player.STATE_ENDED -> Log.d("VideoPlayer", "播放结束")
                        Player.STATE_IDLE -> Log.d("VideoPlayer", "空闲状态")
                    }
                }
            })
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
                useController = showControls // 是否显示播放控制器
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
    showControls: Boolean = true,
    onPlayerReady: ((ExoPlayer) -> Unit)? = null
) {
    val context = LocalContext.current
    val file = java.io.File(videoPath)
    
    // 检查文件是否存在
    if (!file.exists()) {
        Log.e("VideoPlayer", "视频文件不存在: $videoPath")
        // 显示错误提示
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = "视频文件不存在\n可能已被删除或重命名",
                color = androidx.compose.ui.graphics.Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }
    
    val uri = remember(videoPath) {
        Uri.fromFile(file)
    }
    
    Log.d("VideoPlayer", "准备播放视频: $videoPath")
    
    VideoPlayer(
        videoUri = uri,
        autoPlay = autoPlay,
        showControls = showControls,
        onPlayerReady = onPlayerReady
    )
}
