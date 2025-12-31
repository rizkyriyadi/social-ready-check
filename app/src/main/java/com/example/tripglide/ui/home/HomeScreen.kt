package com.example.tripglide.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.material3.*
import com.example.tripglide.ui.components.BottomNavBar
import com.example.tripglide.ui.components.CategorySingleChip
import com.example.tripglide.ui.components.SearchBar
import com.example.tripglide.ui.components.TopBar
import com.example.tripglide.ui.components.TripCard
import com.example.tripglide.ui.home.HomeViewModel
import com.example.tripglide.ui.home.HomeViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext

@Composable
fun HomeScreen(
    onTripClick: () -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(context)
    )
    val state by viewModel.user.collectAsState()
    val shouldNavigate by viewModel.shouldNavigateToOnboarding.collectAsState()
    
    LaunchedEffect(shouldNavigate) {
        if (shouldNavigate) {
            onNavigateToOnboarding()
            viewModel.onOnboardingNavigationHandled()
        }
    }

    Scaffold(
        bottomBar = { BottomNavBar() },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            TopBar(
                userName = state?.displayName ?: "Traveler",
                avatarUrl = state?.photoUrl ?: "",
                onAvatarClick = onProfileClick
            )
            
            // Search Bar
            SearchBar()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Categories
            Text(
                text = "Select your next trip",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listOf("Asia", "Europe", "South America", "North America")) { category ->
                    CategorySingleChip(
                        text = category,
                        isSelected = category == "South America", // Hardcoded selection
                        onClick = {}
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Featured Trip
            TripCard(
                title = "Rio de Janeiro",
                subtitle = "Brazil",
                rating = 5.0,
                reviews = 143,
                imageUrl = "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?q=80&w=2670&auto=format&fit=crop", // Rio image
                modifier = Modifier.padding(horizontal = 24.dp),
                onClick = onTripClick
            )
            
            Spacer(modifier = Modifier.height(100.dp)) // Clearance for bottom bar
        }
    }
}
