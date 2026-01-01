package com.example.tripglide.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val username: String = "", // Unique handle (e.g. @gamer123)
    val bio: String = "",
    val location: String = "",
    val website: String = "",
    val birthDate: Timestamp? = null,
    val gender: String = "", // Optional
    val createdAt: Timestamp = Timestamp.now(),
    val lastLogin: Timestamp = Timestamp.now(),
    val verified: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val contentCreator: Boolean = false,
    val online: Boolean = false,
    val fcmToken: String? = null,
    val socialStats: SocialStats = SocialStats(),
    val gamingStats: GamingStats = GamingStats(),
    val preferences: UserPreferences = UserPreferences(),
    val metadata: UserMetadata = UserMetadata(),
    // Linked Game Accounts
    val linkedAccounts: LinkedGameAccounts = LinkedGameAccounts()
)

/**
 * Container for all linked game accounts
 */
@IgnoreExtraProperties
data class LinkedGameAccounts(
    val dota2: DotaLinkedAccount? = null
    // Future: valorant, lol, etc.
)

/**
 * Dota 2 Linked Account stored in Firestore
 */
@IgnoreExtraProperties
data class DotaLinkedAccount(
    val steamId: String = "",
    val accountId: Long = 0,
    val personaName: String = "",
    val avatarUrl: String = "",
    val profileUrl: String = "",
    val rankTier: Int = 0,
    val leaderboardRank: Int? = null,
    val mmrEstimate: Int? = null,
    val linkedAt: Long = System.currentTimeMillis()
) {
    /**
     * Get the medal name from rank_tier
     * rank_tier is a 2-digit number: first digit = medal (1-8), second digit = stars (0-5)
     */
    val medalName: String get() {
        if (rankTier == 0) return "Uncalibrated"
        val medal = rankTier / 10
        val stars = rankTier % 10
        val medalNames = listOf(
            "", "Herald", "Guardian", "Crusader", "Archon",
            "Legend", "Ancient", "Divine", "Immortal"
        )
        return if (medal in 1..8) {
            if (medal == 8) {
                leaderboardRank?.let { "Immortal #$it" } ?: "Immortal"
            } else {
                "${medalNames[medal]} $stars"
            }
        } else "Unknown"
    }
    
    val medalTier: Int get() = rankTier / 10
    val medalStars: Int get() = rankTier % 10
}

data class SocialStats(
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val likesReceived: Int = 0,
    val reputationScore: Int = 100,
    val badges: List<String> = emptyList(),
    val blockedUserIds: List<String> = emptyList()
)

data class GamingStats(
    val level: Int = 1,
    val experience: Long = 0,
    val currentXp: Long = 0,
    val nextLevelXp: Long = 1000,
    val rank: String = "Novice", // e.g., Bronze, Silver, Gold
    val clanId: String = "",
    val clanRole: String = "",
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val winRate: Double = 0.0,
    val favoriteGameModes: List<String> = emptyList(),
    val activeTournamentIds: List<String> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val recentMatchHistory: List<MatchSummary> = emptyList()
)

data class MatchSummary(
    val matchId: String = "",
    val gameMode: String = "",
    val result: String = "", // Win/Loss/Draw
    val timestamp: Timestamp = Timestamp.now(),
    val score: Int = 0
)

data class Achievement(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val unlockedAt: Timestamp = Timestamp.now()
)

data class UserPreferences(
    val theme: String = "system", // light, dark, system
    val notificationsEnabled: Boolean = true,
    val privacyLevel: String = "public", // public, friends_only, private
    val language: String = "en",
    val autoPlayVideos: Boolean = true
)

data class UserMetadata(
    val deviceModel: String = "",
    val osVersion: String = "",
    val appVersion: String = "",
    val fcmToken: String = "", // For push notifications
    val accountStatus: String = "active" // active, suspended, banned
)

enum class FriendRequestStatus {
    PENDING, ACCEPTED, REJECTED
}

data class FriendRequest(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String = "",
    val receiverId: String = "",
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val timestamp: Timestamp = Timestamp.now()
)

data class Friend(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val username: String = "",
    val since: Timestamp = Timestamp.now()
)
