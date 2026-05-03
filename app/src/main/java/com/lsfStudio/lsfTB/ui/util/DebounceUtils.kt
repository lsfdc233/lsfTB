package com.lsfStudio.lsfTB.ui.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 防抖工具类
 * 
 * 用于防止用户快速重复点击按钮或触发操作
 * 
 * @author lsfTB Team
 */
object DebounceUtils {
    
    private const val TAG = "DebounceUtils"
    
    /**
     * 防抖任务映射表
     * Key: 操作标识
     * Value: 当前正在执行的Job
     */
    private val debounceJobs = mutableMapOf<String, Job>()
    
    /**
     * 执行防抖操作
     * 
     * 如果在指定时间内多次调用同一key的操作，只会执行最后一次
     * 
     * @param scope Coroutine作用域
     * @param key 操作唯一标识（如按钮ID、API路径等）
     * @param delayMs 防抖延迟时间（毫秒），默认500ms
     * @param action 要执行的操作
     */
    fun debounce(
        scope: CoroutineScope,
        key: String,
        delayMs: Long = 500L,
        action: suspend () -> Unit
    ) {
        // 取消之前的任务
        debounceJobs[key]?.cancel()
        
        // 创建新任务
        val job = scope.launch {
            delay(delayMs)
            try {
                action()
            } catch (e: Exception) {
                Log.e(TAG, "❌ 防抖操作执行失败: $key", e)
            } finally {
                // 清理已完成的Job
                debounceJobs.remove(key)
            }
        }
        
        // 保存Job引用
        debounceJobs[key] = job
        
        Log.d(TAG, "🕒 防抖任务已调度: $key (延迟${delayMs}ms)")
    }
    
    /**
     * 立即执行防抖操作（取消防抖）
     * 
     * @param key 操作唯一标识
     * @return true表示有任务被取消并立即执行，false表示没有待执行的任务
     */
    fun executeImmediately(key: String): Boolean {
        val job = debounceJobs[key]
        if (job != null && job.isActive) {
            job.cancel()
            debounceJobs.remove(key)
            Log.d(TAG, "⚡ 防抖任务已立即执行: $key")
            return true
        }
        return false
    }
    
    /**
     * 检查是否有待执行的防抖任务
     * 
     * @param key 操作唯一标识
     * @return true表示有待执行的任务
     */
    fun hasPendingTask(key: String): Boolean {
        val job = debounceJobs[key]
        return job != null && job.isActive
    }
    
    /**
     * 取消所有防抖任务
     */
    fun cancelAll() {
        debounceJobs.values.forEach { it.cancel() }
        debounceJobs.clear()
        Log.d(TAG, "🗑️ 已取消所有防抖任务")
    }
    
    /**
     * 获取待执行任务数量
     * 
     * @return 待执行任务数
     */
    fun getPendingCount(): Int {
        return debounceJobs.count { it.value.isActive }
    }
}
