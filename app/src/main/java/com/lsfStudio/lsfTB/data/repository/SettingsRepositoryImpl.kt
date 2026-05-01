package com.lsfStudio.lsfTB.data.repository

import android.content.Context
import androidx.core.content.edit
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.lsfStudio.lsfTB.ui.UiMode

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    private val prefs by lazy {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    override var uiMode: String
        get() = prefs.getString("ui_mode", UiMode.DEFAULT_VALUE) ?: UiMode.DEFAULT_VALUE
        set(value) = prefs.edit { putString("ui_mode", value) }

    override var checkUpdate: Boolean
        get() = prefs.getBoolean("check_update", true)
        set(value) = prefs.edit { putBoolean("check_update", value) }

    override var checkModuleUpdate: Boolean
        get() = prefs.getBoolean("module_check_update", true)
        set(value) = prefs.edit { putBoolean("module_check_update", value) }

    override var themeMode: Int
        get() = prefs.getInt("color_mode", 0)
        set(value) = prefs.edit { putInt("color_mode", value) }

    override var miuixMonet: Boolean
        get() = prefs.getBoolean("miuix_monet", false)
        set(value) = prefs.edit { putBoolean("miuix_monet", value) }

    override var keyColor: Int
        get() = prefs.getInt("key_color", 0)
        set(value) = prefs.edit { putInt("key_color", value) }

    override var colorStyle: String
        get() = prefs.getString("color_style", PaletteStyle.TonalSpot.name) ?: PaletteStyle.TonalSpot.name
        set(value) = prefs.edit { putString("color_style", value) }

    override var colorSpec: String
        get() = prefs.getString("color_spec", ColorSpec.SpecVersion.Default.name) ?: ColorSpec.SpecVersion.Default.name
        set(value) = prefs.edit { putString("color_spec", value) }

    override var enablePredictiveBack: Boolean
        get() = prefs.getBoolean("enable_predictive_back", false)
        set(value) = prefs.edit { putBoolean("enable_predictive_back", value) }

    override var enableBlur: Boolean
        get() = prefs.getBoolean("enable_blur", false)
        set(value) = prefs.edit { putBoolean("enable_blur", value) }

    override var enableFloatingBottomBar: Boolean
        get() = prefs.getBoolean("enable_floating_bottom_bar", false)
        set(value) = prefs.edit { putBoolean("enable_floating_bottom_bar", value) }

    override var enableFloatingBottomBarBlur: Boolean
        get() = prefs.getBoolean("enable_floating_bottom_bar_blur", false)
        set(value) = prefs.edit { putBoolean("enable_floating_bottom_bar_blur", value) }

    override var pageScale: Float
        get() = prefs.getFloat("page_scale", 1.0f)
        set(value) = prefs.edit { putFloat("page_scale", value) }

    override var enableWebDebugging: Boolean
        get() = prefs.getBoolean("enable_web_debugging", false)
        set(value) = prefs.edit { putBoolean("enable_web_debugging", value) }

    override var enableSmoothCorner: Boolean
        get() = prefs.getBoolean("enable_smooth_corner", true)
        set(value) = prefs.edit { putBoolean("enable_smooth_corner", value) }

    override var disableAllAnimations: Boolean
        get() = prefs.getBoolean("disable_all_animations", false)
        set(value) = prefs.edit { putBoolean("disable_all_animations", value) }

    override var autoJailbreak: Boolean
        get() = false
        set(value) {}

    override suspend fun getSuCompatStatus(): String = "unsupported"

    override suspend fun getSuCompatPersistValue(): Long? = null

    override fun isSuEnabled(): Boolean = false

    override fun setSuEnabled(enabled: Boolean): Boolean = false

    override fun setSuCompatModePref(mode: Int) {}

    override fun getSuCompatModePref(): Int = 0

    override suspend fun getKernelUmountStatus(): String = "unsupported"

    override fun isKernelUmountEnabled(): Boolean = false

    override fun setKernelUmountEnabled(enabled: Boolean): Boolean = false

    override suspend fun getSulogStatus(): String = "unsupported"

    override suspend fun getSulogPersistValue(): Long? = null

    override fun setSulogEnabled(enabled: Boolean): Boolean = false

    override suspend fun getAdbRootStatus(): String = "unsupported"

    override suspend fun getAdbRootPersistValue(): Long? = null

    override fun setAdbRootEnabled(enabled: Boolean): Boolean = false

    override fun isDefaultUmountModules(): Boolean = false

    override fun setDefaultUmountModules(enabled: Boolean): Boolean = false

    override fun isLkmMode(): Boolean = false

    override fun execKsudFeatureSave() {}
}
