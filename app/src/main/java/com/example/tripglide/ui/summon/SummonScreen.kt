package com.example.tripglide.ui.summon

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    
    // Media player for summon sound
    val mediaPlayer = remember {
        MediaPlayer.create(context, R.raw.summon_sound).apply {
            isLooping = true
        }
    }
    
    // Track summon status for initiator
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
        if (!hasResponded && !isInitiator) {
            circleRepo.respondToSummon(circleId, summonId, SummonResponseStatus.TIMEOUT.name)
        }
        onFinish()
    }
    
    // Check summon status for both initiator and receiver
    LaunchedEffect(summon) {
        summon?.let { s ->
            when (s.status) {
                SummonStatus.SUCCESS.name -> {
                    delay(1500)
                    onFinish()
                }
                SummonStatus.FAILED.name -> {
                    delay(1500)
                    onFinish()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A1A1A), Color.Black)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Timer & Status
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isInitiator) "SUMMONING PARTY" else "SQUAD SUMMON",
                    color = Color.Red,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Pulsing Timer
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ), label = "scale"
                )
                
                Text(
                    text = "00:${timeLeft.toString().padStart(2, '0')}",
                    color = if (timeLeft <= 10) Color.Red else Color.White,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.scale(if (timeLeft <= 10) scale else 1f)
                )
            }

            // Center: Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = initiatorPhotoUrl ?: "https://ui-avatars.com/api/?name=$initiatorName&background=random",
                    contentDescription = null,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = if (isInitiator) "Waiting for squad..." else initiatorName,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isInitiator) "Members are being notified" else "is summoning you!",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                // Show responses for everyone
                if (summon != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    ResponseStatusList(summon!!)
                }
            }

            // Bottom: Actions
            if (isInitiator) {
                // Initiator: Cancel button
                InitiatorActions(
                    onCancel = {
                        scope.launch {
                            circleRepo.respondToSummon(circleId, summonId, SummonResponseStatus.DECLINED.name)
                            onFinish()
                        }
                    }
                )
            } else if (!hasResponded) {
                // Receiver: Swipe to answer
                SwipeToAnswer(
                    onSwipeLeft = {
                        hasResponded = true
                        scope.launch {
                            Log.d(TAG, "❌ User DECLINED")
                            circleRepo.respondToSummon(circleId, summonId, SummonResponseStatus.DECLINED.name)
                            onFinish()
                        }
                    },
                    onSwipeRight = {
                        hasResponded = true
                        scope.launch {
                            Log.d(TAG, "✅ User ACCEPTED")
                            circleRepo.respondToSummon(circleId, summonId, SummonResponseStatus.ACCEPTED.name)
                            // Stay on screen to see status
                        }
                    }
                )
            } else {
                // Already responded - show status
                Text(
                    text = "✓ Response Sent",
                    color = Color.Green,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        
        // Show SUCCESS/FAILED overlay
        summon?.let { s ->
            if (s.status == SummonStatus.SUCCESS.name) {
                SuccessOverlay()
            } else if (s.status == SummonStatus.FAILED.name) {
                FailedOverlay()
            }
        }
    }
}

@Composable
fun ResponseStatusList(summon: Summon) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Squad Status",
            color = Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        summon.responses.forEach { (userId, status) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when (status) {
                    SummonResponseStatus.ACCEPTED.name -> Icons.Default.Check
                    SummonResponseStatus.DECLINED.name -> Icons.Default.Close
                    else -> null
                }
                val color = when (status) {
                    SummonResponseStatus.ACCEPTED.name -> Color.Green
                    SummonResponseStatus.DECLINED.name -> Color.Red
                    SummonResponseStatus.PENDING.name -> Color.Yellow
                    else -> Color.Gray
                }
                
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = color,
                        strokeWidth = 2.dp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Hide User ID for release
                Text(
                    text = "Member", 
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = status,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InitiatorActions(onCancel: () -> Unit) {
    Button(
        onClick = onCancel,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text("CANCEL SUMMON", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SuccessOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.Green,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SQUAD READY!",
                color = Color.Green,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FailedOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SUMMON FAILED",
                color = Color.Red,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
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

    Box(
        modifier = Modifier
            .width(width)
            .height(80.dp)
            .background(Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(40.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Decline", color = Color.Red.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
            Text("Accept", color = Color.Green.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
        }
        
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .size(70.dp)
                .clip(CircleShape)
                .background(Color.White)
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
            Icon(Icons.Default.Call, contentDescription = null, tint = Color.Black)
        }
    }
}
