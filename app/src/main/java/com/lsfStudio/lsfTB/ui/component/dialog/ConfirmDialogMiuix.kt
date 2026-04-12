package com.lsfStudio.lsfTB.ui.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import com.lsfStudio.lsfTB.ui.util.HapticFeedbackUtil

/**
 * 确认对话框（Miuix 风格）
 * 支持 Markdown 格式的更新日志显示
 */
@Composable
fun ConfirmDialogMiuix(
    title: String,
    content: String?,
    isMarkdown: Boolean = true, // 默认启用 Markdown 支持
    confirmText: String = "前往下载",
    dismissText: String? = "取消", // 允许为 null，隐藏取消按钮
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    showDialog: MutableState<Boolean>
) {
    val context = LocalContext.current
    
    WindowDialog(
        show = showDialog.value,
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
        title = title,
        onDismissRequest = {
            onDismiss()
            showDialog.value = false
        },
        content = {
            val scrollState = rememberScrollState()
            
            Layout(
                content = {
                    content?.let {
                        if (isMarkdown) {
                            // 使用简单的 Markdown 渲染
                            val annotatedString = parseSimpleMarkdown(it)
                            Text(
                                text = annotatedString,
                                modifier = Modifier
                                    .padding(bottom = 12.dp)
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(scrollState)
                            )
                        } else {
                            Text(
                                text = it,
                                modifier = Modifier
                                    .padding(bottom = 12.dp)
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(scrollState)
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = if (dismissText != null) Arrangement.SpaceBetween else Arrangement.End,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        if (dismissText != null) {
                            TextButton(
                                text = dismissText,
                                onClick = {
                                    HapticFeedbackUtil.lightClick(context)
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
                                HapticFeedbackUtil.lightClick(context)
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

/**
 * 简单的 Markdown 解析器
 * 支持：
 * - ## 标题（加粗）
 * - **粗体**
 * - *斜体*
 * - - 列表项
 * - `代码`
 * - ``` 代码块
 */
@Composable
private fun parseSimpleMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        var inCodeBlock = false
        var codeBlockContent = StringBuilder()
        
        lines.forEachIndexed { index, line ->
            // 检查代码块开始/结束
            if (line.trimStart().startsWith("```") ) {
                if (inCodeBlock) {
                    // 代码块结束，渲染代码块内容
                    withStyle(SpanStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        background = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )) {
                        append(codeBlockContent.toString())
                    }
                    codeBlockContent.clear()
                    inCodeBlock = false
                    append("\n")
                } else {
                    // 代码块开始
                    inCodeBlock = true
                    codeBlockContent.clear()
                }
                return@forEachIndexed
            }
            
            if (inCodeBlock) {
                // 在代码块内，保留原始格式
                codeBlockContent.append(line).append("\n")
                return@forEachIndexed
            }
            
            when {
                // 二级标题 ## xxx
                line.startsWith("## ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = MiuixTheme.textStyles.headline2.fontSize)) {
                        append(line.removePrefix("## "))
                    }
                    append("\n")
                }
                // 三级标题 ### xxx
                line.startsWith("### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = MiuixTheme.textStyles.body1.fontSize)) {
                        append(line.removePrefix("### "))
                    }
                    append("\n")
                }
                // 列表项 - xxx
                line.trimStart().startsWith("- ") -> {
                    append("  • ")
                    append(line.trimStart().removePrefix("- "))
                    append("\n")
                }
                // 空行
                line.isBlank() -> {
                    append("\n")
                }
                // 普通文本，检查是否有 **粗体** 或 *斜体*
                else -> {
                    parseInlineMarkdown(line)
                    append("\n")
                }
            }
        }
        
        // 如果代码块未关闭，仍然渲染
        if (inCodeBlock && codeBlockContent.isNotEmpty()) {
            withStyle(SpanStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                background = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )) {
                append(codeBlockContent.toString())
            }
        }
    }
}

/**
 * 解析行内 Markdown（粗体、斜体、代码）
 */
@Composable
private fun AnnotatedString.Builder.parseInlineMarkdown(text: String) {
    var remaining = text
    
    while (remaining.isNotEmpty()) {
        when {
            // 粗体 **xxx**
            remaining.startsWith("**") -> {
                val endIndex = remaining.indexOf("**", 2)
                if (endIndex != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(remaining.substring(2, endIndex))
                    }
                    remaining = remaining.substring(endIndex + 2)
                } else {
                    append("**")
                    remaining = remaining.substring(2)
                }
            }
            // 斜体 *xxx*
            remaining.startsWith("*") && !remaining.startsWith("**") -> {
                val endIndex = remaining.indexOf("*", 1)
                if (endIndex != -1) {
                    withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                        append(remaining.substring(1, endIndex))
                    }
                    remaining = remaining.substring(endIndex + 1)
                } else {
                    append("*")
                    remaining = remaining.substring(1)
                }
            }
            // 代码 `xxx`
            remaining.startsWith("`") -> {
                val endIndex = remaining.indexOf("`", 1)
                if (endIndex != -1) {
                    withStyle(SpanStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        background = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )) {
                        append(remaining.substring(1, endIndex))
                    }
                    remaining = remaining.substring(endIndex + 1)
                } else {
                    append("`")
                    remaining = remaining.substring(1)
                }
            }
            // 普通字符
            else -> {
                val nextSpecial = listOf("**", "*", "`").mapNotNull { 
                    remaining.indexOf(it).takeIf { idx -> idx != -1 } 
                }.minOrNull() ?: remaining.length
                
                append(remaining.substring(0, nextSpecial))
                remaining = remaining.substring(nextSpecial)
            }
        }
    }
}
