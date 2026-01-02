package com.example.tripglide.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.tripglide.R
import com.example.tripglide.data.model.SummonStatus
import com.example.tripglide.ui.summon.SummonActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Foreground Service to maintain summon session and provide an ongoing notification
 * that allows the user to return to the SummonActivity even after pressing Home/Back.
 * 
 * Features:
 * - Starts when a summon is initiated or received
 * - Displays an ongoing notification (cannot be swiped away)
 * - Tapping notification brings user back to SummonActivity
 * - Observes Firestore for summon status changes
 * - Auto-stops when summon completes (SUCCESS/FAILED)
 */
class SummonForegroundService : Service() {
    
    companion object {
        private const val TAG = "SummonForegroundService"
        const val CHANNEL_ID = "summon_ongoing_channel"
        const val NOTIFICATION_ID = 9999
        
        // Intent extras
        const val EXTRA_CIRCLE_ID = "circleId"
        const val EXTRA_SUMMON_ID = "summonId"
        const val EXTRA_CIRCLE_NAME = "circleName"
        const val EXTRA_INITIATOR_NAME = "initiatorName"
        const val EXTRA_INITIATOR_PHOTO_URL = "initiatorPhotoUrl"
        const val EXTRA_IS_INITIATOR = "isInitiator"
        
        // Actions
        const val ACTION_START = "com.example.tripglide.action.START_SUMMON_SERVICE"
        const val ACTION_STOP = "com.example.tripglide.action.STOP_SUMMON_SERVICE"
        
        /**
         * Start the foreground service with summon details
         */
        fun start(
            context: Context,
            circleId: String,
            summonId: String,
            circleName: String,
            initiatorName: String,
            initiatorPhotoUrl: String?,
            isInitiator: Boolean
        ) {
            val intent = Intent(context, SummonForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CIRCLE_ID, circleId)
                putExtra(EXTRA_SUMMON_ID, summonId)
                putExtra(EXTRA_CIRCLE_NAME, circleName)
                putExtra(EXTRA_INITIATOR_NAME, initiatorName)
                putExtra(EXTRA_INITIATOR_PHOTO_URL, initiatorPhotoUrl)
                putExtra(EXTRA_IS_INITIATOR, isInitiator)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "📢 Service start requested")
        }
        
        /**
         * Stop the foreground service
         */
        fun stop(context: Context) {
            val intent = Intent(context, SummonForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
            Log.d(TAG, "🛑 Service stop requested")
        }
    }
    
    private val firestore = FirebaseFirestore.getInstance()
    private var summonListener: ListenerRegistration? = null
    
    private var circleId: String = ""
    private var summonId: String = ""
    private var circleName: String = ""
    private var initiatorName: String = ""
    private var initiatorPhotoUrl: String? = null
    private var isInitiator: Boolean = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🔧 Service created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📩 onStartCommand: action=${intent?.action}")
        
        when (intent?.action) {
            ACTION_START -> {
                // Extract data from intent
                circleId = intent.getStringExtra(EXTRA_CIRCLE_ID) ?: ""
                summonId = intent.getStringExtra(EXTRA_SUMMON_ID) ?: ""
                circleName = intent.getStringExtra(EXTRA_CIRCLE_NAME) ?: "Squad"
                initiatorName = intent.getStringExtra(EXTRA_INITIATOR_NAME) ?: "Someone"
                initiatorPhotoUrl = intent.getStringExtra(EXTRA_INITIATOR_PHOTO_URL)
                isInitiator = intent.getBooleanExtra(EXTRA_IS_INITIATOR, false)
                
                if (circleId.isEmpty() || summonId.isEmpty()) {
                    Log.e(TAG, "❌ Missing circleId or summonId")
                    stopSelf()
                    return START_NOT_STICKY
                }
                
                // Start foreground with notification
                val notification = buildNotification()
                startForeground(NOTIFICATION_ID, notification)
                Log.d(TAG, "🚀 Foreground started with notification")
                
                // Start observing summon status
                observeSummonStatus()
            }
            
            ACTION_STOP -> {
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        summonListener?.remove()
        Log.d(TAG, "🗑️ Service destroyed")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Summons",
                NotificationManager.IMPORTANCE_LOW // Low to avoid sound/vibration repeatedly
            ).apply {
                description = "Shows when a squad summon is active"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            Log.d(TAG, "📺 Notification channel created")
        }
    }
    
    private fun buildNotification(): Notification {
        // Intent to open SummonActivity when tapped
        val openIntent = Intent(this, SummonActivity::class.java).apply {
            putExtra("circleId", circleId)
            putExtra("summonId", summonId)
            putExtra("initiatorName", initiatorName)
            putExtra("initiatorPhotoUrl", initiatorPhotoUrl)
            putExtra("isInitiator", isInitiator)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = if (isInitiator) "Summoning Squad..." else "🚨 Squad Summon Active!"
        val content = if (isInitiator) {
            "Waiting for response in $circleName"
        } else {
            "$initiatorName is summoning you in $circleName"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true) // Cannot be swiped away
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_menu_view,
                "Open",
                pendingIntent
            )
            .build()
    }
    
    private fun observeSummonStatus() {
        Log.d(TAG, "👀 Starting to observe summon: $circleId/$summonId")
        
        summonListener = firestore.collection("circles")
            .document(circleId)
            .collection("summons")
            .document(summonId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Firestore error: ${error.message}")
                    return@addSnapshotListener
                }
                
                val status = snapshot?.getString("status")
                Log.d(TAG, "📊 Summon status update: $status")
                
                when (status) {
                    SummonStatus.SUCCESS.name, SummonStatus.FAILED.name -> {
                        Log.d(TAG, "🏁 Summon completed with status: $status")
                        // Give UI time to show the result overlay
                        android.os.Handler(mainLooper).postDelayed({
                            stopSelf()
                        }, 2500)
                    }
                }
            }
    }
}
