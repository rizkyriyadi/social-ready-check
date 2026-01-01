package com.example.tripglide.data.repository

import android.util.Log
import com.example.tripglide.data.model.DotaLinkedAccount
import com.example.tripglide.data.network.DotaWinLossResponse
import com.example.tripglide.data.network.OpenDotaService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await

private const val TAG = "DotaRepository"

/**
 * Repository for OpenDota API integration and account linking
 */
class DotaRepository(
    private val openDotaService: OpenDotaService = OpenDotaService.create(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    
    private fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    /**
     * Verify a Dota account exists and fetch profile
     * @param steamFriendId The Steam Friend ID (account_id in OpenDota)
     */
    fun verifyDotaAccount(steamFriendId: String): Flow<Result<DotaLinkedAccount>> = flow {
        try {
            Log.d(TAG, "Verifying Dota account: $steamFriendId")
            
            val response = openDotaService.getPlayerProfile(steamFriendId)
            
            if (!response.isSuccessful) {
                val errorCode = response.code()
                val errorMsg = when (errorCode) {
                    404 -> "Player not found. Check your Friend ID."
                    429 -> "Too many requests. Please try again later."
                    else -> "Failed to fetch profile (Error $errorCode)"
                }
                Log.e(TAG, "API Error: $errorCode - $errorMsg")
                emit(Result.failure(Exception(errorMsg)))
                return@flow
            }
            
            val playerData = response.body()
            
            if (playerData?.profile == null) {
                Log.w(TAG, "Profile is null - player may have private match data")
                emit(Result.failure(Exception("This profile has no public match data. Please enable 'Expose Public Match Data' in Dota 2 settings.")))
                return@flow
            }
            
            val profile = playerData.profile
            val linkedAccount = DotaLinkedAccount(
                steamId = profile.steamId ?: "",
                accountId = profile.accountId,
                personaName = profile.personaName ?: "Unknown",
                avatarUrl = profile.avatarFull ?: profile.avatarMedium ?: profile.avatar ?: "",
                profileUrl = profile.profileUrl ?: "",
                rankTier = playerData.rankTier ?: 0,
                leaderboardRank = playerData.leaderboardRank,
                mmrEstimate = playerData.mmrEstimate?.estimate,
                linkedAt = System.currentTimeMillis()
            )
            
            Log.d(TAG, "Found player: ${linkedAccount.personaName} - ${linkedAccount.medalName}")
            emit(Result.success(linkedAccount))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying account", e)
            emit(Result.failure(Exception("Network error. Please check your connection.")))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Link verified Dota account to user's Firestore profile
     */
    suspend fun linkDotaAccount(account: DotaLinkedAccount): Result<Unit> {
        val userId = getCurrentUserId() 
            ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            Log.d(TAG, "Linking Dota account ${account.accountId} to user $userId")
            
            val accountData = mapOf(
                "steamId" to account.steamId,
                "accountId" to account.accountId,
                "personaName" to account.personaName,
                "avatarUrl" to account.avatarUrl,
                "profileUrl" to account.profileUrl,
                "rankTier" to account.rankTier,
                "leaderboardRank" to account.leaderboardRank,
                "mmrEstimate" to account.mmrEstimate,
                "linkedAt" to account.linkedAt
            )
            
            firestore.collection("users").document(userId)
                .update("linkedAccounts.dota2", accountData)
                .await()
            
            Log.d(TAG, "Dota account linked successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to link account", e)
            Result.failure(e)
        }
    }
    
    /**
     * Unlink Dota account from user's profile
     */
    suspend fun unlinkDotaAccount(): Result<Unit> {
        val userId = getCurrentUserId() 
            ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            Log.d(TAG, "Unlinking Dota account for user $userId")
            
            firestore.collection("users").document(userId)
                .update("linkedAccounts.dota2", null)
                .await()
            
            Log.d(TAG, "Dota account unlinked successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unlink account", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get win/loss stats for a linked account
     */
    fun getWinLoss(accountId: Long): Flow<Result<DotaWinLossResponse>> = flow {
        try {
            Log.d(TAG, "Fetching W/L for account: $accountId")
            
            val response = openDotaService.getPlayerWinLoss(accountId.toString())
            
            if (!response.isSuccessful) {
                emit(Result.failure(Exception("Failed to fetch stats")))
                return@flow
            }
            
            val winLoss = response.body() ?: DotaWinLossResponse()
            Log.d(TAG, "W/L: ${winLoss.win}/${winLoss.lose} (${String.format("%.1f", winLoss.winRate)}%)")
            emit(Result.success(winLoss))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching W/L", e)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Refresh linked account data (rank, name, avatar)
     */
    suspend fun refreshLinkedAccount(): Result<DotaLinkedAccount> {
        val userId = getCurrentUserId() 
            ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            // Get current linked account
            val userDoc = firestore.collection("users").document(userId).get().await()
            val linkedAccounts = userDoc.get("linkedAccounts") as? Map<*, *>
            val dota2 = linkedAccounts?.get("dota2") as? Map<*, *>
            
            if (dota2 == null) {
                return Result.failure(Exception("No Dota account linked"))
            }
            
            val accountId = (dota2["accountId"] as? Long) ?: (dota2["accountId"] as? Number)?.toLong()
                ?: return Result.failure(Exception("Invalid account data"))
            
            // Fetch fresh data from OpenDota
            val response = openDotaService.getPlayerProfile(accountId.toString())
            
            if (!response.isSuccessful || response.body()?.profile == null) {
                return Result.failure(Exception("Failed to refresh profile"))
            }
            
            val playerData = response.body()!!
            val profile = playerData.profile!!
            
            val refreshedAccount = DotaLinkedAccount(
                steamId = profile.steamId ?: "",
                accountId = profile.accountId,
                personaName = profile.personaName ?: "Unknown",
                avatarUrl = profile.avatarFull ?: profile.avatarMedium ?: profile.avatar ?: "",
                profileUrl = profile.profileUrl ?: "",
                rankTier = playerData.rankTier ?: 0,
                leaderboardRank = playerData.leaderboardRank,
                mmrEstimate = playerData.mmrEstimate?.estimate,
                linkedAt = dota2["linkedAt"] as? Long ?: System.currentTimeMillis()
            )
            
            // Update Firestore
            linkDotaAccount(refreshedAccount)
            
            Result.success(refreshedAccount)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing account", e)
            Result.failure(e)
        }
    }
}
