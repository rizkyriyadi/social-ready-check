package com.example.tripglide.data.repository

import com.example.tripglide.data.model.Friend
import com.example.tripglide.data.model.FriendRequest
import com.example.tripglide.data.model.FriendRequestStatus
import com.example.tripglide.data.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FriendRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    // --- Search ---

    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val usernameQuery = query.trim().removePrefix("@").lowercase()
            val usernameDoc = firestore.collection("usernames").document(usernameQuery).get().await()
             
            if (usernameDoc.exists()) {
                val uid = usernameDoc.getString("uid")
                if (uid != null && uid != getCurrentUserId()) {
                    val userSnapshot = firestore.collection("users").document(uid).get().await()
                    val user = userSnapshot.toObject(User::class.java)
                    if (user != null) return Result.success(listOf(user))
                }
            }
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserByUsername(username: String): Result<User?> {
        return try {
            val cleanUsername = username.removePrefix("@").lowercase()
            val usernameDoc = firestore.collection("usernames").document(cleanUsername).get().await()
            if (usernameDoc.exists()) {
                val uid = usernameDoc.getString("uid")
                if (uid != null) {
                    val userDoc = firestore.collection("users").document(uid).get().await()
                    return Result.success(userDoc.toObject(User::class.java))
                }
            }
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserByUid(uid: String): Result<User?> {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            Result.success(doc.toObject(User::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Friend Requests ---

    suspend fun sendFriendRequest(targetUid: String): Result<Unit> {
        val currentUid = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
        if (currentUid == targetUid) return Result.failure(Exception("Cannot add yourself"))

        return try {
            // Check if already friends
            val friendDoc = firestore.collection("users").document(currentUid)
                .collection("friends").document(targetUid).get().await()
            if (friendDoc.exists()) return Result.failure(Exception("Already friends"))

            // Check if request already sent (A -> B)
            val existingReq = firestore.collection("friend_requests")
                .whereEqualTo("senderId", currentUid)
                .whereEqualTo("receiverId", targetUid)
                .whereEqualTo("status", FriendRequestStatus.PENDING.name)
                .get().await()
            
            if (!existingReq.isEmpty) return Result.failure(Exception("Request already sent"))

            // Check if there's a PENDING request from target to current user (B -> A)
            // If so, auto-accept (mutual friend)
            val reverseReq = firestore.collection("friend_requests")
                .whereEqualTo("senderId", targetUid)
                .whereEqualTo("receiverId", currentUid)
                .whereEqualTo("status", FriendRequestStatus.PENDING.name)
                .get().await()
            
            if (!reverseReq.isEmpty) {
                // Mutual request! Auto-accept the reverse request
                val reverseRequest = reverseReq.documents.first().toObject(FriendRequest::class.java)
                if (reverseRequest != null) {
                    acceptFriendRequest(reverseRequest)
                    return Result.success(Unit)
                }
            }

            // Create new request
            val currentUserDoc = firestore.collection("users").document(currentUid).get().await()
            val currentUser = currentUserDoc.toObject(User::class.java)
                ?: return Result.failure(Exception("User not found"))

            val requestRef = firestore.collection("friend_requests").document()
            val request = hashMapOf(
                "id" to requestRef.id,
                "senderId" to currentUid,
                "senderName" to currentUser.displayName,
                "senderPhotoUrl" to currentUser.photoUrl,
                "receiverId" to targetUid,
                "status" to FriendRequestStatus.PENDING.name,
                "timestamp" to Timestamp.now()
            )
            
            requestRef.set(request).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingRequests(): Result<List<FriendRequest>> {
        val currentUid = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
        return try {
            val snapshot = firestore.collection("friend_requests")
                .whereEqualTo("receiverId", currentUid)
                .whereEqualTo("status", FriendRequestStatus.PENDING.name)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await()
            
            val requests = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                FriendRequest(
                    id = data["id"] as? String ?: doc.id,
                    senderId = data["senderId"] as? String ?: "",
                    senderName = data["senderName"] as? String ?: "",
                    senderPhotoUrl = data["senderPhotoUrl"] as? String ?: "",
                    receiverId = data["receiverId"] as? String ?: "",
                    status = try { 
                        FriendRequestStatus.valueOf(data["status"] as? String ?: "PENDING") 
                    } catch (e: Exception) { 
                        FriendRequestStatus.PENDING 
                    },
                    timestamp = data["timestamp"] as? Timestamp ?: Timestamp.now()
                )
            }
            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit> {
        return try {
            // Just update request status to ACCEPTED
            // The Cloud Function will handle adding friends to both users
            firestore.collection("friend_requests").document(request.id)
                .update("status", FriendRequestStatus.ACCEPTED.name).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection("friend_requests").document(requestId)
                .update("status", FriendRequestStatus.REJECTED.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Friends List ---

    suspend fun getFriends(): Result<List<Friend>> {
        val currentUid = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
        return try {
            val snapshot = firestore.collection("users").document(currentUid)
                .collection("friends")
                .get().await()
            
            val friends = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Friend(
                    uid = data["uid"] as? String ?: doc.id,
                    displayName = data["displayName"] as? String ?: "",
                    photoUrl = data["photoUrl"] as? String ?: "",
                    username = data["username"] as? String ?: "",
                    since = data["since"] as? Timestamp ?: Timestamp.now()
                )
            }
            Result.success(friends)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun removeFriend(friendUid: String): Result<Unit> {
        val currentUid = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
        return try {
            // Remove friend from my list
            firestore.collection("users").document(currentUid)
                .collection("friends").document(friendUid).delete().await()
            
            // Remove me from their list (reciprocal)
            firestore.collection("users").document(friendUid)
                .collection("friends").document(currentUid).delete().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun isFriend(targetUid: String): Result<Boolean> {
        val currentUid = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
        return try {
            val doc = firestore.collection("users").document(currentUid)
                .collection("friends").document(targetUid).get().await()
            Result.success(doc.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
