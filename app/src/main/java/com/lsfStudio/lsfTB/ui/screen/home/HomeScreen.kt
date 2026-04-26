package com.lsfStudio.lsfTB.ui.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsfStudio.lsfTB.ui.navigation3.Navigator
import com.lsfStudio.lsfTB.ui.navigation3.Route
import com.lsfStudio.lsfTB.ui.viewmodel.HomeViewModel

@Composable
fun HomePager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true
) {
    val context = LocalContext.current
    val viewModel = viewModel<HomeViewModel> { HomeViewModel(context) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (isCurrentPage) {
        LaunchedEffect(Unit) {
            viewModel.refresh(context)
        }
    }

    val actions = HomeActions(
        onSettingsClick = { navigator.push(Route.Settings) },
        onAboutClick = { navigator.push(Route.About) },
        onOpenAbout = { navigator.push(Route.About) },
        onCheckUpdate = { ctx, enabled -> viewModel.checkUpdate(ctx, enabled) },
        onClearLatestVersionInfo = { viewModel.clearLatestVersionInfo() },
    )

    HomePagerMiuix(
        state = uiState,
        actions = actions,
        bottomInnerPadding = bottomInnerPadding,
    )
}
