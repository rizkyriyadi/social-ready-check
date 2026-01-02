package com.example.tripglide.ui.summon

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.tripglide.service.SummonForegroundService
import com.example.tripglide.ui.theme.TripGlideTheme

private const val TAG = "SummonActivity"

/**
 * Full-screen activity for handling Squad Summons (Ready Check).
 * 
 * Features:
 * - Shows over lock screen
 * - Keeps screen on
 * - Starts foreground service for persistence
 * - Overrides back button to minimize instead of close
 */
class SummonActivity : ComponentActivity() {
    
    private var circleId: String = ""
    private var summonId: String = ""
    private var initiatorName: String = ""
    private var initiatorPhotoUrl: String? = null
    private var isInitiator: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "🎯 SummonActivity onCreate")

        turnScreenOnAndKeyguard()
        
        // Extract intent extras
        circleId = intent.getStringExtra("circleId") ?: run {
            Log.e(TAG, "Missing circleId")
            return finish()
        }
        summonId = intent.getStringExtra("summonId") ?: run {
            Log.e(TAG, "Missing summonId")
            return finish()
        }
        initiatorName = intent.getStringExtra("initiatorName") ?: "Someone"
        initiatorPhotoUrl = intent.getStringExtra("initiatorPhotoUrl")
        isInitiator = intent.getBooleanExtra("isInitiator", false)
        
        Log.d(TAG, "📦 Data: circleId=$circleId, summonId=$summonId, initiator=$initiatorName, isInitiator=$isInitiator")
        
        // Cancel any existing notification for this summon (the one from FCM)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(summonId.hashCode())

        // Start foreground service for persistence
        startSummonForegroundService()
        
        // Handle back button - minimize instead of finish
        setupBackHandler()

        setContent {
            TripGlideTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SummonScreen(
                        circleId = circleId,
                        summonId = summonId,
                        initiatorName = initiatorName,
                        initiatorPhotoUrl = initiatorPhotoUrl,
                        isInitiator = isInitiator,
                        onFinish = { 
                            Log.d(TAG, "👋 Finishing SummonActivity")
                            stopSummonForegroundService()
                            finish() 
                        }
                    )
                }
            }
        }
    }
    
    private fun startSummonForegroundService() {
        Log.d(TAG, "📢 Starting foreground service")
        SummonForegroundService.start(
            context = this,
            circleId = circleId,
            summonId = summonId,
            circleName = "Squad", // Will be updated by the service from Firestore
            initiatorName = initiatorName,
            initiatorPhotoUrl = initiatorPhotoUrl,
            isInitiator = isInitiator
        )
    }
    
    private fun stopSummonForegroundService() {
        Log.d(TAG, "🛑 Stopping foreground service")
        SummonForegroundService.stop(this)
    }
    
    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "⬅️ Back pressed - moving to background instead of finishing")
                // Move to background instead of finishing
                // This allows the user to return via the ongoing notification
                moveTaskToBack(true)
            }
        })
    }

    private fun turnScreenOnAndKeyguard() {
        Log.d(TAG, "🔓 Turning screen on and dismissing keyguard")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        with(getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestDismissKeyguard(this@SummonActivity, null)
            }
        }
        
        // WakeLock to ensure screen stays on
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "TripGlide:SummonWakeLock"
        )
        wakeLock.acquire(60 * 1000L /*60 seconds*/)
        Log.d(TAG, "⚡ WakeLock acquired for 60s")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🗑️ SummonActivity destroyed")
    }
}
