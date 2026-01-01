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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.tripglide.ui.theme.TripGlideTheme

import android.media.MediaPlayer

private const val TAG = "SummonActivity"

class SummonActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "🎯 SummonActivity onCreate")

        turnScreenOnAndKeyguard()

        val circleId = intent.getStringExtra("circleId") ?: run {
            Log.e(TAG, "Missing circleId")
            return finish()
        }
        val summonId = intent.getStringExtra("summonId") ?: run {
            Log.e(TAG, "Missing summonId")
            return finish()
        }
        val initiatorName = intent.getStringExtra("initiatorName") ?: "Someone"
        val initiatorPhotoUrl = intent.getStringExtra("initiatorPhotoUrl")
        val isInitiator = intent.getBooleanExtra("isInitiator", false)
        
        Log.d(TAG, "📦 Data: circleId=$circleId, summonId=$summonId, initiator=$initiatorName, isInitiator=$isInitiator")
        
        // Cancel any existing notification for this summon
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(summonId.hashCode())

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
                            finish() 
                        }
                    )
                }
            }
        }
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
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
