package com.example.tripglide

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.tripglide.navigation.TripGlideNavHost
import com.example.tripglide.ui.theme.TripGlideTheme

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

import com.example.tripglide.data.repository.AuthRepository
import com.example.tripglide.navigation.Screen
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    // State to track notification navigation
    private var pendingChatNavigation: ChatNavigation? = null
    
    data class ChatNavigation(
        val channelId: String,
        val chatType: String
    )
    
    // Notification permission launcher for Android 13+
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
        } else {
            Log.w(TAG, "Notification permission denied")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        requestNotificationPermission()

        // FCM Token Management
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    FirebaseFirestore.getInstance().collection("users").document(user.uid)
                        .update("fcmToken", token)
                }
            }
        }
        
        // Handle notification deep link
        handleIntent(intent)
        
        val authRepository = AuthRepository(this)
        val isLoggedIn = authRepository.isUserLoggedIn()
        val startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route
        
        // splashScreen.setKeepOnScreenCondition { false } 
        
        setContent {
            TripGlideTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    TripGlideNavHost(
                        startDestination = startDestination,
                        pendingChatNavigation = pendingChatNavigation,
                        onChatNavigationConsumed = { pendingChatNavigation = null }
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "OPEN_CHAT") {
            val channelId = intent.getStringExtra("channelId")
            val chatType = intent.getStringExtra("chatType")
            
            Log.d(TAG, "Handling OPEN_CHAT intent: channelId=$channelId, chatType=$chatType")
            
            if (channelId != null && chatType != null) {
                pendingChatNavigation = ChatNavigation(channelId, chatType)
            }
        }
    }
    
    private fun requestNotificationPermission() {
        // Only needed for Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    Log.d(TAG, "Notification permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show rationale if needed, then request
                    Log.d(TAG, "Showing notification permission rationale")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Request the permission directly
                    Log.d(TAG, "Requesting notification permission")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

