package com.example.tripglide.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import com.example.tripglide.ui.booking.BookingDetailScreen
import com.example.tripglide.MainActivity

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CircleDetail : Screen("circle_detail/{circleId}") {
        fun createRoute(circleId: String) = "circle_detail/$circleId"
    }
    object Chat : Screen("chat/{circleId}") {
        fun createRoute(circleId: String) = "chat/$circleId"
    }
    object DirectMessage : Screen("dm/{channelId}") {
        fun createRoute(channelId: String) = "dm/$channelId"
    }
    object BookingDetail : Screen("booking_detail")
    object Login : Screen("login")
    object Profile : Screen("profile")
    object AddFriend : Screen("add_friend")
    object Friends : Screen("friends")
    object EditProfile : Screen("edit_profile")
    object Onboarding : Screen("onboarding")
    object FriendRequests : Screen("friend_requests")
    object UserProfile : Screen("user_profile/{uid}") {
        fun createRoute(uid: String) = "user_profile/$uid"
    }
    object CreateSquad : Screen("create_squad")
    object CircleSettings : Screen("circle_settings/{circleId}") {
        fun createRoute(circleId: String) = "circle_settings/$circleId"
    }
    object MembersList : Screen("members_list/{circleId}") {
        fun createRoute(circleId: String) = "members_list/$circleId"
    }
}

@Composable
fun TripGlideNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route,
    pendingChatNavigation: MainActivity.ChatNavigation? = null,
    onChatNavigationConsumed: () -> Unit = {}
) {
    // Handle pending chat navigation from notification
    LaunchedEffect(pendingChatNavigation) {
        if (pendingChatNavigation != null) {
            when (pendingChatNavigation.chatType) {
                "GROUP" -> {
                    navController.navigate(Screen.Chat.createRoute(pendingChatNavigation.channelId))
                }
                "DIRECT" -> {
                    navController.navigate(Screen.DirectMessage.createRoute(pendingChatNavigation.channelId))
                }
            }
            onChatNavigationConsumed()
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            com.example.tripglide.ui.login.LoginScreen(
                onLoginSuccess = { isOnboardingComplete ->
                    val destination = if (isOnboardingComplete) Screen.Home.route else Screen.Onboarding.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            com.example.tripglide.ui.main.MainScreen(navController = navController)
        }
        composable(Screen.CreateSquad.route) {
            com.example.tripglide.ui.squads.CreateSquadScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.CircleDetail.route,
            arguments = listOf(androidx.navigation.navArgument("circleId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val circleId = backStackEntry.arguments?.getString("circleId") ?: ""
            com.example.tripglide.ui.detail.CircleDetailScreen(
                circleId = circleId,
                onBackClick = { navController.popBackStack() },
                onNavigateToChat = { navController.navigate(Screen.Chat.createRoute(circleId)) },
                onNavigateToSettings = { navController.navigate(Screen.CircleSettings.createRoute(circleId)) }
            )
        }
        composable(
            route = Screen.CircleSettings.route,
            arguments = listOf(androidx.navigation.navArgument("circleId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val circleId = backStackEntry.arguments?.getString("circleId") ?: ""
            com.example.tripglide.ui.detail.CircleSettingsScreen(
                circleId = circleId,
                onBackClick = { navController.popBackStack() },
                onNavigateToMembers = { navController.navigate(Screen.MembersList.createRoute(circleId)) },
                onLeaveCircle = { 
                    navController.popBackStack(Screen.Home.route, inclusive = false) 
                }
            )
        }
        composable(
            route = Screen.MembersList.route,
            arguments = listOf(androidx.navigation.navArgument("circleId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val circleId = backStackEntry.arguments?.getString("circleId") ?: ""
            com.example.tripglide.ui.detail.MemberListScreen(
                circleId = circleId,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(androidx.navigation.navArgument("circleId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val circleId = backStackEntry.arguments?.getString("circleId") ?: ""
            
            // Get circle info for header
            val viewModel: com.example.tripglide.ui.detail.CircleDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.tripglide.ui.detail.CircleDetailViewModelFactory(androidx.compose.ui.platform.LocalContext.current)
            )
            val circle by viewModel.circle.collectAsState()
            
            androidx.compose.runtime.LaunchedEffect(circleId) {
                viewModel.loadCircle(circleId)
            }
            
            com.example.tripglide.ui.chat.ChatScreen(
                channelId = circleId,
                chatType = com.example.tripglide.data.model.ChatType.GROUP,
                channelName = circle?.name ?: "Chat",
                channelImageUrl = circle?.imageUrl,
                onBackClick = { navController.popBackStack() },
                onInfoClick = { navController.navigate(Screen.CircleSettings.createRoute(circleId)) },
                onProfileClick = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) }
            )
        }
        composable(Screen.BookingDetail.route) {
            BookingDetailScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.AddFriend.route) {
             val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
             com.example.tripglide.ui.social.AddFriendScreen(
                 onBackClick = { navController.popBackStack() },
                 currentUid = uid,
                 onNavigateToUserProfile = { targetUid -> 
                     navController.navigate(Screen.UserProfile.createRoute(targetUid))
                 }
             )
        }

        composable(Screen.EditProfile.route) {
            com.example.tripglide.ui.profile.EditProfileScreen(
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.example.tripglide.ui.profile.ProfileViewModelFactory(androidx.compose.ui.platform.LocalContext.current)
                ),
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Onboarding.route) {
            com.example.tripglide.ui.onboarding.OnboardingScreen(
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.example.tripglide.ui.onboarding.OnboardingViewModelFactory(androidx.compose.ui.platform.LocalContext.current)
                ),
                onOnboardingComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(androidx.navigation.navArgument("uid") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            val viewModel: com.example.tripglide.ui.social.SocialViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.tripglide.ui.social.SocialViewModelFactory(androidx.compose.ui.platform.LocalContext.current)
            )
            val user by viewModel.scannedUser.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val requestStatus by viewModel.requestStatus.collectAsState()
            val friends by viewModel.friends.collectAsState()
            
            // Check if this user is in our friends list
            val isFriend = friends.any { it.uid == uid }
            
            androidx.compose.runtime.LaunchedEffect(uid) {
                viewModel.fetchScannedUser(uid)
                viewModel.loadFriends() // Load friends to check relationship
            }
            
            com.example.tripglide.ui.social.UserProfileScreen(
                user = user,
                isLoading = isLoading,
                requestStatus = requestStatus,
                isFriend = isFriend,
                onBackClick = { navController.popBackStack() },
                onAddFriendClick = { viewModel.sendFriendRequest(uid) },
                onRemoveFriendClick = { 
                    viewModel.removeFriend(uid)
                    navController.popBackStack()
                },
                onClearStatus = { viewModel.clearStatus() },
                onMessageClick = { 
                    // Generate DM channel ID and navigate
                    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val channelId = com.example.tripglide.data.model.DMChannel.generateChannelId(currentUserId, uid)
                    navController.navigate(Screen.DirectMessage.createRoute(channelId))
                }
            )
        }

        // Direct Message Screen
        composable(
            route = Screen.DirectMessage.route,
            arguments = listOf(androidx.navigation.navArgument("channelId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
            val chatRepository = androidx.compose.runtime.remember { com.example.tripglide.data.repository.ChatRepositoryImpl() }
            
            val otherUserNameState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("Chat") }
            val otherUserPhotoState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
            val otherUserIdState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
            
            androidx.compose.runtime.LaunchedEffect(channelId) {
                chatRepository.getDMChannel(channelId).onSuccess { channel ->
                    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    val otherProfile = channel?.getOtherParticipant(currentUserId ?: "")
                    otherUserNameState.value = otherProfile?.displayName ?: "Chat"
                    otherUserPhotoState.value = otherProfile?.photoUrl
                    otherUserIdState.value = otherProfile?.userId
                }
            }
            
            com.example.tripglide.ui.chat.ChatScreen(
                channelId = channelId,
                chatType = com.example.tripglide.data.model.ChatType.DIRECT,
                channelName = otherUserNameState.value,
                channelImageUrl = otherUserPhotoState.value,
                onBackClick = { navController.popBackStack() },
                onProfileClick = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) }
            )
        }
    }
}
