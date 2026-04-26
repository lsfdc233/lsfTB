@file:Suppress("UnstableApiUsage")

import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.lsplugin.apksign)
    id("kotlin-parcelize")
}

val androidCompileSdkVersion: Int by rootProject.extra
val androidCompileSdkVersionMinor: Int by rootProject.extra
val androidCompileNdkVersion: String by rootProject.extra
val androidBuildToolsVersion: String by rootProject.extra
val androidMinSdkVersion: Int by rootProject.extra
val androidTargetSdkVersion: Int by rootProject.extra
val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra
val managerVersionCode: Int by rootProject.extra
val managerVersionName: String by rootProject.extra

android {
    namespace = "com.lsfStudio.lsfTB"
    val isPrBuild = project.findProperty("IS_PR_BUILD")?.toString()?.toBoolean() ?: false

    buildTypes {
        debug {
            externalNativeBuild {
                cmake {
                    arguments += listOf("-DCMAKE_CXX_FLAGS_DEBUG=-Og", "-DCMAKE_C_FLAGS_DEBUG=-Og")
                }
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            if (isPrBuild) applicationIdSuffix = ".dev"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            externalNativeBuild {
                cmake {
                    arguments += "-DDEBUG_SYMBOLS_PATH=${layout.buildDirectory.get().asFile.absolutePath}/symbols"
                    arguments += "-DCMAKE_BUILD_TYPE=Release"

                    val releaseFlags = listOf(
                        "-flto", "-ffunction-sections", "-fdata-sections", "-Wl,--gc-sections",
                        "-fno-unwind-tables", "-fno-asynchronous-unwind-tables", "-Wl,--exclude-libs,ALL"
                    )
                    val configFlags = listOf("-Oz", "-DNDEBUG").joinToString(" ")

                    cppFlags += releaseFlags
                    cFlags += releaseFlags

                    arguments += listOf(
                        "-DCMAKE_CXX_FLAGS_RELEASE=$configFlags",
                        "-DCMAKE_C_FLAGS_RELEASE=$configFlags",
                        "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,--gc-sections -Wl,--exclude-libs,ALL -Wl,--icf=all -s -Wl,--hash-style=sysv -Wl,-z,norelro"
                    )
                }
            }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        dex {
            useLegacyPackaging = true
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }



    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    androidResources {
        generateLocaleConfig = true
    }
    compileSdk {
        version =
            release(androidCompileSdkVersion) {
                minorApiLevel = androidCompileSdkVersionMinor
            }
    }
    buildToolsVersion = androidBuildToolsVersion
    ndkVersion = androidCompileNdkVersion

    defaultConfig {
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        versionCode = managerVersionCode
        versionName = managerVersionName

        buildConfigField("boolean", "IS_PR_BUILD", isPrBuild.toString())
        
        // 从 gradle.properties 或 local.properties 读取 GitHub Token（可选）
        val githubToken = project.findProperty("GITHUB_TOKEN")?.toString() ?: ""
        buildConfigField("String", "GITHUB_TOKEN", "\"$githubToken\"")
        
        // 服务器 URL 配置
        val serverUrl = project.findProperty("SERVER_URL")?.toString() ?: "https://www.lsfstudio.top/lsfStudio/api"
        buildConfigField("String", "SERVER_URL", "\"$serverUrl\"")
        
        // 添加构建时间
        val buildTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")
        
        // 只保留 arm64-v8a 架构，移除其他架构以减小APK体积
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
        
        // 📦 启用 16KB 页支持（Android 15+）
        // 确保原生库与 16KB 页面对齐
        packaging {
            jniLibs {
                useLegacyPackaging = false
            }
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
        // 忽略 UnstableApi 的 OptIn 检查（Media3 API）
        disable += "UnsafeOptInUsageError"
    }

    compileOptions {
        sourceCompatibility = androidSourceCompatibility
        targetCompatibility = androidTargetCompatibility
    }
    
    // 配置签名 - 支持 v2+v3+v4
    val keystorePropertiesFile = rootProject.file("sign.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
        
        signingConfigs {
            create("release") {
                // 密钥库文件路径相对于项目根目录
                storeFile = rootProject.file(keystoreProperties.getProperty("KEYSTORE_FILE", ""))
                storePassword = keystoreProperties.getProperty("KEYSTORE_PASSWORD", "")
                keyAlias = keystoreProperties.getProperty("KEY_ALIAS", "")
                keyPassword = keystoreProperties.getProperty("KEY_PASSWORD", "")
                
                // 启用 v2 签名（Android 7.0+）
                enableV2Signing = true
                
                // 启用 v3 签名（Android 9.0+）
                enableV3Signing = true
                
                // 启用 v4 签名（Android 11+，用于流式安装）
                enableV4Signing = true
            }
        }
        
        buildTypes {
            release {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) {
        it.packaging.resources.excludes.addAll(listOf("META-INF/**", "kotlin/**", "org/**", "**.bin"))
    }
}

base {
    archivesName.set(
        "lsfTB_${managerVersionName}_${managerVersionCode}"
    )
}

// 配置APK签名
// 签名信息存储在 sign.properties 文件中
// 需要填入以下信息：
// - KEYSTORE_FILE: 密钥库文件路径（相对于项目根目录）
// - KEYSTORE_PASSWORD: 密钥库密码
// - KEY_ALIAS: 密钥别名
// - KEY_PASSWORD: 密钥密码
//
// 当前配置已启用 v2+v3+v4 签名

dependencies {
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigationevent.compose)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.navigation3.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.blur)
    implementation(libs.miuix.shapes)

    implementation(libs.backdrop)

    implementation(libs.okhttp)

    implementation(libs.material.kolor)
    
    // Apache Commons Codec for Base32 decoding
    implementation("commons-codec:commons-codec:1.16.0")
    
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // DocumentFile for file operations
    implementation("androidx.documentfile:documentfile:1.0.1")
    
    // Shizuku API
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    
    // ExoPlayer for video playback
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")
    
    // CameraX for QR code scanning
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    
    // ML Kit for barcode scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    
    // 🔄 RxJava3 and RxAndroid3
    implementation(libs.rxjava3)
    implementation(libs.rxandroid3)
}
