package com.example.tripglide.data.repository

import android.net.Uri
import android.util.Log
import com.example.tripglide.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

private const val TAG = "ChatRepository"

/**
 * Implementation of ChatRepository using Firebase Firestore and Storage.
 * 
 * Supports:
 * - circles/{circleId}/messages/{messageId} for GROUP chats
 * - chats/{channelId}/messages/{messageId} for DIRECT messages
 */
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ChatRepository {

    private val circlesCollection = firestore.collection("circles")
    private val chatsCollection = firestore.collection("chats")
    private val usersCollection = firestore.collection("users")

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Get the Firestore collection reference based on chat type
     */
    private fun getMessagesCollection(channelId: String, type: ChatType) = when (type) {
        ChatType.GROUP -> circlesCollection.document(channelId).collection("messages")
        ChatType.DIRECT -> chatsCollection.document(channelId).collection("messages")
    }

    /**
     * Get storage path for media based on chat type
     */
    private fun getMediaStoragePath(channelId: String, type: ChatType, fileName: String): String {
        val typeFolder = when (type) {
            ChatType.GROUP -> "circle_media"
            ChatType.DIRECT -> "dm_media"
        }
        return "$typeFolder/$channelId/$fileName"
    }

    // ==================== MESSAGES ====================

    override fun getMessages(
        channelId: String,
        type: ChatType,
        limit: Int
    ): Flow<List<UniversalChatMessage>> = callbackFlow {
        Log.d(TAG, "📥 Subscribing to messages: channelId=$channelId, type=$type")
        
        val messagesRef = getMessagesCollection(channelId, type)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())

        val listener = messagesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "❌ Error getting messages: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            val messages = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(UniversalChatMessage::class.java)?.apply {
                        // Manually set the document ID (avoid @DocumentId conflict with existing data)
                        if (id.isEmpty()) id = doc.id
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to parse message: ${doc.id}", e)
                    null
                }
            } ?: emptyList()

            Log.d(TAG, "📨 Received ${messages.size} messages")
            trySend(messages)
        }

        awaitClose { 
            Log.d(TAG, "🔌 Unsubscribing from messages: $channelId")
            listener.remove() 
        }
    }

    override suspend fun loadMoreMessages(
        channelId: String,
        type: ChatType,
        beforeTimestamp: Timestamp,
        limit: Int
    ): Result<List<UniversalChatMessage>> {
        return try {
            val snapshot = getMessagesCollection(channelId, type)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .whereLessThan("createdAt", beforeTimestamp)
                .limit(limit.toLong())
                .get()
                .await()

            val messages = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(UniversalChatMessage::class.java)?.apply {
                        // Manually set the document ID (avoid @DocumentId conflict with existing data)
                        if (id.isEmpty()) id = doc.id
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to parse message in loadMore: ${doc.id}", e)
                    null
                }
            }
            
            Result.success(messages)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading more messages", e)
            Result.failure(e)
        }
    }

    override suspend fun sendTextMessage(
        channelId: String,
        type: ChatType,
        content: String
    ): Result<UniversalChatMessage> {
        val currentUserId = getCurrentUserId()
            ?: return Result.failure(Exception("Not logged in"))

        return try {
            // Get current user info
            val userDoc = usersCollection.document(currentUserId).get().await()
            val user = userDoc.toObject(User::class.java)
                ?: return Result.failure(Exception("User profile not found"))

            // Ensure DM channel exists if DIRECT type
            if (type == ChatType.DIRECT) {
                ensureDMChannelExists(channelId)
            }

            val messagesRef = getMessagesCollection(channelId, type)
            val messageDoc = messagesRef.document()

            val message = hashMapOf(
                "senderId" to currentUserId,
                "senderName" to user.displayName,
                "senderPhotoUrl" to user.photoUrl,
                "content" to content,
                "type" to UniversalMessageType.TEXT.name,
                "readBy" to mapOf(currentUserId to FieldValue.serverTimestamp()),
                "createdAt" to FieldValue.serverTimestamp()
            )

            // Use transaction to update both message and channel
            firestore.runTransaction { transaction ->
                transaction.set(messageDoc, message)

                // Update last message on parent
                val lastMessageUpdate = mapOf(
                    "lastMessage" to mapOf(
                        "messageId" to messageDoc.id,
                        "senderId" to currentUserId,
                        "senderName" to user.displayName,
                        "content" to content.take(100),
                        "type" to UniversalMessageType.TEXT.name,
                        "timestamp" to FieldValue.serverTimestamp()
                    ),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                when (type) {
                    ChatType.GROUP -> {
                        val circleRef = circlesCollection.document(channelId)
                        transaction.update(circleRef, 
                            "lastMessage.content", content.take(100),
                            "lastMessage.senderName", user.displayName,
                            "lastMessage.senderId", currentUserId,
                            "lastMessage.timestamp", FieldValue.serverTimestamp(),
                            "lastMessage.type", UniversalMessageType.TEXT.name,
                            "updatedAt", FieldValue.serverTimestamp()
                        )
                    }
                    ChatType.DIRECT -> {
                        val channelRef = chatsCollection.document(channelId)
                        transaction.update(channelRef, lastMessageUpdate)
                    }
                }
            }.await()

            // Return constructed message
            val sentMessage = UniversalChatMessage(
                id = messageDoc.id,
                senderId = currentUserId,
                senderName = user.displayName,
                senderPhotoUrl = user.photoUrl,
                content = content,
                type = UniversalMessageType.TEXT.name,
                readBy = mapOf(currentUserId to Timestamp.now()),
                createdAt = Timestamp.now()
            )

            Log.d(TAG, "✅ Text message sent: ${messageDoc.id}")
            Result.success(sentMessage)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending text message", e)
            Result.failure(e)
        }
    }

    override suspend fun sendMediaMessage(
        channelId: String,
        type: ChatType,
        mediaUri: Uri,
        mediaType: UniversalMessageType,
        caption: String?,
        onProgress: ((Float) -> Unit)?
    ): Result<UniversalChatMessage> {
        val currentUserId = getCurrentUserId()
            ?: return Result.failure(Exception("Not logged in"))

        return try {
            // Get current user info
            val userDoc = usersCollection.document(currentUserId).get().await()
            val user = userDoc.toObject(User::class.java)
                ?: return Result.failure(Exception("User profile not found"))

            // Ensure DM channel exists if DIRECT type
            if (type == ChatType.DIRECT) {
                ensureDMChannelExists(channelId)
            }

            // Generate unique filename
            val extension = when (mediaType) {
                UniversalMessageType.IMAGE -> "jpg"
                UniversalMessageType.VIDEO -> "mp4"
                else -> "bin"
            }
            val fileName = "${UUID.randomUUID()}.$extension"
            val storagePath = getMediaStoragePath(channelId, type, fileName)

            // Upload to Firebase Storage
            val storageRef = storage.reference.child(storagePath)
            val uploadTask = storageRef.putFile(mediaUri)

            // Track progress
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = taskSnapshot.bytesTransferred.toFloat() / taskSnapshot.totalByteCount
                onProgress?.invoke(progress)
            }

            // Wait for upload
            uploadTask.await()
            val mediaUrl = storageRef.downloadUrl.await().toString()

            Log.d(TAG, "📤 Media uploaded: $mediaUrl")

            // Create message
            val messagesRef = getMessagesCollection(channelId, type)
            val messageDoc = messagesRef.document()

            val message = hashMapOf(
                "senderId" to currentUserId,
                "senderName" to user.displayName,
                "senderPhotoUrl" to user.photoUrl,
                "content" to (caption ?: ""),
                "type" to mediaType.name,
                "mediaUrl" to mediaUrl,
                "readBy" to mapOf(currentUserId to FieldValue.serverTimestamp()),
                "createdAt" to FieldValue.serverTimestamp()
            )

            // Save to Firestore
            firestore.runTransaction { transaction ->
                transaction.set(messageDoc, message)

                // Update last message preview
                val previewText = when (mediaType) {
                    UniversalMessageType.IMAGE -> "📷 Photo"
                    UniversalMessageType.VIDEO -> "🎬 Video"
                    else -> "📎 Attachment"
                }

                when (type) {
                    ChatType.GROUP -> {
                        val circleRef = circlesCollection.document(channelId)
                        transaction.update(circleRef,
                            "lastMessage.content", previewText,
                            "lastMessage.senderName", user.displayName,
                            "lastMessage.senderId", currentUserId,
                            "lastMessage.timestamp", FieldValue.serverTimestamp(),
                            "lastMessage.type", mediaType.name,
                            "updatedAt", FieldValue.serverTimestamp(),
                            "metadata.mediaCount", FieldValue.increment(1)
                        )
                    }
                    ChatType.DIRECT -> {
                        val channelRef = chatsCollection.document(channelId)
                        transaction.update(channelRef, mapOf(
                            "lastMessage.messageId" to messageDoc.id,
                            "lastMessage.senderId" to currentUserId,
                            "lastMessage.senderName" to user.displayName,
                            "lastMessage.content" to previewText,
                            "lastMessage.type" to mediaType.name,
                            "lastMessage.timestamp" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ))
                    }
                }
            }.await()

            val sentMessage = UniversalChatMessage(
                id = messageDoc.id,
                senderId = currentUserId,
                senderName = user.displayName,
                senderPhotoUrl = user.photoUrl,
                content = caption ?: "",
                type = mediaType.name,
                mediaUrl = mediaUrl,
                readBy = mapOf(currentUserId to Timestamp.now()),
                createdAt = Timestamp.now()
            )

            Log.d(TAG, "✅ Media message sent: ${messageDoc.id}")
            Result.success(sentMessage)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending media message", e)
            Result.failure(e)
        }
    }

    override suspend fun sendSystemMessage(
        channelId: String,
        type: ChatType,
        content: String
    ): Result<Unit> {
        return try {
            val messagesRef = getMessagesCollection(channelId, type)
            
            val message = hashMapOf(
                "senderId" to "SYSTEM",
                "senderName" to "System",
                "senderPhotoUrl" to "",
                "content" to content,
                "type" to UniversalMessageType.SYSTEM.name,
                "createdAt" to FieldValue.serverTimestamp()
            )

            messagesRef.add(message).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending system message", e)
            Result.failure(e)
        }
    }

    // ==================== READ RECEIPTS ====================

    override suspend fun markAsRead(
        channelId: String,
        type: ChatType,
        messageId: String
    ): Result<Unit> {
        val currentUserId = getCurrentUserId()
            ?: return Result.failure(Exception("Not logged in"))

        return try {
            val messageRef = getMessagesCollection(channelId, type).document(messageId)
            
            messageRef.update(
                "readBy.$currentUserId", FieldValue.serverTimestamp()
            ).await()

            Log.d(TAG, "✅ Message marked as read: $messageId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marking message as read", e)
            Result.failure(e)
        }
    }

    override suspend fun markMultipleAsRead(
        channelId: String,
        type: ChatType,
        messageIds: List<String>
    ): Result<Unit> {
        val currentUserId = getCurrentUserId()
            ?: return Result.failure(Exception("Not logged in"))

        if (messageIds.isEmpty()) return Result.success(Unit)

        return try {
            val batch = firestore.batch()
            val messagesCollection = getMessagesCollection(channelId, type)

            messageIds.forEach { messageId ->
                val messageRef = messagesCollection.document(messageId)
                batch.update(messageRef, "readBy.$currentUserId", FieldValue.serverTimestamp())
            }

            batch.commit().await()
            Log.d(TAG, "✅ ${messageIds.size} messages marked as read")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marking multiple messages as read", e)
            Result.failure(e)
        }
    }

    override suspend fun getReadByUsers(
        channelId: String,
        type: ChatType,
        messageId: String
    ): Result<List<User>> {
        return try {
            val messageDoc = getMessagesCollection(channelId, type)
                .document(messageId)
                .get()
                .await()

            val message = messageDoc.toObject(UniversalChatMessage::class.java)
                ?: return Result.failure(Exception("Message not found"))

            val userIds = message.getReadByUserIds()
            if (userIds.isEmpty()) return Result.success(emptyList())

            val users = mutableListOf<User>()
            userIds.chunked(10).forEach { chunk ->
                val snapshot = usersCollection
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                    .get()
                    .await()
                users.addAll(snapshot.toObjects(User::class.java))
            }

            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting read by users", e)
            Result.failure(e)
        }
    }

    // ==================== DM CHANNELS ====================

    override suspend fun getOrCreateDMChannel(otherUserId: String): Result<DMChannel> {
        val currentUserId = getCurrentUserId()
            ?: return Result.failure(Exception("Not logged in"))

        if (currentUserId == otherUserId) {
            return Result.failure(Exception("Cannot create DM with yourself"))
        }

        return try {
            val channelId = DMChannel.generateChannelId(currentUserId, otherUserId)
            val channelRef = chatsCollection.document(channelId)
            val channelDoc = channelRef.get().await()

            if (channelDoc.exists()) {
                val channel = channelDoc.toObject(DMChannel::class.java)
                    ?: return Result.failure(Exception("Failed to parse channel"))
                return Result.success(channel)
            }

            // Create new channel
            val currentUserDoc = usersCollection.document(currentUserId).get().await()
            val otherUserDoc = usersCollection.document(otherUserId).get().await()

            val currentUser = currentUserDoc.toObject(User::class.java)
                ?: return Result.failure(Exception("Current user not found"))
            val otherUser = otherUserDoc.toObject(User::class.java)
                ?: return Result.failure(Exception("Other user not found"))

            val participantProfiles = mapOf(
                currentUserId to mapOf(
                    "userId" to currentUserId,
                    "displayName" to currentUser.displayName,
                    "photoUrl" to currentUser.photoUrl,
                    "online" to currentUser.online
                ),
                otherUserId to mapOf(
                    "userId" to otherUserId,
                    "displayName" to otherUser.displayName,
                    "photoUrl" to otherUser.photoUrl,
                    "online" to otherUser.online
                )
            )

            val channelData = hashMapOf(
                "participants" to listOf(currentUserId, otherUserId),
                "participantProfiles" to participantProfiles,
                "unreadCounts" to mapOf(currentUserId to 0, otherUserId to 0),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            channelRef.set(channelData, SetOptions.merge()).await()

            val newChannel = DMChannel(
                id = channelId,
                participants = listOf(currentUserId, otherUserId),
                participantProfiles = mapOf(
                    currentUserId to ParticipantProfile(
                        userId = currentUserId,
                        displayName = currentUser.displayName,
                        photoUrl = currentUser.photoUrl,
                        online = currentUser.online
                    ),
                    otherUserId to ParticipantProfile(
                        userId = otherUserId,
                        displayName = otherUser.displayName,
                        photoUrl = otherUser.photoUrl,
                        online = otherUser.online
                    )
                ),
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            Log.d(TAG, "✅ DM channel created: $channelId")
            Result.success(newChannel)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting/creating DM channel", e)
            Result.failure(e)
        }
    }

    /**
     * Ensure DM channel document exists before sending message
     */
    private suspend fun ensureDMChannelExists(channelId: String) {
        val currentUserId = getCurrentUserId() ?: return
        
        val channelRef = chatsCollection.document(channelId)
        val channelDoc = channelRef.get().await()

        if (!channelDoc.exists()) {
            // Parse channel ID to get participants
            val parts = channelId.split("_")
            if (parts.size == 2) {
                val otherUserId = if (parts[0] == currentUserId) parts[1] else parts[0]
                getOrCreateDMChannel(otherUserId)
            }
        }
    }

    override fun getDMChannels(): Flow<List<DMChannel>> = callbackFlow {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = chatsCollection
            .whereArrayContains("participants", currentUserId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error getting DM channels", error)
                    close(error)
                    return@addSnapshotListener
                }

                val channels = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DMChannel::class.java)
                } ?: emptyList()

                trySend(channels)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getDMChannel(channelId: String): Result<DMChannel?> {
        return try {
            val doc = chatsCollection.document(channelId).get().await()
            val channel = doc.toObject(DMChannel::class.java)
            Result.success(channel)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting DM channel", e)
            Result.failure(e)
        }
    }

    override suspend fun updateDMChannelSettings(
        channelId: String,
        mute: Boolean?,
        block: Boolean?
    ): Result<Unit> {
        val currentUserId = getCurrentUserId()
            ?: return Result.failure(Exception("Not logged in"))

        return try {
            val channelRef = chatsCollection.document(channelId)
            val updates = mutableMapOf<String, Any>()

            mute?.let {
                if (it) {
                    updates["mutedBy"] = FieldValue.arrayUnion(currentUserId)
                } else {
                    updates["mutedBy"] = FieldValue.arrayRemove(currentUserId)
                }
            }

            block?.let {
                if (it) {
                    updates["blockedBy"] = FieldValue.arrayUnion(currentUserId)
                } else {
                    updates["blockedBy"] = FieldValue.arrayRemove(currentUserId)
                }
            }

            if (updates.isNotEmpty()) {
                channelRef.update(updates).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating DM channel settings", e)
            Result.failure(e)
        }
    }

    // ==================== MEDIA ====================

    override fun getMediaMessages(
        channelId: String,
        type: ChatType,
        mediaTypes: List<UniversalMessageType>
    ): Flow<List<UniversalChatMessage>> = callbackFlow {
        val typeStrings = mediaTypes.map { it.name }
        
        val listener = getMessagesCollection(channelId, type)
            .whereIn("type", typeStrings)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(UniversalChatMessage::class.java)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    // ==================== TYPING INDICATORS ====================

    override suspend fun setTypingStatus(
        channelId: String,
        type: ChatType,
        isTyping: Boolean
    ) {
        val currentUserId = getCurrentUserId() ?: return

        try {
            val typingRef = when (type) {
                ChatType.GROUP -> circlesCollection.document(channelId)
                    .collection("typing").document(currentUserId)
                ChatType.DIRECT -> chatsCollection.document(channelId)
                    .collection("typing").document(currentUserId)
            }

            if (isTyping) {
                typingRef.set(mapOf(
                    "userId" to currentUserId,
                    "timestamp" to FieldValue.serverTimestamp()
                )).await()
            } else {
                typingRef.delete().await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting typing status", e)
        }
    }

    override fun getTypingUsers(
        channelId: String,
        type: ChatType
    ): Flow<List<String>> = callbackFlow {
        val typingRef = when (type) {
            ChatType.GROUP -> circlesCollection.document(channelId).collection("typing")
            ChatType.DIRECT -> chatsCollection.document(channelId).collection("typing")
        }

        val listener = typingRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val typingUserIds = snapshot?.documents
                ?.mapNotNull { it.getString("userId") }
                ?.filter { it != getCurrentUserId() } // Exclude self
                ?: emptyList()

            trySend(typingUserIds)
        }

        awaitClose { listener.remove() }
    }

    // ==================== DELETE/EDIT ====================

    override suspend fun deleteMessage(
        channelId: String,
        type: ChatType,
        messageId: String
    ): Result<Unit> {
        val currentUserId = getCurrentUserId()
            ?: return Result.failure(Exception("Not logged in"))

        return try {
            val messageRef = getMessagesCollection(channelId, type).document(messageId)
            val messageDoc = messageRef.get().await()
            val message = messageDoc.toObject(UniversalChatMessage::class.java)
                ?: return Result.failure(Exception("Message not found"))

            // Only sender can delete their message
            if (message.senderId != currentUserId) {
                return Result.failure(Exception("Cannot delete other user's message"))
            }

            // Soft delete: Update type to DELETED
            messageRef.update(
                "type", UniversalMessageType.DELETED.name,
                "content", "",
                "mediaUrl", null,
                "deletedAt", FieldValue.serverTimestamp()
            ).await()

            Log.d(TAG, "✅ Message deleted: $messageId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting message", e)
            Result.failure(e)
        }
    }

    override suspend fun editMessage(
        channelId: String,
        type: ChatType,
        messageId: String,
        newContent: String
    ): Result<Unit> {
        val currentUserId = getCurrentUserId()
            ?: return Result.failure(Exception("Not logged in"))

        return try {
            val messageRef = getMessagesCollection(channelId, type).document(messageId)
            val messageDoc = messageRef.get().await()
            val message = messageDoc.toObject(UniversalChatMessage::class.java)
                ?: return Result.failure(Exception("Message not found"))

            // Only sender can edit their message
            if (message.senderId != currentUserId) {
                return Result.failure(Exception("Cannot edit other user's message"))
            }

            messageRef.update(
                "content", newContent,
                "editedAt", FieldValue.serverTimestamp()
            ).await()

            Log.d(TAG, "✅ Message edited: $messageId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error editing message", e)
            Result.failure(e)
        }
    }
}
