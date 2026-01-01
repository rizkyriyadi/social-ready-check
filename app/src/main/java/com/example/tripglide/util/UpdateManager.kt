package com.example.tripglide.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.tripglide.BuildConfig
import com.example.tripglide.data.model.AppUpdateInfo
import com.example.tripglide.data.model.DownloadState
import com.example.tripglide.data.model.UpdateCheckResult
import com.example.tripglide.data.network.UpdateService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

private const val TAG = "UpdateManager"
private const val APK_FILE_NAME = "jack_mei_update.apk"

/**
 * OTA Update Manager
 * 
 * Handles checking for updates from GitHub raw JSON and downloading/installing APKs.
 * 
 * Usage:
 * 1. Call checkForUpdates() to check if new version is available
 * 2. If UpdateAvailable, call downloadUpdate() to start download
 * 3. After download completes, call installUpdate() to trigger installation
 */
class UpdateManager(private val context: Context) {

    private val updateService: UpdateService by lazy {
        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UpdateService::class.java)
    }

    private val _updateState = MutableStateFlow<UpdateCheckResult>(UpdateCheckResult.Loading)
    val updateState: StateFlow<UpdateCheckResult> = _updateState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var currentDownloadId: Long = -1
    private var downloadReceiver: BroadcastReceiver? = null

    /**
     * Check for available updates by fetching version.json from GitHub
     */
    suspend fun checkForUpdates(): UpdateCheckResult {
        _updateState.value = UpdateCheckResult.Loading
        
        return try {
            val updateInfo = withContext(Dispatchers.IO) {
                updateService.checkForUpdate()
            }

            Log.d(TAG, "Remote version: ${updateInfo.versionCode}, Local version: ${BuildConfig.VERSION_CODE}")

            val result = if (updateInfo.versionCode > BuildConfig.VERSION_CODE) {
                Log.d(TAG, "Update available: ${updateInfo.versionName}")
                UpdateCheckResult.UpdateAvailable(updateInfo)
            } else {
                Log.d(TAG, "No update available")
                UpdateCheckResult.NoUpdateAvailable
            }
            
            _updateState.value = result
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            val error = UpdateCheckResult.Error(e.message ?: "Unknown error")
            _updateState.value = error
            error
        }
    }

    /**
     * Download the APK update
     */
    fun downloadUpdate(updateInfo: AppUpdateInfo) {
        Log.d(TAG, "Starting download from: ${updateInfo.downloadUrl}")
        _downloadState.value = DownloadState.Downloading(0)

        // Delete old APK if exists
        val oldFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            APK_FILE_NAME
        )
        if (oldFile.exists()) {
            oldFile.delete()
        }

        val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
            .setTitle("Jack & Mei Update v${updateInfo.versionName}")
            .setDescription("Downloading update...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME)
            .setMimeType("application/vnd.android.package-archive")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        currentDownloadId = downloadManager.enqueue(request)

        // Register receiver for download complete
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == currentDownloadId) {
                    Log.d(TAG, "Download complete")
                    
                    // Check download status
                    val query = DownloadManager.Query().setFilterById(currentDownloadId)
                    val cursor = downloadManager.query(query)
                    
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = cursor.getInt(statusIndex)
                        
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            _downloadState.value = DownloadState.Downloaded
                            installUpdate()
                        } else {
                            val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                            val reason = cursor.getInt(reasonIndex)
                            _downloadState.value = DownloadState.Failed("Download failed: $reason")
                        }
                    }
                    cursor.close()
                    
                    // Unregister receiver
                    try {
                        ctxt.unregisterReceiver(this)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error unregistering receiver", e)
                    }
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(downloadReceiver, filter)
        }
    }

    /**
     * Install the downloaded APK
     */
    fun installUpdate() {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            APK_FILE_NAME
        )

        if (!file.exists()) {
            Log.e(TAG, "APK file not found at: ${file.absolutePath}")
            _downloadState.value = DownloadState.Failed("APK file not found")
            return
        }

        Log.d(TAG, "Installing APK from: ${file.absolutePath}")

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK", e)
            _downloadState.value = DownloadState.Failed("Failed to install: ${e.message}")
        }
    }

    /**
     * Get current app version info
     */
    fun getCurrentVersionInfo(): Pair<Int, String> {
        return Pair(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME)
    }

    /**
     * Reset states
     */
    fun resetState() {
        _updateState.value = UpdateCheckResult.Loading
        _downloadState.value = DownloadState.Idle
    }

    /**
     * Cleanup receiver when no longer needed
     */
    fun cleanup() {
        downloadReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
