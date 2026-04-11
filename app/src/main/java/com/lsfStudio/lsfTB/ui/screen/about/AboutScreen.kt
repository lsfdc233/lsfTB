package com.lsfStudio.lsfTB.ui.screen.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.dropUnlessResumed
import com.lsfStudio.lsfTB.BuildConfig
import com.lsfStudio.lsfTB.ui.navigation3.LocalNavigator

@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val htmlString = """
        在 <b><a href="https://github.com/tiann/KernelSU">GitHub</a></b> 查看源码<br/>
        加入我们的 <b><a href="https://t.me/KernelSU">Telegram</a></b> 频道<br/>
        加入我们的 <b><a href="https://pd.qq.com/s/8lipl1brp">QQ</a></b> 频道
    """.trimIndent()
    val state = AboutUiState(
        title = "关于",
        appName = "lsfTB",
        versionName = BuildConfig.VERSION_NAME,
        links = extractLinks(htmlString),
    )
    val actions = AboutScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onOpenLink = uriHandler::openUri,
    )

    AboutScreenMiuix(state, actions)
}
