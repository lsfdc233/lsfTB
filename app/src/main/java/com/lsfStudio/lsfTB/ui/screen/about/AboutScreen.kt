package com.lsfStudio.lsfTB.ui.screen.about

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsfStudio.lsfTB.BuildConfig
import com.lsfStudio.lsfTB.ui.navigation3.LocalNavigator
import com.lsfStudio.lsfTB.ui.viewmodel.AboutViewModel

@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val viewModel = viewModel<AboutViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val htmlString = """
        在 <b><a href="https://github.com/lsfdc233/lsfTB">GitHub</a></b> 查看源码
    """.trimIndent()
    
    // 初始化 ViewModel
    LaunchedEffect(Unit) {
        viewModel.initialize(
            appName = "lsfTB",
            versionName = BuildConfig.VERSION_NAME,
            links = extractLinks(htmlString)
        )
    }
    
    val actions = AboutScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onOpenLink = uriHandler::openUri,
        onCheckUpdate = { viewModel.checkUpdate(context) },
        onDismissUpToDateDialog = { viewModel.dismissUpToDateDialog() },
    )

    AboutScreenMiuix(uiState, actions)
}
