package com.lsfStudio.lsfTB.ui.util

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.lsfStudio.lsfTB.ui.theme.LocalDisableAllAnimations

/**
 * 动画配置工具类
 * 
 * 根据用户设置动态返回动画参数，实现全局动画开关
 */
object AnimationConfig {
    
    /**
     * 获取进入动画
     * 
     * @param defaultEnter 默认的进入动画
     * @return 如果禁用了所有动画则返回空动画，否则返回默认动画
     */
    @Composable
    fun getEnterTransition(defaultEnter: EnterTransition): EnterTransition {
        val disableAllAnimations = LocalDisableAllAnimations.current
        return if (disableAllAnimations) EnterTransition.None else defaultEnter
    }
    
    /**
     * 获取退出动画
     * 
     * @param defaultExit 默认的退出动画
     * @return 如果禁用了所有动画则返回空动画，否则返回默认动画
     */
    @Composable
    fun getExitTransition(defaultExit: ExitTransition): ExitTransition {
        val disableAllAnimations = LocalDisableAllAnimations.current
        return if (disableAllAnimations) ExitTransition.None else defaultExit
    }
    
    /**
     * 获取动画时长
     * 
     * @param defaultDuration 默认动画时长（毫秒）
     * @return 如果禁用了所有动画则返回0，否则返回默认时长
     */
    @Composable
    fun getAnimationDuration(defaultDuration: Int): Int {
        val disableAllAnimations = LocalDisableAllAnimations.current
        return if (disableAllAnimations) 0 else defaultDuration
    }
    
    /**
     * 获取 tween 动画规格
     * 
     * @param durationMillis 默认动画时长
     * @return 如果禁用了所有动画则返回即时动画，否则返回 tween 动画
     */
    @Composable
    fun getTweenAnimationSpec(durationMillis: Int): androidx.compose.animation.core.FiniteAnimationSpec<Float> {
        val disableAllAnimations = LocalDisableAllAnimations.current
        return if (disableAllAnimations) {
            tween(durationMillis = 0)
        } else {
            tween(durationMillis = durationMillis)
        }
    }
}
