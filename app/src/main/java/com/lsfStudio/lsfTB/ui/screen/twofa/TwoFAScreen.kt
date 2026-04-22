package com.lsfStudio.lsfTB.ui.screen.twofa

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.QrCode
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
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.MoreCircle
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.icon.extended.UploadCloud
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.miuixShape
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import com.lsfStudio.lsfTB.ui.component.dialog.ConfirmDialogMiuix
import com.lsfStudio.lsfTB.ui.util.HapticFeedbackUtil
import com.lsfStudio.lsfTB.ui.navigation3.LocalNavigator
import com.lsfStudio.lsfTB.ui.navigation3.Route
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.floor
import org.apache.commons.codec.binary.Base32
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * 四元组数据类（用于解析otpauth URI）
 */
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

/**
 * 2FA 账户类型枚举
 */
enum class OtpType {
    TOTP,  // 基于时间的 OTP
    HOTP   // 基于计数的 OTP
}

/**
 * 2FA 账户数据类
 */
data class TFACodeAccount(
    val id: String,
    val name: String,
    val issuer: String,
    val secret: String,
    val type: OtpType = OtpType.TOTP,  // 默认为 TOTP
    var code: String = "",
    var timeRemaining: Int = 30,
    var counter: Long = 0  // HOTP 计数器
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
    val navigator = LocalNavigator.current
    val scrollBehavior = MiuixScrollBehavior()
    val scope = rememberCoroutineScope()
    
    // 状态管理
    var showAddDialog by remember { mutableStateOf(false) }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showInvalidQRDialog by remember { mutableStateOf(false) } // 无效二维码弹窗
    var accountToDelete by remember { mutableStateOf<TFACodeAccount?>(null) }
    var accounts by remember { 
        mutableStateOf<List<TFACodeAccount>>(loadAccounts(context)) 
    }
    
    // 多选模式
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedAccountIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // 批量操作对话框
    var showBatchExportDialog by remember { mutableStateOf(false) }
    var showBatchDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    // 导入导出相关状态
    var showBackupDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var exportFolderUri by remember { mutableStateOf<Uri?>(null) }
    var importFileUri by remember { mutableStateOf<Uri?>(null) }
    
    // 扫码预填充数据
    var scannedName by remember { mutableStateOf("") }
    var scannedIssuer by remember { mutableStateOf("") }
    var scannedSecret by remember { mutableStateOf("") }
    var scannedType by remember { mutableStateOf(OtpType.TOTP) }
    
    // 编辑相关状态
    var editingAccount by remember { mutableStateOf<TFACodeAccount?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showTypeDescriptionDialog by remember { mutableStateOf(false) }
    var typeDescriptionText by remember { mutableStateOf("") }
    
    // 文件夹选择器（用于导出）
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        exportFolderUri = uri
    }
    
    // 文件选择器（用于导入）
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        importFileUri = uri
    }
    
    // 监听扫码结果
    LaunchedEffect(Unit) {
        try {
            navigator.observeResult<String>("qr_code_scan_result").collect { scannedData ->
                Log.d("TwoFAScreen", "收到扫描结果: $scannedData")
                if (scannedData.isNullOrEmpty()) {
                    Log.e("TwoFAScreen", "扫描结果为空")
                    return@collect
                }
                
                try {
                    // 解析 otpauth:// URI
                    parseOtpAuthUri(scannedData)?.let { result ->
                        val (name, issuer, secret, type) = result
                        Log.d("TwoFAScreen", "解析成功: name=$name, issuer=$issuer, secret=${secret.take(8)}..., type=$type")
                        // 设置预填充数据
                        scannedName = name
                        scannedIssuer = issuer
                        scannedSecret = secret.uppercase().replace(" ", "")
                        scannedType = type
                        // 打开手动输入弹窗
                        showManualInputDialog = true
                    } ?: run {
                        Log.w("TwoFAScreen", "无法解析 OTP Auth URI: $scannedData")
                        // 显示无效二维码提示
                        showInvalidQRDialog = true
                    }
                } catch (e: Exception) {
                    Log.e("TwoFAScreen", "解析URI时发生异常", e)
                    showInvalidQRDialog = true
                }
                
                navigator.clearResult("qr_code_scan_result")
            }
        } catch (e: Exception) {
            Log.e("TwoFAScreen", "监听扫码结果失败", e)
        }
    }
    
    // 倒计时更新（仅用于 TOTP）
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
            if (account.type == OtpType.TOTP) {
                // TOTP: 基于时间自动更新
                val timeRemaining = 30 - (currentTimeSeconds.value % 30).toInt()
                val code = generateTOTP(account.secret)
                account.copy(
                    code = code,
                    timeRemaining = timeRemaining
                )
            } else {
                // HOTP: 保持当前代码和计数器，不自动更新
                // 如果没有代码，生成初始代码（counter = 0）
                if (account.code.isEmpty() || account.code == "000000") {
                    val initialCode = generateHOTP(account.secret, 0)
                    account.copy(
                        code = initialCode,
                        counter = 0,
                        timeRemaining = 0
                    )
                } else {
                    account.copy(timeRemaining = 0)
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // 多选模式顶部栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surface)
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 左侧关闭按钮
                        IconButton(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                isSelectionMode = false
                                selectedAccountIds = emptySet()
                            }
                        ) {
                            Icon(
                                MiuixIcons.Close,
                                "关闭",
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        // 中间标题
                        Text(
                            text = "已选择${selectedAccountIds.size}项",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface
                        )
                        
                        // 右侧菜单按钮（全选）
                        IconButton(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                if (selectedAccountIds.size == accounts.size) {
                                    selectedAccountIds = emptySet()
                                } else {
                                    selectedAccountIds = accounts.map { it.id }.toSet()
                                }
                            }
                        ) {
                            Icon(
                                MiuixIcons.SelectAll,
                                "全选",
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            } else {
                TopAppBar(
                    color = colorScheme.surface,
                    title = "双因素认证",
                    navigationIcon = {
                        // 备份/恢复按钮（左侧）
                        IconButton(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                showBackupDialog = true
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
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
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
                            MiuixIcons.Add,
                            "添加认证",
                            modifier = Modifier.size(40.dp),
                            tint = colorScheme.onPrimary
                        )
                    },
                )
            }
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
                        isSelected = selectedAccountIds.contains(account.id),
                        isSelectionMode = isSelectionMode,
                        onToggleSelection = {
                            HapticFeedbackUtil.lightClick(context)
                            if (selectedAccountIds.contains(account.id)) {
                                selectedAccountIds = selectedAccountIds - account.id
                            } else {
                                selectedAccountIds = selectedAccountIds + account.id
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedAccountIds = setOf(account.id)
                            }
                        },
                        onDelete = {
                            accountToDelete = account
                            showDeleteConfirmDialog = true
                        },
                        onEdit = {
                            editingAccount = account
                            showEditDialog = true
                        },
                        onShowTypeDescription = {
                            typeDescriptionText = if (account.type == OtpType.TOTP) {
                                "TOTP（基于时间的 OTP）\n\nTOTP 是一种基于当前时间生成验证码的算法。验证码每 30 秒自动更新一次，即使不联网也能生成。"
                            } else {
                                "HOTP（基于计数的 OTP）\n\nHOTP 是一种基于计数器生成验证码的算法。每次使用时计数器递增，验证码随之变化。"
                            }
                            showTypeDescriptionDialog = true
                        },
                        onGenerateHOTP = {
                            // 生成新的 HOTP 验证码
                            val newCode = generateHOTP(account.secret, account.counter + 1)
                            val updatedAccount = account.copy(
                                code = newCode,
                                counter = account.counter + 1
                            )
                            accounts = accounts.map { if (it.id == account.id) updatedAccount else it }
                            saveAccounts(context, accounts)
                        }
                    )
                }
                item {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
        
        // 多选模式底部操作栏
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 批量导出按钮
                        Button(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                showBatchExportDialog = true
                            },
                            enabled = selectedAccountIds.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("批量导出")
                        }
                        
                        Spacer(Modifier.width(8.dp))
                        
                        // 批量删除按钮
                        Button(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                showBatchDeleteConfirmDialog = true
                            },
                            enabled = selectedAccountIds.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("批量删除")
                        }
                    }
                }
            }
        }
        
        // 删除确认对话框
        if (showDeleteConfirmDialog && accountToDelete != null) {
            val deleteDialogState = remember { mutableStateOf(true) }
            
            LaunchedEffect(deleteDialogState.value) {
                if (!deleteDialogState.value) {
                    showDeleteConfirmDialog = false
                    accountToDelete = null
                }
            }
            
            ConfirmDialogMiuix(
                title = "删除认证",
                content = "确定要删除 \"${accountToDelete!!.name}\" 吗？此操作不可恢复。",
                isMarkdown = false,
                confirmText = "删除",
                dismissText = "取消",
                onConfirm = {
                    accounts = accounts.filter { it.id != accountToDelete!!.id }
                    saveAccounts(context, accounts)
                    deleteDialogState.value = false
                },
                onDismiss = {
                    deleteDialogState.value = false
                },
                showDialog = deleteDialogState
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
                                // 跳转到扫码页面
                                navigator.push(Route.QRCodeScanner(
                                    title = "扫描 2FA 二维码",
                                    hint = "将 2FA 二维码放入框内"
                                ))
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
                            Icon(MiuixIcons.Edit, null, modifier = Modifier.size(24.dp))
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
                onDismiss = { 
                    showManualInputDialog = false
                    // 清空预填充数据
                    scannedName = ""
                    scannedIssuer = ""
                    scannedSecret = ""
                    scannedType = OtpType.TOTP
                },
                onConfirm = { name, issuer, secret, type ->
                    // 验证密钥格式
                    if (isValidBase32(secret)) {
                        val newAccount = TFACodeAccount(
                            id = System.currentTimeMillis().toString(),
                            name = name,
                            issuer = issuer,
                            secret = secret.uppercase().replace(" ", ""),
                            type = type,
                            code = if (type == OtpType.TOTP) generateTOTP(secret) else "000000",
                            timeRemaining = if (type == OtpType.TOTP) 30 - ((System.currentTimeMillis() / 1000) % 30).toInt() else 0,
                            counter = if (type == OtpType.HOTP) 0L else 0L
                        )
                        accounts = accounts + newAccount
                        saveAccounts(context, accounts)
                        showManualInputDialog = false
                        // 清空预填充数据
                        scannedName = ""
                        scannedIssuer = ""
                        scannedSecret = ""
                        scannedType = OtpType.TOTP
                    } else {
                        // TODO: 显示错误提示
                        Log.e("TwoFAScreen", "无效的 Base32 密钥")
                    }
                },
                prefilledName = scannedName,
                prefilledIssuer = scannedIssuer,
                prefilledSecret = scannedSecret,
                prefilledType = scannedType,
                isEditMode = false,
                isFromScan = scannedName.isNotEmpty()
            )
        }
        
        // 编辑对话框
        if (showEditDialog && editingAccount != null) {
            ManualInputDialogMiuix(
                onDismiss = { 
                    showEditDialog = false
                    editingAccount = null
                },
                onConfirm = { name, issuer, secret, type ->
                    if (isValidBase32(secret)) {
                        val updatedAccount = editingAccount!!.copy(
                            name = name,
                            issuer = issuer,
                            secret = secret.uppercase().replace(" ", ""),
                            type = type,
                            code = if (type == OtpType.TOTP) generateTOTP(secret) else "000000",
                            timeRemaining = if (type == OtpType.TOTP) 30 - ((System.currentTimeMillis() / 1000) % 30).toInt() else 0,
                            counter = if (type == OtpType.HOTP) editingAccount!!.counter else editingAccount!!.counter
                        )
                        accounts = accounts.map { if (it.id == editingAccount!!.id) updatedAccount else it }
                        saveAccounts(context, accounts)
                        showEditDialog = false
                        editingAccount = null
                    }
                },
                prefilledName = editingAccount!!.name,
                prefilledIssuer = editingAccount!!.issuer,
                prefilledSecret = editingAccount!!.secret,
                prefilledType = editingAccount!!.type,
                isEditMode = true,
                isFromScan = false
            )
        }
        
        // 类型说明弹窗
        if (showTypeDescriptionDialog) {
            WindowDialog(
                show = showTypeDescriptionDialog,
                onDismissRequest = { showTypeDescriptionDialog = false }
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = typeDescriptionText,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    TextButton(
                        text = "确定",
                        onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            showTypeDescriptionDialog = false
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // 无效二维码提示弹窗
        if (showInvalidQRDialog) {
            WindowDialog(
                show = showInvalidQRDialog,
                title = "扫描结果",
                onDismissRequest = { showInvalidQRDialog = false }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "这不是有效的 2FA 二维码\n请扫描 otpauth:// 格式的二维码",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    TextButton(
                        text = "确定",
                        onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            showInvalidQRDialog = false
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // 备份/恢复选择对话框
        if (showBackupDialog) {
            WindowDialog(
                show = showBackupDialog,
                onDismissRequest = { showBackupDialog = false }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "请选择操作",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                showBackupDialog = false
                                showExportDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("导出")
                        }
                        
                        Spacer(Modifier.width(12.dp))
                        
                        Button(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                showBackupDialog = false
                                showImportDialog = true
                            },
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("导入")
                        }
                    }
                }
            }
        }
        
        // 导出对话框
        if (showExportDialog) {
            WindowDialog(
                show = showExportDialog,
                title = "导出验证器",
                onDismissRequest = { showExportDialog = false }
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = exportFolderUri?.lastPathSegment ?: "",
                            onValueChange = { },
                            label = "文件夹路径",
                            enabled = false,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                folderPickerLauncher.launch(null)
                            }
                        ) {
                            Text("选择")
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            text = "取消",
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                showExportDialog = false
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                exportFolderUri?.let { folderUri ->
                                    try {
                                        val jsonContent = exportAccountsToJson(accounts)
                                        val fileName = generateTimestampedFileName()
                                        val success = writeJsonToFile(context, folderUri, fileName, jsonContent)
                                        if (success) {
                                            android.widget.Toast.makeText(context, "导出成功: $fileName", android.widget.Toast.LENGTH_SHORT).show()
                                            showExportDialog = false
                                        } else {
                                            android.widget.Toast.makeText(context, "导出失败", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("TwoFAScreen", "导出异常", e)
                                        android.widget.Toast.makeText(context, "导出失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } ?: run {
                                    android.widget.Toast.makeText(context, "请先选择文件夹", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            enabled = exportFolderUri != null
                        ) {
                            Text("导出")
                        }
                    }
                }
            }
        }
        
        // 导入对话框
        if (showImportDialog) {
            WindowDialog(
                show = showImportDialog,
                title = "导入验证器",
                onDismissRequest = { showImportDialog = false }
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = importFileUri?.lastPathSegment ?: "",
                            onValueChange = { },
                            label = "文件路径",
                            enabled = false,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                filePickerLauncher.launch(arrayOf("application/json"))
                            }
                        ) {
                            Text("选择")
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            text = "取消",
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                showImportDialog = false
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                importFileUri?.let {
                                    showImportConfirmDialog = true
                                } ?: run {
                                    android.widget.Toast.makeText(context, "请先选择文件", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            enabled = importFileUri != null
                        ) {
                            Text("导入")
                        }
                    }
                }
            }
        }
        
        // 导入确认对话框
        if (showImportConfirmDialog) {
            val importConfirmDialogState = remember { mutableStateOf(true) }
            
            LaunchedEffect(importConfirmDialogState.value) {
                if (!importConfirmDialogState.value) {
                    showImportConfirmDialog = false
                }
            }
            
            ConfirmDialogMiuix(
                title = "二次确认",
                content = "是否导入文件中的验证器？这将会清除已添加的验证器",
                isMarkdown = false,
                confirmText = "确认导入",
                dismissText = "取消",
                onConfirm = {
                    importFileUri?.let { fileUri ->
                        try {
                            val jsonString = readJsonFromFile(context, fileUri)
                            if (jsonString != null) {
                                val importedAccounts = importAccountsFromJson(jsonString)
                                if (importedAccounts.isNotEmpty()) {
                                    accounts = importedAccounts
                                    saveAccounts(context, accounts)
                                    importConfirmDialogState.value = false
                                    showImportDialog = false
                                    android.widget.Toast.makeText(context, "导入成功: ${importedAccounts.size} 个验证器", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, "文件中没有有效的验证器", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                android.widget.Toast.makeText(context, "读取文件失败", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("TwoFAScreen", "导入异常", e)
                            android.widget.Toast.makeText(context, "导入失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        android.widget.Toast.makeText(context, "文件 URI 为空", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = {
                    importConfirmDialogState.value = false
                },
                showDialog = importConfirmDialogState
            )
        }
        
        // 批量导出对话框
        if (showBatchExportDialog) {
            WindowDialog(
                show = showBatchExportDialog,
                title = "批量导出",
                onDismissRequest = { showBatchExportDialog = false }
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "已选择 ${selectedAccountIds.size} 个验证器",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = exportFolderUri?.lastPathSegment ?: "",
                            onValueChange = { },
                            label = "文件夹路径",
                            enabled = false,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                folderPickerLauncher.launch(null)
                            }
                        ) {
                            Text("选择")
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            text = "取消",
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                showBatchExportDialog = false
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                exportFolderUri?.let { folderUri ->
                                    try {
                                        val selectedAccounts = accounts.filter { it.id in selectedAccountIds }
                                        val jsonContent = exportAccountsToJson(selectedAccounts)
                                        val fileName = generateTimestampedFileName()
                                        val success = writeJsonToFile(context, folderUri, fileName, jsonContent)
                                        if (success) {
                                            android.widget.Toast.makeText(context, "导出成功: $fileName", android.widget.Toast.LENGTH_SHORT).show()
                                            showBatchExportDialog = false
                                            isSelectionMode = false
                                            selectedAccountIds = emptySet()
                                        } else {
                                            android.widget.Toast.makeText(context, "导出失败", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("TwoFAScreen", "导出异常", e)
                                        android.widget.Toast.makeText(context, "导出失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } ?: run {
                                    android.widget.Toast.makeText(context, "请先选择文件夹", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            enabled = exportFolderUri != null
                        ) {
                            Text("导出")
                        }
                    }
                }
            }
        }
        
        // 批量删除确认对话框
        if (showBatchDeleteConfirmDialog) {
            val batchDeleteDialogState = remember { mutableStateOf(true) }
            
            LaunchedEffect(batchDeleteDialogState.value) {
                if (!batchDeleteDialogState.value) {
                    showBatchDeleteConfirmDialog = false
                }
            }
            
            ConfirmDialogMiuix(
                title = "二次确认",
                content = "是否删除所选 ${selectedAccountIds.size} 个验证器？此操作不可逆",
                isMarkdown = false,
                confirmText = "确认删除",
                dismissText = "取消",
                onConfirm = {
                    accounts = accounts.filter { it.id !in selectedAccountIds }
                    saveAccounts(context, accounts)
                    batchDeleteDialogState.value = false
                    isSelectionMode = false
                    selectedAccountIds = emptySet()
                    android.widget.Toast.makeText(context, "删除成功", android.widget.Toast.LENGTH_SHORT).show()
                },
                onDismiss = {
                    batchDeleteDialogState.value = false
                },
                showDialog = batchDeleteDialogState
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
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleSelection: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onShowTypeDescription: () -> Unit,
    onGenerateHOTP: () -> Unit = {}
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(miuixShape(12.dp))
            .clickable {
                if (isSelectionMode) {
                    onToggleSelection()
                }
            }
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection()
                    }
                },
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 多选模式下的复选框
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                    contentDescription = "选择",
                    tint = if (isSelected) colorScheme.primary else Color.Gray,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp)
                        .clickable { onToggleSelection() }
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = account.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    // TOTP/HOTP 标识
                    Box(
                        modifier = Modifier
                            .clip(miuixShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = colorScheme.primary,
                                shape = miuixShape(8.dp)
                            )
                            .clickable {
                                HapticFeedbackUtil.lightClick(context)
                                onShowTypeDescription()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (account.type == OtpType.TOTP) "TOTP" else "HOTP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                }
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
                // TOTP 显示倒计时，HOTP 显示计数器
                if (account.type == OtpType.TOTP) {
                    Text(
                        text = "${account.timeRemaining}s",
                        fontSize = 14.sp,
                        color = if (account.timeRemaining <= 5) Color.Red else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "计数器: ${account.counter}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    // HOTP 生成新验证码按钮
                    if (account.type == OtpType.HOTP) {
                        IconButton(onClick = onGenerateHOTP) {
                            Icon(
                                MiuixIcons.Add,
                                "生成新验证码",
                                tint = colorScheme.primary
                            )
                        }
                    }
                    // 编辑按钮
                    IconButton(onClick = onEdit) {
                        Icon(
                            MiuixIcons.Edit,
                            "编辑",
                            tint = colorScheme.onSurface
                        )
                    }
                    // 删除按钮
                    IconButton(onClick = onDelete) {
                        Icon(
                            MiuixIcons.Delete,
                            "删除",
                            tint = colorScheme.onSurface
                        )
                    }
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
    onConfirm: (name: String, issuer: String, secret: String, type: OtpType) -> Unit,
    prefilledName: String = "",
    prefilledIssuer: String = "",
    prefilledSecret: String = "",
    prefilledType: OtpType = OtpType.TOTP,
    isEditMode: Boolean = false,
    isFromScan: Boolean = false
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(prefilledName) }
    var issuer by remember { mutableStateOf(prefilledIssuer) }
    var secret by remember { mutableStateOf(prefilledSecret) }
    var type by remember { mutableStateOf(prefilledType) }
    
    // 动态标题
    val dialogTitle = when {
        isEditMode -> "修改验证器"
        prefilledName.isNotEmpty() -> "识别到的认证信息"
        else -> "手动添加认证"
    }
    
    // 实时验证
    val validationError = remember(name, secret) {
        validateInput(name, secret)
    }
    val isTextField = validationError != null
    val canAdd = name.isNotBlank() && secret.isNotBlank() && !isTextField
    
    WindowDialog(
        show = true,
        title = dialogTitle,
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // TOTP/HOTP 类型选择（扫码后不可修改）
            Text(
                text = "认证类型",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // TOTP 选项
                val isTOTPSelected = type == OtpType.TOTP
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(miuixShape(12.dp))
                        .background(if (isTOTPSelected) colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                        .border(
                            width = 1.5.dp,
                            color = if (isTOTPSelected) colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                            shape = miuixShape(12.dp)
                        )
                        .clickable(
                            enabled = !isFromScan, // 扫码后不可修改
                            onClick = {
                                if (!isFromScan) {
                                    HapticFeedbackUtil.lightClick(context)
                                    type = OtpType.TOTP
                                }
                            }
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TOTP",
                            fontWeight = FontWeight.Bold,
                            color = if (isTOTPSelected) colorScheme.primary else Color.Gray,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "基于时间",
                            fontSize = 11.sp,
                            color = if (isTOTPSelected) colorScheme.primary else Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                
                // HOTP 选项
                val isHOTPSelected = type == OtpType.HOTP
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(miuixShape(12.dp))
                        .background(if (isHOTPSelected) colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                        .border(
                            width = 1.5.dp,
                            color = if (isHOTPSelected) colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                            shape = miuixShape(12.dp)
                        )
                        .clickable(
                            enabled = !isFromScan, // 扫码后不可修改
                            onClick = {
                                if (!isFromScan) {
                                    HapticFeedbackUtil.lightClick(context)
                                    type = OtpType.HOTP
                                }
                            }
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "HOTP",
                            fontWeight = FontWeight.Bold,
                            color = if (isHOTPSelected) colorScheme.primary else Color.Gray,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "基于计数",
                            fontSize = 11.sp,
                            color = if (isHOTPSelected) colorScheme.primary else Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            
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
                    text = if (isEditMode) "修改" else "添加",
                    onClick = {
                        HapticFeedbackUtil.lightClick(context)
                        onConfirm(name, issuer, secret, type)
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
 * 解析 otpauth:// URI
 * 
 * @param uri otpauth:// URI 字符串
 * @return Quadruple(name, issuer, secret, type) 或 null
 */
private fun parseOtpAuthUri(uri: String): Quadruple<String, String, String, OtpType>? {
    return try {
        if (!uri.startsWith("otpauth://")) {
            Log.e("TwoFAScreen", "不是有效的 otpauth:// URI")
            return null
        }
        
        // 解析 URI
        val androidUri = android.net.Uri.parse(uri)
        val typeStr = androidUri.host ?: "totp" // totp 或 hotp
        val label = androidUri.path?.trimStart('/') ?: ""
        
        // 确定类型
        val type = if (typeStr.equals("hotp", ignoreCase = true)) {
            OtpType.HOTP
        } else {
            OtpType.TOTP
        }
        
        // 解析标签（格式：Issuer:AccountName 或 AccountName）
        val (name, issuer) = if (label.contains(":")) {
            val parts = label.split(":", limit = 2)
            Pair(parts[1].trim(), parts[0].trim())
        } else {
            Pair(label.trim(), "")
        }
        
        // 获取密钥
        val secret = androidUri.getQueryParameter("secret") ?: ""
        
        if (secret.isEmpty()) {
            Log.e("TwoFAScreen", "URI 中缺少 secret 参数")
            return null
        }
        
        Log.d("TwoFAScreen", "解析成功: name=$name, issuer=$issuer, secret=$secret, type=$type")
        Quadruple(name, issuer, secret, type)
    } catch (e: Exception) {
        Log.e("TwoFAScreen", "解析 URI 失败", e)
        null
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
        var tempTime = time
        for (i in 7 downTo 0) {
            data[i] = (tempTime and 0xff).toByte()
            tempTime = tempTime shr 8
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
 * 生成 HOTP 验证码
 */
private fun generateHOTP(secret: String, counter: Long): String {
    return try {
        val base32 = Base32()
        val key = base32.decode(secret.uppercase().replace(" ", ""))
        
        val data = ByteArray(8)
        var tempCounter = counter
        for (i in 7 downTo 0) {
            data[i] = (tempCounter and 0xff).toByte()
            tempCounter = tempCounter shr 8
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
        Log.e("HOTP", "生成验证码失败: ${e.message}")
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
 * 保存账户列表到数据库
 */
private fun saveAccounts(context: Context, accounts: List<TFACodeAccount>) {
    val middleware = TwoFADatabaseMiddleware(context)
    
    // 清空所有旧数据
    middleware.clearAllAccounts()
    
    // 逐个添加账户
    accounts.forEach { account ->
        middleware.addAccount(
            id = account.id,
            name = account.name,
            issuer = account.issuer,
            secret = account.secret,
            type = account.type.name,
            counter = account.counter
        )
    }
    
    Log.d("TwoFAScreen", "✅ 已保存 ${accounts.size} 个账户到数据库")
}

/**
 * 从数据库加载账户列表
 */
private fun loadAccounts(context: Context): List<TFACodeAccount> {
    val middleware = TwoFADatabaseMiddleware(context)
    val accountMaps = middleware.getAllAccounts()
    val accounts = mutableListOf<TFACodeAccount>()
    
    accountMaps.forEach { map ->
        val id = map["id"] as? String ?: return@forEach
        val name = map["name"] as? String ?: ""
        val issuer = map["issuer"] as? String ?: ""
        val secret = map["secret"] as? String ?: ""
        val typeStr = map["type"] as? String ?: "TOTP"
        val counter = (map["counter"] as? Long) ?: 0L
        
        val type = try {
            OtpType.valueOf(typeStr)
        } catch (e: Exception) {
            OtpType.TOTP
        }
        
        if (id.isNotEmpty() && secret.isNotEmpty()) {
            // 为 HOTP 生成初始代码（如果还没有）
            val initialCode = if (type == OtpType.HOTP && counter == 0L) {
                generateHOTP(secret, 0)
            } else {
                "000000"  // TOTP 会在 updatedAccounts 中自动生成
            }
            
            accounts.add(TFACodeAccount(
                id = id,
                name = name,
                issuer = issuer,
                secret = secret,
                type = type,
                code = initialCode,
                timeRemaining = if (type == OtpType.TOTP) 30 else 0,
                counter = counter
            ))
        }
    }
    
    Log.d("TwoFAScreen", "📖 已从数据库加载 ${accounts.size} 个账户")
    return accounts
}

/**
 * 导出验证器到 JSON
 */
private fun exportAccountsToJson(accounts: List<TFACodeAccount>): String {
    val jsonArray = JSONArray()
    accounts.forEach { account ->
        val jsonObject = JSONObject()
        jsonObject.put("id", account.id)
        jsonObject.put("name", account.name)
        jsonObject.put("issuer", account.issuer)
        jsonObject.put("secret", account.secret)
        jsonObject.put("type", account.type.name)
        jsonObject.put("counter", account.counter)
        jsonArray.put(jsonObject)
    }
    return jsonArray.toString(2) // 格式化输出
}

/**
 * 从 JSON 导入验证器
 */
private fun importAccountsFromJson(jsonString: String): List<TFACodeAccount> {
    val accounts = mutableListOf<TFACodeAccount>()
    try {
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val id = jsonObject.getString("id")
            val name = jsonObject.getString("name")
            val issuer = jsonObject.optString("issuer", "")
            val secret = jsonObject.getString("secret")
            val typeStr = jsonObject.optString("type", "TOTP")
            val counter = jsonObject.optLong("counter", 0L)
            
            val type = try {
                OtpType.valueOf(typeStr)
            } catch (e: Exception) {
                OtpType.TOTP
            }
            
            val initialCode = if (type == OtpType.HOTP) {
                generateHOTP(secret, counter)
            } else {
                "000000"
            }
            
            accounts.add(TFACodeAccount(
                id = id,
                name = name,
                issuer = issuer,
                secret = secret,
                type = type,
                code = initialCode,
                timeRemaining = if (type == OtpType.TOTP) 30 else 0,
                counter = counter
            ))
        }
    } catch (e: Exception) {
        Log.e("TwoFAScreen", "导入 JSON 失败", e)
    }
    return accounts
}

/**
 * 生成带时间戳的文件名
 */
private fun generateTimestampedFileName(): String {
    val sdf = SimpleDateFormat("yyyyy-MMm-ddd-HHh-mm m-SSs", Locale.getDefault())
    val timestamp = sdf.format(Date())
    return "$timestamp.json"
}

/**
 * 将 JSON 内容写入文件
 */
private fun writeJsonToFile(context: Context, folderUri: Uri, fileName: String, content: String): Boolean {
    return try {
        val documentFile = DocumentFile.fromTreeUri(context, folderUri)
            ?: return false
        
        val newFile = documentFile.createFile("application/json", fileName)
            ?: return false
        
        context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write(content)
                writer.flush()
            }
        }
        true
    } catch (e: Exception) {
        Log.e("TwoFAScreen", "写入文件失败", e)
        false
    }
}

/**
 * 从文件读取 JSON 内容
 */
private fun readJsonFromFile(context: Context, fileUri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        }
    } catch (e: Exception) {
        Log.e("TwoFAScreen", "读取文件失败", e)
        null
    }
}
