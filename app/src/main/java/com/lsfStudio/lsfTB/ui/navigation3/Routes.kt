package com.lsfStudio.lsfTB.ui.navigation3

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

sealed interface Route : NavKey, Parcelable {
    @Parcelize
    @Serializable
    data object Main : Route

    @Parcelize
    @Serializable
    data object Login : Route

    @Parcelize
    @Serializable
    data object Register : Route

    @Parcelize
    @Serializable
    data object Settings : Route

    @Parcelize
    @Serializable
    data object About : Route

    @Parcelize
    @Serializable
    data object Debug : Route  // Debug 设置页面

    @Parcelize
    @Serializable
    data object ColorPalette : Route

    @Parcelize
    @Serializable
    data object TwoFA : Route
    
    @Parcelize
    @Serializable
    data class QRCodeScanner(
        val title: String = "扫描二维码",
        val hint: String = "将二维码放入框内，即可自动扫描"
    ) : Route
    
    @Parcelize
    @Serializable
    data class ImageViewer(
        val filePath: String,
        val fileName: String,
        val addedTime: Long,
        val fileId: Long = 0,
        val allFilePaths: List<String> = emptyList(),
        val allFileNames: List<String> = emptyList(),
        val allAddedTimes: List<Long> = emptyList(),
        val currentIndex: Int = 0
    ) : Route
    
    @Parcelize
    @Serializable
    data class VideoPlayer(
        val filePath: String,
        val fileName: String,
        val fileId: Long = 0,
        val allFilePaths: List<String> = emptyList(),
        val allFileNames: List<String> = emptyList(),
        val allAddedTimes: List<Long> = emptyList(),
        val currentIndex: Int = 0
    ) : Route
    
    @Parcelize
    @Serializable
    data class ProfessionalVideoPlayer(
        val filePath: String,
        val fileName: String,
        val fileId: Long = 0,
        val allFilePaths: List<String> = emptyList(),
        val allFileNames: List<String> = emptyList(),
        val allAddedTimes: List<Long> = emptyList(),
        val currentIndex: Int = 0
    ) : Route
}
