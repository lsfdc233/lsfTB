package com.lsfStudio.lsfTB.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import kotlinx.coroutines.launch

/**
 * Compose按钮防抖扩展
 * 
 * 为Clickable添加防抖功能，防止快速重复点击
 * 
 * @param enabled 是否启用点击
 * @param debounceMs 防抖延迟时间（毫秒），默认500ms
 * @param onClick 点击回调
 */
@Composable
fun Modifier.debounceClick(
    enabled: Boolean = true,
    debounceMs: Long = 500L,
    onClick: () -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    
    this.then(
        Modifier.clickable(
            enabled = enabled && !isProcessing,
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) {
            if (!isProcessing) {
                isProcessing = true
                scope.launch {
                    try {
                        onClick()
                    } finally {
                        // 延迟重置状态，确保动画完成
                        kotlinx.coroutines.delay(debounceMs)
                        isProcessing = false
                    }
                }
            }
        }
    )
}

/**
 * 简化的防抖点击（使用DebounceUtils）
 * 
 * @param key 操作唯一标识
 * @param debounceMs 防抖延迟时间（毫秒）
 * @param onClick 点击回调
 */
@Composable
fun Modifier.debounceClickWithKey(
    key: String,
    debounceMs: Long = 500L,
    onClick: suspend () -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    
    this.then(
        Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) {
            DebounceUtils.debounce(scope, key, debounceMs) {
                onClick()
            }
        }
    )
}
