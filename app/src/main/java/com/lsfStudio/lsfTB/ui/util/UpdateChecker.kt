package com.lsfStudio.lsfTB.ui.util

import android.content.Context
import android.util.Log
import com.lsfStudio.lsfTB.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
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
suspend fun checkNewVersion(context: Context): LatestVersionInfo {
    return withContext(Dispatchers.IO) {
        Log.d("UpdateChecker", "开始检查更新...")
        
        // 检查网络是否可用
        if (!isNetworkAvailable(context)) {
            Log.e("UpdateChecker", "网络不可用")
            return@withContext LatestVersionInfo.error(0, "网络不可用，请检查网络连接")
        }
        Log.d("UpdateChecker", "网络可用")

        // GitHub API 地址：获取 lsfTB 项目的最新 release
        val url = "https://api.github.com/repos/lsfdc233/lsfTB/releases/latest"
        Log.d("UpdateChecker", "请求 URL: $url")
        
        // 默认返回值（检查失败时使用）
        val defaultValue = LatestVersionInfo.Empty

        runCatching {
            // 创建 HTTP 客户端
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            // 发送请求
            val requestBuilder = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "lsfTB/${BuildConfig.VERSION_CODE}")
            
            // 尝试从 BuildConfig 获取 GitHub Token（如果存在）
            try {
                val tokenField = BuildConfig::class.java.getDeclaredField("GITHUB_TOKEN")
                val token = tokenField.get(null) as? String
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "token $token")
                    Log.d("UpdateChecker", "使用 GitHub Token 进行认证")
                }
            } catch (e: Exception) {
                // 没有配置 token，使用未认证的请求
                Log.d("UpdateChecker", "未配置 GitHub Token，使用未认证请求")
            }
            
            val request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                Log.d("UpdateChecker", "响应码: ${response.code}")
                
                // 检查响应是否成功
                if (!response.isSuccessful) {
                    val errorCode = response.code
                    val errorMessage = when (errorCode) {
                        403 -> "GitHub API 达到请求限制，请稍后重试"
                        404 -> "未找到版本信息，请检查仓库地址"
                        500 -> "GitHub 服务器错误，请稍后重试"
                        502 -> "GitHub 网关错误，请稍后重试"
                        503 -> "GitHub 服务不可用，请稍后重试"
                        else -> "HTTP 错误: $errorCode ${response.message}"
                    }
                    
                    Log.e("UpdateChecker", "$errorMessage")
                    return@runCatching LatestVersionInfo.error(errorCode, errorMessage)
                }

                // 解析 JSON 响应
                val body = response.body.string()
                Log.d("UpdateChecker", "响应体长度: ${body.length}")
                
                if (body.isEmpty()) {
                    Log.e("UpdateChecker", "响应体为空")
                    return@runCatching LatestVersionInfo.error(0, "服务器响应为空")
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
                
                // 调试日志
                Log.d("UpdateChecker", "tagName: $tagName")
                Log.d("UpdateChecker", "versionName: $versionName")
                Log.d("UpdateChecker", "versionCode: $versionCode")
                Log.d("UpdateChecker", "currentVersionCode: ${BuildConfig.VERSION_CODE}")

                // 获取 APK 下载链接
                val assets = json.getJSONArray("assets")
                var downloadUrl = ""
                
                Log.d("UpdateChecker", "assets 数量: ${assets.length()}")
                
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    Log.d("UpdateChecker", "asset[$i]: $name")
                    
                    // 查找 .apk 文件
                    if (name.endsWith(".apk")) {
                        downloadUrl = asset.getString("browser_download_url")
                        Log.d("UpdateChecker", "找到 APK: $downloadUrl")
                        break
                    }
                }
                
                if (downloadUrl.isEmpty()) {
                    Log.w("UpdateChecker", "未找到 APK 文件")
                }

                // 获取当前版本的更新日志
                val currentVersionChangelog = getCurrentVersionChangelog(client, requestBuilder)

                // 返回版本信息
                val result = LatestVersionInfo(
                    versionCode = versionCode,
                    versionName = versionName,
                    downloadUrl = downloadUrl,
                    changelog = changelog,
                    currentVersionChangelog = currentVersionChangelog
                )
                
                Log.d("UpdateChecker", "最终结果: versionCode=$versionCode, versionName=$versionName, hasDownloadUrl=${downloadUrl.isNotEmpty()}")
                result
            }
        }.getOrElse { exception ->
            // 发生异常时打印错误并返回详细错误信息
            val errorMessage = exception.message ?: "未知错误"
            Log.e("UpdateChecker", "检查更新失败: $errorMessage", exception)
            
            // 根据异常类型提供更友好的错误信息
            val friendlyMessage = when {
                errorMessage.contains("timeout", ignoreCase = true) -> "请求超时，请检查网络连接"
                errorMessage.contains("Unable to resolve host", ignoreCase = true) -> "无法连接到服务器，请检查网络"
                errorMessage.contains("connection", ignoreCase = true) -> "网络连接失败，请稍后重试"
                else -> "检查更新失败: $errorMessage"
            }
            
            LatestVersionInfo.error(0, friendlyMessage)
        }
    }
}

/**
 * 从版本标签字符串中解析 versionCode
 * 
 * 支持的格式：
 * - "v11.45.14" -> 提取所有数字组合（114514）
 * - "v1.0.0_1" -> 1
 * - "1.0.0" -> 从 BuildConfig 获取
 * - 其他格式 -> 0
 * 
 * @param tag 版本标签
 * @return 版本代码
 */
private fun parseVersionCode(tag: String): Int {
    return try {
        // 尝试匹配 "vX.X.X_X" 格式（末尾有下划线和数字）
        val regexWithSuffix = Regex("v.*?_(\\d+)$")
        val matchWithSuffix = regexWithSuffix.find(tag)
        if (matchWithSuffix != null) {
            return matchWithSuffix.groupValues[1].toInt()
        }
        
        // 尝试匹配 "v11.45.14" 格式，提取所有数字并组合
        val regexSimple = Regex("v(\\d+)\\.(\\d+)\\.(\\d+)")
        val matchSimple = regexSimple.find(tag)
        if (matchSimple != null) {
            val major = matchSimple.groupValues[1].toIntOrNull() ?: 0
            val minor = matchSimple.groupValues[2].toIntOrNull() ?: 0
            val patch = matchSimple.groupValues[3].toIntOrNull() ?: 0
            // 将版本号转换为整数：major * 10000 + minor * 100 + patch
            return major * 10000 + minor * 100 + patch
        }
        
        // 如果无法解析，返回当前版本代码
        BuildConfig.VERSION_CODE
    } catch (e: Exception) {
        // 解析失败，返回当前版本代码
        BuildConfig.VERSION_CODE
    }
}

/**
 * 获取当前版本的更新日志
 * 
 * @param client HTTP 客户端
 * @param requestBuilder 请求构建器
 * @return 当前版本的更新日志，如果未找到则返回空字符串
 */
private fun getCurrentVersionChangelog(
    client: OkHttpClient,
    requestBuilder: Request.Builder
): String {
    return try {
        val currentVersionCode = BuildConfig.VERSION_CODE
        Log.d("UpdateChecker", "开始获取当前版本 ($currentVersionCode) 的更新日志...")
        
        // 获取所有 releases
        val allReleasesUrl = "https://api.github.com/repos/lsfdc233/lsfTB/releases"
        val request = requestBuilder.url(allReleasesUrl).build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w("UpdateChecker", "获取所有 releases 失败: ${response.code}")
                return ""
            }
            
            val body = response.body.string()
            if (body.isEmpty()) {
                Log.w("UpdateChecker", "releases 响应体为空")
                return ""
            }
            
            val releases = JSONArray(body)
            Log.d("UpdateChecker", "共获取到 ${releases.length()} 个 release")
            
            // 遍历所有 releases，找到与当前版本匹配的
            for (i in 0 until releases.length()) {
                val release = releases.getJSONObject(i)
                val tagName = release.optString("tag_name", "")
                val versionCode = parseVersionCode(tagName)
                
                Log.d("UpdateChecker", "检查 release: $tagName -> versionCode: $versionCode")
                
                if (versionCode == currentVersionCode) {
                    val changelog = release.optString("body", "")
                    Log.d("UpdateChecker", "找到当前版本更新日志，长度: ${changelog.length}")
                    return changelog
                }
            }
            
            Log.w("UpdateChecker", "未找到与当前版本匹配的 release")
            ""
        }
    } catch (e: Exception) {
        Log.e("UpdateChecker", "获取当前版本更新日志失败", e)
        ""
    }
}
