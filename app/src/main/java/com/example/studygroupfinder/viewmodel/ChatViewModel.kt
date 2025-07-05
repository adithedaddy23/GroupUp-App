package com.example.studygroupfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studygroupfinder.firestore.FirestoreRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: FirestoreRepository
) : ViewModel() {

    private val _chats = MutableStateFlow<List<FirestoreRepository.ChatWithEventDetails>>(emptyList())
    val chats: StateFlow<List<FirestoreRepository.ChatWithEventDetails>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<List<FirestoreRepository.Message>>(emptyList())
    val messages: StateFlow<List<FirestoreRepository.Message>> = _messages.asStateFlow()

    private val _currentChatDetails = MutableStateFlow<FirestoreRepository.ChatWithEventDetails?>(null)
    val currentChatDetails: StateFlow<FirestoreRepository.ChatWithEventDetails?> = _currentChatDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var chatsListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null

    fun loadChats() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Start listening for real-time chat updates
                chatsListener?.remove()
                chatsListener = repository.listenForChats { chatList ->
                    viewModelScope.launch {
                        // Get event details for each chat
                        val chatsWithEvents = mutableListOf<FirestoreRepository.ChatWithEventDetails>()

                        for (chat in chatList) {
                            try {
                                val event = repository.getEvent(chat.eventId)
                                if (event != null) {
                                    chatsWithEvents.add(
                                        FirestoreRepository.ChatWithEventDetails(
                                            chat = chat,
                                            event = event
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                println("Error getting event for chat ${chat.id}: ${e.message}")
                            }
                        }

                        _chats.value = chatsWithEvents
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Stop previous listener
                messagesListener?.remove()

                // Start listening for real-time message updates
                messagesListener = repository.listenForMessages(chatId) { messageList ->
                    _messages.value = messageList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun loadChatDetails(chatId: String) {
        viewModelScope.launch {
            try {
                val chatDetails = repository.getChatWithEventDetails(chatId)
                _currentChatDetails.value = chatDetails
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun sendMessage(chatId: String, text: String) {
        viewModelScope.launch {
            try {
                repository.sendMessage(chatId, text)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun markChatAsRead(chatId: String) {
        viewModelScope.launch {
            try {
                repository.markChatAsRead(chatId)
            } catch (e: Exception) {
                // Don't show error for read receipts
                println("Failed to mark chat as read: ${e.message}")
            }
        }
    }

    fun getCurrentUserId(): String {
        return repository.getCurrentUserId()
    }

    fun clearError() {
        _error.value = null
    }

    suspend fun getUserDetails(userId: String): FirestoreRepository.User? {
        return try {
            repository.getUserProfile(userId)
        } catch (e: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatsListener?.remove()
        messagesListener?.remove()
    }
}