package com.example.tripglide.data.repository

import com.example.tripglide.data.model.AuditLog
import com.example.tripglide.data.model.ChatMessage
import com.example.tripglide.data.model.Circle
import com.example.tripglide.data.model.CircleMember
import com.example.tripglide.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Circle operations.
 * Follows Clean Architecture principles for testability and separation of concerns.
 */
interface CircleRepository {

    /**
     * Creates a new Circle and adds the current user as LEADER.
     * Uses Firestore transaction for atomicity.
     * 
     * @param name Display name for the circle
     * @param game Game type (e.g., "Dota 2", "Valorant")
     * @param region Server region (e.g., "SEA", "NA")
     * @return Result containing the new circleId on success
     */
    suspend fun createCircle(name: String, game: String, region: String): Result<String>

    /**
     * Gets all circles where the current user is a member.
     * Returns a Flow for realtime updates via Firestore snapshot listener.
     * 
     * @return Flow emitting updated list of circles
     */
    fun getMyCircles(): Flow<List<Circle>>

    /**
     * Gets a single circle by ID.
     * 
     * @param circleId The circle document ID
     * @return Result containing the Circle or null if not found
     */
    suspend fun getCircleById(circleId: String): Result<Circle?>

    /**
     * Gets realtime stream of messages for a circle.
     * Messages are ordered by createdAt descending (newest first).
     * 
     * @param circleId The circle document ID
     * @return Flow emitting updated list of messages
     */
    fun getCircleMessages(circleId: String): Flow<List<ChatMessage>>

    /**
     * Sends a message to a circle.
     * Uses transaction to:
     * 1. Add message to messages subcollection
     * 2. Update lastMessage on parent circle
     * 3. Atomically increment mediaCount if IMAGE/VIDEO
     * 4. Atomically increment totalSummons if SUMMON
     * 
     * @param circleId The circle document ID
     * @param message The message to send (id will be auto-generated)
     * @return Result indicating success or failure
     */
    suspend fun sendMessage(circleId: String, message: ChatMessage): Result<Unit>

    /**
     * Gets all members of a circle.
     * 
     * @param circleId The circle document ID
     * @return Flow emitting updated list of members
     */
    fun getCircleMembers(circleId: String): Flow<List<CircleMember>>

    /**
     * Gets full user profiles for all members of a circle.
     * 
     * @param circleId The circle document ID
     * @return Flow emitting updated list of User objects
     */
    fun getFullMembers(circleId: String): Flow<List<User>>

    /**
     * Updates circle information (name, image).
     * 
     * @param circleId The circle document ID
     * @param newName New name (optional)
     * @param newImageUrl New image URL (optional)
     * @return Result indicating success or failure
     */
    suspend fun updateCircleInfo(circleId: String, newName: String?, newImageUrl: String?): Result<Unit>

    /**
     * Gets recent activity logs for a circle.
     * 
     * @param circleId The circle document ID
     * @return Flow emitting list of recent audit logs
     */
    fun getCircleActivityLogs(circleId: String): Flow<List<AuditLog>>

    /**
     * Joins an existing circle.
     * Adds current user to members subcollection and memberIds array.
     * 
     * @param circleId The circle document ID
     * @return Result indicating success or failure
     */
    suspend fun joinCircle(circleId: String): Result<Unit>

    /**
     * Joins a circle using an invite code.
     * 
     * @param code The 6-character invite code
     * @return Result containing the circleId on success
     */
    suspend fun joinCircleByCode(code: String): Result<String>

    /**
     * Leaves a circle.
     * Removes current user from members subcollection and memberIds array.
     * If user is LEADER and there are other members, transfers leadership.
     * 
     * @param circleId The circle document ID
     * @return Result indicating success or failure
     */
    suspend fun leaveCircle(circleId: String): Result<Unit>

    /**
     * Updates the last read timestamp for unread badge calculation.
     * 
     * @param circleId The circle document ID
     * @return Result indicating success or failure
     */
    suspend fun markAsRead(circleId: String): Result<Unit>

    /**
     * Gets messages filtered by type (for gallery feature).
     * 
     * @param circleId The circle document ID
     * @param types List of message types to include (e.g., IMAGE, VIDEO)
     * @return Flow emitting filtered messages
     */
    fun getMediaMessages(circleId: String, types: List<String>): Flow<List<ChatMessage>>

    /**
     * Initiates a Summon (Ready Check) for the circle.
     * Fails if a summon is already active.
     */
    suspend fun startSummon(circleId: String): Result<String>

    /**
     * Responds to an active summon.
     */
    suspend fun respondToSummon(circleId: String, summonId: String, status: String): Result<Unit>

    /**
     * Observes the current active summon for a circle.
     */
    fun getActiveSummon(circleId: String, summonId: String): Flow<com.example.tripglide.data.model.Summon?>
    
    /**
     * Clears a stale/stuck activeSummonId from a circle.
     * Use this when a summon times out without proper cleanup.
     */
    suspend fun clearActiveSummon(circleId: String): Result<Unit>
}
