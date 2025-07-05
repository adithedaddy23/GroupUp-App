package com.example.studygroupfinder.viewmodel

import EventJoinStatus
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImagePainter
import com.example.studygroupfinder.firestore.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EventViewModel (
    private val repository: FirestoreRepository,
    private val userId: String
) : ViewModel() {
    data class EventUiState(
        val hostInfo: FirestoreRepository.User? = null,
        val isLoadingHost: Boolean = true,
        val requestStatus: EventJoinStatus = EventJoinStatus.CAN_JOIN,
        val isProcessingRequest: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(EventUiState())
    val uiState: StateFlow<EventUiState> = _uiState.asStateFlow()

    fun handleEventParticipation(event: FirestoreRepository.Event, onSuccess: () -> Unit) {
        if(_uiState.value.isProcessingRequest) return

        val currentStatus = _uiState.value.requestStatus
        if (currentStatus != EventJoinStatus.CAN_JOIN && currentStatus != EventJoinStatus.JOINED) return

        _uiState.update { it.copy(isProcessingRequest = true) }

        viewModelScope.launch {
            try {
                if (currentStatus == EventJoinStatus.CAN_JOIN) {
                    repository.joinEvent(event.id, userId)
                    _uiState.update {
                        it.copy(
                            requestStatus = EventJoinStatus.JOINED,
                            isProcessingRequest = false
                        )
                    }
                } else {
                    repository.leaveEvent(event.id, userId)
                    _uiState.update {
                        it.copy(
                            requestStatus = EventJoinStatus.CAN_JOIN,
                            isProcessingRequest = false
                        )
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessingRequest = false,
                        error = e.message ?: "Failed to update participation"
                    )
                }
            }
        }
    }

    // Load host information
    fun loadHostInfo(hostId: String) {
        _uiState.update { it.copy(isLoadingHost = true) }

        viewModelScope.launch {
            try {
                val host = repository.getUserProfile(hostId)
                _uiState.update {
                    it.copy(
                        hostInfo = host,
                        isLoadingHost = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingHost = false,
                        error = e.message ?: "Failed to load host info"
                    )
                }
            }
        }
    }

    // Determine initial state
    fun determineInitialState(event: FirestoreRepository.Event) {
        val isJoined = event.participants.contains(userId)
        val isFull = event.currentParticipants >= event.maxParticipants
        val isHost = event.hostId == userId

        val status = when {
            isHost -> EventJoinStatus.HOSTING
            isJoined -> EventJoinStatus.JOINED
            isFull -> EventJoinStatus.FULL
            else -> EventJoinStatus.CAN_JOIN
        }

        _uiState.update { it.copy(requestStatus = status) }
    }
}