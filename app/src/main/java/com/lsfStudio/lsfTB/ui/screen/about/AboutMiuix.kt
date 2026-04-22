package com.lsfStudio.lsfTB.ui.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
        
        WindowDialog(
            show = true,
            modifier = Modifier.padding(WindowInsets.systemBars.only(WindowInsetsSides.Top).asPaddingValues()),
            title = "🔧 Debug 模式",
            onDismissRequest = {
                showDebugDialog = false
            },
            content = {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "请输入设备标识符进行验证\n（支持噪声格式：!内容^ 或 (内容)）",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    TextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        label = { Text("设备标识符") },
                        placeholder = { Text("例如: a1b2c3d4e5... 或 a(!noise^)1b2...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        singleLine = false,
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions.Default
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
                                    try {
                                        // 动态加载 LsfEncoder（如果存在）
                                        val lsfEncoderClass = Class.forName("com.lsfStudio.lsfTB.ui.util.LsfEncoder")
                                        val constructor = lsfEncoderClass.getConstructor(Context::class.java)
                                        val encoder = constructor.newInstance(context)
                                        val ensureMethod = lsfEncoderClass.getMethod("ensureTableLoaded")
                                        val isLoaded = ensureMethod.invoke(encoder) as Boolean
                                        
                                        if (!isLoaded) {
                                            Toast.makeText(context, "⚠️ 编码表不可用，无法验证", Toast.LENGTH_LONG).show()
                                            Log.w("AboutMiuix", "⚠️ 编码表不可用")
                                            showDebugDialog = false
                                            return@TextButton
                                        }
                                        
                                        // 动态加载 DataBase
                                        val dataBaseClass = Class.forName("com.lsfStudio.lsfTB.ui.util.DataBase")
                                        val dataBaseConstructor = dataBaseClass.getConstructor(Context::class.java)
                                        val dataBase = dataBaseConstructor.newInstance(context)
                                        
                                        // 获取 OOBE.KEY_DEVICE_ID
                                        val oobeClass = Class.forName("com.lsfStudio.lsfTB.ui.util.OOBE")
                                        val keyDeviceIdField = oobeClass.getDeclaredField("KEY_DEVICE_ID")
                                        val keyDeviceId = keyDeviceIdField.get(null) as String
                                        
                                        // 调用 getMetadataBinary
                                        val getMetadataMethod = dataBaseClass.getMethod("getMetadataBinary", String::class.java)
                                        val storedIdentifierBytes = getMetadataMethod.invoke(dataBase, keyDeviceId) as ByteArray?
                                        
                                        if (storedIdentifierBytes != null && storedIdentifierBytes.isNotEmpty()) {
                                            val storedBinary = String(storedIdentifierBytes, Charsets.UTF_8)
                                            
                                            // 动态加载 OOBESecurity
                                            val oobeSecurityClass = Class.forName("com.lsfStudio.lsfTB.ui.util.OOBESecurity")
                                            val verifyMethod = oobeSecurityClass.getMethod("verifyUserInput", Context::class.java, String::class.java, String::class.java)
                                            val isMatch = verifyMethod.invoke(null, context, userInput, storedBinary) as Boolean
                                            
                                            if (isMatch) {
                                                Toast.makeText(context, "✅ 验证通过！", Toast.LENGTH_LONG).show()
                                                Log.d("AboutMiuix", "✅ 设备标识符验证通过")
                                            } else {
                                                Toast.makeText(context, "❌ 验证失败：标识符不匹配", Toast.LENGTH_LONG).show()
                                                Log.w("AboutMiuix", "❌ 设备标识符验证失败")
                                            }
                                        } else {
                                            Toast.makeText(context, "⚠️ 数据库中未找到设备标识符", Toast.LENGTH_LONG).show()
                                            Log.w("AboutMiuix", "⚠️ 数据库中未找到设备标识符")
                                        }
                                    } catch (e: ClassNotFoundException) {
                                        Toast.makeText(context, "⚠️ 编码模块不可用", Toast.LENGTH_LONG).show()
                                        Log.w("AboutMiuix", "⚠️ 编码模块类未找到")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "❌ 验证错误: ${e.message}", Toast.LENGTH_LONG).show()
                                        Log.e("AboutMiuix", "❌ 验证错误", e)
                                    }
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
