package com.lsfStudio.lsfTB.ui.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 确认对话框（Miuix 风格）
 * 支持 Markdown 格式的更新日志显示
 */
@Composable
fun ConfirmDialogMiuix(
    title: String,
    content: String?,
    isMarkdown: Boolean = false,
    confirmText: String = "前往下载",
    dismissText: String? = "取消", // 允许为 null，隐藏取消按钮
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    showDialog: MutableState<Boolean>
) {
    WindowDialog(
        show = showDialog.value,
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
        title = title,
        onDismissRequest = {
            onDismiss()
            showDialog.value = false
        },
        content = {
            Layout(
                content = {
                    content?.let {
                        Text(
                            text = it,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = if (dismissText != null) Arrangement.SpaceBetween else Arrangement.End,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        if (dismissText != null) {
                            TextButton(
                                text = dismissText,
                                onClick = {
                                    onDismiss()
                                    showDialog.value = false
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(20.dp))
                        }
                        TextButton(
                            text = confirmText,
                            onClick = {
                                onConfirm()
                                showDialog.value = false
                            },
                            modifier = if (dismissText != null) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            ) { measurables, constraints ->
                if (measurables.size != 2) {
                    val button = measurables[0].measure(constraints)
                    layout(constraints.maxWidth, button.height) {
                        button.place(0, 0)
                    }
                } else {
                    val button = measurables[1].measure(constraints)
                    val lazyList = measurables[0].measure(constraints.copy(maxHeight = constraints.maxHeight - button.height))
                    layout(constraints.maxWidth, lazyList.height + button.height) {
                        lazyList.place(0, 0)
                        button.place(0, lazyList.height)
                    }
                }
            }
        }
    )
}
