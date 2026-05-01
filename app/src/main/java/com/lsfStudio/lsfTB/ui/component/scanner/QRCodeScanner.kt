package com.lsfStudio.lsfTB.ui.component.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.lsfStudio.lsfTB.ui.util.HapticFeedbackUtil

/**
 * Miuix 风格扫码组件
 * 
 * 职责：
 * 1. 提供统一的二维码/条形码扫描功能
 * 2. 支持不同服务的回调返回
 * 3. Miuix 风格 UI 设计
 * 4. 自动处理相机权限
 * 
 * @param onScanSuccess 扫码成功回调，返回扫描结果字符串
 * @param onDismiss 关闭扫码页面回调
 * @param title 页面标题
 * @param hint 提示文字
 * @param onScanFailed 扫描失败回调（用于相册扫描未识别）
 */
@Composable
fun QRCodeScanner(
    onScanSuccess: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "扫描二维码",
    hint: String = "将二维码放入框内，即可自动扫描",
    onScanFailed: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    // 状态管理
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var imageAnalyzer: ImageAnalysis? by remember { mutableStateOf(null) }
    var preview: Preview? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var barcodeScanner: BarcodeScanner? by remember { mutableStateOf(null) }
    var isScanning by remember { mutableStateOf(true) } // 控制是否继续扫描
    var showScanFailedDialog by remember { mutableStateOf(false) } // 显示扫描失败弹窗
    
    // 相机执行器
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    // 图片选择启动器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("QRCodeScanner", "选择了图片: $it")
            scanImageFromGallery(context, it, barcodeScanner) { result ->
                if (result != null) {
                    HapticFeedbackUtil.lightClick(context)
                    onScanSuccess(result)
                } else {
                    // 未识别到二维码
                    showScanFailedDialog = true
                }
            }
        }
    }
    
    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner?.close()
            cameraProvider?.unbindAll()
        }
    }
    
    // 权限请求启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            Log.d("QRCodeScanner", "相机权限已授予")
            HapticFeedbackUtil.lightClick(context)
        } else {
            Log.e("QRCodeScanner", "相机权限被拒绝")
            // 检查是否应该显示理由
            val shouldShowRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                context as? android.app.Activity ?: return@rememberLauncherForActivityResult,
                Manifest.permission.CAMERA
            )
            if (!shouldShowRationale) {
                // 用户选择了"不再询问"，需要引导去设置页面
                Log.w("QRCodeScanner", "权限被永久拒绝，需要引导用户去设置")
            }
        }
    }
    
    // 请求相机权限
    LaunchedEffect(Unit) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                hasCameraPermission = true
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    // 初始化条码扫描器
    LaunchedEffect(Unit) {
        try {
            // 使用默认配置初始化条码扫描器
            barcodeScanner = BarcodeScanning.getClient().also {
                Log.d("QRCodeScanner", "✅ BarcodeScanner 已初始化")
            }
        } catch (e: Exception) {
            Log.e("QRCodeScanner", "❌ BarcodeScanner 初始化失败", e)
            // 显示错误提示
            android.widget.Toast.makeText(
                context,
                "二维码扫描器初始化失败，请确保设备支持 Google Play Services",
                android.widget.Toast.LENGTH_LONG
            ).show()
            // 关闭扫码页面
            onDismiss()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // 相机预览
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    
                    // 绑定相机
                    scope.launch {
                        val provider = ProcessCameraProvider.getInstance(ctx).get()
                        cameraProvider = provider
                        
                        // 预览
                        preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        // 图像分析（用于扫码）
                        imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    if (!isScanning) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }
                                    
                                    processImageProxy(
                                        imageProxy,
                                        barcodeScanner,
                                        onScanSuccess
                                    ) {
                                        // 扫描成功后停止扫描
                                        isScanning = false
                                    }
                                }
                            }
                        
                        // 选择后置摄像头
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        
                        try {
                            // 解绑之前的用例
                            provider.unbindAll()
                            
                            // 绑定新的用例
                            camera = provider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalyzer
                            )
                            
                            Log.d("QRCodeScanner", "相机已绑定")
                        } catch (e: Exception) {
                            Log.e("QRCodeScanner", "相机绑定失败", e)
                        }
                    }
                    
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // 顶部控制栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 返回按钮
                    IconButton(onClick = {
                        HapticFeedbackUtil.lightClick(context)
                        onDismiss()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            "返回",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    // 标题
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                    
                    // 右侧按钮组
                    Row {
                        // 从相册扫描按钮
                        IconButton(onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            imagePickerLauncher.launch("image/*")
                        }) {
                            Icon(
                                Icons.Rounded.PhotoLibrary,
                                "从相册扫描",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        // 闪光灯开关
                        IconButton(
                            onClick = {
                                HapticFeedbackUtil.lightClick(context)
                                isFlashOn = !isFlashOn
                                camera?.cameraControl?.enableTorch(isFlashOn)
                            }
                        ) {
                            Icon(
                                if (isFlashOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                                "闪光灯",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
            
            // 扫码框
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(56.dp), // 调整外层边距
                contentAlignment = Alignment.Center
            ) {
                // 外层容器：用于放置角装饰
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                ) {
                    // 四个角的装饰（在外层容器边缘）
                    CornerDecorations()
                }
                
                // 内层容器：扫码框本身（比外层小16dp，每边8dp）
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(16.dp) // 增大间距到16dp
                        .border(
                            width = 2.dp,
                            color = colorScheme.primary.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            
            // 底部提示文字
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = hint,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
            }
        } else {
            // 无权限提示
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "需要相机权限才能扫码",
                        color = Color.White,
                        fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    top.yukonga.miuix.kmp.basic.TextButton(
                        text = "授予权限",
                        onClick = {
                            HapticFeedbackUtil.lightClick(context)
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    }
    
    // 扫描失败弹窗
    if (showScanFailedDialog) {
        WindowDialog(
            show = showScanFailedDialog,
            title = "扫描结果",
            onDismissRequest = { showScanFailedDialog = false }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "未识别到二维码",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                TextButton(
                    text = "确定",
                    onClick = {
                        HapticFeedbackUtil.lightClick(context)
                        showScanFailedDialog = false
                    },
                    colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 处理图像帧进行条码识别
 */
private fun processImageProxy(
    imageProxy: ImageProxy,
    barcodeScanner: BarcodeScanner?,
    onScanSuccess: (String) -> Unit,
    onStopScanning: () -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        
        barcodeScanner?.process(image)
            ?.addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrEmpty()) {
                        Log.d("QRCodeScanner", "扫描成功: $rawValue")
                        // 在主线程回调
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            onScanSuccess(rawValue)
                            onStopScanning()
                        }
                        break
                    }
                }
            }
            ?.addOnFailureListener { e ->
                Log.e("QRCodeScanner", "扫描失败", e)
            }
            ?.addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

/**
 * 从相册图片扫描条码
 */
private fun scanImageFromGallery(
    context: Context,
    uri: Uri,
    barcodeScanner: BarcodeScanner?,
    onResult: (String?) -> Unit
) {
    try {
        val bitmap = MediaStore.Images.Media.getBitmap(
            context.contentResolver,
            uri
        )
        val image = InputImage.fromBitmap(bitmap, 0)
        
        barcodeScanner?.process(image)
            ?.addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (rawValue != null && rawValue.isNotEmpty()) {
                        Log.d("QRCodeScanner", "从相册扫描成功: $rawValue")
                        // 返回扫描结果
                        onResult(rawValue)
                        return@addOnSuccessListener
                    }
                }
                // 未找到二维码
                Log.w("QRCodeScanner", "未在图片中找到二维码")
                onResult(null)
            }
            ?.addOnFailureListener { e ->
                Log.e("QRCodeScanner", "从相册扫描失败", e)
                onResult(null)
            }
    } catch (e: Exception) {
        Log.e("QRCodeScanner", "处理相册图片失败", e)
        onResult(null)
    }
}

/**
 * 扫码框四角装饰
 */
@Composable
private fun CornerDecorations() {
    val cornerColor = colorScheme.primary
    val cornerLength = 24.dp
    val cornerWidth = 4.dp
    val cornerRadius = 4.dp // 减小圆角，不能超过宽度
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 左上角
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(cornerLength, cornerWidth)
                .clip(RoundedCornerShape(topStart = cornerRadius))
                .background(cornerColor)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(cornerWidth, cornerLength)
                .clip(RoundedCornerShape(topStart = cornerRadius))
                .background(cornerColor)
        )
        
        // 右上角
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(cornerLength, cornerWidth)
                .clip(RoundedCornerShape(topEnd = cornerRadius))
                .background(cornerColor)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(cornerWidth, cornerLength)
                .clip(RoundedCornerShape(topEnd = cornerRadius))
                .background(cornerColor)
        )
        
        // 左下角
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(cornerLength, cornerWidth)
                .clip(RoundedCornerShape(bottomStart = cornerRadius))
                .background(cornerColor)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(cornerWidth, cornerLength)
                .clip(RoundedCornerShape(bottomStart = cornerRadius))
                .background(cornerColor)
        )
        
        // 右下角
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(cornerLength, cornerWidth)
                .clip(RoundedCornerShape(bottomEnd = cornerRadius))
                .background(cornerColor)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(cornerWidth, cornerLength)
                .clip(RoundedCornerShape(bottomEnd = cornerRadius))
                .background(cornerColor)
        )
    }
}
