package com.example.tripglide.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.tripglide.R
import com.example.tripglide.data.model.ChatType
import com.example.tripglide.data.repository.ChatRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "NotificationReply"

/**
 * BroadcastReceiver to handle quick reply from notifications
 */
class NotificationReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(KEY_REPLY_TEXT)?.toString()

        if (replyText.isNullOrBlank()) {
            Log.w(TAG, "Empty reply text")
            return
        }

        val channelId = intent.getStringExtra(KEY_CHANNEL_ID) ?: return
        val chatType = intent.getStringExtra(KEY_CHAT_TYPE) ?: "GROUP"
        val notificationId = intent.getIntExtra(KEY_NOTIFICATION_ID, 0)

        Log.d(TAG, "📤 Quick reply: $replyText to $channelId ($chatType)")

        // Show "Sending..." notification
        showSendingNotification(context, notificationId)

        // Send the message
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chatRepo = ChatRepositoryImpl()
                val type = if (chatType == "DIRECT") ChatType.DIRECT else ChatType.GROUP

                val result = chatRepo.sendTextMessage(channelId, type, replyText)
                
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Reply sent successfully")
                    // Update notification to show sent
                    showSentNotification(context, notificationId)
                } else {
                    Log.e(TAG, "❌ Failed to send reply: ${result.exceptionOrNull()?.message}")
                    showErrorNotification(context, notificationId, "Failed to send")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending reply", e)
                showErrorNotification(context, notificationId, e.message ?: "Error")
            }
        }
    }

    private fun showSendingNotification(context: Context, notificationId: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_DM_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sending...")
            .setProgress(0, 0, true)
            .setOngoing(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun showSentNotification(context: Context, notificationId: Int) {
        // Cancel the notification after successful send
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun showErrorNotification(context: Context, notificationId: Int, error: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_DM_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Failed to send")
            .setContentText(error)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
