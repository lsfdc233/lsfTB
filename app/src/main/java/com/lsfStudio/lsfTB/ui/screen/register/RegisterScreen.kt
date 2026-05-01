package com.lsfStudio.lsfTB.ui.screen.register

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lsfStudio.lsfTB.ui.theme.LocalColorMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import java.io.ByteArrayOutputStream

/**
 * 将 Uri 转换为 Base64 字符串
 */
private fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        
        if (bytes != null) {
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/png;base64,$base64"
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("RegisterScreen", "Uri 转 Base64 失败", e)
        null
    }
}

/**
 * 注册页面
 * 
 * 使用 Miuix 控件，适配莫奈取色
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val colorMode = LocalColorMode.current
    val navigator = com.lsfStudio.lsfTB.ui.navigation3.LocalNavigator.current
    val scope = rememberCoroutineScope()
    
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
    
    // 输入框状态
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var verifyCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isSendingCode by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(0) }  // 倒计时秒数
    
    // 错误状态
    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var verifyCodeError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    
    // FocusRequesters 用于控制焦点和滚动
    val usernameFocus = remember { FocusRequester() }
    val emailFocus = remember { FocusRequester() }
    val verifyCodeFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val confirmPasswordFocus = remember { FocusRequester() }
    
    // 滚动状态
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // 头像相关
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    
    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            avatarUri = it
        }
    }
    
    // 倒计时协程
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)  // 添加垂直滚动
        ) {
            // 顶部留白
            Spacer(modifier = Modifier.height(60.dp))
            
            // Logo 和标题区域
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                // App Logo
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "App Logo",
                        modifier = Modifier.size(32.dp),
                        tint = primaryColor
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // 标题文字
                Column {
                    Text(
                        text = "创建账户",
                        fontSize = 24.sp,
                        color = colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "加入 lsfTB",
                        fontSize = 14.sp,
                        color = colorScheme.onBackground.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // 圆形头像选择器
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = primaryColor.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .clickable {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null) {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Select Avatar",
                        modifier = Modifier.size(48.dp),
                        tint = primaryColor.copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "点击选择头像",
                fontSize = 12.sp,
                color = colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
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
                placeholder = { Text("请输入用户名") },
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
            
            // 邮箱输入框 + 发送验证码按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        emailError = null  // 清除错误
                    },
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (emailError != null) 2.dp else 1.dp,
                            color = if (emailError != null) Color.Red else colorScheme.onSurface.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp)),
                    placeholder = { Text("请输入邮箱") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = if (emailError != null) Color.Red else primaryColor
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colorScheme.surface,
                        unfocusedContainerColor = colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                
                Button(
                    onClick = {
                        if (email.isBlank()) {
                            emailError = "请输入邮箱地址"
                            return@Button
                        }
                        
                        isSendingCode = true
                        scope.launch {
                            try {
                                Log.d("RegisterScreen", "📧 发送验证码到: $email")
                                
                                // 构建请求体
                                val requestBody = JSONObject().apply {
                                    put("email_address", email)
                                }.toString()
                                
                                // 调用发送验证码 API
                                val result = withContext(Dispatchers.IO) {
                                    val url = "https://www.lsfstudio.top/lsfStudio/api/account/send-verify-code"
                                    val request = com.lsfStudio.lsfTB.ui.util.NetworkClient.buildPostRequestWithChallenge(
                                        context = context,
                                        url = url,
                                        path = "/lsfStudio/api/account/send-verify-code",
                                        body = requestBody.toRequestBody("application/json".toMediaType()),
                                        bodyContent = requestBody,
                                        useChallengeResponse = true  // ✅ 所有请求统一使用 Challenge-Response
                                    )
                                    
                                    val response = com.lsfStudio.lsfTB.ui.util.NetworkClient.execute(request)
                                    val responseBody = response.body?.string()
                                    
                                    Triple(response.isSuccessful, response.code, responseBody)
                                }
                                
                                val (isSuccess, code, responseBody) = result
                                
                                // 不管状态码，都尝试解析响应体
                                if (responseBody != null) {
                                    try {
                                        val json = JSONObject(responseBody)
                                        
                                        if (json.getBoolean("success")) {
                                            Log.d("RegisterScreen", "✅ 验证码发送成功")
                                            emailError = null
                                            // 启动60秒倒计时
                                            countdown = 60
                                        } else {
                                            val message = json.getString("message")
                                            Log.e("RegisterScreen", "❌ 发送失败: $message")
                                            
                                            // 根据错误码设置错误状态
                                            emailError = when (message) {
                                                "EMAIL ADDRESS IS USED" -> "该邮箱已被注册"
                                                else -> message
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("RegisterScreen", "❌ 解析JSON失败", e)
                                        emailError = "响应格式错误"
                                    }
                                } else {
                                    Log.e("RegisterScreen", "❌ 响应体为空: code=$code")
                                    emailError = "网络请求失败 ($code)"
                                }
                                
                            } catch (e: Exception) {
                                Log.e("RegisterScreen", "❌ 发送验证码异常", e)
                                emailError = "发送失败: ${e.message}"
                            } finally {
                                isSendingCode = false
                            }
                        }
                    },
                    enabled = !isSendingCode && countdown == 0  // 倒计时期间禁用
                ) {
                    if (isSendingCode) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (countdown > 0) "${countdown}s" else "发送验证码",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // 邮箱错误提示
            if (emailError != null) {
                Text(
                    text = emailError!!,
                    fontSize = 12.sp,
                    color = Color.Red,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 验证码输入框
            TextField(
                value = verifyCode,
                onValueChange = { 
                    verifyCode = it
                    verifyCodeError = null  // 清除错误
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (verifyCodeError != null) 2.dp else 1.dp,
                        color = if (verifyCodeError != null) Color.Red else colorScheme.onSurface.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp)),
                placeholder = { Text("请输入验证码") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            
            // 验证码错误提示
            if (verifyCodeError != null) {
                Text(
                    text = verifyCodeError!!,
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
                    .focusRequester(passwordFocus)  // 添加焦点控制
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
                    imeAction = ImeAction.Next  // 下一个按钮
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
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 确认密码输入框
            TextField(
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                    confirmPasswordError = null  // 清除错误
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(confirmPasswordFocus)  // 添加焦点控制
                    .border(
                        width = if (confirmPasswordError != null) 2.dp else 1.dp,
                        color = if (confirmPasswordError != null) Color.Red else colorScheme.onSurface.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp)),
                placeholder = { Text("请再次输入密码") },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,  // 使用密码键盘类型
                    imeAction = ImeAction.Done  // 完成按钮
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Confirm Password",
                        tint = if (confirmPasswordError != null) Color.Red else primaryColor
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { confirmPasswordVisible = !confirmPasswordVisible }
                    ) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (confirmPasswordVisible) "隐藏密码" else "显示密码",
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
            
            // 确认密码错误提示
            if (confirmPasswordError != null) {
                Text(
                    text = confirmPasswordError!!,
                    fontSize = 12.sp,
                    color = Color.Red,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 注册按钮
            Button(
                onClick = {
                    // 清除所有错误
                    usernameError = null
                    emailError = null
                    verifyCodeError = null
                    passwordError = null
                    confirmPasswordError = null
                    
                    // 验证输入
                    if (username.isBlank()) {
                        usernameError = "请输入用户名"
                        return@Button
                    }
                    if (email.isBlank()) {
                        emailError = "请输入邮箱"
                        return@Button
                    }
                    if (verifyCode.isBlank()) {
                        verifyCodeError = "请输入验证码"
                        return@Button
                    }
                    if (password.isBlank()) {
                        passwordError = "请输入密码"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        confirmPasswordError = "两次输入的密码不一致"
                        return@Button
                    }
                    
                    isLoading = true
                    scope.launch {
                        try {
                            Log.d("RegisterScreen", "📝 开始注册: $username")
                            
                            // 1. 将头像转换为 Base64
                            var avatarBase64: String? = null
                            if (avatarUri != null) {
                                avatarBase64 = withContext(Dispatchers.IO) {
                                    uriToBase64(context, avatarUri!!)
                                }
                            }
                            
                            // 2. 构建请求体
                            val requestBody = JSONObject().apply {
                                put("username", username)
                                put("password", password)
                                put("email_address", email)
                                put("verify_code", verifyCode)
                                put("register_ip", "")  // 服务端会自动获取
                                if (avatarBase64 != null) {
                                    put("avatar", avatarBase64)
                                }
                            }.toString()
                            
                            Log.d("RegisterScreen", "📤 请求体: $requestBody")
                            
                            // 3. 调用注册 API
                            val result = withContext(Dispatchers.IO) {
                                val url = "https://www.lsfstudio.top/lsfStudio/api/account/register"
                                val request = com.lsfStudio.lsfTB.ui.util.NetworkClient.buildPostRequestWithChallenge(
                                    context = context,
                                    url = url,
                                    path = "/lsfStudio/api/account/register",
                                    body = requestBody.toRequestBody("application/json".toMediaType()),
                                    bodyContent = requestBody,
                                    useChallengeResponse = true  // ✅ 所有请求统一使用 Challenge-Response
                                )
                                
                                val response = com.lsfStudio.lsfTB.ui.util.NetworkClient.execute(request)
                                val responseBody = response.body?.string()
                                
                                Triple(response.isSuccessful, response.code, responseBody)
                            }
                            
                            val (isSuccess, code, responseBody) = result
                            
                            Log.d("RegisterScreen", "📥 响应: code=$code, body=$responseBody")
                            
                            // 4. 处理响应（不管状态码，都尝试解析响应体）
                            if (responseBody != null) {
                                try {
                                    val json = JSONObject(responseBody)
                                    
                                    if (json.getBoolean("success")) {
                                        Log.d("RegisterScreen", "✅ 注册成功")
                                        Toast.makeText(context, "注册成功！", Toast.LENGTH_SHORT).show()
                                        
                                        // 返回登录页面
                                        navigator.pop()
                                    } else {
                                        val message = json.getString("message")
                                        Log.e("RegisterScreen", "❌ 注册失败: $message")
                                        
                                        // 根据错误码设置对应输入框的错误状态
                                        when (message) {
                                            "USERNAME IS USED" -> usernameError = "该用户名已被注册"
                                            "NULL VERIFY CODE" -> verifyCodeError = "该邮箱无注册请求"
                                            "WRONG VERIFY CODE" -> verifyCodeError = "验证码错误"
                                            "EMAIL ADDRESS IS USED" -> emailError = "该邮箱已被注册"
                                            "REGISTER SUCCESS" -> {
                                                Toast.makeText(context, "注册成功！", Toast.LENGTH_SHORT).show()
                                                navigator.pop()
                                            }
                                            else -> {
                                                // 未知错误，显示在第一个有错误的输入框
                                                if (usernameError == null) usernameError = message
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("RegisterScreen", "❌ 解析JSON失败", e)
                                    if (usernameError == null) usernameError = "响应格式错误"
                                }
                            } else {
                                Log.e("RegisterScreen", "❌ 响应体为空: code=$code")
                                // 网络错误，显示在用户名输入框
                                if (usernameError == null) usernameError = "网络请求失败 ($code)"
                            }
                            
                        } catch (e: Exception) {
                            Log.e("RegisterScreen", "❌ 注册异常", e)
                            // 异常错误，显示在用户名输入框
                            if (usernameError == null) usernameError = "注册失败: ${e.message}"
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
                        text = "注册",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 底部协议提示
            Text(
                text = "注册代表您同意 注册协议 和 隐私政策",
                fontSize = 12.sp,
                color = colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
