package com.example.tripglide.ui.summon

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tripglide.R
import com.example.tripglide.data.model.Summon
import com.example.tripglide.data.model.SummonResponseStatus
import com.example.tripglide.data.model.SummonStatus
import com.example.tripglide.data.repository.CircleRepositoryImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val TAG = "SummonScreen"

// Theme colors for summon UI
private val SummonGreen = Color(0xFF4CAF50)
private val SummonRed = Color(0xFFE53935)
private val SummonGold = Color(0xFFFFB300)
private val SummonGrey = Color(0xFF757575)
private val SummonDarkBg = Color(0xFF121212)
private val SummonCardBg = Color(0xFF1E1E1E)

@Composable
fun SummonScreen(
    circleId: String,
    summonId: String,
    initiatorName: String,
    initiatorPhotoUrl: String?,
    isInitiator: Boolean = false,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val circleRepo = remember { CircleRepositoryImpl() }
    var timeLeft by remember { mutableStateOf(60) }
    var hasResponded by remember { mutableStateOf(isInitiator) } // Initiator already responded
    var userResponse by remember { mutableStateOf<String?>(if (isInitiator) SummonResponseStatus.ACCEPTED.name else null) }
    
    // Media player for summon sound
    val mediaPlayer = remember {
        MediaPlayer.create(context, R.raw.summon_sound).apply {
            isLooping = true
        }
    }
    
    // Track summon status in realtime
    val summon by circleRepo.getActiveSummon(circleId, summonId).collectAsState(initial = null)
    
    // Start playing sound and cleanup on dispose
    DisposableEffect(Unit) {
        mediaPlayer.start()
        onDispose {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
    }
    
    // Countdown timer
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        // Timer expired - mark as timeout if not responded
        if (!hasResponded && !isInitiator) {
            circleRepo.respondToSummon(circleId, summonId, SummonResponseStatus.TIMEOUT.name)
        }
        // Give a moment for final status to sync
        delay(1500)
        onFinish()
    }
    
    // Check summon status changes
    LaunchedEffect(summon) {
        summon?.let { s ->
            when (s.status) {
                SummonStatus.SUCCESS.name -> {
                    delay(2000)
                    onFinish()
                }
                SummonStatus.FAILED.name -> {
                    delay(2000)
                    onFinish()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SummonDarkBg)
    ) {
        // Background Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E),
                            SummonDarkBg
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ===== TOP: Circle Context Header =====
            CircleContextHeader(
                circleName = summon?.circleName ?: "Squad",
                circleImageUrl = summon?.circleImageUrl,
                memberCount = summon?.memberInfoMap?.size ?: 0,
                timeLeft = timeLeft
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // ===== CENTER: Member Status Grid =====
            if (summon != null) {
                MemberStatusGrid(
                    summon = summon!!,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Loading state
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SummonGold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // ===== BOTTOM: Actions =====
            ActionArea(
                isInitiator = isInitiator,
                hasResponded = hasResponded,
                userResponse = userResponse,
                onAccept = {
                    hasResponded = true
                    userResponse = SummonResponseStatus.ACCEPTED.name
                    scope.launch {
                        Log.d(TAG, "✅ User ACCEPTED")
                        circleRepo.respondToSummon(circleId, summonId, SummonResponseStatus.ACCEPTED.name)
                    }
                },
                onDecline = {
                    hasResponded = true
                    userResponse = SummonResponseStatus.DECLINED.name
                    scope.launch {
                        Log.d(TAG, "❌ User DECLINED")
                        circleRepo.respondToSummon(circleId, summonId, SummonResponseStatus.DECLINED.name)
                    }
                },
                onCancel = {
                    scope.launch {
                        Log.d(TAG, "🚫 Initiator CANCELLED")
                        circleRepo.respondToSummon(circleId, summonId, SummonResponseStatus.DECLINED.name)
                        onFinish()
                    }
                }
            )
        }
        
        // ===== SUCCESS/FAILED Overlay =====
        summon?.let { s ->
            when (s.status) {
                SummonStatus.SUCCESS.name -> SuccessOverlay()
                SummonStatus.FAILED.name -> FailedOverlay()
            }
        }
    }
}

@Composable
fun CircleContextHeader(
    circleName: String,
    circleImageUrl: String?,
    memberCount: Int,
    timeLeft: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Status Label
        Text(
            text = "🚨 SQUAD SUMMON",
            color = SummonRed,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Circle Avatar
        AsyncImage(
            model = circleImageUrl ?: "https://ui-avatars.com/api/?name=${circleName}&background=random&size=160",
            contentDescription = "Circle Photo",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .border(3.dp, SummonGold, CircleShape)
                .shadow(8.dp, CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Circle Name
        Text(
            text = circleName,
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        // Subtitle
        Text(
            text = "Waiting for $memberCount members...",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Timer
        Box(
            modifier = Modifier
                .background(
                    if (timeLeft <= 10) SummonRed.copy(alpha = 0.2f) else SummonCardBg,
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "00:${timeLeft.toString().padStart(2, '0')}",
                color = if (timeLeft <= 10) SummonRed else Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.scale(if (timeLeft <= 10) scale else 1f)
            )
        }
    }
}

@Composable
fun MemberStatusGrid(
    summon: Summon,
    modifier: Modifier = Modifier
) {
    val memberList = summon.memberInfoMap.entries.toList()
    
    Column(modifier = modifier) {
        Text(
            text = "Squad Status",
            color = Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(memberList) { (userId, memberInfo) ->
                val status = summon.responses[userId] ?: SummonResponseStatus.PENDING.name
                MemberStatusCard(
                    displayName = memberInfo.displayName,
                    photoUrl = memberInfo.photoUrl,
                    status = status
                )
            }
        }
    }
}

@Composable
fun MemberStatusCard(
    displayName: String,
    photoUrl: String,
    status: String
) {
    val (borderColor, statusIcon, statusText) = when (status) {
        SummonResponseStatus.ACCEPTED.name -> Triple(SummonGreen, Icons.Default.Check, "Ready")
        SummonResponseStatus.DECLINED.name -> Triple(SummonRed, Icons.Default.Close, "Declined")
        SummonResponseStatus.TIMEOUT.name -> Triple(SummonGrey, Icons.Default.Warning, "Timeout")
        else -> Triple(SummonGold, null, "Waiting...")
    }
    
    // Animation for accepted state
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ), label = "glowAlpha"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .border(
                width = if (status == SummonResponseStatus.ACCEPTED.name) 3.dp else 2.dp,
                color = if (status == SummonResponseStatus.ACCEPTED.name) 
                    borderColor.copy(alpha = glowAlpha) 
                else 
                    borderColor.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = SummonCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Avatar with status overlay
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = photoUrl.ifEmpty { "https://ui-avatars.com/api/?name=$displayName&background=random" },
                    contentDescription = displayName,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(2.dp, borderColor, CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                // Status badge
                if (statusIcon != null) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(borderColor, CircleShape)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else {
                    // Loading spinner for pending
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = SummonGold,
                        strokeWidth = 2.dp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Name
            Text(
                text = displayName,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Status text
            Text(
                text = statusText,
                color = borderColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ActionArea(
    isInitiator: Boolean,
    hasResponded: Boolean,
    userResponse: String?,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            isInitiator -> {
                // Initiator view: Cancel button
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = SummonRed.copy(alpha = 0.8f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("CANCEL SUMMON", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Waiting for squad members...",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            !hasResponded -> {
                // Receiver who hasn't responded: Swipe to answer
                SwipeToAnswer(
                    onSwipeLeft = onDecline,
                    onSwipeRight = onAccept
                )
            }
            
            else -> {
                // Already responded
                val responseColor = when (userResponse) {
                    SummonResponseStatus.ACCEPTED.name -> SummonGreen
                    SummonResponseStatus.DECLINED.name -> SummonRed
                    else -> Color.Gray
                }
                val responseText = when (userResponse) {
                    SummonResponseStatus.ACCEPTED.name -> "✓ You're Ready!"
                    SummonResponseStatus.DECLINED.name -> "✗ You Declined"
                    else -> "Response Sent"
                }
                
                Text(
                    text = responseText,
                    color = responseColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Waiting for others to respond...",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun SuccessOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Animated checkmark
            val scale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                label = "successScale"
            )
            
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = SummonGreen,
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SQUAD READY!",
                color = SummonGreen,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Everyone accepted the summon",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun FailedOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = SummonRed,
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SUMMON FAILED",
                color = SummonRed,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Not everyone was ready",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun SwipeToAnswer(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val width = 300.dp
    val widthPx = with(LocalDensity.current) { width.toPx() }
    val anchor = widthPx / 3

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Swipe to respond",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box(
            modifier = Modifier
                .width(width)
                .height(80.dp)
                .background(SummonCardBg, RoundedCornerShape(40.dp))
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(40.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("✗ Decline", color = SummonRed.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                Text("Accept ✓", color = SummonGreen.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
            }
            
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .size(70.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color.White, Color(0xFFE0E0E0))
                        )
                    )
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            offsetX += delta
                        },
                        onDragStopped = {
                            if (offsetX > anchor) {
                                onSwipeRight()
                            } else if (offsetX < -anchor) {
                                onSwipeLeft()
                            } else {
                                offsetX = 0f
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call, 
                    contentDescription = "Respond",
                    tint = SummonDarkBg,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
