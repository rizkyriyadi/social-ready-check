package com.example.tripglide.data.model

import com.google.gson.annotations.SerializedName

/**
 * OTA Update response model from GitHub raw JSON
 */
data class AppUpdateInfo(
    @SerializedName("versionCode") val versionCode: Int,
    @SerializedName("versionName") val versionName: String,
    @SerializedName("releaseNotes") val releaseNotes: String,
    @SerializedName("downloadUrl") val downloadUrl: String,
    @SerializedName("forceUpdate") val forceUpdate: Boolean = false
)

/**
 * Update check result states
 */
sealed class UpdateCheckResult {
    object Loading : UpdateCheckResult()
    object NoUpdateAvailable : UpdateCheckResult()
    data class UpdateAvailable(val updateInfo: AppUpdateInfo) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * Download state for tracking APK download progress
 */
sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    object Downloaded : DownloadState()
    data class Failed(val message: String) : DownloadState()
}
