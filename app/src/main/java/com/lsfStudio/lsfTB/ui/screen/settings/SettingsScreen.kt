package com.lsfStudio.lsfTB.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsfStudio.lsfTB.ui.navigation3.Navigator
import com.lsfStudio.lsfTB.ui.navigation3.Route
import com.lsfStudio.lsfTB.ui.viewmodel.SettingsViewModel

@Composable
fun SettingPager(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {
    val context = LocalContext.current
    val viewModel = viewModel<SettingsViewModel> {
        SettingsViewModel(context)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val actions = SettingsScreenActions(
        onSetCheckUpdate = viewModel::setCheckUpdate,
        onOpenTheme = { navigator.push(Route.ColorPalette) },
        onSetEnableWebDebugging = viewModel::setEnableWebDebugging,
        onOpenAbout = { navigator.push(Route.About) },
        onOpenDebug = { navigator.push(Route.Debug) },  // Debug 设置入口
        onSetDisableAllAnimations = viewModel::setDisableAllAnimations,  // 去掉所有动画效果
        onOpenLogin = { navigator.push(Route.Login) },  // 打开登录页面
    )

    SettingPagerMiuix(uiState, actions, bottomInnerPadding)
}
