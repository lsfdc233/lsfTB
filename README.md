# lsfTB

<div style="text-align: center;">

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.7+-4285F4?logo=jetpackcompose)
![API](https://img.shields.io/badge/API-31+-34A853?logo=android)
![Version](https://img.shields.io/badge/Version-v1.6.9-blue)

某神秘初中牲突发奇想基于miuix制作的个人数字中枢

</div>

---

## 项目简介

lsfTB 是一个采用最新 Android 技术栈构建的应用程序，具有以下特点：

- HyperOS 风格界面：基于 Miuix KMP 框架，提供原生的 MIUI 视觉体验
- 多主题支持：支持浅色、深色、跟随系统以及 Monet 动态取色
- 私密保险箱：使用 AES-256-GCM 硬件级加密保护用户文件
- 云端账户系统：支持设备绑定、用户签到、等级经验系统
- 服务端权限控制：基于服务端下发的 permissions 配置动态控制功能入口

## 特别鸣谢

- [KernelSU](https://github.com/tiann/KernelSU) - 令lsfdc突发奇想并提供UI设计思路
- [Miuix KMP](https://github.com/yukonga/Miuix-KMP) - MIUI 风格的 Compose Multiplatform UI 库
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Android 现代 UI 工具包
- [Backdrop](https://github.com/Kyant0/backdrop) - 模糊效果库

## 技术栈

### 核心框架
- **语言**：Kotlin 2.0+
- **UI 框架**：Jetpack Compose
- **UI 组件库**：Miuix KMP
- **架构**：MVVM + Clean Architecture
- **加密技术**：Android Keystore + AES-256-GCM
- **网络通信**：OkHttp3 + 双链路挑战-响应机制
- **数据存储**：SQLite（本地）+ MySQL（服务端）

### 构建工具
- **Gradle**：9.4.1
- **Android Gradle Plugin**：9.1.0
- **Compile SDK**：37 (Android 17)
- **Min SDK**：31 (Android 12)
- **Target SDK**：37 (Android 17)
- **NDK**：30.0.14904198
- **Java**：21

## 项目结构

```
lsfTB/
├── app/
│   ├── src/main/
│   │   ├── java/com/lsfStudio/lsfTB/
│   │   │   ├── data/                      # 数据层
│   │   │   │   ├── database/              # 数据库（空）
│   │   │   │   ├── model/                 # 数据模型
│   │   │   │   │   └── VaultFile.kt       # 保险箱文件模型
│   │   │   │   └── repository/            # 数据仓库接口与实现
│   │   │   │       ├── CategoryRepository.kt
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
│   │   │   │   │   ├── scanner/           # 二维码扫描器
│   │   │   │   │   ├── video/             # 视频播放器组件
│   │   │   │   │   ├── FloatingBottomBar.kt      # 浮动底部栏
│   │   │   │   │   ├── KeyEventBlocker.kt        # 按键事件拦截器
│   │   │   │   │   ├── MenuPositionProvider.kt   # 菜单位置提供者
│   │   │   │   │   └── UserInfoCard.kt           # 用户信息卡片
│   │   │   │   ├── modifier/              # Compose Modifier扩展
│   │   │   │   │   └── DragGestureInspector.kt   # 拖拽手势检查器
│   │   │   │   ├── navigation3/           # 导航系统
│   │   │   │   │   ├── DeepLinkResolver.kt       # 深度链接解析
│   │   │   │   │   ├── Navigator.kt              # 导航控制器
│   │   │   │   │   └── Routes.kt                 # 路由定义
│   │   │   │   ├── screen/                # 页面模块
│   │   │   │   │   ├── about/             # 关于页面
│   │   │   │   │   │   ├── AboutMiuix.kt
│   │   │   │   │   │   ├── AboutScreen.kt
│   │   │   │   │   │   ├── AboutUiState.kt
│   │   │   │   │   │   └── AboutUtils.kt
│   │   │   │   │   ├── colorpalette/      # 主题配色页面
│   │   │   │   │   │   ├── ColorPaletteScreen.kt
│   │   │   │   │   │   ├── ColorPaletteScreenMiuix.kt
│   │   │   │   │   │   └── ColorPaletteUiState.kt
│   │   │   │   │   ├── debug/             # Debug调试页面
│   │   │   │   │   │   └── DebugSettingsScreen.kt
│   │   │   │   │   ├── home/              # 主页
│   │   │   │   │   │   ├── HomeMiuix.kt
│   │   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   │   ├── HomeUiState.kt
│   │   │   │   │   │   └── HomeUtils.kt
│   │   │   │   │   ├── login/             # 登录页面
│   │   │   │   │   │   └── LoginScreen.kt
│   │   │   │   │   ├── morefeatures/      # 更多功能
│   │   │   │   │   │   └── MoreFeaturesScreen.kt
│   │   │   │   │   ├── register/          # 注册页面
│   │   │   │   │   │   └── RegisterScreen.kt
│   │   │   │   │   ├── settings/          # 设置页面
│   │   │   │   │   │   ├── SettingsMiuix.kt
│   │   │   │   │   │   ├── SettingsScreen.kt
│   │   │   │   │   │   └── SettingsUiState.kt
│   │   │   │   │   ├── twofa/             # 双因素认证
│   │   │   │   │   │   ├── TwoFADatabaseMiddleware.kt # 2FA数据库中间件
│   │   │   │   │   │   └── TwoFAScreen.kt
│   │   │   │   │   └── vault/             # 私密保险箱
│   │   │   │   │       ├── DatabaseTest.kt          # 数据库测试
│   │   │   │   │       ├── ImageViewerScreen.kt     # 图片查看器
│   │   │   │   │       ├── ProfessionalVideoPlayerScreen.kt # 专业视频播放器
│   │   │   │   │       ├── VaultScreen.kt           # 保险箱主页面
│   │   │   │   │       ├── VaultDatabaseHelper.kt   # Vault表结构定义
│   │   │   │   │       ├── VaultDatabaseMiddleware.kt # Vault数据库中间件
│   │   │   │   │       └── VideoPlayerScreen.kt     # 视频播放器
│   │   │   │   ├── theme/                 # 主题系统
│   │   │   │   │   ├── Colors.kt          # 颜色定义
│   │   │   │   │   ├── MiuixTheme.kt      # Miuix主题配置
│   │   │   │   │   └── Theme.kt           # 应用主题
│   │   │   │   ├── util/                  # 工具类
│   │   │   │   │   ├── AccountManager.kt  # 账户管理（SharedPreferences）
│   │   │   │   │   ├── AnimationConfig.kt # 动画配置
│   │   │   │   │   ├── BlurExt.kt         # 模糊效果扩展
│   │   │   │   │   ├── Colors.kt          # 颜色工具
│   │   │   │   │   ├── DataBase.kt        # 主数据库模块
│   │   │   │   │   ├── DebounceModifier.kt# 防抖修饰符
│   │   │   │   │   ├── DebounceUtils.kt   # 防抖工具函数
│   │   │   │   │   ├── Debug.kt           # Debug模式检测工具
│   │   │   │   │   ├── DebugShellReceiver.kt  # Debug调试接口
│   │   │   │   │   ├── DeferredContent.kt # 延迟内容加载
│   │   │   │   │   ├── DownloadManager.kt # 下载管理器
│   │   │   │   │   ├── ErrorReporter.kt   # 错误报告系统
│   │   │   │   │   ├── HanziToPinyin.java # 汉字转拼音
│   │   │   │   │   ├── HapticFeedbackUtil.kt  # 震动反馈工具
│   │   │   │   │   ├── KeystoreManager.kt # 密钥管理
│   │   │   │   │   ├── LatestVersionInfo.kt # 最新版本信息
│   │   │   │   │   ├── MessageManager.kt  # 消息管理器
│   │   │   │   │   ├── Network.kt         # 网络工具
│   │   │   │   │   ├── NetworkClient.kt   # 网络通信中枢
│   │   │   │   │   ├── NotificationHelper.kt # 通知助手
│   │   │   │   │   ├── OOBE.kt            # 设备绑定验证模块
│   │   │   │   │   ├── OemHelper.kt       # OEM检测工具
│   │   │   │   │   ├── ShareUtil.kt       # 分享工具
│   │   │   │   │   ├── ShizukuUtil.kt     # Shizuku工具
│   │   │   │   │   ├── SuperIslandHelper.kt # 超级岛助手
│   │   │   │   │   ├── UpdateChecker.kt   # 更新检查器
│   │   │   │   │   ├── UserManager.kt     # 用户信息管理（SQLite）
│   │   │   │   │   └── VaultEncryptionManager.kt  # Vault文件加密管理器
│   │   │   │   ├── viewmodel/             # ViewModel
│   │   │   │   │   ├── AboutViewModel.kt
│   │   │   │   │   ├── CategoryManager.kt
│   │   │   │   │   ├── CategoryViewModel.kt
│   │   │   │   │   ├── HomeViewModel.kt
│   │   │   │   │   ├── MainActivityUiState.kt
│   │   │   │   │   ├── MainActivityViewModel.kt
│   │   │   │   │   └── SettingsViewModel.kt
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

## 配置说明

### ABI 架构
当前项目仅支持 arm64-v8a 架构。

### 并行构建
项目已启用 Gradle 并行构建。

### Vault 文件加密

私密保险箱使用 AES-256-GCM 对文件进行硬件级加密：

- **加密算法**：AES-256-GCM（带认证标签）
- **密钥存储**：Android Keystore（硬件保护，不可导出）
- **文件格式**：.tb（加密后的文件扩展名）
- **临时文件**：预览时自动解密到 cache 目录，使用后删除


## 许可证

本项目采用 MIT 许可证。

---

<div style="text-align: center;">

Made by lsfdc 

</div>
