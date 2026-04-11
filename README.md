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
- **Compile SDK**：37 (Android 15)
- **Min SDK**：31 (Android 12)
- **Target SDK**：37 (Android 15)
- **NDK**：30.0.14904198
- **Java**：21

## 🏗️ 项目结构

```
lsfTB/
├── app/
│   ├── src/main/
│   │   ├── java/com/lsfStudio/lsfTB/
│   │   │   ├── data/              # 数据层
│   │   │   │   ├── model/         # 数据模型
│   │   │   │   └── repository/    # 数据仓库
│   │   │   ├── ui/                # UI 层
│   │   │   │   ├── component/     # 可复用组件
│   │   │   │   ├── navigation3/   # 导航系统
│   │   │   │   ├── screen/        # 页面
│   │   │   │   │   ├── about/     # 关于页面
│   │   │   │   │   ├── colorpalette/  # 主题设置页面
│   │   │   │   │   ├── home/      # 主页
│   │   │   │   │   └── settings/  # 设置页面
│   │   │   │   ├── theme/         # 主题系统
│   │   │   │   ├── util/          # 工具类
│   │   │   │   ├── viewmodel/     # ViewModel
│   │   │   │   └── MainActivity.kt
│   │   │   ├── lsfTBApplication.kt  # Application 类
│   │   │   └── Natives.kt         # Native 方法
│   │   ├── res/                   # 资源文件
│   │   │   ├── mipmap-*/          # 启动图标
│   │   │   ├── values/            # 主题和颜色
│   │   │   └── xml/               # XML 配置
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml         # 依赖版本管理
├── build.gradle.kts               # 根项目配置
├── settings.gradle.kts            # 项目设置
└── sign.properties                # 签名配置（需自行创建）
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
