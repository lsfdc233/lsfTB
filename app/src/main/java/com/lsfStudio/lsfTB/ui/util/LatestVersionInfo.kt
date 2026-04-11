package com.lsfStudio.lsfTB.ui.util

/**
 * 最新版本信息数据类
 * 
 * @param versionCode 最新版本代码
 * @param versionName 最新版本名称
 * @param downloadUrl APK下载链接
 * @param changelog 更新日志
 * @param errorCode 错误码（0表示无错误）
 * @param errorMessage 错误信息
 */
data class LatestVersionInfo(
    val versionCode: Int = 0,
    val versionName: String = "",
    val downloadUrl: String = "",
    val changelog: String = "",
    val errorCode: Int = 0, // HTTP 状态码，0表示无错误
    val errorMessage: String = "" // 错误描述
) {
    companion object {
        /**
         * 空版本信息（表示无更新或检查失败）
         */
        val Empty = LatestVersionInfo()
        
        /**
         * 创建错误信息
         */
        fun error(code: Int, message: String): LatestVersionInfo {
            return LatestVersionInfo(
                errorCode = code,
                errorMessage = message
            )
        }
    }
}
