package com.example.tripglide.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import com.example.tripglide.ui.booking.BookingDetailScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CircleDetail : Screen("circle_detail/{circleId}") {
        fun createRoute(circleId: String) = "circle_detail/$circleId"
    }
    object Chat : Screen("chat/{circleId}") {
        fun createRoute(circleId: String) = "chat/$circleId"
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
}

@Composable
fun TripGlideNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
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
        composable(
            route = Screen.CircleDetail.route,
            arguments = listOf(androidx.navigation.navArgument("circleId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val circleId = backStackEntry.arguments?.getString("circleId") ?: ""
            com.example.tripglide.ui.detail.CircleDetailScreen(
                circleId = circleId,
                onBackClick = { navController.popBackStack() },
                onNavigateToChat = { navController.navigate(Screen.Chat.createRoute(circleId)) }
            )
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(androidx.navigation.navArgument("circleId") { type = androidx.navigation.NavType.StringType })
        ) {
             // Placeholder Chat Screen
             androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                 androidx.compose.material3.Text("Chat Screen Placeholder")
             }
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
                onClearStatus = { viewModel.clearStatus() }
            )
        }
    }
}
