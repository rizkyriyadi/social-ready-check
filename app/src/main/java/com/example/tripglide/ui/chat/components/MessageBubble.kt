package com.example.tripglide.ui.chat.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tripglide.data.model.DisplayMessage
import com.example.tripglide.data.model.ParticipantProfile
import com.example.tripglide.data.model.UniversalMessageType
import java.text.SimpleDateFormat
import java.util.*

// Premium Theme Colors
private val MyBubbleGradient = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
private val OtherBubbleBg = Color(0xFF1C1C1E)
private val SystemBubbleBg = Color(0xFF2C2C2E)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF8E8E93)
private val AccentBlue = Color(0xFF007AFF)
private val DeletedTextColor = Color(0xFF636366)

/**
 * Smart Message Bubble Component
 * 
 * Features:
 * - Gradient bubble for "me", dark gray for "others"
 * - Smart corner rounding based on message grouping
 * - Avatar display for first message in group
 * - Read receipts with small avatars
 * - Media support (image/video) with tap handler
 * - System message styling
 * - Clickable user profiles
 */
@Composable
fun MessageBubble(
    displayMessage: DisplayMessage,
    senderProfile: com.example.tripglide.data.model.User? = null,
    onImageClick: ((String) -> Unit)? = null,
    onVideoClick: ((String) -> Unit)? = null,
    onProfileClick: ((String) -> Unit)? = null,  // userId -> navigate to profile
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val message = displayMessage.message
    val isMe = displayMessage.isMe
    val isFirstInGroup = displayMessage.isFirstInGroup
    val isLastInGroup = displayMessage.isLastInGroup
    val isNextSameSender = displayMessage.isNextMessageSameSender

    // Handle system messages differently
    if (message.isSystemMessage()) {
        SystemMessageBubble(
            content = message.content,
            timestamp = message.createdAt?.toDate()
        )
        return
    }

    // Handle deleted messages
    if (message.isDeleted()) {
        DeletedMessageBubble(isMe = isMe)
        return
    }

    // Dynamic corner radius based on grouping
    val topStartRadius = if (isMe) 20.dp else if (isFirstInGroup) 20.dp else 8.dp
    val topEndRadius = if (isMe) if (isFirstInGroup) 20.dp else 8.dp else 20.dp
    val bottomStartRadius = if (isMe) 20.dp else if (isNextSameSender) 8.dp else 20.dp
    val bottomEndRadius = if (isMe) if (isNextSameSender) 8.dp else 20.dp else 20.dp

    val bubbleShape = RoundedCornerShape(
        topStart = topStartRadius,
        topEnd = topEndRadius,
        bottomStart = bottomStartRadius,
        bottomEnd = bottomEndRadius
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (isMe) 60.dp else 8.dp,
                end = if (isMe) 8.dp else 60.dp,
                top = if (isFirstInGroup) 8.dp else 2.dp,
                bottom = 2.dp
            )
            .animateContentSize(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Avatar for others (only show on first message in group)
        if (!isMe) {
            if (isFirstInGroup) {
                AsyncImage(
                    model = senderProfile?.photoUrl ?: message.senderPhotoUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { 
                            onProfileClick?.invoke(message.senderId)
                        },
                    contentScale = ContentScale.Crop
                )
            } else {
                Spacer(modifier = Modifier.width(32.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Message Content Column
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            // Sender name with Dota rank (for others, only on first message) - clickable
            if (!isMe && isFirstInGroup) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = 4.dp, bottom = 2.dp)
                        .clickable { onProfileClick?.invoke(message.senderId) }
                ) {
                    Text(
                        text = senderProfile?.displayName ?: message.senderName,
                        color = AccentBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    // Show Dota rank badge if linked
                    senderProfile?.linkedAccounts?.dota2?.let { dota ->
                        Spacer(modifier = Modifier.width(6.dp))
                        DotaRankBadgeSmall(
                            medalName = dota.medalName,
                            medalTier = dota.medalTier
                        )
                    }
                }
            }

            // Bubble
            Box(
                modifier = Modifier
                    .then(
                        if (isMe) {
                            Modifier.background(
                                brush = Brush.horizontalGradient(MyBubbleGradient),
                                shape = bubbleShape
                            )
                        } else {
                            Modifier.background(OtherBubbleBg, bubbleShape)
                        }
                    )
                    .clip(bubbleShape)
                    .then(
                        if (onLongClick != null) {
                            Modifier.clickable(onClick = { /* Short click does nothing */ })
                        } else Modifier
                    )
            ) {
                when (message.type) {
                    UniversalMessageType.IMAGE.name -> {
                        ImageMessageContent(
                            imageUrl = message.mediaUrl ?: "",
                            caption = message.content,
                            onClick = { onImageClick?.invoke(message.mediaUrl ?: "") }
                        )
                    }
                    UniversalMessageType.VIDEO.name -> {
                        VideoMessageContent(
                            thumbnailUrl = message.thumbnailUrl ?: message.mediaUrl ?: "",
                            caption = message.content,
                            onClick = { onVideoClick?.invoke(message.mediaUrl ?: "") }
                        )
                    }
                    else -> {
                        TextMessageContent(
                            content = message.content,
                            isEdited = message.editedAt != null
                        )
                    }
                }
            }

            // Timestamp and read receipts (only on last message in group)
            if (isLastInGroup) {
                Row(
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Timestamp
                    Text(
                        text = formatMessageTime(message.createdAt?.toDate()),
                        color = TextSecondary,
                        fontSize = 10.sp
                    )

                    // Read receipts (for my messages)
                    if (isMe && displayMessage.readByProfiles.isNotEmpty()) {
                        ReadReceiptAvatars(
                            profiles = displayMessage.readByProfiles.take(3)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextMessageContent(
    content: String,
    isEdited: Boolean
) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = content,
            color = TextPrimary,
            fontSize = 15.sp,
            lineHeight = 20.sp
        )
        if (isEdited) {
            Text(
                text = "(edited)",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun ImageMessageContent(
    imageUrl: String,
    caption: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.widthIn(max = 260.dp)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Image message",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.33f) // 4:3 default
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            contentScale = ContentScale.Crop
        )
        if (!caption.isNullOrBlank()) {
            Text(
                text = caption,
                color = TextPrimary,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun VideoMessageContent(
    thumbnailUrl: String,
    caption: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.widthIn(max = 260.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "Video thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Play button overlay
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        if (!caption.isNullOrBlank()) {
            Text(
                text = caption,
                color = TextPrimary,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun SystemMessageBubble(
    content: String,
    timestamp: Date?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = content,
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(SystemBubbleBg, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun DeletedMessageBubble(isMe: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isMe) 60.dp else 48.dp,
                end = if (isMe) 8.dp else 60.dp,
                top = 4.dp,
                bottom = 4.dp
            ),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text = "🚫 Message deleted",
            color = DeletedTextColor,
            fontSize = 13.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            modifier = Modifier
                .background(
                    Color(0xFF1C1C1E).copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ReadReceiptAvatars(
    profiles: List<ParticipantProfile>
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-4).dp)
    ) {
        profiles.forEach { profile ->
            AsyncImage(
                model = profile.photoUrl,
                contentDescription = "${profile.displayName} read",
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F0F0F), CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * Date Divider Component
 */
@Composable
fun DateDivider(
    timestamp: com.google.firebase.Timestamp?,
    modifier: Modifier = Modifier
) {
    val dateText = formatDateDivider(timestamp?.toDate())
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(Color(0xFF3A3A3C))
        )
        Text(
            text = dateText,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(Color(0xFF3A3A3C))
        )
    }
}

// ==================== HELPER FUNCTIONS ====================

private fun formatMessageTime(date: Date?): String {
    if (date == null) return ""
    val format = SimpleDateFormat("h:mm a", Locale.getDefault())
    return format.format(date)
}

private fun formatDateDivider(date: Date?): String {
    if (date == null) return ""
    
    val today = Calendar.getInstance()
    val messageDate = Calendar.getInstance().apply { time = date }
    
    return when {
        isSameDay(today, messageDate) -> "Today"
        isYesterday(today, messageDate) -> "Yesterday"
        isSameWeek(today, messageDate) -> {
            SimpleDateFormat("EEEE", Locale.getDefault()).format(date) // "Monday"
        }
        isSameYear(today, messageDate) -> {
            SimpleDateFormat("MMMM d", Locale.getDefault()).format(date) // "January 15"
        }
        else -> {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date) // "Jan 15, 2025"
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(today: Calendar, other: Calendar): Boolean {
    val yesterday = Calendar.getInstance().apply {
        time = today.time
        add(Calendar.DAY_OF_YEAR, -1)
    }
    return isSameDay(yesterday, other)
}

private fun isSameWeek(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)
}

private fun isSameYear(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
}

/**
 * Small Dota rank badge for display next to usernames
 */
@Composable
fun DotaRankBadgeSmall(
    medalName: String,
    medalTier: Int,
    modifier: Modifier = Modifier
) {
    val badgeColor = getDotaMedalColor(medalTier)
    
    Box(
        modifier = modifier
            .background(
                badgeColor.copy(alpha = 0.2f),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            medalName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            color = badgeColor
        )
    }
}

/**
 * Get medal color based on tier (1-8)
 */
@Composable
private fun getDotaMedalColor(tier: Int): Color {
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
