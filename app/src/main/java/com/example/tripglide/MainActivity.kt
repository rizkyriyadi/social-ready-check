package com.example.tripglide

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

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
}

