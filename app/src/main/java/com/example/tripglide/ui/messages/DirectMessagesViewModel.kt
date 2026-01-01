package com.example.tripglide.ui.messages

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripglide.data.model.DMChannel
import com.example.tripglide.data.model.Friend
import com.example.tripglide.data.repository.ChatRepository
import com.example.tripglide.data.repository.ChatRepositoryImpl
import com.example.tripglide.data.repository.FriendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private const val TAG = "DirectMessagesViewModel"

class DirectMessagesViewModel(
    private val chatRepository: ChatRepository,
    private val friendRepository: FriendRepository
) : ViewModel() {

    private val _dmChannels = MutableStateFlow<List<DMChannel>>(emptyList())
    val dmChannels: StateFlow<List<DMChannel>> = _dmChannels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    fun loadDMChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.getDMChannels()
                .catch { e ->
                    Log.e(TAG, "Error loading DM channels", e)
                    _dmChannels.value = emptyList()
                    _isLoading.value = false
                }
                .collect { channels ->
                    Log.d(TAG, "Loaded ${channels.size} DM channels")
                    _dmChannels.value = channels
                    _isLoading.value = false
                }
        }
    }
    
    fun loadFriends() {
        viewModelScope.launch {
            friendRepository.getFriends()
                .onSuccess { friendsList ->
                    _friends.value = friendsList
                }
                .onFailure { e ->
                    Log.e(TAG, "Error loading friends", e)
                }
        }
    }
    
    suspend fun createOrGetDMChannel(otherUserId: String): Result<DMChannel> {
        return chatRepository.getOrCreateDMChannel(otherUserId)
    }
}

class DirectMessagesViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DirectMessagesViewModel::class.java)) {
            val chatRepo = ChatRepositoryImpl()
            val friendRepo = FriendRepository()
            @Suppress("UNCHECKED_CAST")
            return DirectMessagesViewModel(chatRepo, friendRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
