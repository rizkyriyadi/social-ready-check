package com.example.tripglide.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripglide.data.model.User
import com.example.tripglide.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    init {
        loadUserProfile()
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
            } else {
                // If fetch fails, maybe logout? Or retry.
            }
        }
    }

    private val _shouldNavigateToOnboarding = MutableStateFlow(false)
    val shouldNavigateToOnboarding: StateFlow<Boolean> = _shouldNavigateToOnboarding.asStateFlow()

    fun onOnboardingNavigationHandled() {
        _shouldNavigateToOnboarding.value = false
    }

    fun updateUserName(newName: String) {
        viewModelScope.launch {
            val result = authRepository.updateUserName(newName)
            if (result.isSuccess) {
                // Optimistic update or reload
                loadUserProfile()
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}

class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(AuthRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
