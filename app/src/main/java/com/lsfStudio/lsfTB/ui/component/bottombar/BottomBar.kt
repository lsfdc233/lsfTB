package com.lsfStudio.lsfTB.ui.component.bottombar

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import com.lsfStudio.lsfTB.ui.LocalUiMode
import com.lsfStudio.lsfTB.ui.UiMode
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import kotlin.math.abs

class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int, disableAnimations: Boolean = false) {
        if (targetIndex == selectedPage) return

        navJob?.cancel()

        selectedPage = targetIndex
        isNavigating = true

        val distance = abs(targetIndex - pagerState.currentPage).coerceAtLeast(2)
        val duration = 100 * distance + 100
        val layoutInfo = pagerState.layoutInfo
        val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
        val currentDistanceInPages = targetIndex - pagerState.currentPage - pagerState.currentPageOffsetFraction
        val scrollPixels = currentDistanceInPages * pageSize

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                if (disableAnimations) {
                    // 禁用动画：直接跳转到目标页面
                    pagerState.scrollToPage(targetIndex)
                } else {
                    // 启用动画：平滑滚动
                    pagerState.animateScrollBy(
                        value = scrollPixels,
                        animationSpec = tween(easing = EaseInOut, durationMillis = duration)
                    )
                }
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetIndex) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    /**
     * 同步页面偏移量（用于底栏拖动时页面跟随）
     * @param offset 偏移量，范围 0.0 到 (pageCount - 1)
     */
    suspend fun syncPageOffset(offset: Float) {
        val clampedOffset = offset.coerceIn(0f, (pagerState.pageCount - 1).toFloat())
        val targetPage = clampedOffset.toInt()
        val pageOffset = clampedOffset - targetPage
        
        // 计算相对于当前页面的偏移
        val currentPage = pagerState.currentPage
        val currentPageOffset = pagerState.currentPageOffsetFraction
        
        // 如果目标页面与当前页面相同，直接更新偏移
        if (targetPage == currentPage) {
            // pageOffset 需要在 -0.5 到 0.5 范围内
            val normalizedOffset = (pageOffset - 0.5f).coerceIn(-0.5f, 0.5f)
            pagerState.scrollToPage(currentPage, normalizedOffset)
        } else {
            // 如果目标页面不同，直接跳转到目标页面（不带动画）
            // 注意：Pager 不支持跨页面的部分偏移显示
            pagerState.scrollToPage(targetPage, 0f)
        }
        
        // 更新选中页面
        val roundedPage = (clampedOffset + 0.5f).toInt()
        if (selectedPage != roundedPage) {
            selectedPage = roundedPage
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): MainPagerState {
    return remember(pagerState, coroutineScope) {
        MainPagerState(pagerState, coroutineScope)
    }
}

@Composable
fun BottomBar(
    blurBackdrop: LayerBackdrop?,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    BottomBarMiuix(blurBackdrop, backdrop, modifier)
}

@Composable
fun SideRail(
    blurBackdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
) {
    // SideRail not used in current implementation
}
