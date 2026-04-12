package com.lsfStudio.lsfTB.ui.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.window.WindowDialog
import com.lsfStudio.lsfTB.ui.util.HapticFeedbackUtil

/**
 * 统一弹窗管理器
 * 集中管理应用中所有的弹窗，方便统一调用和管理
 */
object DialogManager {
    
    /**
     * 显示确认对话框
     * 
     * @param show 控制对话框显示的状态
     * @param title 标题
     * @param message 消息内容
     * @param confirmText 确认按钮文本
     * @param dismissText 取消按钮文本（null则不显示）
     * @param onConfirm 确认回调
     * @param onDismiss 取消回调
     */
    @Composable
    fun ConfirmDialog(
        show: MutableState<Boolean>,
        title: String,
        message: String,
        confirmText: String = "确定",
        dismissText: String? = "取消",
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        val context = LocalContext.current
        
        WindowDialog(
            show = show.value,
            title = title,
            onDismissRequest = {
                onDismiss()
                show.value = false
            }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = message,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    horizontalArrangement = if (dismissText != null) Arrangement.SpaceBetween else Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (dismissText != null) {
                        TextButton(
                            text = dismissText,
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                onDismiss()
                                show.value = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    TextButton(
                        text = confirmText,
                        onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            onConfirm()
                            show.value = false
                        },
                        modifier = if (dismissText != null) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    }
    
    /**
     * 显示更新提示对话框
     * 
     * @param show 控制对话框显示的状态
     * @param versionName 新版本号
     * @param updateLog 更新日志
     * @param onDownload 下载回调
     * @param onDismiss 取消回调
     */
    @Composable
    fun UpdateDialog(
        show: MutableState<Boolean>,
        versionName: String,
        updateLog: String,
        onDownload: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        ConfirmDialogMiuix(
            title = "发现新版本 $versionName",
            content = updateLog,
            isMarkdown = true,
            confirmText = "立即下载",
            dismissText = "取消",
            onConfirm = onDownload,
            onDismiss = onDismiss,
            showDialog = show
        )
    }
    
    /**
     * 显示已是最新版本提示
     * 
     * @param show 控制对话框显示的状态
     * @param currentVersion 当前版本号
     * @param onDismiss 确定回调
     */
    @Composable
    fun LatestVersionDialog(
        show: MutableState<Boolean>,
        currentVersion: String,
        onDismiss: () -> Unit = {}
    ) {
        val context = LocalContext.current
        
        WindowDialog(
            show = show.value,
            title = "检查更新",
            onDismissRequest = {
                onDismiss()
                show.value = false
            }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "当前已是最新版本 ($currentVersion)",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                TextButton(
                    text = "确定",
                    onClick = {
                        HapticFeedbackUtil.lightClick(context)
                        onDismiss()
                        show.value = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
    
    /**
     * 显示添加认证方式选择对话框
     * 
     * @param show 控制对话框显示的状态
     * @param onScanQRCode 扫码添加回调
     * @param onManualInput 手动输入回调
     * @param onDismiss 取消回调
     */
    @Composable
    fun AddAuthMethodDialog(
        show: MutableState<Boolean>,
        onScanQRCode: () -> Unit = {},
        onManualInput: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        val context = LocalContext.current
        
        WindowDialog(
            show = show.value,
            title = "添加认证",
            onDismissRequest = {
                onDismiss()
                show.value = false
            }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "选择添加方式",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        text = "扫码添加",
                        onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            onScanQRCode()
                            show.value = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                    
                    Spacer(Modifier.width(12.dp))
                    
                    TextButton(
                        text = "手动输入",
                        onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            onManualInput()
                            show.value = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                TextButton(
                    text = "取消",
                    onClick = {
                        HapticFeedbackUtil.lightClick(context)
                        onDismiss()
                        show.value = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    
    /**
     * 显示手动添加认证对话框
     * 
     * @param show 控制对话框显示的状态
     * @param secretKey 密钥输入状态
     * @param accountName 账户名输入状态
     * @param onAdd 添加回调
     * @param onDismiss 取消回调
     */
    @Composable
    fun ManualAddAuthDialog(
        show: MutableState<Boolean>,
        secretKey: MutableState<String>,
        accountName: MutableState<String>,
        onAdd: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        val context = LocalContext.current
        
        WindowDialog(
            show = show.value,
            title = "手动添加认证",
            onDismissRequest = {
                onDismiss()
                show.value = false
            }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 这里需要根据实际的TextField实现来调整
                // 由于TextField需要导入，暂时留空，由调用方实现具体内容
                
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        text = "取消",
                        onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            onDismiss()
                            show.value = false
                        }
                    )
                    
                    Spacer(Modifier.width(12.dp))
                    
                    TextButton(
                        text = "添加",
                        onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            onAdd()
                            show.value = false
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    }
    
    /**
     * 显示标签编辑对话框
     * 
     * @param show 控制对话框显示的状态
     * @param tagInput 标签输入状态
     * @param hint 输入框提示文本
     * @param onSave 保存回调
     * @param onDismiss 取消回调
     */
    @Composable
    fun TagEditDialog(
        show: MutableState<Boolean>,
        tagInput: MutableState<String>,
        hint: String = "输入标签（用逗号分隔）",
        onSave: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        val context = LocalContext.current
        
        WindowDialog(
            show = show.value,
            title = "添加标签",
            onDismissRequest = {
                onDismiss()
                show.value = false
                tagInput.value = ""
            }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // TextField由调用方在外部实现，这里只提供框架
                // 实际使用时需要在调用方添加TextField
                
                Spacer(Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        text = "取消",
                        onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            show.value = false
                            tagInput.value = ""
                            onDismiss()
                        }
                    )
                    
                    Spacer(Modifier.width(12.dp))
                    
                    TextButton(
                        text = "确定",
                        onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            onSave()
                            show.value = false
                            tagInput.value = ""
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    }
}
