package com.lsfStudio.lsfTB.ui.util

import android.content.Context
import com.lsfStudio.lsfTB.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 检查新版本
 * 
 * 通过 GitHub API 获取最新 release 信息
 * 
 * @param context 上下文，用于检查网络状态
 * @return 最新版本信息，如果无更新或检查失败则返回 Empty
 */
fun checkNewVersion(context: Context): LatestVersionInfo {
    // 检查网络是否可用
    if (!isNetworkAvailable(context)) {
        return LatestVersionInfo.Empty
    }

    // GitHub API 地址：获取 lsfTB 项目的最新 release
    val url = "https://api.github.com/repos/lsfdc233/lsfTB/releases/latest"
    
    // 默认返回值（检查失败时使用）
    val defaultValue = LatestVersionInfo.Empty

    return runCatching {
        // 创建 HTTP 客户端
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        // 发送请求
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/vnd.github.v3+json")
            .addHeader("User-Agent", "lsfTB/${BuildConfig.VERSION_CODE}")
            .build()

        client.newCall(request).execute().use { response ->
            // 检查响应是否成功
            if (!response.isSuccessful) {
                return@runCatching defaultValue
            }

            // 解析 JSON 响应
            val body = response.body.string()
            if (body.isEmpty()) {
                return@runCatching defaultValue
            }
            val json = JSONObject(body)

            // 获取更新日志
            val changelog = json.optString("body", "")

            // 获取版本号（从 tag_name 或 name 字段）
            val tagName = json.optString("tag_name", "")
            val versionName = json.optString("name", tagName)

            // 从版本号字符串中提取 versionCode
            // 假设格式为 "v1.0.0" 或 "1.0.0"
            val versionCode = parseVersionCode(tagName)

            // 获取 APK 下载链接
            val assets = json.getJSONArray("assets")
            var downloadUrl = ""
            
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                
                // 查找 .apk 文件
                if (name.endsWith(".apk")) {
                    downloadUrl = asset.getString("browser_download_url")
                    break
                }
            }

            // 返回版本信息
            LatestVersionInfo(
                versionCode = versionCode,
                versionName = versionName,
                downloadUrl = downloadUrl,
                changelog = changelog
            )
        }
    }.getOrElse {
        // 发生异常时返回默认值
        defaultValue
    }
}

/**
 * 从版本标签字符串中解析 versionCode
 * 
 * 支持的格式：
 * - "v1.0.0_1" -> 1
 * - "1.0.0" -> 从 BuildConfig 获取
 * - 其他格式 -> 0
 * 
 * @param tag 版本标签
 * @return 版本代码
 */
private fun parseVersionCode(tag: String): Int {
    return try {
        // 尝试匹配 "vX.X.X_X" 格式
        val regex = Regex("v.*?(\\d+)$")
        val matchResult = regex.find(tag)
        if (matchResult != null) {
            matchResult.groupValues[1].toInt()
        } else {
            // 如果无法解析，返回当前版本代码
            BuildConfig.VERSION_CODE
        }
    } catch (e: Exception) {
        // 解析失败，返回当前版本代码
        BuildConfig.VERSION_CODE
    }
}
