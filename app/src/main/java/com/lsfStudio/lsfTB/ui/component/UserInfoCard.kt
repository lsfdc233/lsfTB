package com.lsfStudio.lsfTB.ui.component

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lsfStudio.lsfTB.BuildConfig
import com.lsfStudio.lsfTB.ui.theme.LocalColorMode
import com.lsfStudio.lsfTB.ui.util.AccountManager
import com.lsfStudio.lsfTB.ui.util.HapticFeedbackUtil
import com.lsfStudio.lsfTB.ui.util.MessageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * 用户信息卡片组件
 * 
 * 显示用户头像、用户名、标签、等级和经验条
 * 背景使用莫奈取色风格
 */
@Composable
fun UserInfoCard(
    modifier: Modifier = Modifier,
    onSignInClick: () -> Unit = {}, // 点击登录按钮
    onCardClick: () -> Unit = {}, // 点击卡片
    cardHeight: Dp = 180.dp
) {
    val context = LocalContext.current
    val colorMode = LocalColorMode.current
    
    // ⭐ 从 UserManager 读取用户信息（SQLite user_info 表）
    var userInfo by remember { mutableStateOf(com.lsfStudio.lsfTB.ui.util.UserManager.getUserInfo(context)) }
    val isLoggedIn = userInfo != null
    
    // 签到状态
    var isSigningIn by remember { mutableStateOf(false) }
    
    // 解析标签（用 ` 分割）
    val tags = if (isLoggedIn && userInfo?.tag?.isNotEmpty() == true) {
        userInfo!!.tag.split("`").filter { it.isNotEmpty() }
    } else {
        emptyList()
    }
    
    // 读取莫奈取色开关状态
    val enableMonet = remember {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean("miuix_monet", false)
    }
    
    // Miuix 蓝色（默认颜色）
    val miuixBlue = Color(0xFF3B82F6)
    
    // 根据莫奈取色开关状态选择颜色
    val backgroundColor = if (enableMonet) {
        // 莫奈取色模式：使用从主题获取的背景色
        colorScheme.background
    } else {
        // 关闭莫奈取色：使用白色
        Color(0xFFFFFFFF)
    }
    
    val accentColor = if (enableMonet) {
        // 莫奈取色模式：使用主题的强调色
        colorScheme.primary
    } else {
        // 关闭莫奈取色：统一使用 Miuix 蓝色
        miuixBlue
    }
    
    val progressColor = if (enableMonet) {
        // 莫奈取色模式：使用主题的强调色
        colorScheme.primary
    } else {
        // 关闭莫奈取色：使用 Miuix 蓝色
        miuixBlue
    }
    
    // 经验条动画
    val progress = if (isLoggedIn && userInfo != null) {
        // ⭐ 计算当前等级内的进度（而不是总进度）
        val currentLevelExp = userInfo!!.experience
        val nextLevelExp = userInfo!!.nextLevel
        
        // 如果 nextLevel 是下一等级的经验阈值，需要计算当前等级的起始经验
        // 假设每级所需经验是固定的或者递增的
        // 这里简化处理：直接使用 experience / nextLevel
        if (nextLevelExp > 0) {
            (currentLevelExp.toFloat() / nextLevelExp.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)  // ⭐ 添加缓动函数
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clickable(enabled = !isLoggedIn) { onCardClick() } // 未登录时点击跳转到登录页
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 上半部分：头像、用户名、标签
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：头像
                    UserInfoAvatar(
                        modifier = Modifier.size(64.dp),
                        avatarPath = if (isLoggedIn) userInfo!!.avatarPath else null, // 未登录时显示默认头像
                        accentColor = accentColor
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // 中间：用户信息
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 用户名
                        Text(
                            text = if (isLoggedIn) userInfo!!.username else "未登录，点击登录/注册", // 未登录时显示提示文字
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C2C2C)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 标签和等级（仅在已登录时显示）
                        if (isLoggedIn && userInfo != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 用户组标签
                                if (userInfo!!.group.isNotEmpty()) {
                                    TagBadge(
                                        text = userInfo!!.group,
                                        backgroundColor = accentColor.copy(alpha = 0.15f),
                                        textColor = accentColor
                                    )
                                }
                                
                                // 自定义标签
                                tags.forEach { tag ->
                                    TagBadge(
                                        text = tag,
                                        backgroundColor = accentColor.copy(alpha = 0.15f),
                                        textColor = accentColor
                                    )
                                }
                                
                                // 等级标签
                                LevelBadge(
                                    level = userInfo!!.level,
                                    accentColor = accentColor
                                )

                            }
                        }
                    }
                    
                    // 右侧：签到按钮（仅在已登录时显示）
                    if (isLoggedIn && userInfo != null) {
                        SignInButton(
                            modifier = Modifier,
                            isSignedIn = userInfo!!.isCheckedIn,
                            isLoading = isSigningIn,
                            onClick = {
                                if (!isSigningIn && !userInfo!!.isCheckedIn) {
                                    HapticFeedbackUtil.lightClick(context)
                                    performCheckIn(context, 
                                        onStateChange = { loading -> isSigningIn = loading },
                                        onSuccess = { updatedUserInfo ->
                                            userInfo = updatedUserInfo
                                        }
                                    )
                                }
                            },
                            accentColor = accentColor,
                            compact = true
                        )
                    }
                }
                
                // 下半部分：经验条（与用户名左对齐，仅在已登录时显示）
                if (isLoggedIn && userInfo != null) {
                    ExperienceBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 80.dp), // 头像宽度(64dp) + 间距(16dp)
                        progress = animatedProgress,
                        experience = userInfo!!.experience,
                        maxExperience = userInfo!!.nextLevel,
                        progressColor = progressColor,
                        backgroundColor = accentColor.copy(alpha = 0.2f),
                        showTextOnRight = true,
                        barHeight = 8.dp
                    )
                }
            }
        }
    }
}

/**
 * 用户头像组件
 */
@Composable
private fun UserInfoAvatar(
    modifier: Modifier = Modifier,
    avatarPath: String?,
    accentColor: Color
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 外圈装饰
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = accentColor.copy(alpha = 0.3f),
                radius = size.minDimension / 2,
                style = Stroke(width = 4.dp.toPx())
            )
        }
        
        // 头像背景
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f))
                .border(2.dp, accentColor.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (avatarPath != null) {
                // 加载本地头像（参考注册页面的实现）
                AsyncImage(
                    model = avatarPath,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "Avatar",
                    modifier = Modifier.size(32.dp),
                    tint = accentColor
                )
            }
        }
    }
}

/**
 * 标签徽章组件
 */
@Composable
private fun TagBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

/**
 * 等级徽章组件
 */
@Composable
private fun LevelBadge(
    level: Int,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "Lv.$level",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

/**
 * 经验条组件
 */
@Composable
private fun ExperienceBar(
    modifier: Modifier = Modifier,
    progress: Float,
    experience: Int,
    maxExperience: Int,
    progressColor: Color,
    backgroundColor: Color,
    showTextOnRight: Boolean = true,
    barHeight: Dp = 8.dp
) {
    Box(
        modifier = modifier
    ) {
        // 背景条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(4.dp))
                .background(backgroundColor)
        )
        
        // 进度条
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(barHeight)
                .clip(RoundedCornerShape(4.dp))
                .background(progressColor)
        )
        
        // 经验文字
        if (showTextOnRight) {
            Text(
                text = "$experience/$maxExperience",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

/**
 * 执行签到操作
 */
private fun performCheckIn(
    context: Context,
    onStateChange: (Boolean) -> Unit, // 用于更新 isSigningIn 状态
    onSuccess: (com.lsfStudio.lsfTB.ui.util.UserManager.UserInfo) -> Unit
) {
    val currentUserInfo = com.lsfStudio.lsfTB.ui.util.UserManager.getUserInfo(context) ?: return
    
    CoroutineScope(Dispatchers.Main).launch {
        try {
            onStateChange(true) // 设置加载中
            MessageManager.showToast(context, "正在签到...", android.widget.Toast.LENGTH_SHORT)
            
            // 构建请求体
            val requestBody = JSONObject().apply {
                put("username", currentUserInfo.username)
            }.toString()
            
            // 在后台线程执行网络请求
            val result = withContext(Dispatchers.IO) {
                val url = "${BuildConfig.SERVER_URL}/lsfStudio/api/account/checkin"
                val response = com.lsfStudio.lsfTB.ui.util.NetworkClient.send(
                    context = context,
                    method = "POST",
                    url = url,
                    path = "/lsfStudio/api/account/checkin",
                    bodyContent = requestBody
                )
                
                Triple(response.isSuccessful, response.code, response.body)
            }
            
            val (isSuccess, code, responseBody) = result
            
            if (responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val success = jsonResponse.optBoolean("success", false)
                val message = jsonResponse.optString("message", "")
                
                if (success) {
                    // 解析签到数据
                    val data = jsonResponse.optJSONObject("data")
                    if (data != null) {
                        val updatedUserInfo = com.lsfStudio.lsfTB.ui.util.UserManager.UserInfo(
                            username = data.optString("username", currentUserInfo.username),
                            group = currentUserInfo.group,
                            tag = currentUserInfo.tag,
                            level = data.optInt("level", currentUserInfo.level),
                            experience = data.optInt("experience", currentUserInfo.experience),
                            points = data.optInt("points", currentUserInfo.points),
                            nextLevel = data.optInt("next_level", currentUserInfo.nextLevel),
                            isCheckedIn = data.optBoolean("already_checked_in", true),
                            avatarPath = currentUserInfo.avatarPath
                        )
                        
                        // ⭐ 保存到 UserManager（SQLite user_info 表）
                        com.lsfStudio.lsfTB.ui.util.UserManager.updateCheckinStatus(context, updatedUserInfo)
                        
                        // 更新UI
                        onSuccess(updatedUserInfo)
                        
                        // 显示成功提示
                        val consecutiveDays = data.optInt("consecutive_days", 1)
                        val rewards = data.optJSONObject("rewards")
                        val pointsGained = rewards?.optInt("points", 0) ?: 0
                        val expGained = rewards?.optInt("experience", 0) ?: 0
                        
                        val toastMessage = if (message == "ALREADY_CHECKED_IN") {
                            "今日已签到 ✓\n连续签到 $consecutiveDays 天"
                        } else {
                            "签到成功！\n+$pointsGained 积分 +$expGained 经验\n连续签到 $consecutiveDays 天"
                        }
                        
                        MessageManager.showToast(context, toastMessage, android.widget.Toast.LENGTH_LONG)
                    }
                } else {
                    // 签到失败
                    when (message) {
                        "ALREADY_CHECKED_IN" -> {
                            MessageManager.showToast(context, "今日已签到 ✓", android.widget.Toast.LENGTH_SHORT)
                            // 更新本地状态
                            val updatedUserInfo = currentUserInfo.copy(isCheckedIn = true)
                            
                            // ⭐ 更新 UserManager
                            com.lsfStudio.lsfTB.ui.util.UserManager.updateCheckinStatus(context, updatedUserInfo)
                            
                            onSuccess(updatedUserInfo)
                        }
                        "DEVICE_NOT_BOUND" -> {
                            MessageManager.showToast(context, "设备未绑定到此账号", android.widget.Toast.LENGTH_LONG)
                        }
                        "DEVICE_NOT_REGISTERED" -> {
                            MessageManager.showToast(context, "设备未注册", android.widget.Toast.LENGTH_LONG)
                        }
                        "ACCOUNT_BANNED" -> {
                            MessageManager.showToast(context, "账号已被禁用", android.widget.Toast.LENGTH_LONG)
                        }
                        else -> {
                            MessageManager.showToast(context, "签到失败: $message", android.widget.Toast.LENGTH_LONG)
                        }
                    }
                }
            } else {
                MessageManager.showToast(context, "签到失败: 无响应", android.widget.Toast.LENGTH_LONG)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("UserInfoCard", "签到异常", e)
            MessageManager.showToast(context, "签到异常: ${e.message}", android.widget.Toast.LENGTH_LONG)
        } finally {
            onStateChange(false) // 结束加载
        }
    }
}

/**
 * 签到按钮组件
 */
@Composable
private fun SignInButton(
    modifier: Modifier = Modifier,
    isSignedIn: Boolean,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    accentColor: Color,
    compact: Boolean = false
) {
    Box(
        modifier = modifier
            .clickable(enabled = !isSignedIn && !isLoading) { onClick() }
            .clip(RoundedCornerShape(if (compact) 16.dp else 24.dp))
            .background(
                when {
                    isLoading -> Color(0xFFE0E0E0)
                    isSignedIn -> Color(0xFFF5F5F5)
                    else -> accentColor
                }
            )
            .border(
                width = 1.dp,
                color = when {
                    isLoading -> Color(0xFFCCCCCC)
                    isSignedIn -> Color(0xFFE0E0E0)
                    else -> accentColor
                },
                shape = RoundedCornerShape(if (compact) 16.dp else 24.dp)
            )
            .padding(
                horizontal = if (compact) 14.dp else 20.dp,
                vertical = if (compact) 6.dp else 10.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when {
                isLoading -> "签到中..."
                isSignedIn -> "已签到"
                else -> "签到"
            },
            fontSize = if (compact) 13.sp else 16.sp,
            fontWeight = FontWeight.Medium,
            color = when {
                isLoading -> Color(0xFF999999)
                isSignedIn -> Color(0xFF666666)
                else -> Color.White
            }
        )
    }
}
