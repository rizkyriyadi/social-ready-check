package com.example.tripglide.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.example.tripglide.MainActivity
import com.example.tripglide.R
import com.example.tripglide.ui.summon.SummonActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL

private const val TAG = "FCMService"

// Notification Channels
const val CHANNEL_CIRCLE_MESSAGES = "circle_messages"
const val CHANNEL_DM_MESSAGES = "dm_messages"
const val CHANNEL_FRIEND_REQUESTS = "friend_requests"

// Intent Keys for Quick Reply
const val KEY_REPLY_TEXT = "key_reply_text"
const val KEY_NOTIFICATION_ID = "notification_id"
const val KEY_CHANNEL_ID = "channel_id"
const val KEY_CHAT_TYPE = "chat_type"

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        createChatNotificationChannels()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("fcmToken", token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "📩 FCM Message received! Data: ${message.data}")
        
        val type = message.data["type"]
        
        when (type) {
            "SUMMON" -> handleSummonNotification(message.data)
            "CIRCLE_MESSAGE" -> handleCircleMessageNotification(message.data)
            "DM_MESSAGE" -> handleDirectMessageNotification(message.data)
            else -> {
                val title = message.notification?.title ?: "TripGlide"
                val body = message.notification?.body ?: ""
                showNotification(title, body, type)
            }
        }
    }
    
    // ==================== CHAT NOTIFICATIONS ====================
    
    private fun handleCircleMessageNotification(data: Map<String, String>) {
        val senderId = data["senderId"] ?: ""
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        
        // Don't show notification for own messages
        if (senderId == currentUserId) {
            Log.d(TAG, "Skipping notification for own message")
            return
        }
        
        val channelId = data["channelId"] ?: return
        val circleName = data["circleName"] ?: "Circle"
        val senderName = data["senderName"] ?: "Someone"
        val senderPhotoUrl = data["senderPhotoUrl"] ?: ""
        val messageContent = data["messageContent"] ?: ""
        val messageType = data["messageType"] ?: "TEXT"
        
        CoroutineScope(Dispatchers.IO).launch {
            showCircleMessageNotification(
                channelId = channelId,
                circleName = circleName,
                senderName = senderName,
                senderPhotoUrl = senderPhotoUrl,
                messageContent = messageContent,
                messageType = messageType
            )
        }
    }
    
    private fun handleDirectMessageNotification(data: Map<String, String>) {
        val senderId = data["senderId"] ?: ""
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        
        // Don't show notification for own messages
        if (senderId == currentUserId) {
            Log.d(TAG, "Skipping notification for own message")
            return
        }
        
        val channelId = data["channelId"] ?: return
        val senderName = data["senderName"] ?: "Someone"
        val senderPhotoUrl = data["senderPhotoUrl"] ?: ""
        val messageContent = data["messageContent"] ?: ""
        val messageType = data["messageType"] ?: "TEXT"
        
        CoroutineScope(Dispatchers.IO).launch {
            showDirectMessageNotification(
                channelId = channelId,
                senderName = senderName,
                senderPhotoUrl = senderPhotoUrl,
                messageContent = messageContent,
                messageType = messageType
            )
        }
    }
    
    private suspend fun showCircleMessageNotification(
        channelId: String,
        circleName: String,
        senderName: String,
        senderPhotoUrl: String,
        messageContent: String,
        messageType: String
    ) {
        val notificationId = channelId.hashCode()
        val content = getMessageContent(messageContent, messageType)
        
        // Load sender avatar
        val senderBitmap = loadBitmapFromUrl(senderPhotoUrl)
        
        // Build person for messaging style
        val sender = Person.Builder()
            .setName(senderName)
            .apply { 
                senderBitmap?.let { setIcon(IconCompat.createWithBitmap(it)) }
            }
            .build()

        // Create tap intent - deep link to circle chat
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = "OPEN_CHAT"
        }
        // Use Bundle to pass extras (required for FLAG_MUTABLE to work with extras)
        tapIntent.putExtra(KEY_CHANNEL_ID, channelId)
        tapIntent.putExtra(KEY_CHAT_TYPE, "GROUP")
        
        Log.d(TAG, "Creating GROUP notification intent: channelId=$channelId")
        
        val tapPendingIntent = PendingIntent.getActivity(
            this, notificationId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // Create quick reply action
        val replyAction = createReplyAction(channelId, "GROUP", notificationId)

        // Build notification with MessagingStyle
        val messagingStyle = NotificationCompat.MessagingStyle(
            Person.Builder().setName("Me").build()
        )
            .setConversationTitle("$circleName")
            .setGroupConversation(true)
            .addMessage(content, System.currentTimeMillis(), sender)

        val notification = NotificationCompat.Builder(this, CHANNEL_CIRCLE_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(messagingStyle)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .addAction(replyAction)
            .setGroup("group_$channelId")
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
    
    private suspend fun showDirectMessageNotification(
        channelId: String,
        senderName: String,
        senderPhotoUrl: String,
        messageContent: String,
        messageType: String
    ) {
        val notificationId = channelId.hashCode()
        val content = getMessageContent(messageContent, messageType)
        
        // Load sender avatar
        val senderBitmap = loadBitmapFromUrl(senderPhotoUrl)

        // Build person for messaging style
        val sender = Person.Builder()
            .setName(senderName)
            .apply { 
                senderBitmap?.let { setIcon(IconCompat.createWithBitmap(it)) }
            }
            .build()

        // Create tap intent - deep link to DM
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = "OPEN_CHAT"
        }
        // Use Bundle to pass extras (required for FLAG_MUTABLE to work with extras)
        tapIntent.putExtra(KEY_CHANNEL_ID, channelId)
        tapIntent.putExtra(KEY_CHAT_TYPE, "DIRECT")
        
        Log.d(TAG, "Creating DM notification intent: channelId=$channelId")
        
        val tapPendingIntent = PendingIntent.getActivity(
            this, notificationId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // Create quick reply action
        val replyAction = createReplyAction(channelId, "DIRECT", notificationId)

        // Build notification with MessagingStyle (1-to-1)
        val messagingStyle = NotificationCompat.MessagingStyle(
            Person.Builder().setName("Me").build()
        )
            .addMessage(content, System.currentTimeMillis(), sender)

        val notification = NotificationCompat.Builder(this, CHANNEL_DM_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(messagingStyle)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .addAction(replyAction)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
    
    private fun createReplyAction(
        channelId: String,
        chatType: String,
        notificationId: Int
    ): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(KEY_REPLY_TEXT)
            .setLabel("Reply...")
            .build()

        val replyIntent = Intent(this, NotificationReplyReceiver::class.java).apply {
            putExtra(KEY_CHANNEL_ID, channelId)
            putExtra(KEY_CHAT_TYPE, chatType)
            putExtra(KEY_NOTIFICATION_ID, notificationId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            this, notificationId, replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()
    }
    
    private fun getMessageContent(content: String, type: String): String {
        return when (type) {
            "IMAGE" -> "📷 Photo"
            "VIDEO" -> "🎥 Video"
            "AUDIO" -> "🎵 Voice message"
            "FILE" -> "📎 File"
            "SYSTEM" -> content
            else -> content
        }
    }
    
    private suspend fun loadBitmapFromUrl(url: String): Bitmap? {
        if (url.isEmpty()) return null
        return try {
            val inputStream = URL(url).openStream()
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load avatar: ${e.message}")
            null
        }
    }
    
    private fun createChatNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Circle messages channel
            val circleChannel = NotificationChannel(
                CHANNEL_CIRCLE_MESSAGES,
                "Circle Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Messages from your circles/squads"
                enableVibration(true)
                enableLights(true)
            }

            // DM messages channel
            val dmChannel = NotificationChannel(
                CHANNEL_DM_MESSAGES,
                "Direct Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Private messages from friends"
                enableVibration(true)
                enableLights(true)
            }

            // Friend requests channel
            val friendChannel = NotificationChannel(
                CHANNEL_FRIEND_REQUESTS,
                "Friend Requests",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Friend request notifications"
            }

            notificationManager.createNotificationChannels(listOf(circleChannel, dmChannel, friendChannel))
        }
    }
    
    // ==================== SUMMON NOTIFICATIONS ====================
    
    private fun handleSummonNotification(data: Map<String, String>) {
        val circleId = data["circleId"] ?: return
        val summonId = data["summonId"] ?: return
        val initiatorName = data["initiatorName"] ?: "Someone"
        val initiatorPhotoUrl = data["initiatorPhotoUrl"]
        val initiatorId = data["initiatorId"]
        
        // Check if current user is the initiator
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val isInitiator = currentUserId != null && currentUserId == initiatorId
        
        Log.d(TAG, "🔔 SUMMON notification! circleId=$circleId, summonId=$summonId, from=$initiatorName, isInitiator=$isInitiator")
        
        // Wake up the device and launch activity directly
        wakeUpAndLaunchSummon(circleId, summonId, initiatorName, initiatorPhotoUrl, isInitiator)
        
        // Also show notification as fallback (but not for initiator since they're already in the UI)
        if (!isInitiator) {
            showSummonNotification(circleId, summonId, initiatorName, initiatorPhotoUrl)
        }
    }
    
    private fun wakeUpAndLaunchSummon(circleId: String, summonId: String, initiatorName: String, initiatorPhotoUrl: String?, isInitiator: Boolean) {
        Log.d(TAG, "⚡ Waking up device and launching SummonActivity... isInitiator=$isInitiator")
        
        // Acquire wake lock to turn on screen
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or 
            PowerManager.ACQUIRE_CAUSES_WAKEUP or 
            PowerManager.ON_AFTER_RELEASE,
            "TripGlide:SummonWakeLock"
        )
        wakeLock.acquire(10 * 1000L) // 10 seconds
        
        // Launch SummonActivity directly
        val intent = Intent(this, SummonActivity::class.java).apply {
            putExtra("circleId", circleId)
            putExtra("summonId", summonId)
            putExtra("initiatorName", initiatorName)
            putExtra("initiatorPhotoUrl", initiatorPhotoUrl)
            putExtra("isInitiator", isInitiator)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or 
                Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
        }
        
        try {
            startActivity(intent)
            Log.d(TAG, "✅ SummonActivity launched directly!")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to launch SummonActivity: ${e.message}", e)
        }
    }

    private fun showSummonNotification(circleId: String, summonId: String, initiatorName: String, initiatorPhotoUrl: String?) {
        Log.d(TAG, "🚀 Creating SUMMON notification...")
        
        val channelId = "tripglide_summon_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Get default ringtone
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delete old channel first to reset settings
            notificationManager.deleteNotificationChannel(channelId)
            
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()
            
            val channel = NotificationChannel(
                channelId,
                "Squad Summons",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming Squad Summons - Full Screen Call Alert"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                setSound(ringtoneUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Main intent - opens SummonActivity when notification is tapped
        val summonIntent = Intent(this, SummonActivity::class.java).apply {
            putExtra("circleId", circleId)
            putExtra("summonId", summonId)
            putExtra("initiatorName", initiatorName)
            putExtra("initiatorPhotoUrl", initiatorPhotoUrl)
            putExtra("isInitiator", false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            summonId.hashCode(),
            summonIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Full screen intent (same as content intent)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 
            summonId.hashCode() + 1000, 
            summonIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🚨 SQUAD SUMMON")
            .setContentText("$initiatorName is summoning you! Tap to respond.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$initiatorName is calling for a squad summon! Tap to respond."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent) // Opens SummonActivity when tapped
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setSound(ringtoneUri)
            .setTimeoutAfter(30000)
            .setOngoing(true)

        Log.d(TAG, "📲 Showing notification with ID: ${summonId.hashCode()}")
        notificationManager.notify(summonId.hashCode(), notificationBuilder.build())
    }

    private fun showNotification(title: String, body: String, type: String?) {
        val channelId = "tripglide_notifications"
        val context: Context = this
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TripGlide Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Friend requests and social notifications"
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra("notification_type", type)
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
