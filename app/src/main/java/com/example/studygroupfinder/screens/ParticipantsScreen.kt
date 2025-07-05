package com.example.studygroupfinder.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.studygroupfinder.R
import com.example.studygroupfinder.firestore.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantsScreen(
    eventId: String,
    hostId: String,
    repository: FirestoreRepository = FirestoreRepository(),
    navController: NavController? = null,
    context: Context = LocalContext.current
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid ?: ""
    var participants by remember { mutableStateOf<List<FirestoreRepository.Participant>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var participantToRemove by remember { mutableStateOf<FirestoreRepository.Participant?>(null) }
    val scope = rememberCoroutineScope()

    // Fetch participants
    fun loadParticipants() {
        scope.launch {
            isLoading = true
            try {
                participants = repository.getEventParticipants(eventId)
                error = null
            } catch (e: Exception) {
                error = e.message ?: "Failed to load participants"
            } finally {
                isLoading = false
            }
        }
    }

    // Initial load
    LaunchedEffect(eventId) {
        loadParticipants()
    }

    // Remove participant confirmation dialog
    if (showRemoveDialog && participantToRemove != null) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Participant") },
            text = { Text("Are you sure you want to remove ${participantToRemove?.name} from this event?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        participantToRemove?.let { participant ->
                            scope.launch {
                                try {
                                    // 1. Optimistically update UI
                                    val tempParticipants = participants.toMutableList()
                                    tempParticipants.remove(participant)
                                    participants = tempParticipants

                                    // 2. Perform actual removal
                                    repository.removeParticipant(
                                        eventId = eventId,
                                        userId = participant.userId,
                                        hostId = hostId
                                    )

                                    // 3. Refresh to ensure consistency
                                    loadParticipants()
                                    Toast.makeText(
                                        context,
                                        "${participant.name} removed successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    // Show more detailed error message
                                    Log.d("Participant", e.message.toString())
                                } finally {
                                    loadParticipants()
                                }
                            }
                        }
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRemoveDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Participants") },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Add a chat button in the top bar to go to event chat
                    IconButton(
                        onClick = {
                            // Navigate to event chat room
                            navController?.navigate("chat/$eventId")
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.chat_),
                            contentDescription = "Event Chat",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                participants.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No participants yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(participants) { participant ->
                            ParticipantItem(
                                participant = participant,
                                isHost = hostId == currentUserId,
                                onRemove = {
                                    participantToRemove = participant
                                    showRemoveDialog = true
                                },
                                onChat = {
                                    // Navigate to event chat room instead of individual chat
                                    navController?.navigate("chat/$eventId")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParticipantItem(
    participant: FirestoreRepository.Participant,
    isHost: Boolean,
    onRemove: () -> Unit,
    onChat: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp), // Subtle spacing between items
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side - Profile and info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f) // Take available space
            ) {
                // Enhanced profile picture with online indicator
                Box {
                    if (participant.profilePic != null) {
                        AsyncImage(
                            model = participant.profilePic,
                            contentDescription = "Profile picture of ${participant.name}",
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .border(
                                    2.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    CircleShape
                                ),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                        )
                                    )
                                )
                                .border(
                                    2.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = participant.name.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Name
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = participant.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Right side - Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chat button with improved design - now goes to event chat
                Surface(
                    onClick = onChat,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.chat_),
                            contentDescription = "Go to event chat",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Remove button - only visible for host
                if (isHost) {
                    Surface(
                        onClick = onRemove,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove ${participant.name}",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
