package com.lsfStudio.lsfTB.ui.screen.debug

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lsfStudio.lsfTB.ui.component.dialog.ConfirmDialogMiuix
import com.lsfStudio.lsfTB.ui.navigation3.LocalNavigator
import com.lsfStudio.lsfTB.ui.theme.LocalEnableBlur
import com.lsfStudio.lsfTB.ui.util.AccountManager
import com.lsfStudio.lsfTB.ui.util.BlurredBar
import com.lsfStudio.lsfTB.ui.util.DebugShellReceiver
import com.lsfStudio.lsfTB.ui.util.HapticFeedbackUtil
import com.lsfStudio.lsfTB.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * Debug 设置页面
 * 
 * 提供开发者调试选项，仅在通过版本号点击验证后显示
 */
@Composable
fun DebugSettingsScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface
    
    // 对话框状态
    var showDisableConfirmDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = "Debug 设置",
                    navigationIcon = {
                        IconButton(onClick = { 
                            HapticFeedbackUtil.lightClick(context)
                            navigator.pop() 
                        }) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = "返回",
                                tint = colorScheme.onBackground
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        ArrowPreference(
                            title = "禁用开发者模式",
                            summary = "关闭后将隐藏此入口，需重新验证才能开启",
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                showDisableConfirmDialog = true
                            }
                        )
                    }
                    
                    // 退出登录按钮（仅在已登录时显示）
                    if (AccountManager.isLoggedIn(context)) {
                        Card(
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .fillMaxWidth(),
                        ) {
                            ArrowPreference(
                                title = "退出登录",
                                summary = "清除本地用户信息和头像数据",
                                onClick = {
                                    HapticFeedbackUtil.lightClick(context)
                                    showLogoutConfirmDialog = true
                                }
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
    
    // 禁用开发者模式确认对话框
    ConfirmDialogMiuix(
        title = "禁用开发者模式",
        content = "确定要禁用开发者模式吗？\n\n禁用后，Debug 设置入口将隐藏。如需重新启用，需要在「关于」页面连续点击版本号5次并输入正确的设备标识符。",
        confirmText = "禁用",
        dismissText = "取消",
        onConfirm = {
            HapticFeedbackUtil.lightClick(context)
            DebugShellReceiver.disableDevMode(context)
            Toast.makeText(context, "✅ 开发者模式已禁用", Toast.LENGTH_SHORT).show()
            Log.d("DebugSettings", "✅ 开发者模式已禁用")
            showDisableConfirmDialog = false
            // 延迟后返回上一页
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                navigator.pop()
            }, 300)
        },
        onDismiss = {
            showDisableConfirmDialog = false
        },
        showDialog = remember { mutableStateOf(false) }.also { dialogState ->
            dialogState.value = showDisableConfirmDialog
        }
    )
    
    // 退出登录确认对话框
    ConfirmDialogMiuix(
        title = "退出登录",
        content = "确定要退出登录吗？\n\n此操作将清除本地用户信息和头像数据，但不会影响服务器上的账户。",
        confirmText = "退出",
        dismissText = "取消",
        onConfirm = {
            HapticFeedbackUtil.lightClick(context)
            AccountManager.clearUserInfo(context)
            Toast.makeText(context, "✅ 已退出登录", Toast.LENGTH_SHORT).show()
            Log.d("DebugSettings", "✅ 已退出登录")
            showLogoutConfirmDialog = false
        },
        onDismiss = {
            showLogoutConfirmDialog = false
        },
        showDialog = remember { mutableStateOf(false) }.also { dialogState ->
            dialogState.value = showLogoutConfirmDialog
        }
    )
}
