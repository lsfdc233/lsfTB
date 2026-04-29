/**
 * 主Activity - 应用的入口点
 * 
 * 负责：
 * 1. 初始化应用UI和主题
 * 2. 管理系统栏样式（状态栏、导航栏）
 * 3. 设置导航系统和页面路由
 * 4. 提供全局CompositionLocal上下文
 * 
 * @author lsfTB Team
 */
package com.lsfStudio.lsfTB.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.lsfStudio.lsfTB.ui.component.bottombar.BottomBar
import com.lsfStudio.lsfTB.ui.component.bottombar.MainPagerState
import com.lsfStudio.lsfTB.ui.component.bottombar.SideRail
import com.lsfStudio.lsfTB.ui.component.bottombar.rememberMainPagerState
import com.lsfStudio.lsfTB.ui.navigation3.LocalNavigator
import com.lsfStudio.lsfTB.ui.navigation3.Navigator
import com.lsfStudio.lsfTB.ui.navigation3.Route
import com.lsfStudio.lsfTB.ui.navigation3.rememberNavigator
import com.lsfStudio.lsfTB.ui.screen.twofa.TwoFAScreen
import com.lsfStudio.lsfTB.ui.component.scanner.QRCodeScanner
import com.lsfStudio.lsfTB.ui.screen.about.AboutScreen
import com.lsfStudio.lsfTB.ui.screen.colorpalette.ColorPaletteScreen
import com.lsfStudio.lsfTB.ui.screen.debug.DebugSettingsScreen
import com.lsfStudio.lsfTB.ui.screen.home.HomePager
import com.lsfStudio.lsfTB.ui.screen.settings.SettingPager
import com.lsfStudio.lsfTB.ui.screen.vault.VaultScreen
import com.lsfStudio.lsfTB.ui.screen.vault.ImageViewerScreen
import com.lsfStudio.lsfTB.ui.screen.vault.VideoPlayerScreen
import com.lsfStudio.lsfTB.ui.screen.vault.ProfessionalVideoPlayerScreen
import com.lsfStudio.lsfTB.ui.theme.lsfTBTheme
import com.lsfStudio.lsfTB.ui.theme.LocalColorMode
import com.lsfStudio.lsfTB.ui.theme.LocalEnableBlur
import com.lsfStudio.lsfTB.ui.theme.LocalEnableFloatingBottomBar
import com.lsfStudio.lsfTB.ui.theme.LocalEnableFloatingBottomBarBlur
import com.lsfStudio.lsfTB.ui.util.rememberBlurBackdrop
import com.lsfStudio.lsfTB.ui.util.rememberContentReady
import com.lsfStudio.lsfTB.ui.viewmodel.MainActivityViewModel
import com.lsfStudio.lsfTB.BuildConfig
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.layerBackdrop as miuixLayerBackdrop
class MainActivity : ComponentActivity() {

    /**
     * Activity创建时的初始化逻辑
     * 
     * 执行流程：
     * 1. 设置Jetpack Compose内容视图
     * 2. 初始化ViewModel并观察UI状态
     * 3. 根据主题模式配置系统栏样式
     * 4. 设置导航器和页面缩放
     * 5. 提供全局CompositionLocal上下文
     * 6. 渲染NavDisplay导航系统
     */
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 📦 第一步：确保 MetaData 表存在（必须在 OOBE 之前）
        try {
            val dataBase = com.lsfStudio.lsfTB.ui.util.DataBase(applicationContext)
            if (!dataBase.tableExists(com.lsfStudio.lsfTB.ui.util.DataBase.TABLE_METADATA)) {
                android.util.Log.w("MainActivity", "⚠️ MetaData 表不存在，正在创建...")
                dataBase.executeSQL("""
                    CREATE TABLE IF NOT EXISTS ${com.lsfStudio.lsfTB.ui.util.DataBase.TABLE_METADATA} (
                        ${com.lsfStudio.lsfTB.ui.util.DataBase.COL_META_KEY} TEXT PRIMARY KEY,
                        ${com.lsfStudio.lsfTB.ui.util.DataBase.COL_META_VALUE} BLOB
                    )
                """)
                android.util.Log.d("MainActivity", "✅ MetaData 表创建成功")
            } else {
                android.util.Log.d("MainActivity", "✅ MetaData 表已存在")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ MetaData 表初始化失败", e)
        }
        
        // 🔧 第二步：OOBE 初始化（设备绑定验证）- 可选功能
        try {
            // 直接调用 OOBE 单例方法，不使用反射
            val oobeResult = com.lsfStudio.lsfTB.ui.util.OOBE.initialize(applicationContext)
            
            if (oobeResult) {
                android.util.Log.d("MainActivity", "✅ OOBE 初始化完成")
                // 🔄 注意：启动时检查更新已移至 HomeMiuix，由 Settings 开关控制
            } else {
                android.util.Log.w("MainActivity", "⚠️ OOBE 验证未通过")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ OOBE 初始化失败", e)
        }
        
        // 🗄️ Vault数据库中间件初始化（确保表存在）
        try {
            val vaultMiddleware = com.lsfStudio.lsfTB.ui.screen.vault.VaultDatabaseMiddleware(applicationContext)
            vaultMiddleware.initialize()
            android.util.Log.d("MainActivity", "✅ Vault数据库中间件初始化完成")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Vault数据库中间件初始化失败", e)
        }
        
        // 🔐 2FA数据库中间件初始化（确保表存在）
        try {
            val twoFAMiddleware = com.lsfStudio.lsfTB.ui.screen.twofa.TwoFADatabaseMiddleware(applicationContext)
            twoFAMiddleware.initialize()
            android.util.Log.d("MainActivity", "✅ 2FA数据库中间件初始化完成")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ 2FA数据库中间件初始化失败", e)
        }
        
        // 🧪 数据库测试（仅调试模式，测试完成后请注释）
        // if (BuildConfig.DEBUG) {
        //     com.lsfStudio.lsfTB.ui.screen.vault.DatabaseTest.runTest(applicationContext)
        // }

        // 设置Compose内容视图
        setContent {
            // 初始化MainActivityViewModel，管理全局UI状态
            val viewModel = viewModel<MainActivityViewModel> {
                MainActivityViewModel(applicationContext)
            }
            
            // 收集UI状态流，使用Lifecycle感知确保只在活跃时更新
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val appSettings = uiState.appSettings
            
            // 计算当前是否为深色模式
            // 优先级：用户设置 > 系统设置
            val darkMode = appSettings.colorMode.isDark || (appSettings.colorMode.isSystem && isSystemInDarkTheme())

            // 监听深色模式变化，动态更新系统栏样式
            DisposableEffect(darkMode) {
                // 启用边缘到边缘显示（Edge-to-Edge）
                enableEdgeToEdge(
                    // 状态栏样式：自动适配深色/浅色模式，背景透明
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                    // 导航栏样式：自动适配深色/浅色模式，背景透明
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                )
                // 禁用导航栏对比度强制，允许完全透明
                window.isNavigationBarContrastEnforced = false
                onDispose { }
            }

            // 创建导航器，默认路由为Main页面
            val navigator = rememberNavigator(Route.Main)
            
            // 获取系统密度，用于计算页面缩放
            val systemDensity = LocalDensity.current
            // 根据用户设置的pageScale计算实际密度
            // 这样可以实现页面整体缩放效果
            val density = remember(systemDensity, uiState.pageScale) {
                Density(systemDensity.density * uiState.pageScale, systemDensity.fontScale)
            }

            // 提供全局CompositionLocal上下文，所有子Composable都可以访问这些值
            CompositionLocalProvider(
                // 导航器：用于页面跳转
                LocalNavigator provides navigator,
                // 密度：用于页面缩放
                LocalDensity provides density,
                // 颜色模式：控制主题颜色
                LocalColorMode provides appSettings.colorMode.value,
                // 模糊效果开关
                LocalEnableBlur provides uiState.enableBlur,
                // 浮动底栏开关
                LocalEnableFloatingBottomBar provides uiState.enableFloatingBottomBar,
                // 浮动底栏模糊效果开关
                LocalEnableFloatingBottomBarBlur provides uiState.enableFloatingBottomBarBlur
            ) {
                // 应用主题，传入应用设置
                lsfTBTheme(appSettings = appSettings) {
                    // 定义导航显示组件
                    val navDisplay = @Composable {
                        NavDisplay(
                            // 导航回退栈
                            backStack = navigator.backStack,
                            // 导航条目装饰器：支持状态保存和ViewModel
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator()
                            ),
                            // 返回按钮处理
                            onBack = { navigator.pop() },
                            // 路由提供者：定义所有可导航的页面
                            entryProvider = entryProvider {
                                // 主页路由
                                entry<Route.Main> { MainScreen() }
                                // 设置页路由（复用MainScreen）
                                entry<Route.Settings> { MainScreen() }
                                // 关于页面路由
                                entry<Route.About> { AboutScreen() }
                                // Debug 设置页面路由
                                entry<Route.Debug> { DebugSettingsScreen() }
                                // 调色板/主题设置页面路由
                                entry<Route.ColorPalette> { ColorPaletteScreen() }
                                // 2FA 双因素认证页面路由
                                entry<Route.TwoFA> { TwoFAScreen() }
                                // 二维码扫描器路由
                                entry<Route.QRCodeScanner> { route ->
                                    QRCodeScanner(
                                        title = route.title,
                                        hint = route.hint,
                                        onScanSuccess = { result ->
                                            // 扫描成功后直接 pop，不 setResult
                                            Log.d("MainActivity", "扫码成功: $result")
                                            try {
                                                // 先设置结果（如果 navigator 可用）
                                                val requestKey = "qr_code_scan_result"
                                                if (navigator != null) {
                                                    navigator.setResult<String>(requestKey, result)
                                                    Log.d("MainActivity", "setResult 完成")
                                                } else {
                                                    Log.w("MainActivity", "navigator 为 null，跳过 setResult")
                                                }
                                                
                                                // 延迟后 pop
                                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                    try {
                                                        Log.d("MainActivity", "执行 pop, backStack size: ${navigator?.backStack?.size ?: 0}")
                                                        if (navigator != null && navigator.backStack.size > 1) {
                                                            navigator.pop()
                                                            Log.d("MainActivity", "pop 成功")
                                                        } else {
                                                            Log.w("MainActivity", "无法 pop: navigator=${navigator != null}, backStack size=${navigator?.backStack?.size ?: 0}")
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("MainActivity", "pop 失败", e)
                                                    }
                                                }, 500) // 增加到500ms确保稳定
                                            } catch (e: Exception) {
                                                Log.e("MainActivity", "扫码回调异常", e)
                                            }
                                        },
                                        onDismiss = {
                                            Log.d("MainActivity", "onDismiss, backStack size: ${navigator?.backStack?.size ?: 0}")
                                            try {
                                                if (navigator != null && navigator.backStack.size > 1) {
                                                    navigator.pop()
                                                }
                                            } catch (e: Exception) {
                                                Log.e("MainActivity", "onDismiss pop 失败", e)
                                            }
                                        }
                                    )
                                }
                                // 图片查看器路由
                                entry<Route.ImageViewer> { route ->
                                    // 将路径列表转换为VaultFile列表
                                    val allFiles = if (route.allFilePaths.isNotEmpty()) {
                                        route.allFilePaths.mapIndexed { index, path ->
                                            com.lsfStudio.lsfTB.data.model.VaultFile(
                                                id = route.allAddedTimes.getOrNull(index) ?: 0L,
                                                originalName = route.allFileNames.getOrNull(index) ?: "",
                                                encryptedName = "",
                                                filePath = path,
                                                fileType = com.lsfStudio.lsfTB.data.model.FileType.IMAGE,
                                                tags = emptyList(),
                                                addedTime = route.allAddedTimes.getOrNull(index) ?: 0L
                                            )
                                        }
                                    } else {
                                        null
                                    }
                                    
                                    ImageViewerScreen(
                                        filePath = route.filePath,
                                        fileName = route.fileName,
                                        addedTime = route.addedTime,
                                        fileId = route.fileId,
                                        onBack = { navigator.pop() },
                                        allFiles = allFiles,
                                        currentIndex = route.currentIndex,
                                        onFileChanged = { newIndex ->
                                            // 当文件切换时，更新路由（不推新页面，只是更新当前页面内容）
                                            // 这里实际上不需要做任何事，因为ImageViewerScreen内部会处理
                                        }
                                    )
                                }
                                // 视频播放器路由
                                entry<Route.VideoPlayer> { route ->
                                    // 将路径列表转换为VaultFile列表
                                    val allFiles = if (route.allFilePaths.isNotEmpty()) {
                                        route.allFilePaths.mapIndexed { index, path ->
                                            com.lsfStudio.lsfTB.data.model.VaultFile(
                                                id = route.allAddedTimes.getOrNull(index) ?: 0L,
                                                originalName = route.allFileNames.getOrNull(index) ?: "",
                                                encryptedName = "",
                                                filePath = path,
                                                fileType = com.lsfStudio.lsfTB.data.model.FileType.VIDEO,
                                                tags = emptyList(),
                                                addedTime = route.allAddedTimes.getOrNull(index) ?: 0L
                                            )
                                        }
                                    } else {
                                        null
                                    }
                                    
                                    VideoPlayerScreen(
                                        filePath = route.filePath,
                                        fileName = route.fileName,
                                        fileId = route.fileId,
                                        onBack = { navigator.pop() },
                                        allFiles = allFiles,
                                        currentIndex = route.currentIndex
                                    )
                                }
                                // 横屏专业视频播放器路由
                                entry<Route.ProfessionalVideoPlayer> { route ->
                                    val allFiles = if (route.allFilePaths.isNotEmpty()) {
                                        route.allFilePaths.mapIndexed { index, path ->
                                            com.lsfStudio.lsfTB.data.model.VaultFile(
                                                id = route.allAddedTimes.getOrNull(index) ?: 0L,
                                                originalName = route.allFileNames.getOrNull(index) ?: "",
                                                encryptedName = "",
                                                filePath = path,
                                                fileType = com.lsfStudio.lsfTB.data.model.FileType.VIDEO,
                                                tags = emptyList(),
                                                addedTime = route.allAddedTimes.getOrNull(index) ?: 0L
                                            )
                                        }
                                    } else {
                                        null
                                    }
                                    
                                    ProfessionalVideoPlayerScreen(
                                        filePath = route.filePath,
                                        fileName = route.fileName,
                                        fileId = route.fileId,
                                        onBack = { navigator.pop() },
                                        allFiles = allFiles,
                                        currentIndex = route.currentIndex
                                    )
                                }
                            }
                        )
                    }

                    // 使用Scaffold作为根布局
                    Scaffold { navDisplay() }
                }
            }
        }
    }
}

/**
 * 主页Pager状态的CompositionLocal键
 * 
 * 用于在组件树中共享主页Pager的状态信息
 * 如果未提供，访问时会抛出错误
 */
val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> { error("LocalMainPagerState not provided") }

/**
 * 底部导航栏可见性状态的CompositionLocal键
 * 
 * 用于控制底部导航栏的显示/隐藏
 * 第一个值：是否显示底部导航栏
 * 第二个值：设置可见性的函数
 */
val LocalBottomBarVisibility = staticCompositionLocalOf<Pair<Boolean, (Boolean) -> Unit>> { error("LocalBottomBarVisibility not provided") }

/**
 * 主屏幕Composable函数
 * 
 * 显示包含两个页面的HorizontalPager：
 * - 页面0：HomePager（主页）
 * - 页面1：SettingPager（设置页）
 * 
 * 特性：
 * - 支持底部导航栏
 * - 支持模糊背景效果
 * - 支持浮动底栏
 * - 页面切换动画
 * 
 * @param bottomInnerPadding 底部内边距
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen() {
    // 获取导航控制器
    val navController = LocalNavigator.current
    // 获取模糊效果开关状态
    val enableBlur = LocalEnableBlur.current
    // 获取浮动底栏开关状态
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    // 获取浮动底栏模糊效果开关状态
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    
    // 创建Pager状态，共5个页面
    val pagerState = rememberPagerState(pageCount = { 5 })
    // 创建主页Pager状态管理器
    val mainPagerState = rememberMainPagerState(pagerState)
    
    // 获取表面颜色，用于背景绘制
    val surfaceColor = MiuixTheme.colorScheme.surface
    // 创建模糊背景
    val blurBackdrop = rememberBlurBackdrop(enableBlur)

    // 创建图层背景，用于非模糊状态下的背景绘制
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    // 监听当前页面变化，同步页面状态
    LaunchedEffect(mainPagerState.pagerState.currentPage) {
        mainPagerState.syncPage()
    }
    
    // 底部导航栏可见性状态
    var isBottomBarVisible by remember { mutableStateOf(true) }
    val setBottomBarVisibility: (Boolean) -> Unit = { visible ->
        isBottomBarVisible = visible
    }

    // 提供MainPagerState给子组件
    CompositionLocalProvider(
        LocalMainPagerState provides mainPagerState,
        LocalBottomBarVisibility provides Pair(isBottomBarVisible, setBottomBarVisibility)
    ) {
        // 检查内容是否已准备好
        val contentReady = rememberContentReady()
        
        // 定义Pager内容Composable
        val pagerContent = @Composable { bottomInnerPadding: Dp ->
            // 如果启用了模糊效果，应用模糊背景修饰符
            Box(modifier = if (blurBackdrop != null) Modifier.miuixLayerBackdrop(blurBackdrop) else Modifier) {
                // 水平分页器，包含主页、2FA 和设置页
                HorizontalPager(
                    modifier = Modifier
                        // 如果启用浮动底栏且启用模糊，应用图层背景
                        .then(if (enableFloatingBottomBar && enableFloatingBottomBarBlur) Modifier.layerBackdrop(backdrop) else Modifier),
                    state = mainPagerState.pagerState,
                    // 视口外页面数量（0表示不预加载）
                    beyondViewportPageCount = if (contentReady) 0 else 0,
                    // 禁用用户手动滑动切换页面
                    userScrollEnabled = false,
                ) { page ->
                    // 判断当前页面是否为已稳定显示的页面
                    val isCurrentPage = page == mainPagerState.pagerState.settledPage
                    // 只在当前页面或内容已准备好时渲染
                    if (isCurrentPage || contentReady) {
                        when (page) {
                            // 页面0：主页
                            0 -> HomePager(navController, bottomInnerPadding, isCurrentPage)
                            // 页面1：2FA 双因素认证
                            1 -> TwoFAScreen()
                            // 页面2：更多功能
                            2 -> com.lsfStudio.lsfTB.ui.screen.morefeatures.MoreFeaturesScreen()
                            // 页面3：私密保险箱
                            3 -> VaultScreen()
                            // 页面4：设置页
                            4 -> SettingPager(navController, bottomInnerPadding)
                        }
                    }
                }
            }
        }

        // 定义底部导航栏Composable - 支持动画隐藏
        val bottomBar = @Composable {
            androidx.compose.animation.AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = androidx.compose.animation.slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
                ),
                exit = androidx.compose.animation.slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BottomBar(
                        blurBackdrop = blurBackdrop,
                        backdrop = backdrop,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }

        // 使用Scaffold布局，包含底部导航栏
        Scaffold(bottomBar = bottomBar) { innerPadding ->
            // 渲染Pager内容，传入底部内边距
            pagerContent(innerPadding.calculateBottomPadding())
        }
    }
}
