package com.lsfStudio.lsfTB.ui.component.bottombar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.lsfStudio.lsfTB.ui.LocalMainPagerState
import com.lsfStudio.lsfTB.ui.component.FloatingBottomBar
import com.lsfStudio.lsfTB.ui.component.FloatingBottomBarItem
import com.lsfStudio.lsfTB.ui.theme.LocalEnableFloatingBottomBar
import com.lsfStudio.lsfTB.ui.theme.LocalEnableFloatingBottomBarBlur
import com.lsfStudio.lsfTB.ui.util.BlurredBar
import com.lsfStudio.lsfTB.ui.util.HapticFeedbackUtil
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.MoreCircle
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BottomBarMiuix(
    blurBackdrop: LayerBackdrop?,
    backdrop: Backdrop,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val mainState = LocalMainPagerState.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    val disableAllAnimations = com.lsfStudio.lsfTB.ui.theme.LocalDisableAllAnimations.current

    // 用于跟踪页面偏移量
    var pageOffset by remember { mutableFloatStateOf(0f) }
    
    // 监听页面偏移量变化，同步到Pager
    // 注意：由于 Compose Pager 的限制，无法实现实时页面跟随
    // 只能在拖动结束时切换页面
    LaunchedEffect(pageOffset) {
        if (!disableAllAnimations && pageOffset > 0f) {
            // 暂时禁用实时跟随，避免崩溃
            // mainState.syncPageOffset(pageOffset)
        }
    }

    val items = BottomBarDestination.entries.map { destination ->
        NavigationItem(
            label = destination.label,
            icon = destination.icon,
        )
    }
    if (!enableFloatingBottomBar) {
        BlurredBar(blurBackdrop) {
            NavigationBar(
                modifier = modifier,
                color = if (blurBackdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
                content = {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            modifier = Modifier.weight(1f),
                            icon = item.icon,
                            label = item.label,
                            selected = mainState.selectedPage == index,
                            onClick = {
                                // 仅在切换到不同页面时触发震动
                                if (mainState.selectedPage != index) {
                                    HapticFeedbackUtil.lightClick(context)
                                }
                                mainState.animateToPage(index, disableAllAnimations)
                            }
                        )
                    }
                }
            )
        }
    } else {
        FloatingBottomBar(
            modifier = modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            selectedIndex = { mainState.selectedPage },
            onSelected = { 
                // 仅在切换到不同页面时触发震动
                if (mainState.selectedPage != it) {
                    HapticFeedbackUtil.lightClick(context)
                }
                mainState.animateToPage(it, disableAllAnimations) 
            },
            backdrop = backdrop,
            tabsCount = items.size,
            isBlurEnabled = enableFloatingBottomBarBlur,
            onPageOffsetChanged = { offset ->
                // 更新页面偏移量，触发 LaunchedEffect
                pageOffset = offset
            },
        ) {
            items.forEachIndexed { index, item ->
                FloatingBottomBarItem(
                    onClick = {
                        // 仅在切换到不同页面时触发震动
                        if (mainState.selectedPage != index) {
                            HapticFeedbackUtil.lightClick(context)
                        }
                        mainState.animateToPage(index, disableAllAnimations)
                    },
                    modifier = Modifier.defaultMinSize(minWidth = 64.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }
    }
}

enum class BottomBarDestination(
    val label: String,
    val icon: ImageVector,
) {
    Home("主页", MiuixIcons.ChevronForward),  // Miuix 没有 Cottage，使用 ChevronForward 临时替代
    TwoFA("认证", MiuixIcons.Lock),  // Shield → Lock
    MoreFeatures("更多功能", MiuixIcons.MoreCircle),  // 新增：更多功能
    Vault("保险箱", MiuixIcons.Folder),
    Setting("我的", Icons.Rounded.Person)  // 设置 → 我的，使用用户头像图标
}
