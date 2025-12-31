package com.example.tripglide.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.tripglide.data.model.GamingStats
import com.example.tripglide.data.model.SocialStats
import com.example.tripglide.data.model.User
import com.example.tripglide.data.model.UserMetadata
import com.example.tripglide.data.model.UserPreferences
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("859046868812-tsr0g2vaa1okbuuraoul0ij7pa14eilq.apps.googleusercontent.com")
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    suspend fun signInWithGoogle(intent: Intent): Result<Boolean> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val account = task.await()
            val idToken = account.idToken ?: return Result.failure(Exception("No ID Token"))
            
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Auth failed"))

            // Check if user exists, if not create complex profile
            val userDocRef = firestore.collection("users").document(firebaseUser.uid)
            val snapshot = userDocRef.get().await()

            if (!snapshot.exists()) {
                // New User - Create rich profile
                val newUser = User(
                    uid = firebaseUser.uid,
                    displayName = firebaseUser.displayName ?: "Traveler",
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    username = "@${firebaseUser.email?.split("@")?.get(0) ?: "user"}", // Default handle
                    createdAt = Timestamp.now(),
                    lastLogin = Timestamp.now(),
                    verified = false,
                    onboardingCompleted = false,
                    contentCreator = false,
                    online = true,
                    socialStats = SocialStats(
                        reputationScore = 100,
                        badges = listOf("Newcomer")
                    ),
                    gamingStats = GamingStats(
                        level = 1,
                        rank = "Novice",
                        nextLevelXp = 1000
                    ),
                    preferences = UserPreferences(),
                    metadata = UserMetadata(
                        deviceModel = android.os.Build.MODEL,
                        osVersion = android.os.Build.VERSION.RELEASE,
                        appVersion = "1.0.0",
                        accountStatus = "active"
                    )
                )
                
                // Create User and Username reservation in batch or sequentially
                // Using transaction to be safe or just sequential (User creation is key)
                firestore.runTransaction { transaction ->
                     transaction.set(userDocRef, newUser)
                     val cleanUsername = newUser.username.removePrefix("@").lowercase()
                     val usernameRef = firestore.collection("usernames").document(cleanUsername)
                     transaction.set(usernameRef, mapOf("uid" to newUser.uid))
                }.await()
            } else {
                // Existing User - Update last login
                userDocRef.update("lastLogin", Timestamp.now()).await()
            }
            
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getGoogleSignInIntent() = googleSignInClient.signInIntent

    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null
    
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun getUserProfile(): Result<User> {
        val uid = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            val user = snapshot.toObject(User::class.java)
            if (user != null) Result.success(user) else Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserName(newName: String): Result<Unit> {
        val uid = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
        return try {
            firestore.collection("users").document(uid).update("displayName", newName).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkUsernameAvailability(username: String): Boolean {
        return try {
            val doc = firestore.collection("usernames").document(username.lowercase()).get().await()
            !doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateProfile(displayName: String, bio: String, newUsername: String): Result<Unit> {
        val uid = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
        return try {
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection("users").document(uid)
                val userSnapshot = transaction.get(userRef)
                val oldUsername = userSnapshot.getString("username") ?: ""
                
                // Only update username if it changed
                if (newUsername.isNotEmpty() && newUsername != oldUsername) {
                    val cleanUsername = newUsername.removePrefix("@").lowercase()
                    val usernameRef = firestore.collection("usernames").document(cleanUsername)
                    
                    if (transaction.get(usernameRef).exists()) {
                        throw Exception("Username already taken")
                    }
                    
                    // Reserve new username
                    transaction.set(usernameRef, mapOf("uid" to uid))
                    
                    // Release old username if it was valid and EXISTS
                    if (oldUsername.isNotEmpty()) {
                        val oldClean = oldUsername.removePrefix("@").lowercase()
                        val oldUsernameRef = firestore.collection("usernames").document(oldClean)
                        if (transaction.get(oldUsernameRef).exists()) {
                             transaction.delete(oldUsernameRef)
                        }
                    }
                    
                    transaction.update(userRef, "username", "@$cleanUsername")
                }
                
                transaction.update(userRef, mapOf(
                    "displayName" to displayName,
                    "bio" to bio
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun uploadProfileImage(imageUri: Uri): Result<String> {
        val uid = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
        return try {
            val ref = storage.reference.child("users/$uid/profile.jpg")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            
            // Update Firestore with new photo URL
            firestore.collection("users").document(uid).update("photoUrl", downloadUrl).await()
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeOnboarding(displayName: String, photoUrl: String, username: String): Result<Unit> {
        val uid = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
        return try {
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection("users").document(uid)
                val userSnapshot = transaction.get(userRef)
                
                // Username handling
                val cleanUsername = username.removePrefix("@").lowercase()
                val usernameRef = firestore.collection("usernames").document(cleanUsername)
                
                if (transaction.get(usernameRef).exists()) {
                    // Check if I already own it (idempotency)
                    val existingUid = transaction.get(usernameRef).getString("uid")
                    if (existingUid != uid) {
                        throw Exception("Username taken")
                    }
                } else {
                     transaction.set(usernameRef, mapOf("uid" to uid))
                }
                
                // Cleanup old username if exists and different (unlikely for new user but safe)
                val oldUsername = userSnapshot.getString("username") ?: ""
                if (oldUsername.isNotEmpty() && oldUsername != "@$cleanUsername") {
                     val oldClean = oldUsername.removePrefix("@").lowercase()
                     transaction.delete(firestore.collection("usernames").document(oldClean))
                }

                transaction.update(userRef, mapOf(
                    "displayName" to displayName,
                    "photoUrl" to photoUrl, // Ideally this is already set by upload, but confirm here
                    "username" to "@$cleanUsername",
                    "onboardingCompleted" to true
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
