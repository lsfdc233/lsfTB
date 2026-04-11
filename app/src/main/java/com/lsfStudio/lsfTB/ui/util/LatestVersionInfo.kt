package com.lsfStudio.lsfTB.ui.util

/**
 * 最新版本信息数据类
 * 
 * @param versionCode 最新版本代码
 * @param versionName 最新版本名称
 * @param downloadUrl APK下载链接
 * @param changelog 更新日志
 */
data class LatestVersionInfo(
    val versionCode: Int = 0,
    val versionName: String = "",
    val downloadUrl: String = "",
    val changelog: String = ""
) {
    companion object {
        /**
         * 空版本信息（表示无更新或检查失败）
         */
        val Empty = LatestVersionInfo()
    }
}
