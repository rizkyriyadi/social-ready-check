package com.example.tripglide.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tripglide.ui.booking.BookingDetailScreen
import com.example.tripglide.ui.detail.TripDetailScreen
import com.example.tripglide.ui.home.HomeScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object TripDetail : Screen("trip_detail")
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
            HomeScreen(
                onTripClick = { navController.navigate(Screen.TripDetail.route) },
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.TripDetail.route) {
            TripDetailScreen(
                onBackClick = { navController.popBackStack() },
                onTourClick = { navController.navigate(Screen.BookingDetail.route) }
            )
        }
        composable(Screen.BookingDetail.route) {
            BookingDetailScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Profile.route) {
            com.example.tripglide.ui.profile.ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onAddFriendClick = { navController.navigate(Screen.AddFriend.route) },
                onFriendsClick = { navController.navigate(Screen.Friends.route) },
                onEditProfileClick = { navController.navigate(Screen.EditProfile.route) },
                onFriendRequestsClick = { navController.navigate(Screen.FriendRequests.route) }
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
        composable(Screen.Friends.route) {
            com.example.tripglide.ui.social.FriendsScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.example.tripglide.ui.social.SocialViewModelFactory(androidx.compose.ui.platform.LocalContext.current)
                ),
                onFriendClick = { friendUid -> 
                    navController.navigate(Screen.UserProfile.createRoute(friendUid))
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
        composable(Screen.FriendRequests.route) {
            com.example.tripglide.ui.social.FriendRequestsScreen(
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.example.tripglide.ui.social.SocialViewModelFactory(androidx.compose.ui.platform.LocalContext.current)
                ),
                onBackClick = { navController.popBackStack() }
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
