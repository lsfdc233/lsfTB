# lsfTB

<div style="text-align: center;">

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.7+-4285F4?logo=jetpackcompose)
![API](https://img.shields.io/badge/API-31+-34A853?logo=android)

某神秘初中牲突发奇想基于miuix制作的个人数字中枢

</div>

---

## 📖 项目简介

lsfTB 是一个采用最新 Android 技术栈构建的应用程序，具有以下特点：

- 🎨 **HyperOS 风格界面**：基于 Miuix KMP 框架，提供原生的 MIUI 视觉体验
- 🌓 **多主题支持**：支持浅色、深色、跟随系统以及 Monet 动态取色
- 💩 **高性能(划掉)**：传奇史山

## 🙏 特别鸣谢

- [KernelSU](https://github.com/tiann/KernelSU) - 令lsfdc突发奇想并提供UI设计思路
- [Miuix KMP](https://github.com/yukonga/Miuix-KMP) - MIUI 风格的 Compose Multiplatform UI 库
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Android 现代 UI 工具包
- [Backdrop](https://github.com/Kyant0/backdrop) - 模糊效果库

## 🛠️ 技术栈

### 核心框架
- **语言**：Kotlin 2.0+
- **UI 框架**：Jetpack Compose
- **UI 组件库**：Miuix KMP
- **架构**：MVVM + Clean Architecture

### 构建工具
- **Gradle**：9.4.1
- **Android Gradle Plugin**：9.1.0
- **Compile SDK**：37 (Android 17)
- **Min SDK**：31 (Android 12)
- **Target SDK**：37 (Android 17)
- **NDK**：30.0.14904198
- **Java**：21

## 🏗️ 项目结构

```
lsfTB/
├── app/
│   ├── src/main/
│   │   ├── java/com/lsfStudio/lsfTB/
│   │   │   ├── data/                      # 数据层
│   │   │   │   ├── model/                 # 数据模型
│   │   │   │   │   └── VaultFile.kt      # 保险箱文件模型
│   │   │   │   └── repository/            # 数据仓库接口与实现
│   │   │   │       ├── SettingsRepository.kt
│   │   │   │       └── SettingsRepositoryImpl.kt
│   │   │   ├── ui/                        # UI 层
│   │   │   │   ├── animation/             # 动画效果
│   │   │   │   │   ├── DampedDragAnimation.kt    # 阻尼拖拽动画
│   │   │   │   │   └── InteractiveHighlight.kt   # 交互高亮效果
│   │   │   │   ├── component/             # 可复用UI组件
│   │   │   │   │   ├── bottombar/         # 底部导航栏相关
│   │   │   │   │   ├── dialog/            # 对话框组件
│   │   │   │   │   ├── filter/            # 筛选器组件
│   │   │   │   │   ├── FloatingBottomBar.kt      # 浮动底部栏
│   │   │   │   │   ├── KeyEventBlocker.kt        # 按键事件拦截器
│   │   │   │   │   └── MenuPositionProvider.kt   # 菜单位置提供者
│   │   │   │   ├── modifier/              # Compose Modifier扩展
│   │   │   │   │   └── DragGestureInspector.kt   # 拖拽手势检查器
│   │   │   │   ├── navigation3/           # 导航系统
│   │   │   │   │   ├── DeepLinkResolver.kt       # 深度链接解析
│   │   │   │   │   ├── Navigator.kt              # 导航控制器
│   │   │   │   │   └── Routes.kt                 # 路由定义
│   │   │   │   ├── screen/                # 页面模块
│   │   │   │   │   ├── about/             # 关于页面
│   │   │   │   │   │   ├── AboutScreen.kt
│   │   │   │   │   │   ├── AboutScreenMiuix.kt
│   │   │   │   │   │   ├── AboutViewModel.kt
│   │   │   │   │   │   └── UpdateChecker.kt
│   │   │   │   │   ├── colorpalette/      # 主题配色页面
│   │   │   │   │   │   ├── ColorPaletteScreen.kt
│   │   │   │   │   │   ├── ColorPaletteViewModel.kt
│   │   │   │   │   │   └── ColorUtils.kt
│   │   │   │   │   ├── home/              # 主页
│   │   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   │   ├── HomeScreenActions.kt
│   │   │   │   │   │   ├── HomeScreenMiuix.kt
│   │   │   │   │   │   └── HomeViewModel.kt
│   │   │   │   │   ├── settings/          # 设置页面
│   │   │   │   │   │   ├── SettingsScreen.kt
│   │   │   │   │   │   ├── SettingsScreenMiuix.kt
│   │   │   │   │   │   └── SettingsViewModel.kt
│   │   │   │   │   ├── twofa/             # 双因素认证
│   │   │   │   │   │   └── TwoFAScreen.kt
│   │   │   │   │   └── vault/             # 私密保险箱
│   │   │   │   │       ├── ImageViewerScreen.kt  # 图片/视频查看器
│   │   │   │   │       ├── VaultScreen.kt        # 保险箱主页面
│   │   │   │   │       └── VideoPlayerScreen.kt  # 视频播放器
│   │   │   │   ├── theme/                 # 主题系统
│   │   │   │   │   ├── Colors.kt          # 颜色定义
│   │   │   │   │   ├── MiuixTheme.kt      # Miuix主题配置
│   │   │   │   │   └── Theme.kt           # 应用主题
│   │   │   │   ├── util/                  # 工具类
│   │   │   │   │   ├── DialogManager.kt   # 对话框管理
│   │   │   │   │   ├── HapticFeedbackUtil.kt  # 震动反馈工具
│   │   │   │   │   ├── PermissionHelper.kt    # 权限助手
│   │   │   │   │   ├── ShareUtil.kt       # 分享工具
│   │   │   │   │   └── WindowInsetsUtil.kt    # 窗口 insets 工具
│   │   │   │   ├── viewmodel/             # ViewModel
│   │   │   │   │   ├── AboutViewModel.kt
│   │   │   │   │   ├── ColorPaletteViewModel.kt
│   │   │   │   │   ├── HomeViewModel.kt
│   │   │   │   │   ├── SettingsViewModel.kt
│   │   │   │   │   └── VaultViewModel.kt
│   │   │   │   ├── MainActivity.kt        # 主Activity
│   │   │   │   └── UiMode.kt              # UI模式枚举
│   │   │   └── lsfTBApplication.kt        # Application类
│   │   ├── res/                           # 资源文件
│   │   │   ├── drawable/                  # 矢量图形
│   │   │   ├── mipmap-*/                  # 启动图标（多密度）
│   │   │   │   ├── ic_launcher.webp
│   │   │   │   ├── ic_launcher_background.webp
│   │   │   │   ├── ic_launcher_foreground.webp
│   │   │   │   └── ic_launcher_round.webp
│   │   │   ├── values/                    # 值资源
│   │   │   │   ├── colors.xml             # 颜色定义
│   │   │   │   └── themes.xml             # 主题定义
│   │   │   └── xml/                       # XML配置
│   │   │       ├── backup_rules.xml       # 备份规则
│   │   │       ├── data_extraction_rules.xml  # 数据提取规则
│   │   │       ├── file_paths.xml         # 文件路径配置
│   │   │       ├── filepaths.xml          # 文件路径配置（兼容）
│   │   │       └── network_security_config.xml  # 网络安全配置
│   │   ├── assets/                        # 静态资源
│   │   │   └── github-markdown.css        # GitHub Markdown样式
│   │   └── AndroidManifest.xml            # 应用清单
│   ├── build.gradle.kts                   # 模块构建配置
│   └── proguard-rules.pro                 # ProGuard混淆规则
├── gradle/
│   ├── wrapper/                           # Gradle Wrapper
│   ├── gradle-daemon-jvm.properties       # Gradle守护进程JVM配置
│   └── libs.versions.toml                 # 依赖版本目录（Catalog）
├── build.gradle.kts                       # 根项目构建配置
├── settings.gradle.kts                    # 项目设置
├── gradle.properties                      # Gradle属性配置
├── local.properties                       # 本地配置（SDK路径等）
├── sign.properties                        # 签名配置（需自行创建）
├── lsfTB.jks                              # 签名密钥库
└── README.md                              # 项目说明文档
```

## ⚙️ 配置说明

### ABI 架构
当前项目仅支持 **arm64-v8a** 架构。

### 并行构建
项目已启用 Gradle 并行构建。

## 📄 许可证

什么是MIT许可证？




---

<div style="text-align: center;">

Made by lsfdc 

</div>
