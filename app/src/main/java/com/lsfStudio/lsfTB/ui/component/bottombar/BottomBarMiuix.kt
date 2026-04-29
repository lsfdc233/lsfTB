package com.lsfStudio.lsfTB.ui.component.bottombar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    val mainState = LocalMainPagerState.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current

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
                                mainState.animateToPage(index)
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
            onSelected = { mainState.animateToPage(it) },
            backdrop = backdrop,
            tabsCount = items.size,
            isBlurEnabled = enableFloatingBottomBarBlur,
        ) {
            items.forEachIndexed { index, item ->
                FloatingBottomBarItem(
                    onClick = {
                        mainState.animateToPage(index)
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
    Setting("设置", MiuixIcons.Settings)
}
