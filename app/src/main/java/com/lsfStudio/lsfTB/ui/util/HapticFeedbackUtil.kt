package com.lsfStudio.lsfTB.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 震动反馈工具类
 * 提供小米风格的震动反馈
 */
object HapticFeedbackUtil {
    
    /**
     * 轻触震动（用于按钮点击）
     */
    fun lightClick(context: Context) {
        performHapticFeedback(context, VibrationEffect.EFFECT_CLICK)
    }
    
    /**
     * 长按震动（用于长按操作）
     */
    fun longPress(context: Context) {
        performHapticFeedback(context, VibrationEffect.EFFECT_HEAVY_CLICK)
    }
    
    /**
     * 选中震动（用于选择操作）
     */
    fun select(context: Context) {
        performHapticFeedback(context, VibrationEffect.EFFECT_DOUBLE_CLICK)
    }
    
    /**
     * 自定义震动时长
     */
    fun customVibrate(context: Context, duration: Long = 50) {
        val vibrator = getVibrator(context)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }
    }
    
    /**
     * 执行震动反馈
     */
    private fun performHapticFeedback(context: Context, effectId: Int) {
        val vibrator = getVibrator(context)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createPredefined(effectId))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    }
    
    /**
     * 获取Vibrator实例
     */
    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
