package com.example.tripglide.data.model

import com.google.firebase.Timestamp

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
    val socialStats: SocialStats = SocialStats(),
    val gamingStats: GamingStats = GamingStats(),
    val preferences: UserPreferences = UserPreferences(),
    val metadata: UserMetadata = UserMetadata()
)

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
