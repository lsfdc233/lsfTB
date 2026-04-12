package com.lsfStudio.lsfTB.ui.screen.twofa

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.MoreCircle
import top.yukonga.miuix.kmp.icon.extended.UploadCloud
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import com.lsfStudio.lsfTB.ui.component.dialog.ConfirmDialogMiuix
import com.lsfStudio.lsfTB.ui.util.HapticFeedbackUtil
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.floor
import org.apache.commons.codec.binary.Base32
import android.util.Log

/**
 * 2FA 账户数据类
 */
data class TFACodeAccount(
    val id: String,
    val name: String,
    val issuer: String,
    val secret: String,
    var code: String = "",
    var timeRemaining: Int = 30
)

/**
 * 2FA（双因素认证）页面
 * 参考 KernelSU 模块页面 UI 设计
 */
@Composable
fun TwoFAScreen(
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    
    // 状态管理
    var showAddDialog by remember { mutableStateOf(false) }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<TFACodeAccount?>(null) }
    var accounts by remember { 
        mutableStateOf<List<TFACodeAccount>>(loadAccounts(context)) 
    }
    
    // 倒计时更新（使用 derivedStateOf 确保持续计算）
    val currentTimeSeconds = remember { mutableStateOf(System.currentTimeMillis() / 1000) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeSeconds.value = System.currentTimeMillis() / 1000
        }
    }
    
    // 根据当前时间计算所有账户的代码
    val updatedAccounts = remember(currentTimeSeconds.value, accounts) {
        accounts.map { account ->
            val timeRemaining = 30 - (currentTimeSeconds.value % 30).toInt()
            val code = generateTOTP(account.secret)
            account.copy(
                code = code,
                timeRemaining = timeRemaining
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                color = colorScheme.surface,
                title = "双因素认证",
                navigationIcon = {
                    // 备份/恢复按钮（左侧）
                    IconButton(
                        onClick = {
                            // TODO: 打开备份/恢复功能
                        },
                    ) {
                        Icon(
                            imageVector = MiuixIcons.UploadCloud,
                            tint = colorScheme.onSurface,
                            contentDescription = "备份/恢复"
                        )
                    }
                },
                actions = {
                    // 选项按钮（右侧）
                    Box {
                        val showTopPopup = remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showTopPopup.value = true },
                            holdDownState = showTopPopup.value
                        ) {
                            Icon(
                                imageVector = MiuixIcons.MoreCircle,
                                tint = colorScheme.onSurface,
                                contentDescription = "选项"
                            )
                        }
                        // TODO: 添加弹出菜单
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            // 蓝色圆形按钮（右下角，调整位置）
            FloatingActionButton(
                modifier = Modifier
                    .padding(bottom = 80.dp, end = 20.dp)
                    .clip(CircleShape),
                shadowElevation = 0.dp,
                onClick = {
                    // 震动反馈
                    HapticFeedbackUtil.lightClick(context)
                    showAddDialog = true
                },
                content = {
                    Icon(
                        Icons.Rounded.Add,
                        "添加认证",
                        modifier = Modifier.size(40.dp),
                        tint = colorScheme.onPrimary
                    )
                },
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        // 参考主页实现：始终可滚动的 LazyColumn
        LazyColumn(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .overScrollVertical()
                .scrollEndHaptic()
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            overscrollEffect = null,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (updatedAccounts.isEmpty()) {
                // 空状态
                item {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "点击下方 + 按钮添加认证",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                // 账户列表
                items(updatedAccounts, key = { it.id }) { account ->
                    TFACodeAccountItem(
                        account = account,
                        onDelete = {
                            accountToDelete = account
                            showDeleteConfirmDialog = true
                        }
                    )
                }
                item {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
        
        // 删除确认对话框（参照发现新版本弹窗样式）
        if (showDeleteConfirmDialog && accountToDelete != null) {
            ConfirmDialogMiuix(
                title = "删除认证",
                content = "确定要删除 \"${accountToDelete!!.name}\" 吗？此操作不可恢复。",
                isMarkdown = false,
                confirmText = "删除",
                dismissText = "取消",
                onConfirm = {
                    accounts = accounts.filter { it.id != accountToDelete!!.id }
                    saveAccounts(context, accounts)
                },
                onDismiss = {},
                showDialog = remember(showDeleteConfirmDialog) { mutableStateOf(showDeleteConfirmDialog) }.also { dialogState ->
                    LaunchedEffect(showDeleteConfirmDialog) {
                        dialogState.value = showDeleteConfirmDialog
                        if (!showDeleteConfirmDialog) {
                            accountToDelete = null
                        }
                    }
                }
            )
        }
        
        // 添加方式选择对话框（Miuix 风格）
        if (showAddDialog) {
            WindowDialog(
                show = showAddDialog,
                title = "添加认证",
                onDismissRequest = { showAddDialog = false }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "请选择添加方式",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // 扫码添加按钮（扩大）
                        Button(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                showAddDialog = false
                                // TODO: 跳转到扫码页面
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Icon(Icons.Rounded.QrCode, null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("扫码添加", fontSize = 16.sp)
                        }
                        
                        Spacer(Modifier.width(12.dp))
                        
                        // 手动输入按钮（扩大）
                        Button(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                showAddDialog = false
                                showManualInputDialog = true
                            },
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("手动输入", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
        
        // 手动输入对话框（Miuix 风格）
        if (showManualInputDialog) {
            ManualInputDialogMiuix(
                onDismiss = { showManualInputDialog = false },
                onConfirm = { name, issuer, secret ->
                    // 验证密钥格式
                    if (isValidBase32(secret)) {
                        val newAccount = TFACodeAccount(
                            id = System.currentTimeMillis().toString(),
                            name = name,
                            issuer = issuer,
                            secret = secret.uppercase().replace(" ", ""),
                            code = generateTOTP(secret),
                            timeRemaining = 30 - ((System.currentTimeMillis() / 1000) % 30).toInt()
                        )
                        accounts = accounts + newAccount
                        saveAccounts(context, accounts)
                        showManualInputDialog = false
                    } else {
                        // TODO: 显示错误提示
                        Log.e("TwoFAScreen", "无效的 Base32 密钥")
                    }
                }
            )
        }
    }
}

/**
 * 2FA 账户卡片组件
 */
@Composable
fun TFACodeAccountItem(
    account: TFACodeAccount,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (account.issuer.isNotBlank()) {
                    Text(
                        text = account.issuer,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = account.code.chunked(3).joinToString(" "), // 缩小缝隙
                    fontWeight = FontWeight.ExtraBold, // 加粗
                    fontSize = 42.sp, // 继续加大字体
                    color = colorScheme.primary,
                    letterSpacing = 2.sp // 缩小字母间距
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${account.timeRemaining}s",
                    fontSize = 14.sp,
                    color = if (account.timeRemaining <= 5) Color.Red else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        "删除",
                        tint = colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * 手动输入对话框（Miuix 风格）
 */
@Composable
fun ManualInputDialogMiuix(
    onDismiss: () -> Unit,
    onConfirm: (name: String, issuer: String, secret: String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var issuer by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    
    // 实时验证
    val validationError = remember(name, secret) {
        validateInput(name, secret)
    }
    val isTextField = validationError != null
    val canAdd = name.isNotBlank() && secret.isNotBlank() && !isTextField
    
    WindowDialog(
        show = true,
        title = "手动添加认证",
        onDismissRequest = onDismiss
    ) {
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(scrollState)
        ) {
            // 账户名称输入框（放大）
            TextField(
                value = name,
                onValueChange = { name = it },
                label = "账户名称",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // 发行者输入框（放大）
            TextField(
                value = issuer,
                onValueChange = { issuer = it },
                label = "发行者（可选）",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // 密钥输入框（放大）
            TextField(
                value = secret,
                onValueChange = { secret = it.uppercase().replace(" ", "") },
                label = "密钥（Base32 格式）",
                modifier = Modifier.fillMaxWidth()
            )
            
            // 错误提示文字
            if (isTextField && validationError != null) {
                Text(
                    text = validationError,
                    color = colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 按钮行（参考 ConfirmDialogMiuix 样式，扩大到与扫码/手动输入按钮相同）
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                TextButton(
                    text = "取消",
                    onClick = {
                        HapticFeedbackUtil.lightClick(context)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = "添加",
                    onClick = {
                        HapticFeedbackUtil.lightClick(context)
                        onConfirm(name, issuer, secret)
                    },
                    enabled = canAdd,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                )
            }
        }
    }
}

/**
 * 生成 TOTP 验证码
 */
private fun generateTOTP(secret: String): String {
    return try {
        val base32 = Base32()
        val key = base32.decode(secret.uppercase().replace(" ", ""))
        val time = System.currentTimeMillis() / 1000 / 30
        
        val data = ByteArray(8)
        for (i in 7 downTo 0) {
            data[i] = (time and 0xff).toByte()
            time shr 8
        }
        
        val mac = Mac.getInstance("HmacSHA1")
        val secretKey = SecretKeySpec(key, "HmacSHA1")
        mac.init(secretKey)
        val hash = mac.doFinal(data)
        
        val offset = hash[hash.size - 1].toInt() and 0xf
        val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
                    ((hash[offset + 1].toInt() and 0xff) shl 16) or
                    ((hash[offset + 2].toInt() and 0xff) shl 8) or
                    (hash[offset + 3].toInt() and 0xff)
        
        val otp = binary % 1000000
        String.format("%06d", otp)
    } catch (e: Exception) {
        Log.e("TOTP", "生成验证码失败: ${e.message}")
        "000000"
    }
}

/**
 * 验证输入
 */
private fun validateInput(name: String, secret: String): String? {
    if (name.isBlank()) {
        return "账户名称不能为空"
    }
    
    if (secret.isBlank()) {
        return "密钥不能为空"
    }
    
    // 检查 Base32 格式
    if (!isValidBase32(secret)) {
        return "密钥格式错误，只支持 A-Z 和 2-7 的字符"
    }
    
    // 检查密钥长度（通常 16-32 个字符）
    val cleanSecret = secret.uppercase().replace(" ", "")
    if (cleanSecret.length < 16) {
        return "密钥长度过短（至少 16 个字符）"
    }
    if (cleanSecret.length > 64) {
        return "密钥长度过长（最多 64 个字符）"
    }
    
    return null
}

/**
 * 验证 Base32 格式
 */
private fun isValidBase32(input: String): Boolean {
    val base32Pattern = "^[A-Z2-7]+=*$".toRegex()
    return base32Pattern.matches(input.uppercase().replace(" ", ""))
}

/**
 * 保存账户列表到 SharedPreferences
 */
private fun saveAccounts(context: Context, accounts: List<TFACodeAccount>) {
    val prefs = context.getSharedPreferences("2fa_accounts", Context.MODE_PRIVATE)
    val editor = prefs.edit()
    
    // 清除旧数据
    editor.clear()
    
    // 保存账户数量
    editor.putInt("account_count", accounts.size)
    
    // 保存每个账户
    accounts.forEachIndexed { index, account ->
        editor.putString("account_${index}_id", account.id)
        editor.putString("account_${index}_name", account.name)
        editor.putString("account_${index}_issuer", account.issuer)
        editor.putString("account_${index}_secret", account.secret)
    }
    
    editor.apply()
}

/**
 * 从 SharedPreferences 加载账户列表
 */
private fun loadAccounts(context: Context): List<TFACodeAccount> {
    val prefs = context.getSharedPreferences("2fa_accounts", Context.MODE_PRIVATE)
    val count = prefs.getInt("account_count", 0)
    val accounts = mutableListOf<TFACodeAccount>()
    
    for (i in 0 until count) {
        val id = prefs.getString("account_${i}_id", "") ?: ""
        val name = prefs.getString("account_${i}_name", "") ?: ""
        val issuer = prefs.getString("account_${i}_issuer", "") ?: ""
        val secret = prefs.getString("account_${i}_secret", "") ?: ""
        
        if (id.isNotEmpty() && secret.isNotEmpty()) {
            accounts.add(TFACodeAccount(
                id = id,
                name = name,
                issuer = issuer,
                secret = secret,
                code = "000000",
                timeRemaining = 30
            ))
        }
    }
    
    return accounts
}
