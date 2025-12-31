package com.example.tripglide.ui.social

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripglide.data.model.User
import com.example.tripglide.data.repository.FriendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.tripglide.data.model.Friend
import com.example.tripglide.data.model.FriendRequest

class SocialViewModel(
    private val friendRepository: FriendRepository
) : ViewModel() {

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _scannedUser = MutableStateFlow<User?>(null)
    val scannedUser: StateFlow<User?> = _scannedUser.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _requestStatus = MutableStateFlow<String?>(null)
    val requestStatus: StateFlow<String?> = _requestStatus.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val pendingRequests: StateFlow<List<FriendRequest>> = _pendingRequests.asStateFlow()

    fun loadFriends() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = friendRepository.getFriends()
            if (result.isSuccess) {
                _friends.value = result.getOrNull() ?: emptyList()
            }
            _isLoading.value = false
        }
    }

    fun loadPendingRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = friendRepository.getPendingRequests()
            if (result.isSuccess) {
                _pendingRequests.value = result.getOrNull() ?: emptyList()
            }
            _isLoading.value = false
        }
    }

    fun acceptRequest(request: FriendRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = friendRepository.acceptFriendRequest(request)
            if (result.isSuccess) {
                _pendingRequests.value = _pendingRequests.value.filter { it.id != request.id }
                _requestStatus.value = "Friend added!"
            } else {
                _requestStatus.value = "Error: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = friendRepository.rejectFriendRequest(requestId)
            if (result.isSuccess) {
                _pendingRequests.value = _pendingRequests.value.filter { it.id != requestId }
            }
            _isLoading.value = false
        }
    }

    fun fetchScannedUser(uid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = friendRepository.getUserByUid(uid)
            if (result.isSuccess) {
                _scannedUser.value = result.getOrNull()
            }
            _isLoading.value = false
        }
    }

    fun clearScannedUser() {
        _scannedUser.value = null
    }

    fun searchUsers(query: String) {
        val cleanQuery = query.trim().removePrefix("@")
        if (cleanQuery.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = friendRepository.searchUsers(cleanQuery)
            if (result.isSuccess) {
                _searchResults.value = result.getOrNull() ?: emptyList()
            }
            _isLoading.value = false
        }
    }

    fun sendFriendRequest(targetUid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = friendRepository.sendFriendRequest(targetUid)
            if (result.isSuccess) {
                _requestStatus.value = "Request Sent!"
            } else {
                _requestStatus.value = "Error: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }
    
    fun removeFriend(uid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = friendRepository.removeFriend(uid)
            if (result.isSuccess) {
                _friends.value = _friends.value.filter { it.uid != uid }
                _requestStatus.value = "Friend removed"
            } else {
                _requestStatus.value = "Error: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }
    
    fun clearStatus() {
        _requestStatus.value = null
    }
}

class SocialViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SocialViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SocialViewModel(FriendRepository()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
