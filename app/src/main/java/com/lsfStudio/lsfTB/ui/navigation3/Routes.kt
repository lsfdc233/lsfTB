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
    data object Settings : Route

    @Parcelize
    @Serializable
    data object About : Route

    @Parcelize
    @Serializable
    data object ColorPalette : Route

    @Parcelize
    @Serializable
    data object TwoFA : Route
    
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
}
