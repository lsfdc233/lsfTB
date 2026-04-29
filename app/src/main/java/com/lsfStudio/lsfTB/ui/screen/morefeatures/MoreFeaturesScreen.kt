package com.lsfStudio.lsfTB.ui.screen.morefeatures

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lsfStudio.lsfTB.ui.theme.LocalEnableBlur
import com.lsfStudio.lsfTB.ui.util.BlurredBar
import com.lsfStudio.lsfTB.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * 更多功能页面
 * 
 * 这是一个空白页面，用于展示更多功能入口
 * 类似 HomePage 的动态标题结构
 */
@Composable
fun MoreFeaturesScreen() {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface
    
    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = "更多功能",
                    scrollBehavior = scrollBehavior
                )
            }
        },
        popupHost = { }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "更多功能开发中...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = colorScheme.onSurfaceVariantSummary
            )
        }
    }
}
