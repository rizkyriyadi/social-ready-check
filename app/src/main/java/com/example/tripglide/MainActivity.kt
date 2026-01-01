package com.example.tripglide

import android.os.Bundle
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
        
        val authRepository = AuthRepository(this)
        val isLoggedIn = authRepository.isUserLoggedIn()
        val startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route
        
        // splashScreen.setKeepOnScreenCondition { false } 
        
        setContent {
            TripGlideTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    TripGlideNavHost(startDestination = startDestination)
                }
            }
        }
    }
}
