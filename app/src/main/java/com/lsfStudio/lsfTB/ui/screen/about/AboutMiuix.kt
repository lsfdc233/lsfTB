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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
    
    // 对话框状态
    var showUpdateDialog by remember { mutableStateOf(false) }
    
    // 当检查到新版本时显示对话框
    androidx.compose.runtime.LaunchedEffect(state.latestVersionInfo) {
        if (state.latestVersionInfo.versionCode > 0) {
            showUpdateDialog = true
        }
    }

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
        // 更新对话框
        if (showUpdateDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showUpdateDialog = false
                    // 关闭对话框后清除状态
                    actions.onCheckUpdate // 这个不会触发，只是占位
                },
                title = { Text("发现新版本") },
                text = {
                    Column {
                        Text("${state.latestVersionInfo.versionName}")
                        if (state.latestVersionInfo.changelog.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(state.latestVersionInfo.changelog.take(200))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showUpdateDialog = false
                        if (state.latestVersionInfo.downloadUrl.isNotEmpty()) {
                            actions.onOpenLink(state.latestVersionInfo.downloadUrl)
                        }
                    }) {
                        Text("前往下载")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
        
        // “已是最新版本”对话框
        if (state.showUpToDateDialog) {
            AlertDialog(
                onDismissRequest = { actions.onDismissUpToDateDialog() },
                title = { Text("已是最新版本") },
                text = { Text("当前版本：${state.versionName}") },
                confirmButton = {
                    TextButton(onClick = { actions.onDismissUpToDateDialog() }) {
                        Text("确定")
                    }
                }
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
                overscrollEffect = null,
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
