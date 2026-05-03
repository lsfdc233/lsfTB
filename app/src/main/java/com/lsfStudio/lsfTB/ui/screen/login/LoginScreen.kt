package com.lsfStudio.lsfTB.ui.screen.login

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lsfStudio.lsfTB.ui.theme.LocalColorMode
import com.lsfStudio.lsfTB.ui.util.AccountManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * 登录页面
 * 
 * 使用 Miuix 控件，适配莫奈取色
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val colorMode = LocalColorMode.current
    val navigator = com.lsfStudio.lsfTB.ui.navigation3.LocalNavigator.current
    
    // 读取莫奈取色开关状态
    val enableMonet = remember {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean("miuix_monet", false)
    }
    
    // Miuix 蓝色（默认颜色）
    val miuixBlue = Color(0xFF3B82F6)
    
    // 根据莫奈取色开关状态选择颜色
    val primaryColor = if (enableMonet) {
        colorScheme.primary
    } else {
        miuixBlue
    }
    
    val onPrimaryColor = if (enableMonet) {
        colorScheme.onPrimary
    } else {
        Color.White
    }
    
    // 输入框状态
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // 错误状态
    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // 顶部留白
            Spacer(modifier = Modifier.height(80.dp))
            
            // Logo 和标题区域
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                // App Logo
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "App Logo",
                        modifier = Modifier.size(36.dp),
                        tint = primaryColor
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // 标题文字
                Column {
                    Text(
                        text = "欢迎来到",
                        fontSize = 18.sp,
                        color = colorScheme.onBackground.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "lsfTB",
                        fontSize = 28.sp,
                        color = colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // 用户名输入框
            TextField(
                value = username,
                onValueChange = { 
                    username = it
                    usernameError = null  // 清除错误
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (usernameError != null) 2.dp else 1.dp,
                        color = if (usernameError != null) Color.Red else colorScheme.onSurface.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp)),
                placeholder = { Text("请输入用户名/邮箱") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Username",
                        tint = if (usernameError != null) Color.Red else primaryColor
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            
            // 用户名错误提示
            if (usernameError != null) {
                Text(
                    text = usernameError!!,
                    fontSize = 12.sp,
                    color = Color.Red,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 密码输入框
            TextField(
                value = password,
                onValueChange = { 
                    password = it
                    passwordError = null  // 清除错误
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (passwordError != null) 2.dp else 1.dp,
                        color = if (passwordError != null) Color.Red else colorScheme.onSurface.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp)),
                placeholder = { Text("请输入密码") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,  // 使用密码键盘类型
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done  // 完成按钮
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password",
                        tint = if (passwordError != null) Color.Red else primaryColor
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible }
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                            tint = colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            
            // 密码错误提示
            if (passwordError != null) {
                Text(
                    text = passwordError!!,
                    fontSize = 12.sp,
                    color = Color.Red,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
            
            // 忘记密码和注册链接
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "忘记密码",
                    fontSize = 14.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onForgotPasswordClick() }
                )
                
                Text(
                    text = "新用户注册",
                    fontSize = 14.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        // 跳转到注册页面
                        navigator.push(com.lsfStudio.lsfTB.ui.navigation3.Route.Register)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 登录按钮
            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "请输入用户名和密码", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    isLoading = true
                    scope.launch {
                        try {
                            Log.d("LoginScreen", "🔐 开始登录: $username")
                            
                            // 构建请求体
                            val requestBody = JSONObject().apply {
                                put("username", username)
                                put("password", password)
                                put(
                                    "device_id",
                                    com.lsfStudio.lsfTB.ui.util.DataBase(context)
                                        .getMetadataBase64("device_id")
                                )
                            }.toString()
                            
                            // 在后台线程执行网络请求
                            val result = withContext(Dispatchers.IO) {
                                val url = "https://www.lsfstudio.top/lsfStudio/api/account/login"
                                val response = com.lsfStudio.lsfTB.ui.util.NetworkClient.send(
                                    context = context,
                                    method = "POST",
                                    url = url,
                                    path = "/lsfStudio/api/account/login",
                                    bodyContent = requestBody
                                )
                                
                                Triple(response.isSuccessful, response.code, response.body)
                            }
                            
                            val (isSuccess, code, responseBody) = result
                            
                            // 5. 处理响应（不管状态码，都尝试解析响应体）
                            if (responseBody != null) {
                                try {
                                    val json = JSONObject(responseBody)
                                    
                                    if (json.getBoolean("success")) {
                                        val data = json.getJSONObject("data")
                                        
                                        // 提取用户信息
                                        val userInfo = AccountManager.UserInfo(
                                            username = data.getString("username"),
                                            group = data.getString("group"),
                                            tag = data.optString("tag", ""),
                                            level = data.getInt("level"),
                                            experience = data.getInt("experience"),
                                            points = data.optInt("points", 0),
                                            nextLevel = data.getInt("next_level"),
                                            isCheckedIn = data.optString("is_checked_in", "false").toBoolean(),
                                            avatarPath = null  // 头像稍后处理
                                        )
                                        
                                        // 保存头像（如果有）
                                        val avatarBase64 = data.optString("avatar", "")
                                        if (avatarBase64.isNotEmpty()) {
                                            val avatarPath = AccountManager.saveAvatar(context, avatarBase64, userInfo.username)
                                            // 更新 userInfo 的 avatarPath
                                            val finalUserInfo = userInfo.copy(avatarPath = avatarPath)
                                            AccountManager.saveUserInfo(context, finalUserInfo)
                                        } else {
                                            AccountManager.saveUserInfo(context, userInfo)
                                        }
                                        
                                        Log.d("LoginScreen", "✅ 登录成功: ${userInfo.username}")
                                        Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                                        
                                        // 关闭登录界面
                                        onLoginSuccess()
                                    } else {
                                        val message = json.getString("message")
                                        Log.e("LoginScreen", "❌ 登录失败: $message")
                                        
                                        // 根据错误消息设置对应的错误状态
                                        when (message) {
                                            "UNKNOWN USER" -> {
                                                usernameError = "用户不存在"
                                            }
                                            "YOU ARE PROHIBITED FROM LOGGING IN" -> {
                                                usernameError = "账户已被封禁，禁止登录"
                                            }
                                            "WRONG PASSWORD" -> {
                                                passwordError = "密码错误"
                                            }
                                            else -> {
                                                // 其他错误显示在用户名输入框
                                                usernameError = message
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("LoginScreen", "❌ 解析JSON失败", e)
                                    if (usernameError == null) usernameError = "响应格式错误"
                                }
                            } else {
                                Log.e("LoginScreen", "❌ 响应体为空: code=$code")
                                // 网络错误，显示在用户名输入框
                                if (usernameError == null) usernameError = "网络请求失败 ($code)"
                            }
                            
                        } catch (e: Exception) {
                            Log.e("LoginScreen", "❌ 登录异常", e)
                            // 异常错误，显示在用户名输入框
                            if (usernameError == null) usernameError = "登录失败: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "登录",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 底部协议提示
            Text(
                text = "注册或登录代表您同意 注册协议 和 隐私政策",
                fontSize = 12.sp,
                color = colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
