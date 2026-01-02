package com.example.tripglide.ui.detail.tabs

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tripglide.data.model.*
import com.example.tripglide.ui.detail.SummonState
import androidx.compose.animation.core.*

// Premium Theme Colors
private val DarkMetalBg = Color(0xFF0F0F0F)
private val CardSurface = Color(0xFF1C1C1E)
private val TextSecondary = Color(0xFF8E8E93)
private val ShinyGold = Color(0xFFFFD700)
private val AccentRed = Color(0xFFCC2B2B)
private val White = Color.White

/**
 * HQ (Headquarters) Tab Content
 * 
 * Contains:
 * - Premium Summon Button (Hero element)
 * - Hall of Reputation Widget (Summon Stats)
 * - Active Members with Dota Ranks
 */
@Composable
fun HQTabContent(
    circleId: String,
    circle: Circle?,
    members: List<User>,
    summonStats: SummonStats?,
    summonState: SummonState,
    onSummon: () -> Unit,
    onClearSummon: () -> Unit,
    onMemberClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAlreadyActiveDialog by remember { mutableStateOf(false) }
    var activeSummonId by remember { mutableStateOf<String?>(null) }
    
    // Handle summon state changes
    LaunchedEffect(summonState) {
        when (val state = summonState) {
            is SummonState.Success -> {
                val intent = Intent(context, com.example.tripglide.ui.summon.SummonActivity::class.java).apply {
                    putExtra("circleId", circleId)
                    putExtra("summonId", state.summonId)
                    putExtra("initiatorName", "You")
                    putExtra("isInitiator", true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            is SummonState.AlreadyActive -> {
                activeSummonId = state.existingSummonId
                showAlreadyActiveDialog = true
            }
            is SummonState.Error -> {
                Toast.makeText(context, "❌ ${state.message}", Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }
    
    // Dialog for already active summon
    if (showAlreadyActiveDialog && activeSummonId != null) {
        AlertDialog(
            onDismissRequest = { showAlreadyActiveDialog = false },
            title = { Text("Summon Already Active", color = White) },
            text = { Text("There's an active summon in progress. Would you like to join it or clear it?", color = Color.Gray) },
            containerColor = CardSurface,
            confirmButton = {
                TextButton(onClick = {
                    showAlreadyActiveDialog = false
                    val intent = Intent(context, com.example.tripglide.ui.summon.SummonActivity::class.java).apply {
                        putExtra("circleId", circleId)
                        putExtra("summonId", activeSummonId)
                        putExtra("initiatorName", "Someone")
                        putExtra("isInitiator", false)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Join", color = Color.Green)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAlreadyActiveDialog = false
                    onClearSummon()
                    Toast.makeText(context, "Summon cleared!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Clear & Retry", color = Color.Red)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkMetalBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Premium Summon Button (Hero)
        item {
            PremiumSummonButton(
                onClick = onSummon,
                isLoading = summonState is SummonState.Loading
            )
        }
        
        // Hall of Reputation
        item {
            HallOfReputationWidget(stats = summonStats)
        }
        
        // Squad Members Header
        item {
            Text(
                text = "Squad Members",
                color = White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        // Member Cards
        items(members) { user ->
            MemberCard(
                user = user,
                onClick = { onMemberClick(user.uid) }
            )
        }
        
        // Bottom spacer
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Premium Summon Button with shimmer effect
 */
@Composable
private fun PremiumSummonButton(
    onClick: () -> Unit, 
    isLoading: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(45.dp))
            .background(
                Brush.linearGradient(
                    colors = if (isLoading) listOf(Color(0xFF666666), Color(0xFF888888))
                             else listOf(AccentRed, Color(0xFFE84545))
                )
            )
            .clickable(enabled = !isLoading) { onClick() }
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0f),
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0f)
                        ),
                        start = Offset(translateAnim.value, 0f),
                        end = Offset(translateAnim.value + 100f, 100f)
                    )
                )
            }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (isLoading) "SUMMONING..." else "SUMMON PARTY",
                        color = White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Notify all members",
                        color = White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = White.copy(alpha = 0.6f))
        }
    }
}

/**
 * Hall of Reputation Widget - Shows summon statistics leaders
 */
@Composable
fun HallOfReputationWidget(stats: SummonStats?) {
    Column {
        Text(
            text = "⚔️ Hall of Reputation",
            color = ShinyGold,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (stats == null || (stats.warlord == null && stats.loyal == null && stats.ghost == null)) {
            // Empty state
            Surface(
                color = CardSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No stats yet. Start summoning to unlock!",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                stats.warlord?.let { 
                    item { ReputationCard("👑", "Warlord", it) }
                }
                stats.loyal?.let {
                    item { ReputationCard("🛡️", "Loyal", it) }
                }
                stats.ghost?.let {
                    item { ReputationCard("👻", "Ghost", it) }
                }
            }
        }
    }
}

@Composable
private fun ReputationCard(
    emoji: String, 
    title: String, 
    info: SummonLeaderInfo
) {
    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(140.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                title, 
                color = ShinyGold, 
                fontWeight = FontWeight.Bold, 
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            AsyncImage(
                model = info.photoUrl.ifEmpty { 
                    "https://ui-avatars.com/api/?name=${info.displayName}&background=random" 
                },
                contentDescription = info.displayName,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                info.displayName, 
                color = White, 
                fontSize = 12.sp, 
                maxLines = 1
            )
            Text(
                if (info.isPercentage) "${info.value}%" else "${info.value} summons",
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Member Card with Dota Rank visual
 */
@Composable
fun MemberCard(
    user: User,
    onClick: () -> Unit
) {
    val dotaAccount = user.linkedAccounts.dota2
    val borderColor = dotaAccount?.let { getDotaBorderColor(it.medalTier) } ?: Color.Transparent

    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Dota rank border
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .then(
                        if (borderColor != Color.Transparent) {
                            Modifier.border(2.dp, borderColor, CircleShape)
                        } else Modifier
                    )
                    .padding(2.dp)
            ) {
                AsyncImage(
                    model = user.photoUrl.ifEmpty {
                        "https://ui-avatars.com/api/?name=${user.displayName}&background=random"
                    },
                    contentDescription = user.displayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.displayName,
                    color = White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                dotaAccount?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    DotaRankBadge(
                        medalName = it.medalName,
                        medalTier = it.medalTier
                    )
                }
            }

            // Online indicator
            if (user.online) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF34C759), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun DotaRankBadge(
    medalName: String,
    medalTier: Int
) {
    val badgeColor = getDotaBorderColor(medalTier)
    
    Box(
        modifier = Modifier
            .background(
                badgeColor.copy(alpha = 0.2f),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            medalName,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = badgeColor
        )
    }
}

private fun getDotaBorderColor(tier: Int): Color = when (tier) {
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
