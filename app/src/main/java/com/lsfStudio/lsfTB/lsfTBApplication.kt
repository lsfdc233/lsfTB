package com.lsfStudio.lsfTB

import android.app.Application
import android.os.Build
import android.os.UserManager
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * lsfTB应用程序类
 * 
 * 职责：
 * 1. 作为应用的入口点，在应用启动时初始化
 * 2. 实现ViewModelStoreOwner接口，提供应用级别的ViewModel存储
 * 3. 管理全局的ViewModel生命周期
 * 
 * @author lsfTB Team
 */
class lsfTBApplication : Application(), ViewModelStoreOwner {

    // 应用级别的ViewModel存储器
    // 用于存储在应用整个生命周期内共享的ViewModel
    private val appViewModelStore by lazy { ViewModelStore() }

    /**
     * 应用创建时的初始化方法
     * 在应用进程启动时调用，早于任何Activity的onCreate
     */
    override fun onCreate() {
        super.onCreate()
        // 在这里可以添加全局初始化逻辑
    }

    /**
     * 获取应用级别的ViewModelStore
     * 
     * @return ViewModelStore实例，用于管理应用级ViewModel
     */
    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
}
