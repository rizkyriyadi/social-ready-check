package com.example.tripglide.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.tripglide.navigation.Screen
import com.example.tripglide.ui.components.BottomNavBar
import com.example.tripglide.ui.home.DashboardScreen
import com.example.tripglide.ui.squads.SquadsScreen
import com.example.tripglide.ui.profile.ProfileScreen

import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripglide.ui.social.SocialViewModel
import com.example.tripglide.ui.social.SocialViewModelFactory
import com.example.tripglide.ui.social.FriendsScreen

@Composable
fun MainScreen(
    navController: NavHostController
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    
    // ViewModel for Friends Tab
    val socialViewModel: SocialViewModel = viewModel(
        factory = SocialViewModelFactory(context)
    )

    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> DashboardScreen()
                1 -> SquadsScreen(
                    onCircleClick = { circleId -> 
                        navController.navigate(Screen.CircleDetail.createRoute(circleId)) 
                    }
                )
                2 -> FriendsScreen(
                    viewModel = socialViewModel,
                    onFriendClick = { friendUid -> 
                         navController.navigate(Screen.UserProfile.createRoute(friendUid))
                    },
                    onAddFriendClick = { navController.navigate(Screen.AddFriend.route) }
                )
                3 -> ProfileScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onEditProfileClick = { navController.navigate(Screen.EditProfile.route) },
                    onAddFriendClick = { navController.navigate(Screen.AddFriend.route) }
                )
            }
        }
    }
}
