package com.example.tripglide.ui.detail.tabs

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.tripglide.data.model.*
import com.example.tripglide.data.repository.CircleRepository
import com.example.tripglide.data.repository.CircleRepositoryImpl
import com.example.tripglide.ui.chat.components.FullScreenMediaViewer
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Premium Theme Colors
private val DarkMetalBg = Color(0xFF0F0F0F)
private val CardSurface = Color(0xFF1C1C1E)
private val TextSecondary = Color(0xFF8E8E93)
private val AccentBlue = Color(0xFF007AFF)
private val AccentRed = Color(0xFFFF3B30)
private val White = Color.White

/**
 * Board Tab Content - Social Feed & Memories
 * 
 * Features:
 * - Create Post header
 * - Feed View (LazyColumn of posts)
 * - Gallery View (Staggered Grid of media)
 * - Comment Bottom Sheet
 * - Full Screen Image Viewer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardTabContent(
    circleId: String,
    onProfileClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BoardViewModel = viewModel(factory = BoardViewModelFactory(circleId))
) {
    val context = LocalContext.current
    val posts by viewModel.posts.collectAsState()
    val mediaPosts by viewModel.mediaPosts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var isGalleryView by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf<String?>(null) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var showCreatePost by remember { mutableStateOf(false) }
    
    // Full Screen Image Viewer
    selectedImageUrl?.let { url ->
        FullScreenMediaViewer(
            imageUrl = url,
            onDismiss = { selectedImageUrl = null },
            onDownload = { },
            onShare = { }
        )
    }
    
    // Comment Bottom Sheet
    showComments?.let { postId ->
        CommentBottomSheet(
            circleId = circleId,
            postId = postId,
            onDismiss = { showComments = null }
        )
    }
    
    // Create Post Dialog
    if (showCreatePost) {
        CreatePostDialog(
            onDismiss = { showCreatePost = false },
            onPost = { content, mediaUri ->
                viewModel.createPost(content, mediaUri?.toString())
                showCreatePost = false
                Toast.makeText(context, "Post created!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkMetalBg)
    ) {
        // View Toggle
        ViewModeToggle(
            isGalleryView = isGalleryView,
            onToggle = { isGalleryView = it }
        )
        
        if (isGalleryView) {
            // Gallery View - Staggered Grid
            if (mediaPosts.isEmpty()) {
                EmptyStateBox(
                    emoji = "🖼️",
                    title = "No memories yet",
                    subtitle = "Photos and videos will appear here"
                )
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp
                ) {
                    items(mediaPosts) { post ->
                        GalleryItem(
                            post = post,
                            onClick = { selectedImageUrl = post.mediaUrl }
                        )
                    }
                }
            }
        } else {
            // Feed View
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Create Post Header
                item {
                    CreatePostHeader(onClick = { showCreatePost = true })
                }
                
                if (posts.isEmpty() && !isLoading) {
                    item {
                        EmptyStateBox(
                            emoji = "📝",
                            title = "No posts yet",
                            subtitle = "Be the first to share something!"
                        )
                    }
                }
                
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                        onLike = { viewModel.toggleLike(post) },
                        onComment = { showComments = post.id },
                        onImageClick = { selectedImageUrl = post.mediaUrl },
                        onProfileClick = onProfileClick,
                        onDelete = { viewModel.deletePost(post.id) }
                    )
                }
                
                // Bottom spacer
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

/**
 * View Mode Toggle - Feed vs Gallery
 */
@Composable
private fun ViewModeToggle(
    isGalleryView: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        FilterChip(
            selected = !isGalleryView,
            onClick = { onToggle(false) },
            label = { Text("Feed") },
            leadingIcon = { Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                selectedLabelColor = AccentBlue
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilterChip(
            selected = isGalleryView,
            onClick = { onToggle(true) },
            label = { Text("Gallery") },
            leadingIcon = { Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(18.dp)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                selectedLabelColor = AccentBlue
            )
        )
    }
}

/**
 * Create Post Header
 */
@Composable
private fun CreatePostHeader(onClick: () -> Unit) {
    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar
            val currentUser = FirebaseAuth.getInstance().currentUser
            AsyncImage(
                model = currentUser?.photoUrl ?: "https://ui-avatars.com/api/?name=User",
                contentDescription = "Your avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = "Share something with the squad...",
                color = TextSecondary,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Image",
                tint = AccentBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Post Card - Individual post in feed
 */
@Composable
private fun PostCard(
    post: Post,
    currentUserId: String,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onImageClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    onDelete: () -> Unit
) {
    val isLiked = post.isLikedBy(currentUserId)
    val isOwner = post.authorId == currentUserId
    
    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Author Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.authorPhotoUrl.ifEmpty {
                        "https://ui-avatars.com/api/?name=${post.authorName}"
                    },
                    contentDescription = post.authorName,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick(post.authorId) },
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        post.authorName,
                        color = White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        formatTimestamp(post.createdAt),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                
                // Delete option for owner
                if (isOwner) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Content
            if (post.content.isNotBlank()) {
                Text(
                    post.content,
                    color = White,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            
            // Media
            post.mediaUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.33f)
                        .clip(RoundedCornerShape(0.dp))
                        .clickable { onImageClick() },
                    contentScale = ContentScale.Crop
                )
            }
            
            // Footer - Like & Comment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Like Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLike() }
                ) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) AccentRed else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${post.likeCount}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                
                // Comment Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onComment() }
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = "Comment",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${post.commentCount}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Gallery Item - Single media item in grid
 */
@Composable
private fun GalleryItem(
    post: Post,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = post.mediaUrl,
            contentDescription = "Gallery image",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (post.isVideo()) 16f/9f else 1f),
            contentScale = ContentScale.Crop
        )
        
        // Video indicator
        if (post.isVideo()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Empty State Box
 */
@Composable
private fun EmptyStateBox(
    emoji: String,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

/**
 * Create Post Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostDialog(
    onDismiss: () -> Unit,
    onPost: (content: String, mediaUri: Uri?) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        title = { Text("Create Post", color = White) },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("What's on your mind?", color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        cursorColor = AccentBlue,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = TextSecondary
                    ),
                    maxLines = 5
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Image preview or add button
                if (selectedImageUri != null) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = White
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Photo")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPost(content, selectedImageUri) },
                enabled = content.isNotBlank() || selectedImageUri != null,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

/**
 * Comment Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    circleId: String,
    postId: String,
    onDismiss: () -> Unit,
    viewModel: CommentViewModel = viewModel(factory = CommentViewModelFactory(circleId, postId))
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val comments by viewModel.comments.collectAsState()
    var commentText by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(TextSecondary, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Text(
                "Comments",
                color = White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Comment List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Text(
                            "No comments yet. Be the first!",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 32.dp)
                        )
                    }
                }
                
                items(comments, key = { it.id }) { comment ->
                    CommentItem(comment = comment)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Input Field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Write a comment...", color = TextSecondary) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        cursorColor = AccentBlue,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = TextSecondary
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            viewModel.addComment(commentText)
                            commentText = ""
                        }
                    },
                    enabled = commentText.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (commentText.isNotBlank()) AccentBlue else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: PostComment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = comment.authorPhotoUrl.ifEmpty {
                "https://ui-avatars.com/api/?name=${comment.authorName}"
            },
            contentDescription = comment.authorName,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(10.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    comment.authorName,
                    color = White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    formatTimestamp(comment.createdAt),
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                comment.content,
                color = White,
                fontSize = 14.sp,
                lineHeight = 18.sp
            )
        }
    }
}

// ==================== HELPER FUNCTIONS ====================

private fun formatTimestamp(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""
    val date = timestamp.toDate()
    val now = Date()
    val diff = now.time - date.time
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}

// ==================== VIEWMODELS ====================

class BoardViewModel(
    private val circleId: String,
    private val repository: CircleRepository
) : ViewModel() {
    
    val posts = repository.getPosts(circleId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val mediaPosts = repository.getMediaPosts(circleId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun createPost(content: String, mediaUrl: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.createPost(circleId, content, mediaUrl, if (mediaUrl != null) "IMAGE" else null)
            _isLoading.value = false
        }
    }
    
    fun toggleLike(post: Post) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val isCurrentlyLiked = post.isLikedBy(currentUserId)
        
        viewModelScope.launch {
            repository.togglePostLike(circleId, post.id, !isCurrentlyLiked)
        }
    }
    
    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(circleId, postId)
        }
    }
}

class BoardViewModelFactory(private val circleId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BoardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BoardViewModel(circleId, CircleRepositoryImpl()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class CommentViewModel(
    private val circleId: String,
    private val postId: String,
    private val repository: CircleRepository
) : ViewModel() {
    
    val comments = repository.getComments(circleId, postId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun addComment(content: String) {
        viewModelScope.launch {
            repository.addComment(circleId, postId, content)
        }
    }
}

class CommentViewModelFactory(
    private val circleId: String,
    private val postId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CommentViewModel(circleId, postId, CircleRepositoryImpl()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
