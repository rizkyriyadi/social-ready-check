package com.example.tripglide.ui.social

import android.graphics.Bitmap
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.tripglide.data.model.User
import com.example.tripglide.ui.components.IOSButton
import com.example.tripglide.ui.components.IOSCard
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(
    onBackClick: () -> Unit,
    currentUid: String,
    onNavigateToUserProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: SocialViewModel = viewModel(
        factory = SocialViewModelFactory(context)
    )
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val requestStatus by viewModel.requestStatus.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Search", "Scan QR", "My QR") // Added Scan QR tab

    LaunchedEffect(requestStatus) {
        requestStatus?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatus()
        }
    }

    val scannedUser by viewModel.scannedUser.collectAsState()

    if (scannedUser != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearScannedUser() },
            title = { Text("Add Friend?") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                     Image(
                        painter = rememberAsyncImagePainter(scannedUser!!.photoUrl.ifEmpty { "https://i.pravatar.cc/300" }),
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(scannedUser!!.displayName, fontWeight = FontWeight.Bold)
                    Text(scannedUser!!.username, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (scannedUser!!.bio.isNotEmpty()) {
                        Text(scannedUser!!.bio, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.sendFriendRequest(scannedUser!!.uid)
                    viewModel.clearScannedUser()
                }) {
                    Text("Add Friend")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearScannedUser() }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Friend", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF2F2F7))
            )
        },
        containerColor = Color(0xFFF2F2F7)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                       TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF007AFF)
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, color = if (selectedTab == index) Color(0xFF007AFF) else Color.Gray) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            when (selectedTab) {
                0 -> SearchSection(
                    viewModel = viewModel,
                    searchResults = searchResults,
                    isLoading = isLoading,
                    onNavigateToUserProfile = onNavigateToUserProfile
                )
                1 -> ScanQrSection(onQrScanned = { uid ->
                    viewModel.fetchScannedUser(uid)
                })
                2 -> MyQrCodeSection(currentUid)
            }
        }
    }
}

@Composable
fun SearchSection(
    viewModel: SocialViewModel,
    searchResults: List<User>,
    isLoading: Boolean,
    onNavigateToUserProfile: (String) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { 
                query = it
                // Optional: Live search as user types, or just update state. 
                // Currently keeping live search but cleaning it in VM.
                viewModel.searchUsers(it) 
            },
            placeholder = { Text("Search username") }, // Simplified placeholder
            prefix = { Text("@", fontWeight = FontWeight.Bold, color = Color.Gray) }, // Added Prefix
            trailingIcon = { Icon(Icons.Default.Search, null) }, // Moved to trailing
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true, // Prevent new line
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Search
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { 
                   viewModel.searchUsers(query)
                }
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(searchResults) { user ->
                    UserResultItem(
                        user = user, 
                        onAddClick = { viewModel.sendFriendRequest(user.uid) },
                        onProfileClick = { onNavigateToUserProfile(user.uid) }
                    )
                }
            }
            
            if (query.isNotEmpty() && searchResults.isEmpty() && !isLoading) {
                 Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    Text("No users found", color = Color.Gray)
                }
            }
        }
    }
}

private var sentRequestUids = mutableSetOf<String>()

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun ScanQrSection(onQrScanned: (String) -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(android.Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        IOSCard {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = androidx.camera.view.PreviewView(ctx)
                        val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)
                        
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = androidx.camera.core.Preview.Builder().build()
                            preview.setSurfaceProvider(previewView.surfaceProvider)

                            val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                                .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(androidx.core.content.ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                processImageProxy(imageProxy, onQrScanned)
                            }

                            val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    (ctx as androidx.lifecycle.LifecycleOwner),
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (exc: Exception) {
                                exc.printStackTrace()
                            }
                        }, androidx.core.content.ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Overlay text
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Point camera at a Friend QR Code", color = Color.White)
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission is required to scan QR codes.")
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
fun processImageProxy(
    imageProxy: androidx.camera.core.ImageProxy,
    onQrScanned: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = com.google.mlkit.vision.common.InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        val scanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (rawValue != null && rawValue.startsWith("tripglide:add_friend:")) {
                        val uid = rawValue.removePrefix("tripglide:add_friend:")
                        onQrScanned(uid)
                    }
                }
            }
            .addOnFailureListener {
                // Task failed with an exception
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

@Composable
fun UserResultItem(
    user: User, 
    onAddClick: () -> Unit,
    onProfileClick: () -> Unit = {}
) {
    var isSent by remember { mutableStateOf(false) }
    
    IOSCard(
        modifier = Modifier.clickable { onProfileClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = rememberAsyncImagePainter(user.photoUrl.ifEmpty { "https://i.pravatar.cc/300" }),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(user.displayName, fontWeight = FontWeight.SemiBold)
                Text(user.username.ifEmpty { "@${user.uid.take(5)}" }, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            
            Button(
                onClick = {
                    if (!isSent) {
                        onAddClick()
                        isSent = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSent) Color(0xFF34C759) else Color(0xFF007AFF)
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(if (isSent) "✓ Sent" else "Add", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MyQrCodeSection(uid: String) {
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            val writer = MultiFormatWriter()
            try {
                // Encode "tripglide:add_friend:$uid"
                val matrix: BitMatrix = writer.encode("tripglide:add_friend:$uid", BarcodeFormat.QR_CODE, 512, 512)
                val width = matrix.width
                val height = matrix.height
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    }
                }
                qrBitmap = bmp
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IOSCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Your QR Code",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Share this code to add friends instantly",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "My QR Code",
                        modifier = Modifier
                            .size(250.dp)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                } else {
                    CircularProgressIndicator()
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = uid,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}
