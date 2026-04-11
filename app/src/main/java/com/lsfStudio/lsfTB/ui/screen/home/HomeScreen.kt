package com.lsfStudio.lsfTB.ui.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    val viewModel = viewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (isCurrentPage) {
        LaunchedEffect(Unit) {
            viewModel.refresh()
        }
    }

    val actions = HomeActions(
        onSettingsClick = { navigator.push(Route.Settings) },
        onAboutClick = { navigator.push(Route.About) },
        onOpenAbout = { navigator.push(Route.About) },
    )

    HomePagerMiuix(
        state = uiState,
        actions = actions,
        bottomInnerPadding = bottomInnerPadding,
    )
}
