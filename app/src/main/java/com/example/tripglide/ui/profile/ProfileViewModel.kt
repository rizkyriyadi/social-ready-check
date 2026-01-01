package com.example.tripglide.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripglide.data.model.DotaLinkedAccount
import com.example.tripglide.data.model.User
import com.example.tripglide.data.repository.AuthRepository
import com.example.tripglide.data.repository.DotaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DotaLinkState {
    data object Idle : DotaLinkState
    data object Loading : DotaLinkState
    data class VerificationSuccess(val account: DotaLinkedAccount) : DotaLinkState
    data object LinkSuccess : DotaLinkState
    data class Error(val message: String) : DotaLinkState
}

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val dotaRepository: DotaRepository = DotaRepository()
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _dotaLinkState = MutableStateFlow<DotaLinkState>(DotaLinkState.Idle)
    val dotaLinkState: StateFlow<DotaLinkState> = _dotaLinkState.asStateFlow()

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
    
    // ==================== DOTA LINKING ====================
    
    /**
     * Verify a Dota account by Steam Friend ID
     */
    fun verifyDotaAccount(steamFriendId: String) {
        if (steamFriendId.isBlank()) {
            _dotaLinkState.value = DotaLinkState.Error("Please enter your Dota Friend ID")
            return
        }
        
        viewModelScope.launch {
            _dotaLinkState.value = DotaLinkState.Loading
            
            dotaRepository.verifyDotaAccount(steamFriendId).collect { result ->
                if (result.isSuccess) {
                    _dotaLinkState.value = DotaLinkState.VerificationSuccess(result.getOrThrow())
                } else {
                    _dotaLinkState.value = DotaLinkState.Error(
                        result.exceptionOrNull()?.message ?: "Verification failed"
                    )
                }
            }
        }
    }
    
    /**
     * Confirm and save the verified Dota account
     */
    fun confirmDotaLink(account: DotaLinkedAccount) {
        viewModelScope.launch {
            _dotaLinkState.value = DotaLinkState.Loading
            
            val result = dotaRepository.linkDotaAccount(account)
            if (result.isSuccess) {
                _dotaLinkState.value = DotaLinkState.LinkSuccess
                loadUserProfile() // Refresh user to show linked account
            } else {
                _dotaLinkState.value = DotaLinkState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to link account"
                )
            }
        }
    }
    
    /**
     * Unlink the Dota account
     */
    fun unlinkDotaAccount() {
        viewModelScope.launch {
            _dotaLinkState.value = DotaLinkState.Loading
            
            val result = dotaRepository.unlinkDotaAccount()
            if (result.isSuccess) {
                _dotaLinkState.value = DotaLinkState.Idle
                loadUserProfile() // Refresh user
            } else {
                _dotaLinkState.value = DotaLinkState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to unlink account"
                )
            }
        }
    }
    
    fun resetDotaLinkState() {
        _dotaLinkState.value = DotaLinkState.Idle
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
