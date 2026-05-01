package com.lsfStudio.lsfTB.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

enum class ColorMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2),
    MONET_SYSTEM(3),
    MONET_LIGHT(4),
    MONET_DARK(5),
    DARK_AMOLED(6);

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: SYSTEM
    }

    val isSystem: Boolean get() = value == 0 || value == 3
    val isDark: Boolean get() = value == 2 || value == 5 || value == 6
    val isAmoled: Boolean get() = value == 6
    val isMonet: Boolean get() = value >= 3

    fun toNonMonetMode(): Int = when (this) {
        MONET_SYSTEM -> 0
        MONET_LIGHT -> 1
        MONET_DARK, DARK_AMOLED -> 2
        else -> value
    }

    fun toMonetMode(): Int = when (this) {
        SYSTEM -> 3
        LIGHT -> 4
        DARK -> 5
        else -> value
    }
}

data class AppSettings(
    val colorMode: ColorMode,
    val keyColor: Int,
    val paletteStyle: PaletteStyle,
    val colorSpec: ColorSpec.SpecVersion,
    val enableSmoothCorner: Boolean,
)

object ThemeController {
    fun getAppSettings(context: Context): AppSettings {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        var colorModeValue = prefs.getInt("color_mode", ColorMode.SYSTEM.value)

        val miuixMonet = prefs.getBoolean("miuix_monet", false)
        val initialColorMode = ColorMode.fromValue(colorModeValue)
        colorModeValue = if (!miuixMonet && initialColorMode.isMonet) {
            initialColorMode.toNonMonetMode()
        } else if (miuixMonet && !initialColorMode.isMonet) {
            initialColorMode.toMonetMode()
        } else {
            colorModeValue
        }

        val colorMode = ColorMode.fromValue(colorModeValue)
        val keyColor = prefs.getInt("key_color", 0)
        val paletteStyleStr = prefs.getString("color_style", PaletteStyle.TonalSpot.name)
        val paletteStyle = try {
            PaletteStyle.valueOf(paletteStyleStr!!)
        } catch (_: Exception) {
            PaletteStyle.TonalSpot
        }
        val colorSpecStr = prefs.getString("color_spec", ColorSpec.SpecVersion.Default.name)
        val colorSpec = try {
            ColorSpec.SpecVersion.valueOf(colorSpecStr!!)
        } catch (_: Exception) {
            ColorSpec.SpecVersion.Default
        }

        val enableSmoothCorner = prefs.getBoolean("enable_smooth_corner", true)

        return AppSettings(colorMode, keyColor, paletteStyle, colorSpec, enableSmoothCorner)
    }
}

/**
 * lsfTB应用主题Composable函数
 * 
 * 提供全局主题配置，包括颜色模式、Monet支持等
 * 
 * @param appSettings 应用设置，如果为null则从SharedPreferences读取
 * @param content 主题包裹的内容
 */
@Composable
fun lsfTBTheme(
    appSettings: AppSettings? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val currentAppSettings = appSettings ?: ThemeController.getAppSettings(context)

    MiuixlsfTBTheme(
        appSettings = currentAppSettings,
        content = content
    )
}

@Composable
@ReadOnlyComposable
fun isInDarkTheme(): Boolean {
    return when (LocalColorMode.current) {
        1, 4 -> false
        2, 5, 6 -> true
        else -> isSystemInDarkTheme()
    }
}

val LocalColorMode = staticCompositionLocalOf { 0 }

val LocalEnableBlur = staticCompositionLocalOf { false }

val LocalEnableFloatingBottomBar = staticCompositionLocalOf { false }

val LocalEnableFloatingBottomBarBlur = staticCompositionLocalOf { false }

val LocalDisableAllAnimations = staticCompositionLocalOf { false }  // 去掉所有动画效果

val LocalEnableMonet = staticCompositionLocalOf { false }  // 莫奈取色开关
