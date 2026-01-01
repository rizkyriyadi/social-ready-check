package com.example.tripglide.ui.chat.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tripglide.R
import com.example.tripglide.data.network.DotaRecentMatch
import com.example.tripglide.data.network.DotaWinLossResponse

// Dota Theme Colors
private val DotaRed = Color(0xFFFF6046)
private val DotaBackground = Color(0xFF0F0F0F)
private val CardSurface = Color(0xFF1C1C1E)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF8E8E93)

/**
 * Dota 2 Menu Bottom Sheet
 * Shows Dota-related features for the circle chat
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DotaMenuBottomSheet(
    onDismiss: () -> Unit,
    onViewHeroStats: () -> Unit,
    onViewRecentMatches: () -> Unit,
    onViewLeaderboard: () -> Unit,
    onStartPartyFinder: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DotaBackground,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.dota2_logo),
                        contentDescription = "Dota 2 Logo",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Dota 2 Tools",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Menu Items
            DotaMenuItem(
                emoji = "🎯",
                title = "Hero Stats",
                subtitle = "View hero performance & meta",
                onClick = onViewHeroStats
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            DotaMenuItem(
                emoji = "📊",
                title = "Recent Matches",
                subtitle = "Check squad members' recent games",
                onClick = onViewRecentMatches
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            DotaMenuItem(
                emoji = "🏆",
                title = "Leaderboard",
                subtitle = "Compare ranks with your squad",
                onClick = onViewLeaderboard
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            DotaMenuItem(
                emoji = "🎮",
                title = "Party Finder",
                subtitle = "Find players to queue with",
                onClick = onStartPartyFinder
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DotaMenuItem(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                emoji,
                fontSize = 28.sp
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            
            Text(
                "›",
                fontSize = 24.sp,
                color = TextSecondary
            )
        }
    }
}

/**
 * Squad Leaderboard showing Dota ranks
 */
@Composable
fun DotaSquadLeaderboard(
    members: List<DotaSquadMember>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onMemberClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Squad Ranks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            IconButton(onClick = onRefresh, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = DotaRed
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextSecondary
                    )
                }
            }
        }
        
        // Member List
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(members.sortedByDescending { it.rankTier }) { member ->
                DotaMemberRankCard(
                    member = member,
                    onClick = { onMemberClick(member.odotaId) }
                )
            }
        }
    }
}

@Composable
fun DotaMemberRankCard(
    member: DotaSquadMember,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with rank border
            Box {
                AsyncImage(
                    model = member.avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Name and rank
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    member.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (member.personaName.isNotEmpty() && member.personaName != member.displayName) {
                    Text(
                        member.personaName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            
            // Rank Badge
            DotaRankBadge(
                medalName = member.medalName,
                medalTier = member.medalTier
            )
        }
    }
}

/**
 * Dota rank badge component
 */
@Composable
fun DotaRankBadge(
    medalName: String,
    medalTier: Int,
    modifier: Modifier = Modifier
) {
    val badgeColor = getMedalColor(medalTier)
    
    Box(
        modifier = modifier
            .background(
                badgeColor.copy(alpha = 0.15f),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            medalName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = badgeColor
        )
    }
}

/**
 * Data class for squad member with Dota info
 */
data class DotaSquadMember(
    val odotaId: String,
    val odotaAccountId: Long,
    val displayName: String,
    val personaName: String,
    val avatarUrl: String,
    val rankTier: Int,
    val medalName: String,
    val medalTier: Int,
    val mmrEstimate: Int? = null,
    val winRate: Float? = null
)

/**
 * Get medal color based on tier
 */
@Composable
fun getMedalColor(tier: Int): Color {
    return when (tier) {
        1 -> Color(0xFF8B7355) // Herald
        2 -> Color(0xFF8B8B8B) // Guardian
        3 -> Color(0xFF4CAF50) // Crusader
        4 -> Color(0xFF2196F3) // Archon
        5 -> Color(0xFFFFC107) // Legend
        6 -> Color(0xFF9C27B0) // Ancient
        7 -> Color(0xFFE91E63) // Divine
        8 -> Color(0xFFFFD700) // Immortal
        else -> Color.Gray
    }
}

/**
 * Recent matches list component
 */
@Composable
fun DotaRecentMatchesList(
    matches: List<DotaRecentMatch>,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Recent Matches",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            IconButton(onClick = onRefresh, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = DotaRed
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextSecondary
                    )
                }
            }
        }
        
        if (matches.isEmpty() && !isLoading) {
            Text(
                "No recent matches found",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(matches) { match ->
                    RecentMatchCard(match = match)
                }
            }
        }
    }
}

@Composable
fun RecentMatchCard(match: DotaRecentMatch) {
    val isWin = (match.playerSlot < 128) == match.radiantWin
    
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWin) Color(0xFF1B3D1B) else Color(0xFF3D1B1B)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Win/Loss indicator
            Text(
                if (isWin) "W" else "L",
                color = if (isWin) Color(0xFF4CAF50) else Color(0xFFE53935),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Hero ID: ${match.heroId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    "${match.kills}/${match.deaths}/${match.assists}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            
            Text(
                "${match.duration / 60}m",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
    }
}
