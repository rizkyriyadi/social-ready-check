package com.example.tripglide

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class TripGlideApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase App Check in Debug mode to fix "No AppCheckProvider installed"
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
        
        // Register FCM token
        updateFcmToken()
    }
    
    private fun updateFcmToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("metadata.fcmToken", token)
        }
    }
}
