package com.example.tripglide.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripglide.data.model.Circle
import com.example.tripglide.data.model.User
import com.example.tripglide.data.repository.AuthRepository
import com.example.tripglide.data.repository.CircleRepository
import com.example.tripglide.data.repository.CircleRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val circles: List<Circle>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val circleRepository: CircleRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _circlesFlow = circleRepository.getMyCircles()
        .catch { _ -> 
            // We'll handle this in the combine below or separately
            emit(emptyList())
        }

    val uiState: StateFlow<HomeUiState> = combine(_circlesFlow, _selectedFilter) { circles, filter ->
        try {
            val filtered = if (filter == "All") {
                circles
            } else {
                circles.filter { it.game.equals(filter, ignoreCase = true) }
            }
            HomeUiState.Success(filtered)
        } catch (e: Exception) {
            HomeUiState.Error(if (e.message?.contains("PERMISSION_DENIED") == true) 
                "Check Internet/Access" else e.message ?: "Unknown error")
        }
    }.catch { e ->
        emit(HomeUiState.Error(if (e.message?.contains("PERMISSION_DENIED") == true) 
            "Check Internet/Access" else e.message ?: "Unknown error"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState.Loading
    )

    init {
        loadUserProfile()
    }

    fun setFilter(category: String) {
        _selectedFilter.value = category
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            val result = authRepository.getUserProfile()
            if (result.isSuccess) {
                val user = result.getOrNull()
                _user.value = user
                if (user?.onboardingCompleted == false) {
                    _shouldNavigateToOnboarding.value = true
                }
            }
        }
    }

    private val _shouldNavigateToOnboarding = MutableStateFlow(false)
    val shouldNavigateToOnboarding: StateFlow<Boolean> = _shouldNavigateToOnboarding.asStateFlow()

    fun onOnboardingNavigationHandled() {
        _shouldNavigateToOnboarding.value = false
    }

    fun signOut() {
        authRepository.signOut()
    }
}

class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val authRepo = AuthRepository(context)
            // Manual DI for CircleRepository
            val circleRepo = CircleRepositoryImpl()
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(authRepo, circleRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
