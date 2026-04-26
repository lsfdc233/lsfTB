package com.lsfStudio.lsfTB.ui.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.LaunchedEffect
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
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.lsfStudio.lsfTB.R
import com.lsfStudio.lsfTB.ui.component.dialog.ConfirmDialogMiuix
import com.lsfStudio.lsfTB.ui.theme.LocalEnableBlur
import com.lsfStudio.lsfTB.ui.util.BlurredBar
import com.lsfStudio.lsfTB.ui.util.DataBase
import com.lsfStudio.lsfTB.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.miuixShape
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

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
    
    // 连续点击计数器
    var clickCount by remember { mutableStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }
    
    // Debug 弹窗状态
    var showDebugDialog by remember { mutableStateOf(false) }
    var debugDialogClickTime by remember { mutableStateOf(0L) }  // 记录弹窗打开时间

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
            isMarkdown = true, // 启用 Markdown 支持
            confirmText = "立即下载",
            dismissText = "取消",
            onConfirm = {
                // 使用内部下载器
                actions.onStartDownload()
            },
            onDismiss = {
                actions.onDismissUpdateDialog()
            },
            showDialog = remember { mutableStateOf(false) }.also { dialogState ->
                LaunchedEffect(state.latestVersionInfo) {
                    if (state.latestVersionInfo.versionCode > 0) {
                        dialogState.value = true
                    }
                }
            }
        )
        
        // “已是最新版本”对话框
        if (state.showUpToDateDialog) {
            val currentChangelog = state.latestVersionInfo.currentVersionChangelog
            Log.d("AboutMiuix", "currentChangelog 长度: ${currentChangelog.length}")
            Log.d("AboutMiuix", "currentChangelog 内容: $currentChangelog")
            
            val content = buildString {
                append("当前版本：${state.versionName}")
                append("\n\n您已使用最新版本，无需更新。")
                
                if (currentChangelog.isNotEmpty()) {
                    append("\n\n当前版本更新日志：\n")
                    append(currentChangelog)
                } else {
                    Log.w("AboutMiuix", "currentChangelog 为空！")
                }
            }
            
            Log.d("AboutMiuix", "最终 content 长度: ${content.length}")
            
            ConfirmDialogMiuix(
                title = "已是最新版本",
                content = content,
                isMarkdown = true, // 启用 Markdown 支持
                confirmText = "确定",
                dismissText = null, // 不显示取消按钮
                onConfirm = {
                    actions.onDismissUpToDateDialog()
                },
                onDismiss = {
                    actions.onDismissUpToDateDialog()
                },
                showDialog = remember(state.showUpToDateDialog) { mutableStateOf(state.showUpToDateDialog) }.also { dialogState ->
                    LaunchedEffect(state.showUpToDateDialog) {
                        dialogState.value = state.showUpToDateDialog
                    }
                }
            )
        }
        
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .overScrollVertical()
                    .scrollEndHaptic()
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
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable {
                                    val currentTime = System.currentTimeMillis()
                                    
                                    // 如果距离上次点击超过2秒，重置计数
                                    if (currentTime - lastClickTime > 2000) {
                                        clickCount = 0
                                    }
                                    
                                    clickCount++
                                    lastClickTime = currentTime
                                    
                                    Log.d("AboutMiuix", "版本号点击次数: $clickCount")
                                    
                                    // 连续点击5次触发 Debug 弹窗
                                    if (clickCount >= 5) {
                                        showDebugDialog = true
                                        debugDialogClickTime = System.currentTimeMillis()  // 记录打开时间
                                        clickCount = 0
                                        Log.d("AboutMiuix", "🔧 触发 Debug 模式")
                                    }
                                }
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
                    
                    // 设备信息卡片（使用主页的信息预览样式）
                    val context = LocalContext.current
                    val metadataList = remember { com.lsfStudio.lsfTB.ui.util.OOBE.getAllMetadata(context) }
                    
                    if (metadataList.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .clip(miuixShape(12.dp))
                                .combinedClickable(
                                    onClick = { },
                                    onLongClick = {
                                        // 震动反馈
                                        com.lsfStudio.lsfTB.ui.util.HapticFeedbackUtil.lightClick(context)
                                        
                                        // 格式化文本
                                        val clipboardText = buildString {
                                            metadataList.forEach { (title, content) ->
                                                append("$title：\n$content\n")
                                            }
                                        }
                                        
                                        // 复制到剪贴板
                                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("设备信息", clipboardText)
                                        clipboardManager.setPrimaryClip(clip)
                                        
                                        // 显示 Toast
                                        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                                        
                                        Log.d("AboutMiuix", "✅ 设备信息已复制到剪贴板")
                                    }
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                metadataList.forEachIndexed { index, (title, content) ->
                                    InfoText(
                                        title = title,
                                        content = content,
                                        bottomPadding = if (index == metadataList.size - 1) 0.dp else 16.dp
                                    )
                                }
                            }
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
    
    // Debug 弹窗 - 设备标识符验证
    if (showDebugDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        var userInput by remember { mutableStateOf("") }
        
        // 🔧 检测当前是否为开发版（数据库中存储的是 "dev"）
        val isDevMode = remember {
            try {
                val dataBaseClass = Class.forName("com.lsfStudio.lsfTB.ui.util.DataBase")
                val dataBaseConstructor = dataBaseClass.getConstructor(Context::class.java)
                val dataBase = dataBaseConstructor.newInstance(context)
                
                val oobeClass = Class.forName("com.lsfStudio.lsfTB.ui.util.OOBE")
                val keyDeviceIdField = oobeClass.getDeclaredField("KEY_DEVICE_ID")
                val keyDeviceId = keyDeviceIdField.get(null) as String
                
                val getMetadataMethod = dataBaseClass.getMethod("getMetadataText", String::class.java)
                val storedValue = getMetadataMethod.invoke(dataBase, keyDeviceId) as String?
                
                storedValue == "dev"
            } catch (e: Exception) {
                Log.w("AboutMiuix", "⚠️ 检测开发版状态失败", e)
                false
            }
        }
        
        WindowDialog(
            show = true,
            modifier = Modifier.padding(WindowInsets.systemBars.only(WindowInsetsSides.Top).asPaddingValues()),
            title = "🔧 Debug 模式",
            onDismissRequest = {
                // 防抖：弹窗打开后 500ms 内不响应关闭（防止快速点击误关闭）
                val currentTime = System.currentTimeMillis()
                if (currentTime - debugDialogClickTime > 500) {
                    showDebugDialog = false
                }
            },
            content = {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "请输入设备标识符进行验证",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // 🔧 只在开发版时显示提示
                    if (isDevMode) {
                        Text(
                            text = "💡 开发版标识符均为 dev",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    
                    top.yukonga.miuix.kmp.basic.TextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        label = "设备标识符",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        singleLine = false,
                        maxLines = 5
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            text = "取消",
                            onClick = {
                                showDebugDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        TextButton(
                            text = "验证",
                            onClick = {
                                if (userInput.isNotBlank()) {
                                    // 使用后端 API 进行验证
                                    verifyIdentifierWithServer(context, userInput)
                                } else {
                                    Toast.makeText(context, "⚠️ 请输入标识符", Toast.LENGTH_SHORT).show()
                                }
                                showDebugDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun InfoText(
    title: String,
    content: String,
    bottomPadding: androidx.compose.ui.unit.Dp = 16.dp
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

/**
 * 使用后端 API 验证标识符
 */
private fun verifyIdentifierWithServer(context: Context, userInput: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            Log.d("AboutMiuix", "🔍 开始验证标识符...")
            
            // 获取设备 ID
            val deviceId = com.lsfStudio.lsfTB.ui.util.KeystoreManager.getDeviceId(context)
            
            Log.d("AboutMiuix", "   Device ID: $deviceId")
            Log.d("AboutMiuix", "   输入文本: $userInput")
            
            // 构建请求体
            val jsonBody = org.json.JSONObject()
            jsonBody.put("deviceId", deviceId)
            jsonBody.put("inputText", userInput)
            val verifyJson = jsonBody.toString()
            
            val requestBody = okhttp3.RequestBody.create(
                "application/json; charset=utf-8".toMediaType(),
                verifyJson
            )
            
            // 获取服务器 URL
            val serverInfoClass = Class.forName("com.lsfStudio.lsfTB.ui.util.ServerInfo")
            val serverUrlField = serverInfoClass.getDeclaredField("ServerUrl")
            serverUrlField.isAccessible = true
            val serverUrl = serverUrlField.get(null) as String
            
            // 构建请求（NetworkClient 自动签名）
            val request = com.lsfStudio.lsfTB.ui.util.NetworkClient.buildPostRequest(
                context = context,
                url = "$serverUrl/verify",
                path = "/lsfStudio/api/verify",
                body = requestBody,
                bodyContent = verifyJson  // 传递原始 JSON 字符串用于签名
            )
            
            // 执行请求
            val response = com.lsfStudio.lsfTB.ui.util.NetworkClient.execute(request)
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = org.json.JSONObject(responseBody)
                val verified = jsonResponse.optBoolean("verified", false)
                val message = jsonResponse.optString("message", "未知状态")
                val isExact = jsonResponse.optBoolean("isExact", false)
                
                // 在主线程显示结果
                withContext(Dispatchers.Main) {
                    if (verified) {
                        if (isExact) {
                            Toast.makeText(context, "✅ 验证通过：$message", Toast.LENGTH_LONG).show()
                            // 启用开发者模式
                            com.lsfStudio.lsfTB.ui.util.DebugShellReceiver.enableDevMode(context)
                            Log.d("AboutMiuix", "✅ 标识符验证通过，开发者模式已启用")
                        } else {
                            Toast.makeText(context, "⚠️ 验证通过（有缺失）：$message", Toast.LENGTH_LONG).show()
                            com.lsfStudio.lsfTB.ui.util.DebugShellReceiver.enableDevMode(context)
                            Log.w("AboutMiuix", "⚠️ 标识符验证通过但有缺失字符")
                        }
                    } else {
                        Toast.makeText(context, "❌ 验证失败：$message", Toast.LENGTH_LONG).show()
                        Log.w("AboutMiuix", "❌ 标识符验证失败")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "❌ 验证请求失败: ${response.code}", Toast.LENGTH_LONG).show()
                }
                Log.e("AboutMiuix", "❌ 验证请求失败: ${response.code}, 响应: $responseBody")
            }
            
            response.close()
            
        } catch (e: Exception) {
            Log.e("AboutMiuix", "❌ 验证异常", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "❌ 验证错误: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
