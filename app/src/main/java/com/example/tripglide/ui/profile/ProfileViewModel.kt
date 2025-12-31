package com.example.tripglide.ui.profile

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

class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.getUserProfile()
            if (result.isSuccess) {
                _user.value = result.getOrNull()
            }
            _isLoading.value = false
        }
    }

    private val _usernameError = MutableStateFlow<String?>(null)
    val usernameError: StateFlow<String?> = _usernameError.asStateFlow()

    fun checkUsername(username: String) {
        viewModelScope.launch {
            if (username.length < 3) {
                _usernameError.value = "Too short"
                return@launch
            }
            val isAvailable = authRepository.checkUsernameAvailability(username)
            _usernameError.value = if (isAvailable) null else "Username taken"
        }
    }

    fun updateProfile(displayName: String, bio: String, username: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.updateProfile(displayName, bio, username)
            if (result.isSuccess) {
                loadUserProfile()
            } else {
                // Handle error (could expose via another state)
            }
            _isLoading.value = false
        }
    }

    fun updateProfileImage(uri: android.net.Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.uploadProfileImage(uri)
            if (result.isSuccess) {
                loadUserProfile()
            }
            _isLoading.value = false
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(AuthRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
