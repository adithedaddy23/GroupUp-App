package com.example.studygroupfinder.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.studygroupfinder.R
import com.example.studygroupfinder.firestore.FirestoreRepository
import com.example.studygroupfinder.viewmodel.AuthState
import com.example.studygroupfinder.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs


@Composable
fun MyEventsScreen(
    onNewEventClick: () -> Unit,
    authViewModel: AuthViewModel,
    onEventClick: (String) -> Unit,
    repository: FirestoreRepository = FirestoreRepository(),
    navController: NavController? = null
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: ""

    var joinedEvents by remember { mutableStateOf<List<FirestoreRepository.Event>>(emptyList()) }
    var createdEvents by remember { mutableStateOf<List<FirestoreRepository.Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var isProcessingLeave by remember { mutableStateOf<String?>(null) } // Track which event is being processed

    // Delete dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<FirestoreRepository.Event?>(null) }

    // Participants dialog state
    var showParticipantsDialog by remember { mutableStateOf(false) }
    var selectedEventForParticipants by remember { mutableStateOf<FirestoreRepository.Event?>(null) }
    var participants by remember { mutableStateOf<List<FirestoreRepository.Participant>>(emptyList()) }
    var isLoadingParticipants by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current



    // Observe authentication state
    val authState by authViewModel.authState.observeAsState()

    // Handle auth state changes
    LaunchedEffect(authState) {
        if (authState == AuthState.NotAuthenticated) {
            navController?.navigate("auth") {
                popUpTo(0)
            }
        }
    }

    // Function to refresh events
    fun refreshEvents() {
        if (userId.isNotEmpty()) {
            scope.launch {
                isLoading = true
                try {
                    joinedEvents = repository.getUserJoinedEvents(userId)
                    createdEvents = repository.getUserHostedEvents(userId)
                    error = null
                } catch (e: Exception) {
                    error = e.message ?: "Failed to load events"
                    joinedEvents = emptyList()
                    createdEvents = emptyList()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // Fetch User's Events
    LaunchedEffect(userId) {
        refreshEvents()
    }

    // Function to load participants
    fun loadParticipants(eventId: String) {
        scope.launch {
            isLoadingParticipants = true
            try {
                participants = repository.getEventParticipants(eventId)
            } catch (e: Exception) {
                error = e.message ?: "Failed to load participants"
                participants = emptyList()
            } finally {
                isLoadingParticipants = false
            }
        }
    }

    // Function to handle leaving an event
    fun handleLeaveEvent(event: FirestoreRepository.Event) {
        if (isProcessingLeave != null) return // Prevent multiple simultaneous operations

        isProcessingLeave = event.id
        scope.launch {
            try {
                repository.leaveEvent(event.id, userId)
                Toast.makeText(context, "You've left the event", Toast.LENGTH_SHORT).show()
                refreshEvents() // Refresh the events list
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isProcessingLeave = null
            }
        }
    }

    // Delete confirmation dialog
    // Improved delete confirmation dialog handler
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete this event? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        eventToDelete?.let { event ->
                            scope.launch {
                                try {
                                    repository.deleteEvent(event.id, event.hostId)
                                    // Show success message
                                    Toast.makeText(context, "Event deleted successfully", Toast.LENGTH_SHORT).show()
                                    // Refresh the events list
                                    refreshEvents()
                                } catch (e: Exception) {
                                    // Show error message
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    // Still refresh to show current state
                                    refreshEvents()
                                }
                            }
                        }
                        eventToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        eventToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Participants dialog
    if (showParticipantsDialog) {
        ParticipantsListDialog(
            participants = participants,
            isHost = true,
            onDismiss = {
                showParticipantsDialog = false
                selectedEventForParticipants = null
                participants = emptyList()
            },
            onRemoveParticipant = { participantId ->
                selectedEventForParticipants?.let { event ->
                    scope.launch {
                        try {
                            repository.removeParticipant(event.id, participantId, event.hostId)
                            // Reload participants
                            loadParticipants(event.id)
                            // Refresh events to update participant count
                            refreshEvents()
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to remove participant"
                        }
                    }
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    // Navigate to new event screen when FAB is clicked
                    navController?.navigate("newEvents")
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = "New Event") },
                text = { Text("New Event") },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        // Only detect swipes if the drag is significant enough
                        if (abs(dragAmount) > 20f) { // Adjust threshold as needed
                            when {
                                dragAmount > 0 -> { // Swipe right
                                    if (selectedTab > 0) {
                                        selectedTab--
                                    }
                                }
                                dragAmount < 0 -> { // Swipe left
                                    if (selectedTab < 1) { // Assuming you have 2 tabs
                                        selectedTab++
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Joined Events") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Created Events") }
                )
            }

            // Tab Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Error Loading events")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            // Retry Logic
                            error = null
                            refreshEvents()
                        }) { Text("Retry") }
                    }
                }
                else -> {
                    val eventsToShow = when (selectedTab) {
                        0 -> joinedEvents
                        1 -> createdEvents
                        else -> emptyList()
                    }

                    if (eventsToShow.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = when (selectedTab) {
                                    0 -> "No events joined yet"
                                    1 -> "No events created yet"
                                    else -> "No events"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (selectedTab == 1) {
                                Text(
                                    text = "Tap the + button to create an event",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(eventsToShow) { event ->
                                EventCard(
                                    event = event,
                                    onClick = { onEventClick(event.id) },
                                    navController = navController,
                                    isCreatedEvent = selectedTab == 1, // Only true for created events tab
                                    isJoinedEvent = selectedTab == 0, // True for joined events tab
                                    onDelete = if (selectedTab == 1) {
                                        {
                                            eventToDelete = event
                                            showDeleteDialog = true
                                        }
                                    } else null,
                                    onParticipantsClick = if (selectedTab == 1) {
                                        {
                                            // Wrap the navigation call in a lambda
                                            navController?.navigate("participantScreen/${event.id}/${event.hostId}")
                                        }
                                    } else null,
                                    onLeave = if (selectedTab == 0) {
                                        { handleLeaveEvent(event) }
                                    } else null,
                                    isProcessingLeave = isProcessingLeave == event.id,
                                )
                            }

                            // Add some bottom padding for the FAB
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventCard(
    event: FirestoreRepository.Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCreatedEvent: Boolean = false,
    isJoinedEvent: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onParticipantsClick: (() -> Unit)? = null,
    onLeave: (() -> Unit)? = null,
    isProcessingLeave: Boolean = false,
    navController: NavController? = null,
    repository: FirestoreRepository = FirestoreRepository()
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Host information state - only for joined events
    var hostInfo by remember { mutableStateOf<FirestoreRepository.User?>(null) }
    var isLoadingHost by remember { mutableStateOf(false) }

    // Fetch host information only for joined events
    LaunchedEffect(event.hostId) {
        if (isJoinedEvent) {
            isLoadingHost = true
            try {
                hostInfo = repository.getUserProfile(event.hostId)
            } catch (e: Exception) {
                // Handle error silently or log it
            } finally {
                isLoadingHost = false
            }
        }
    }

    // Calculate if event is full
    val isFull = event.currentParticipants >= event.maxParticipants
    val participationRatio = event.currentParticipants.toFloat() / event.maxParticipants.toFloat()

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with title and action buttons
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Action buttons row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Leave button for joined events
                    if (isJoinedEvent && onLeave != null) {
                        OutlinedButton(
                            onClick = onLeave,
                            enabled = !isProcessingLeave,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isProcessingLeave)
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
                                else
                                    MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            if (isProcessingLeave) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Leave event",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Leave",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Delete button for created events
                    if (isCreatedEvent && onDelete != null) {
                        DeleteButton(onClick = onDelete)
                    }
                }
            }

            // Host Information Row - Only for joined events
            if (isJoinedEvent) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Host Profile Picture
                    if (isLoadingHost) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        AsyncImage(
                            model = hostInfo?.profilePic ?: "",
                            contentDescription = "Host profile picture",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.account),
                            placeholder = painterResource(R.drawable.account)
                        )
                    }

                    // Host Name and "Hosted by" text
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Hosted by",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isLoadingHost) "Loading..." else (hostInfo?.name ?: "Unknown Host"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Description
            if (event.description.isNotBlank()) {
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }

            // Date and Time Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = dateFormat.format(event.date),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.clock),
                                contentDescription = "Time",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = timeFormat.format(event.date),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Location Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = event.locationName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Participants section with progress indicator (clickable for created events)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = if (isCreatedEvent && navController != null) {
                    Modifier.clickable {
                        navController.navigate("participantScreen/${event.id}/${event.hostId}")
                    }
                } else {
                    Modifier
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Participants",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Participants",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isCreatedEvent && onParticipantsClick != null) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "View participants",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isFull -> MaterialTheme.colorScheme.errorContainer
                            participationRatio > 0.7f -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "${event.currentParticipants}/${event.maxParticipants}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = when {
                                isFull -> MaterialTheme.colorScheme.onErrorContainer
                                participationRatio > 0.7f -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Progress bar
                LinearProgressIndicator(
                    progress = { participationRatio.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when {
                        isFull -> MaterialTheme.colorScheme.error
                        participationRatio > 0.7f -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                // Status text
                val spotsLeft = event.maxParticipants - event.currentParticipants
                Text(
                    text = when {
                        isFull -> "Event is full"
                        else -> "$spotsLeft ${if (spotsLeft == 1) "spot" else "spots"} remaining"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFull) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Open Maps Button
            Button(
                onClick = {
                    val locationUri = Uri.parse("geo:${event.location.latitude},${event.location.longitude}?q=${Uri.encode(event.locationName)}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, locationUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    try {
                        context.startActivity(mapIntent)
                    } catch (e: ActivityNotFoundException) {
                        val genericMapIntent = Intent(Intent.ACTION_VIEW, locationUri)
                        try {
                            context.startActivity(genericMapIntent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "No map application found", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.tertiary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.map),
                    contentDescription = "Open in Maps",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open in Maps",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

@Composable
fun DeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete event",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun ParticipantsListDialog(
    participants: List<FirestoreRepository.Participant>, // Added the missing type
    isHost: Boolean = false,
    onDismiss: () -> Unit,
    onRemoveParticipant: ((String) -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Participants (${participants.size})",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            if (participants.isEmpty()) {
                Text(
                    text = "No participants yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(participants) { participant ->
                        ParticipantItem(
                            participant = participant,
                            isHost = isHost,
                            onRemove = if (isHost && onRemoveParticipant != null) {
                                { onRemoveParticipant(participant.userId) }
                            } else null
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ParticipantItem(
    participant: FirestoreRepository.Participant,
    isHost: Boolean = false,
    onRemove: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Profile picture or placeholder
        if (participant.profilePic != null) {
            AsyncImage(
                model = participant.profilePic,
                contentDescription = "${participant.name}'s profile picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = participant.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Name
        Text(
            text = participant.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // Remove button (only for hosts)
        if (isHost && onRemove != null) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Remove participant",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}