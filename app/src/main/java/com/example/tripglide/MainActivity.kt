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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
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
