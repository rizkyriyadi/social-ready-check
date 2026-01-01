package com.example.tripglide.data.network

import com.google.gson.annotations.SerializedName

/**
 * OpenDota API Response Models
 */

// Player Profile Response from GET /players/{account_id}
data class DotaPlayerResponse(
    @SerializedName("profile")
    val profile: DotaProfile?,
    
    @SerializedName("rank_tier")
    val rankTier: Int?,
    
    @SerializedName("leaderboard_rank")
    val leaderboardRank: Int?,
    
    @SerializedName("mmr_estimate")
    val mmrEstimate: MmrEstimate?
)

data class DotaProfile(
    @SerializedName("account_id")
    val accountId: Long,
    
    @SerializedName("personaname")
    val personaName: String?,
    
    @SerializedName("name")
    val name: String?,
    
    @SerializedName("plus")
    val plus: Boolean?,
    
    @SerializedName("cheese")
    val cheese: Int?,
    
    @SerializedName("steamid")
    val steamId: String?,
    
    @SerializedName("avatar")
    val avatar: String?,
    
    @SerializedName("avatarmedium")
    val avatarMedium: String?,
    
    @SerializedName("avatarfull")
    val avatarFull: String?,
    
    @SerializedName("profileurl")
    val profileUrl: String?,
    
    @SerializedName("last_login")
    val lastLogin: String?,
    
    @SerializedName("loccountrycode")
    val locCountryCode: String?,
    
    @SerializedName("is_contributor")
    val isContributor: Boolean?,
    
    @SerializedName("is_subscriber")
    val isSubscriber: Boolean?
)

data class MmrEstimate(
    @SerializedName("estimate")
    val estimate: Int?
)

// Win/Loss Response from GET /players/{account_id}/wl
data class DotaWinLossResponse(
    @SerializedName("win")
    val win: Int = 0,
    
    @SerializedName("lose")
    val lose: Int = 0
) {
    val totalGames: Int get() = win + lose
    val winRate: Float get() = if (totalGames > 0) (win.toFloat() / totalGames) * 100 else 0f
}

// Recent Match from GET /players/{account_id}/recentMatches
data class DotaRecentMatch(
    @SerializedName("match_id")
    val matchId: Long,
    
    @SerializedName("player_slot")
    val playerSlot: Int,
    
    @SerializedName("radiant_win")
    val radiantWin: Boolean,
    
    @SerializedName("duration")
    val duration: Int,
    
    @SerializedName("game_mode")
    val gameMode: Int,
    
    @SerializedName("lobby_type")
    val lobbyType: Int,
    
    @SerializedName("hero_id")
    val heroId: Int,
    
    @SerializedName("start_time")
    val startTime: Long,
    
    @SerializedName("kills")
    val kills: Int,
    
    @SerializedName("deaths")
    val deaths: Int,
    
    @SerializedName("assists")
    val assists: Int,
    
    @SerializedName("xp_per_min")
    val xpPerMin: Int?,
    
    @SerializedName("gold_per_min")
    val goldPerMin: Int?,
    
    @SerializedName("hero_damage")
    val heroDamage: Int?,
    
    @SerializedName("tower_damage")
    val towerDamage: Int?,
    
    @SerializedName("hero_healing")
    val heroHealing: Int?,
    
    @SerializedName("last_hits")
    val lastHits: Int?
) {
    // Radiant is slots 0-127, Dire is 128-255
    val isRadiant: Boolean get() = playerSlot < 128
    val isWin: Boolean get() = isRadiant == radiantWin
}

/**
 * Parsed Dota Profile to store in Firestore
 */
data class LinkedDotaAccount(
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
     * 1x = Herald, 2x = Guardian, 3x = Crusader, 4x = Archon
     * 5x = Legend, 6x = Ancient, 7x = Divine, 8x = Immortal
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
                // Immortal doesn't have stars, might have leaderboard rank
                leaderboardRank?.let { "Immortal #$it" } ?: "Immortal"
            } else {
                "${medalNames[medal]} $stars"
            }
        } else "Unknown"
    }
    
    /**
     * Get medal icon resource hint (for mapping to local drawable)
     */
    val medalTier: Int get() = rankTier / 10
    val medalStars: Int get() = rankTier % 10
}
