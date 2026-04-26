package com.lsfStudio.lsfTB.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.lsfStudio.lsfTB.ui.component.dialog.ConfirmDialogMiuix
import com.lsfStudio.lsfTB.ui.theme.LocalEnableBlur
import com.lsfStudio.lsfTB.ui.util.BlurredBar
import com.lsfStudio.lsfTB.ui.util.DownloadManager
import com.lsfStudio.lsfTB.ui.util.MessageManager
import com.lsfStudio.lsfTB.ui.util.ShizukuUtil
import com.lsfStudio.lsfTB.ui.util.rememberBlurBackdrop
import com.lsfStudio.lsfTB.ui.util.rememberShizukuConnectionState
import com.lsfStudio.lsfTB.ui.util.HapticFeedbackUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun HomePagerMiuix(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface
    
    // Shizuku 连接状态
    val isShizukuConnected = rememberShizukuConnectionState()
    
    // 🔄 启动时检查更新
    LaunchedEffect(Unit) {
        android.util.Log.d("HomeMiuix", "📱 Home 页面加载，准备检查更新")
        // 延迟 1 秒后检查更新，确保页面已加载
        kotlinx.coroutines.delay(1000)
        actions.onCheckUpdate(context, state.checkUpdateEnabled)
    }

    Scaffold(
        topBar = {
            TopBar(
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                barColor = barColor,
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        // 🔄 更新对话框（发现新版本时显示）
        if (state.latestVersionInfo.versionCode > 0) {
            ConfirmDialogMiuix(
                title = "发现新版本",
                content = if (state.latestVersionInfo.changelog.isNotEmpty()) {
                    "${state.latestVersionInfo.versionName}\n\n${state.latestVersionInfo.changelog}"
                } else {
                    state.latestVersionInfo.versionName
                },
                isMarkdown = true,
                confirmText = "立即下载",
                dismissText = "取消",
                onConfirm = {
                    android.util.Log.d("HomeMiuix", "🚀 用户点击下载新版本: ${state.latestVersionInfo.versionName}")
                    
                    // 清除更新信息
                    actions.onClearLatestVersionInfo()
                    
                    // 启动下载
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val fileName = "lsfTB_${state.latestVersionInfo.versionName.replace(".", "_")}.apk"
                            DownloadManager.download(
                                context = context,
                                url = state.latestVersionInfo.downloadUrl,
                                fileName = fileName,
                                versionName = state.latestVersionInfo.versionName,
                                callback = object : DownloadManager.DownloadCallback {
                                    override fun onProgress(progress: Int, speed: String, remainingTime: String) {
                                        android.util.Log.d("HomeMiuix", "下载进度: $progress% | 速度: $speed | 剩余: $remainingTime")
                                    }
                                    
                                    override fun onSuccess(file: java.io.File) {
                                        android.util.Log.d("HomeMiuix", "✅ 下载成功: ${file.absolutePath}")
                                    }
                                    
                                    override fun onError(error: String) {
                                        android.util.Log.e("HomeMiuix", "❌ 下载失败: $error")
                                    }
                                }
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("HomeMiuix", "❌ 启动下载失败", e)
                        }
                    }
                },
                onDismiss = {
                    android.util.Log.d("HomeMiuix", "用户取消更新")
                    actions.onClearLatestVersionInfo()
                },
                showDialog = remember(state.latestVersionInfo.versionCode) { mutableStateOf(state.latestVersionInfo.versionCode > 0) }.also { dialogState ->
                    LaunchedEffect(state.latestVersionInfo.versionCode) {
                        dialogState.value = state.latestVersionInfo.versionCode > 0
                    }
                }
            )
        }
        LazyColumn(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .overScrollVertical()
                .scrollEndHaptic()
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                
                // Shizuku 连接状态卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧：图标 + 文字
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isShizukuConnected.value) MiuixIcons.Ok else MiuixIcons.Info,
                                contentDescription = if (isShizukuConnected.value) "已连接" else "未连接",
                                modifier = Modifier.size(40.dp),
                                tint = if (isShizukuConnected.value) colorScheme.primary else colorScheme.onSurfaceVariantSummary
                            )
                            Spacer(Modifier.padding(horizontal = 12.dp))
                            Text(
                                text = if (isShizukuConnected.value) "Shizuku 已连接" else "未连接到 Shizuku",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onBackground
                            )
                        }
                        
                        // 右侧：连接/刷新按钮
                        Button(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                if (isShizukuConnected.value) {
                                    // 已连接，点击刷新状态
                                    isShizukuConnected.value = ShizukuUtil.isConnected()
                                } else {
                                    // 未连接，请求权限
                                    if (!ShizukuUtil.isShizukuAvailable()) {
                                        // 显示提示（使用MessageManager自动适配超级岛）
                                        MessageManager.showToast(context, "Shizuku 服务未启动，请先启动 Shizuku", Toast.LENGTH_LONG)
                                    } else {
                                        ShizukuUtil.requestShizukuPermission()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Text(
                                text = if (isShizukuConnected.value) "刷新" else "连接",
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                // 系统信息卡片
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        InfoText(
                            title = "应用名称",
                            content = state.appName
                        )
                        InfoText(
                            title = "应用版本",
                            content = state.appVersion
                        )
                        InfoText(
                            title = "构建类型",
                            content = com.lsfStudio.lsfTB.ui.util.DebugShellReceiver.getBuildType(context).replaceFirstChar { it.uppercase() }
                        )
                        InfoText(
                            title = "构建时间",
                            content = com.lsfStudio.lsfTB.ui.util.DebugShellReceiver.getBuildTime(context),
                            bottomPadding = 0.dp
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                // 关于卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = actions.onOpenAbout
                ) {
                    BasicComponent(
                        title = "关于应用",
                        summary = "查看应用信息和版本详情",
                        startAction = {
                            Icon(
                                MiuixIcons.Info,
                                "关于",
                                modifier = Modifier.padding(end = 16.dp),
                                tint = colorScheme.onBackground,
                            )
                        }
                    )
                }
                
                Spacer(Modifier.height(bottomInnerPadding))
            }
        }
    }
}

@Composable
private fun InfoText(
    title: String,
    content: String,
    bottomPadding: Dp = 16.dp
) {
    Text(
        text = title,
        fontSize = MiuixTheme.textStyles.headline1.fontSize,
        fontWeight = FontWeight.Medium,
        color = colorScheme.onSurface
    )
    Text(
        text = content,
        fontSize = MiuixTheme.textStyles.body2.fontSize,
        color = colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(top = 2.dp, bottom = bottomPadding)
    )
}

@Composable
private fun TopBar(
    scrollBehavior: ScrollBehavior,
    backdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop?,
    barColor: Color,
) {
    BlurredBar(backdrop) {
        TopAppBar(
            color = barColor,
            title = "lsfTB",
            scrollBehavior = scrollBehavior
        )
    }
}
