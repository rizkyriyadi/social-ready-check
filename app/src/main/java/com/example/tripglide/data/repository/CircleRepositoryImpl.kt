package com.example.tripglide.data.repository

import com.example.tripglide.data.model.ChatMessage
import com.example.tripglide.data.model.Circle
import com.example.tripglide.data.model.CircleMember
import com.example.tripglide.data.model.MemberRole
import com.example.tripglide.data.model.MessageType
import com.example.tripglide.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implementation of CircleRepository using Firebase Firestore.
 * 
 * Uses:
 * - Coroutines (suspend functions) for one-shot operations
 * - Kotlin Flow (callbackFlow) for realtime updates
 * - Firestore transactions for atomic operations
 * 
 * Constructor is annotated with @Inject for future Hilt integration,
 * but also supports manual instantiation for consistency with existing codebase.
 */
class CircleRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : CircleRepository {

    private val circlesCollection = firestore.collection("circles")

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    // ==================== CREATE ====================

    override suspend fun createCircle(name: String, game: String, region: String): Result<String> {
        val currentUserId = getCurrentUserId() 
            ?: return Result.failure(Exception("Not logged in"))

        return try {
            // Get current user info for member document
            val userDoc = firestore.collection("users").document(currentUserId).get().await()
            val user = userDoc.toObject(User::class.java)
                ?: return Result.failure(Exception("User profile not found"))

            // Generate unique invite code
            val inviteCode = generateInviteCode()

            val circleId = firestore.runTransaction { transaction ->
                // Create circle document reference
                val circleRef = circlesCollection.document()
                val newCircleId = circleRef.id

                // Build circle data
                val circle = hashMapOf(
                    "id" to newCircleId,
                    "name" to name,
                    "game" to game,
                    "region" to region,
                    "ownerId" to currentUserId,
                    "memberIds" to listOf(currentUserId),
                    "code" to inviteCode,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastMessage" to mapOf(
                        "content" to "",
                        "senderName" to "",
                        "senderId" to "",
                        "timestamp" to null,
                        "type" to MessageType.TEXT.name
                    ),
                    "metadata" to mapOf(
                        "totalSummons" to 0,
                        "mediaCount" to 0,
                        "memberCount" to 1
                    ),
                    "settings" to mapOf(
                        "isPublic" to false,
                        "allowMediaUpload" to true,
                        "muteNotifications" to false
                    )
                )

                // Build member data
                val member = hashMapOf(
                    "userId" to currentUserId,
                    "displayName" to user.displayName,
                    "photoUrl" to user.photoUrl,
                    "joinedAt" to FieldValue.serverTimestamp(),
                    "role" to MemberRole.LEADER.name,
                    "stats" to mapOf(
                        "messagesCount" to 0,
                        "mediaUploaded" to 0,
                        "summonsTriggered" to 0
                    ),
                    "lastReadAt" to FieldValue.serverTimestamp()
                )

                // Write both documents atomically
                transaction.set(circleRef, circle)
                transaction.set(
                    circleRef.collection("members").document(currentUserId),
                    member
                )

                // Add system message for circle creation
                val systemMessageRef = circleRef.collection("messages").document()
                val systemMessage = hashMapOf(
                    "id" to systemMessageRef.id,
                    "senderId" to "SYSTEM",
                    "senderName" to "System",
                    "senderPhotoUrl" to "",
                    "content" to "${user.displayName} created the circle",
                    "type" to MessageType.SYSTEM.name,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                transaction.set(systemMessageRef, systemMessage)

                newCircleId
            }.await()

            Result.success(circleId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== READ ====================

    override fun getMyCircles(): Flow<List<Circle>> = callbackFlow {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = circlesCollection
            .whereArrayContains("memberIds", currentUserId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val circles = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Circle::class.java)
                } ?: emptyList()

                trySend(circles)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getCircleById(circleId: String): Result<Circle?> {
        return try {
            val doc = circlesCollection.document(circleId).get().await()
            val circle = doc.toObject(Circle::class.java)
            Result.success(circle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCircleMessages(circleId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = circlesCollection.document(circleId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100) // Limit for performance, implement pagination as needed
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    override fun getCircleMembers(circleId: String): Flow<List<CircleMember>> = callbackFlow {
        val listener = circlesCollection.document(circleId)
            .collection("members")
            .orderBy("joinedAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val members = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CircleMember::class.java)
                } ?: emptyList()

                trySend(members)
            }

        awaitClose { listener.remove() }
    }

    override fun getMediaMessages(circleId: String, types: List<String>): Flow<List<ChatMessage>> = callbackFlow {
        val listener = circlesCollection.document(circleId)
            .collection("messages")
            .whereIn("type", types)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    // ==================== SEND MESSAGE ====================

    override suspend fun sendMessage(circleId: String, message: ChatMessage): Result<Unit> {
        val currentUserId = getCurrentUserId()
            ?: return Result.failure(Exception("Not logged in"))

        return try {
            // Get current user info
            val userDoc = firestore.collection("users").document(currentUserId).get().await()
            val user = userDoc.toObject(User::class.java)
                ?: return Result.failure(Exception("User profile not found"))

            firestore.runTransaction { transaction ->
                val circleRef = circlesCollection.document(circleId)
                val messageRef = circleRef.collection("messages").document()
                val memberRef = circleRef.collection("members").document(currentUserId)

                // Build message data
                val messageData = hashMapOf(
                    "id" to messageRef.id,
                    "senderId" to currentUserId,
                    "senderName" to user.displayName,
                    "senderPhotoUrl" to user.photoUrl,
                    "content" to message.content,
                    "type" to message.type,
                    "mediaUrl" to message.mediaUrl,
                    "thumbnailUrl" to message.thumbnailUrl,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "metadata" to message.metadata
                )

                // Add message document
                transaction.set(messageRef, messageData)

                // Update lastMessage on circle
                val lastMessageUpdate = mapOf(
                    "lastMessage.content" to message.content.take(100), // Truncate for preview
                    "lastMessage.senderName" to user.displayName,
                    "lastMessage.senderId" to currentUserId,
                    "lastMessage.timestamp" to FieldValue.serverTimestamp(),
                    "lastMessage.type" to message.type,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                transaction.update(circleRef, lastMessageUpdate)

                // Atomic increments based on message type
                val messageType = message.type
                
                if (messageType == MessageType.IMAGE.name || messageType == MessageType.VIDEO.name) {
                    transaction.update(circleRef, "metadata.mediaCount", FieldValue.increment(1))
                    transaction.update(memberRef, "stats.mediaUploaded", FieldValue.increment(1))
                }

                if (messageType == MessageType.SUMMON.name) {
                    transaction.update(circleRef, "metadata.totalSummons", FieldValue.increment(1))
                    transaction.update(memberRef, "stats.summonsTriggered", FieldValue.increment(1))
                }

                // Always increment message count for member
                transaction.update(memberRef, "stats.messagesCount", FieldValue.increment(1))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== JOIN/LEAVE ====================

    override suspend fun joinCircle(circleId: String): Result<Unit> {
        val currentUserId = getCurrentUserId()
            ?: return Result.failure(Exception("Not logged in"))

        return try {
            val userDoc = firestore.collection("users").document(currentUserId).get().await()
            val user = userDoc.toObject(User::class.java)
                ?: return Result.failure(Exception("User profile not found"))

            firestore.runTransaction { transaction ->
                val circleRef = circlesCollection.document(circleId)
                val memberRef = circleRef.collection("members").document(currentUserId)

                // Check if circle exists
                val circleDoc = transaction.get(circleRef)
                if (!circleDoc.exists()) {
                    throw Exception("Circle not found")
                }

                // Check if already a member
                val memberDoc = transaction.get(memberRef)
                if (memberDoc.exists()) {
                    throw Exception("Already a member")
                }

                // Add to memberIds array
                transaction.update(circleRef, "memberIds", FieldValue.arrayUnion(currentUserId))
                transaction.update(circleRef, "metadata.memberCount", FieldValue.increment(1))
                transaction.update(circleRef, "updatedAt", FieldValue.serverTimestamp())

                // Create member document
                val member = hashMapOf(
                    "userId" to currentUserId,
                    "displayName" to user.displayName,
                    "photoUrl" to user.photoUrl,
                    "joinedAt" to FieldValue.serverTimestamp(),
                    "role" to MemberRole.MEMBER.name,
                    "stats" to mapOf(
                        "messagesCount" to 0,
                        "mediaUploaded" to 0,
                        "summonsTriggered" to 0
                    ),
                    "lastReadAt" to FieldValue.serverTimestamp()
                )
                transaction.set(memberRef, member)

                // Add system message
                val systemMessageRef = circleRef.collection("messages").document()
                val systemMessage = hashMapOf(
                    "id" to systemMessageRef.id,
                    "senderId" to "SYSTEM",
                    "senderName" to "System",
                    "senderPhotoUrl" to "",
                    "content" to "${user.displayName} joined the circle",
                    "type" to MessageType.SYSTEM.name,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                transaction.set(systemMessageRef, systemMessage)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinCircleByCode(code: String): Result<String> {
        if (getCurrentUserId() == null) {
            return Result.failure(Exception("Not logged in"))
        }

        return try {
            // Find circle by code
            val querySnapshot = circlesCollection
                .whereEqualTo("code", code.uppercase())
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("Invalid invite code"))
            }

            val circleId = querySnapshot.documents.first().id
            
            // Use existing joinCircle logic
            val joinResult = joinCircle(circleId)
            if (joinResult.isFailure) {
                return Result.failure(joinResult.exceptionOrNull() ?: Exception("Join failed"))
            }

            Result.success(circleId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveCircle(circleId: String): Result<Unit> {
        val currentUserId = getCurrentUserId()
            ?: return Result.failure(Exception("Not logged in"))

        return try {
            val userDoc = firestore.collection("users").document(currentUserId).get().await()
            val user = userDoc.toObject(User::class.java)

            firestore.runTransaction { transaction ->
                val circleRef = circlesCollection.document(circleId)
                val memberRef = circleRef.collection("members").document(currentUserId)

                // Get circle to check ownership
                val circleDoc = transaction.get(circleRef)
                val ownerId = circleDoc.getString("ownerId")
                @Suppress("UNCHECKED_CAST")
                val memberIds = circleDoc.get("memberIds") as? List<String> ?: emptyList()

                // If owner is leaving and there are other members, transfer ownership
                if (ownerId == currentUserId && memberIds.size > 1) {
                    val newOwnerId = memberIds.first { it != currentUserId }
                    transaction.update(circleRef, "ownerId", newOwnerId)
                    
                    // Update new owner's role to LEADER
                    val newOwnerMemberRef = circleRef.collection("members").document(newOwnerId)
                    transaction.update(newOwnerMemberRef, "role", MemberRole.LEADER.name)
                }

                // If owner is the only member, delete the circle
                if (ownerId == currentUserId && memberIds.size == 1) {
                    // Note: Subcollections need to be deleted separately (via Cloud Function or client)
                    transaction.delete(circleRef)
                } else {
                    // Remove from memberIds array
                    transaction.update(circleRef, "memberIds", FieldValue.arrayRemove(currentUserId))
                    transaction.update(circleRef, "metadata.memberCount", FieldValue.increment(-1))
                    transaction.update(circleRef, "updatedAt", FieldValue.serverTimestamp())

                    // Add system message
                    val systemMessageRef = circleRef.collection("messages").document()
                    val systemMessage = hashMapOf(
                        "id" to systemMessageRef.id,
                        "senderId" to "SYSTEM",
                        "senderName" to "System",
                        "senderPhotoUrl" to "",
                        "content" to "${user?.displayName ?: "A member"} left the circle",
                        "type" to MessageType.SYSTEM.name,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    transaction.set(systemMessageRef, systemMessage)
                }

                // Delete member document
                transaction.delete(memberRef)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== UTILITIES ====================

    override suspend fun markAsRead(circleId: String): Result<Unit> {
        val currentUserId = getCurrentUserId()
            ?: return Result.failure(Exception("Not logged in"))

        return try {
            circlesCollection.document(circleId)
                .collection("members")
                .document(currentUserId)
                .update("lastReadAt", FieldValue.serverTimestamp())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates a unique 6-character alphanumeric invite code.
     */
    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Exclude confusing chars (0, O, 1, I)
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }
}
