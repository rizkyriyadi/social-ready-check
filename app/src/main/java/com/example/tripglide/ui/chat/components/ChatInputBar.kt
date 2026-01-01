package com.example.tripglide.ui.chat.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tripglide.data.model.MediaUploadState
import com.example.tripglide.data.model.UniversalMessageType

// Theme Colors
private val InputBgColor = Color(0xFF1C1C1E)
private val InputBorderColor = Color(0xFF3A3A3C)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF8E8E93)
private val AccentBlue = Color(0xFF007AFF)
private val AccentPurple = Color(0xFF6366F1)
private val DarkBackground = Color(0xFF0F0F0F)

/**
 * Chat Input Bar Component
 * 
 * Features:
 * - Text input with auto-grow
 * - Media picker (image/video)
 * - Upload progress indicator
 * - Local preview while uploading
 * - Typing indicator integration
 */
@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onMediaSelected: (Uri, UniversalMessageType) -> Unit,
    uploadState: MediaUploadState,
    onTypingChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showMediaOptions by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            // Determine media type based on MIME type
            onMediaSelected(it, UniversalMessageType.IMAGE)
        }
        showMediaOptions = false
    }

    // Video picker launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            onMediaSelected(it, UniversalMessageType.VIDEO)
        }
        showMediaOptions = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBackground)
    ) {
        // Upload Progress Bar (when uploading)
        AnimatedVisibility(
            visible = uploadState is MediaUploadState.Uploading,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            UploadProgressBar(uploadState as? MediaUploadState.Uploading)
        }

        // Media Options Row (expandable)
        AnimatedVisibility(
            visible = showMediaOptions,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            MediaOptionsRow(
                onPhotoClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onVideoClick = {
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                onCameraClick = {
                    // TODO: Implement camera capture
                    showMediaOptions = false
                },
                onDismiss = { showMediaOptions = false }
            )
        }

        // Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Add/Media Button
            IconButton(
                onClick = { showMediaOptions = !showMediaOptions },
                modifier = Modifier
                    .size(40.dp)
                    .background(InputBgColor, CircleShape)
            ) {
                Icon(
                    imageVector = if (showMediaOptions) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (showMediaOptions) "Close" else "Add media",
                    tint = AccentBlue
                )
            }

            // Text Input Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp, max = 120.dp)
                    .background(InputBgColor, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = { newValue ->
                        onValueChange(newValue)
                        onTypingChanged(newValue.isNotEmpty())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { 
                            if (!it.isFocused) {
                                onTypingChanged(false)
                            }
                        },
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(AccentBlue),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (value.isNotBlank()) {
                                onSendClick()
                                keyboardController?.hide()
                            }
                        }
                    ),
                    enabled = enabled,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (value.isEmpty()) {
                                Text(
                                    text = "Message...",
                                    color = TextSecondary,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            // Send Button
            AnimatedVisibility(
                visible = value.isNotBlank(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                IconButton(
                    onClick = {
                        if (value.isNotBlank()) {
                            onSendClick()
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(AccentBlue, AccentPurple)
                            ),
                            shape = CircleShape
                        ),
                    enabled = enabled && value.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Safe area padding for gesture navigation
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun MediaOptionsRow(
    onPhotoClick: () -> Unit,
    onVideoClick: () -> Unit,
    onCameraClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MediaOptionButton(
            icon = Icons.Filled.Add,
            label = "Photo",
            onClick = onPhotoClick
        )
        MediaOptionButton(
            icon = Icons.Filled.Star,
            label = "Video",
            onClick = onVideoClick
        )
        MediaOptionButton(
            icon = Icons.Filled.Add,
            label = "Camera",
            onClick = onCameraClick
        )
    }
}

@Composable
private fun MediaOptionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AccentBlue,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun UploadProgressBar(
    uploadState: MediaUploadState.Uploading?
) {
    if (uploadState == null) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Local preview thumbnail
        AsyncImage(
            model = uploadState.localUri,
            contentDescription = "Upload preview",
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Uploading ${uploadState.fileName}...",
                color = TextPrimary,
                fontSize = 12.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { uploadState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = AccentBlue,
                trackColor = InputBgColor,
            )
        }

        Text(
            text = "${(uploadState.progress * 100).toInt()}%",
            color = AccentBlue,
            fontSize = 12.sp
        )
    }
}

/**
 * Typing Indicator Component
 */
@Composable
fun TypingIndicator(
    typingUsers: List<com.example.tripglide.data.model.ParticipantProfile>,
    modifier: Modifier = Modifier
) {
    if (typingUsers.isEmpty()) return

    val text = when {
        typingUsers.size == 1 -> "${typingUsers[0].displayName} is typing..."
        typingUsers.size == 2 -> "${typingUsers[0].displayName} and ${typingUsers[1].displayName} are typing..."
        else -> "Several people are typing..."
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Animated dots
        TypingDots()
        
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun TypingDots() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "typing_dot_$index")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 150)
                ),
                label = "typing_dot_alpha_$index"
            )

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(TextSecondary.copy(alpha = alpha), CircleShape)
            )
        }
    }
}
