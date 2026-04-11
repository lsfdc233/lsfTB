package com.lsfStudio.lsfTB.ui.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lsfStudio.lsfTB.R
import com.lsfStudio.lsfTB.ui.component.dialog.ConfirmDialogMiuix
import com.lsfStudio.lsfTB.ui.theme.LocalEnableBlur
import com.lsfStudio.lsfTB.ui.util.BlurredBar
import com.lsfStudio.lsfTB.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.miuixShape
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun AboutScreenMiuix(
    state: AboutUiState,
    actions: AboutScreenActions,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = state.title,
                    navigationIcon = {
                        IconButton(
                            onClick = actions.onBack
                        ) {
                            val layoutDirection = LocalLayoutDirection.current
                            Icon(
                                modifier = Modifier.graphicsLayer {
                                    if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                                },
                                imageVector = MiuixIcons.Back,
                                contentDescription = null,
                                tint = colorScheme.onBackground
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        // 更新对话框（使用 KernelSU 风格的确认对话框）
        ConfirmDialogMiuix(
            title = "发现新版本",
            content = if (state.latestVersionInfo.changelog.isNotEmpty()) {
                "${state.latestVersionInfo.versionName}\n\n${state.latestVersionInfo.changelog}"
            } else {
                state.latestVersionInfo.versionName
            },
            confirmText = "前往下载",
            dismissText = "取消",
            onConfirm = {
                // 使用内部下载器
                actions.onStartDownload()
            },
            onDismiss = {
                actions.onDismissUpdateDialog()
            },
            showDialog = remember { mutableStateOf(false) }.also { dialogState ->
                androidx.compose.runtime.LaunchedEffect(state.latestVersionInfo) {
                    if (state.latestVersionInfo.versionCode > 0) {
                        dialogState.value = true
                    }
                }
            }
        )
        
        // “已是最新版本”对话框
        if (state.showUpToDateDialog) {
            val currentChangelog = state.latestVersionInfo.currentVersionChangelog
            val content = buildString {
                append("当前版本：${state.versionName}")
                append("\n\n您已使用最新版本，无需更新。")
                
                if (currentChangelog.isNotEmpty()) {
                    append("\n\n当前版本更新日志：\n")
                    append(currentChangelog)
                }
            }
            
            ConfirmDialogMiuix(
                title = "已是最新版本",
                content = content,
                confirmText = "确定",
                dismissText = null, // 不显示取消按钮
                onConfirm = {
                    actions.onDismissUpToDateDialog()
                },
                onDismiss = {
                    actions.onDismissUpToDateDialog()
                },
                showDialog = remember { mutableStateOf(true) }
            )
        }
        
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null
                // 性能优化：减少预加载项数，降低内存占用和重组次数
                // beyondBoundsItemCount = 0  // 默认已经是最优值
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(miuixShape(16.dp))
                                .background(Color.White)
                        ) {
                            Image(
                                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                                contentDescription = null,
                                contentScale = FixedScale(1f)
                            )
                        }
                        Text(
                            modifier = Modifier.padding(top = 12.dp),
                            text = state.appName,
                            fontWeight = FontWeight.Medium,
                            fontSize = 26.sp
                        )
                        Text(
                            text = state.versionName,
                            fontSize = 14.sp
                        )
                    }
                }
                item {
                    Card(
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        ArrowPreference(
                            title = "立即检查更新",
                            summary = if (state.isCheckingUpdate) "检查中..." else "点击检查是否有新版本",
                            startAction = {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = "立即检查更新",
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onCheckUpdate,
                            enabled = !state.isCheckingUpdate
                        )
                        
                        state.links.forEach {
                            ArrowPreference(
                                title = it.fullText,
                                onClick = {
                                    actions.onOpenLink(it.url)
                                }
                            )
                        }
                    }
                    Spacer(
                        Modifier.height(
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                    WindowInsets.captionBar.asPaddingValues().calculateBottomPadding()
                        )
                    )
                }
            }
        }
    }
}
