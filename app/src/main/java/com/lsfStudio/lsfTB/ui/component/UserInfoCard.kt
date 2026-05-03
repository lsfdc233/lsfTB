package com.lsfStudio.lsfTB.ui.component

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
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
import com.lsfStudio.lsfTB.ui.theme.LocalColorMode
import com.lsfStudio.lsfTB.ui.util.AccountManager
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
    
    // 从 AccountManager 读取用户信息
    val userInfo = remember { AccountManager.getUserInfo(context) }
    val isLoggedIn = userInfo != null
    
    // 解析标签（用 ` 分割）
    val tags = if (isLoggedIn && userInfo!!.tag.isNotEmpty()) {
        userInfo.tag.split("`").filter { it.isNotEmpty() }
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
    val progress = if (isLoggedIn) {
        (userInfo!!.experience.toFloat() / userInfo.nextLevel.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000)
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
                        if (isLoggedIn) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 用户组标签
                                if (userInfo!!.group.isNotEmpty()) {
                                    TagBadge(
                                        text = userInfo.group,
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
                                    level = userInfo.level,
                                    accentColor = accentColor
                                )

                                TagBadge(
                                    text = "${userInfo.points} 积分",
                                    backgroundColor = accentColor.copy(alpha = 0.15f),
                                    textColor = accentColor
                                )
                            }
                        }
                    }
                    
                    // 右侧：签到按钮（仅在已登录时显示）
                    if (isLoggedIn) {
                        SignInButton(
                            modifier = Modifier,
                            isSignedIn = userInfo!!.isCheckedIn,
                            onClick = onSignInClick,
                            accentColor = accentColor,
                            compact = true
                        )
                    }
                }
                
                // 下半部分：经验条（与用户名左对齐，仅在已登录时显示）
                if (isLoggedIn) {
                    ExperienceBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 80.dp), // 头像宽度(64dp) + 间距(16dp)
                        progress = animatedProgress,
                        experience = userInfo!!.experience,
                        maxExperience = userInfo.nextLevel,
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
 * 签到按钮组件
 */
@Composable
private fun SignInButton(
    modifier: Modifier = Modifier,
    isSignedIn: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    compact: Boolean = false
) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(if (compact) 16.dp else 24.dp))
            .background(
                if (isSignedIn) Color(0xFFF5F5F5) else accentColor
            )
            .border(
                width = 1.dp,
                color = if (isSignedIn) Color(0xFFE0E0E0) else accentColor,
                shape = RoundedCornerShape(if (compact) 16.dp else 24.dp)
            )
            .padding(
                horizontal = if (compact) 14.dp else 20.dp,
                vertical = if (compact) 6.dp else 10.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isSignedIn) "已签到" else "签到",
            fontSize = if (compact) 13.sp else 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSignedIn) Color(0xFF666666) else Color.White
        )
    }
}
