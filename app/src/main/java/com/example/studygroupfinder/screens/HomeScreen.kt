import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.studygroupfinder.LocationService
import com.example.studygroupfinder.R
import com.example.studygroupfinder.firestore.FirestoreRepository
import com.example.studygroupfinder.viewmodel.AuthState
import com.example.studygroupfinder.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studygroupfinder.LocationPermissionHandler
import com.example.studygroupfinder.viewmodel.EventViewModel
import com.example.studygroupfinder.viewmodel.EventViewModelFactory
import java.text.DateFormat
import java.util.Date

enum class EventJoinStatus {
    CAN_JOIN,    // User can join
    JOINED,      // User has joined
    FULL,        // Event is at capacity
    HOSTING      // User is the host
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    repository: FirestoreRepository = FirestoreRepository(),
    navController: NavController? = null,
    context: Context = LocalContext.current,
    locationService: LocationService = LocationService(context)
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: ""
    var isLoading by remember { mutableStateOf(false) }
    var allEvents by remember { mutableStateOf<List<FirestoreRepository.Event>>(emptyList()) }
    var filteredEvents by remember { mutableStateOf<List<FirestoreRepository.Event>>(emptyList()) }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var availableTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()
    val authState by authViewModel.authState.observeAsState()
    var error by remember { mutableStateOf<String?>(null) }
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var locationPermissionGranted by remember { mutableStateOf(false) }
    var isLocationLoading by remember { mutableStateOf(true) }
    var locationRequestInitiated by remember { mutableStateOf(false) }

    fun refreshEvents(center: GeoPoint) {
        if(userId.isNotEmpty()) {
            scope.launch {
                isLoading = true
                try {
                    val osmdroidGeoPoint = org.osmdroid.util.GeoPoint(center.latitude, center.longitude)
                    val allNearByEvents = repository.getNearbyEvents(osmdroidGeoPoint, 100.0)

                    allEvents = allNearByEvents.filter { it.hostId != userId }

                    // Extract all unique tags
                    availableTags = allEvents.flatMap { it.tags }.toSet()

                    // Apply filter
                    filteredEvents = if (selectedTags.isEmpty()) {
                        allEvents
                    } else {
                        allEvents.filter { event ->
                            event.tags.any { tag -> selectedTags.contains(tag) }
                        }
                    }

                    error = null
                } catch (e: Exception) {
                    error = e.message ?: "Failed to load events"
                    allEvents = emptyList()
                    filteredEvents = emptyList()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // Apply filter when selectedTags change
    LaunchedEffect(selectedTags, allEvents) {
        filteredEvents = if (selectedTags.isEmpty()) {
            allEvents
        } else {
            allEvents.filter { event ->
                event.tags.any { tag -> selectedTags.contains(tag) }
            }
        }
    }

    fun requestLocationWithService() {
        if (locationRequestInitiated) return
        locationRequestInitiated = true
        isLocationLoading = true
        error = null

        if (!locationService.isLocationEnabled()) {
            error = "Please enable location services in device settings"
            isLocationLoading = false
            locationRequestInitiated = false
            return
        }

        locationService.requestCurrentLocation(
            onLocationUpdate = { geoPoint ->
                currentLocation = geoPoint
                isLocationLoading = false
                locationRequestInitiated = false
                refreshEvents(geoPoint)
            },
            onError = { e ->
                error = when {
                    e.message?.contains("timeout") == true ->
                        "Getting location is taking longer than expected. Please ensure you're in an area with good GPS signal."
                    e.message?.contains("Location services disabled") == true ->
                        "Please enable location services in device settings"
                    e.message?.contains("permission") == true ->
                        "Location permission is required to find events near you"
                    else -> e.message ?: "Failed to get current location"
                }
                isLocationLoading = false
                locationRequestInitiated = false
            }
        )
    }

    LocationPermissionHandler(
        onPermissionGranted = {
            locationPermissionGranted = true
            requestLocationWithService()
        },
        onPermissionDenied = {
            locationPermissionGranted = false
            error = "Location permission is required to find events near you"
            isLocationLoading = false
            locationRequestInitiated = false
        }
    )

    LaunchedEffect(authState) {
        if (authState == AuthState.NotAuthenticated) {
            navController?.navigate("auth") {
                popUpTo(0)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Nearby Events",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Filter indicator
            if (selectedTags.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.filter),
                            contentDescription = "Filter active",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "${selectedTags.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // Tag Filter Section
        if (availableTags.isNotEmpty() && !isLocationLoading && error == null) {
            TagFilterSection(
                availableTags = availableTags,
                selectedTags = selectedTags,
                onTagToggle = { tag ->
                    selectedTags = if (selectedTags.contains(tag)) {
                        selectedTags - tag
                    } else {
                        selectedTags + tag
                    }
                },
                onClearAll = { selectedTags = emptySet() }
            )
        }

        when {
            isLocationLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Getting your location...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (locationPermissionGranted) {
                            OutlinedButton(
                                onClick = {
                                    locationRequestInitiated = false
                                    requestLocationWithService()
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
            error != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!locationPermissionGranted) {
                                Button(
                                    onClick = {
                                        isLocationLoading = true
                                        error = null
                                        locationRequestInitiated = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Grant Permission")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        error = null
                                        locationRequestInitiated = false
                                        requestLocationWithService()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Retry")
                                }
                            }

                            if (error?.contains("location services") == true) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val intent = Intent(Settings.ACTION_SETTINGS)
                                            context.startActivity(intent)
                                        }
                                    }
                                ) {
                                    Text("Settings")
                                }
                            }
                        }
                    }
                }
            }
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading nearby events...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            filteredEvents.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "No events",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (selectedTags.isNotEmpty()) "No events match your filters" else "No nearby events found",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (selectedTags.isNotEmpty()) "Try adjusting your tag filters" else "Check back later or create your own event!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selectedTags.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { selectedTags = emptySet() }
                                ) {
                                    Text("Clear Filters")
                                }
                            }
                            Button(
                                onClick = {
                                    currentLocation?.let { refreshEvents(it) } ?: run {
                                        locationRequestInitiated = false
                                        requestLocationWithService()
                                    }
                                }
                            ) {
                                Text("Refresh")
                            }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = filteredEvents,
                        key = { event -> event.id } // Crucial for stable scrolling
                    ) { event ->
                        EventCard(
                            event = event,
                            userId = userId,
                            repository = repository,
                            onClick = { /* handle click */ },
                            onRefreshNeeded = { currentLocation?.let { refreshEvents(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(locationService) {
        onDispose {
            locationService.stopLocationUpdates()
        }
    }
}

@Composable
fun TagFilterSection(
    availableTags: Set<String>,
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.filter),
                    contentDescription = "Filter",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Filter by Tags",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selectedTags.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Clear All",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Tag chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(availableTags.toList()) { tag ->
                val isSelected = selectedTags.contains(tag)
                FilterChip(
                    onClick = { onTagToggle(tag) },
                    label = {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    },
                    selected = isSelected,
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.dp
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(36.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    event: FirestoreRepository.Event,
    userId: String,
    repository: FirestoreRepository,
    isCreatedEvent: Boolean = false,
    onRefreshNeeded: () -> Unit = {}
) {
    // Move expensive formatters to remember
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    // Use derivedStateOf for computed values
    val participationRatio by remember(event.currentParticipants, event.maxParticipants) {
        derivedStateOf { event.currentParticipants.toFloat() / event.maxParticipants.toFloat() }
    }

    // Use remember to prevent recomposition when values haven't changed
    val spotsLeft by remember(event.maxParticipants, event.currentParticipants) {
        derivedStateOf { event.maxParticipants - event.currentParticipants }
    }

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
            // Title - stable and simple
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Extract HostInfoRow to separate composable for better recomposition control
            HostInfoRow(event, userId, repository)

            // Description
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            // Tags - extract to separate composable
            EventTags(event.tags)

            // Date and Time - optimized row
            EventDateTimeRow(event.date, dateFormat, timeFormat)

            // Location - simple row
            EventLocationRow(event.locationName)

            // Participants section - extract to separate composable
            ParticipantsSection(
                participationRatio = participationRatio,
                currentParticipants = event.currentParticipants,
                maxParticipants = event.maxParticipants,
                spotsLeft = spotsLeft,
                event = event,
                userId = userId,
                repository = repository,
                onRefreshNeeded = onRefreshNeeded
            )

            // Open Maps Button - stable component
            OpenMapsButton(event.location, event.locationName)
        }
    }
}

// Extracted components for better recomposition control:

@Composable
private fun HostInfoRow(
    event: FirestoreRepository.Event,
    userId: String,
    repository: FirestoreRepository
) {
    val viewModel: EventViewModel = viewModel(
        key = "host_${event.hostId}", // Add unique key based on hostId
        factory = EventViewModelFactory(repository, userId)
    )
    val uiState by viewModel.uiState.collectAsState()

    // Load host info only once when event changes
    LaunchedEffect(event.hostId) {
        viewModel.loadHostInfo(event.hostId)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Host Profile Picture - use remember to cache the image
        if (uiState.isLoadingHost) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            AsyncImage(
                model = uiState.hostInfo?.profilePic ?: "",
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.account),
                placeholder = painterResource(R.drawable.account)
            )
        }

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
                text = if (uiState.isLoadingHost) "Loading..." else (uiState.hostInfo?.name ?: "Unknown Host"),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (uiState.requestStatus == EventJoinStatus.HOSTING) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Text(
                    text = "You",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventTags(tags: List<String>) {
    if (tags.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            tags.forEach { tag ->
                key(tag) { // Add key for each tag to help Compose identify them
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventDateTimeRow(date: Date, dateFormat: DateFormat, timeFormat: DateFormat) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Date
        IconTextRow(
            icon = Icons.Default.DateRange,
            iconColor = MaterialTheme.colorScheme.primary,
            text = dateFormat.format(date),
            backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(0.3f)
        )

        // Time
        IconTextRow(
            icon = painterResource(R.drawable.clock),
            iconColor = MaterialTheme.colorScheme.secondary,
            text = timeFormat.format(date),
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(0.3f)
        )
    }
}

@Composable
private fun IconTextRow(
    icon: Any,
    iconColor: Color,
    text: String,
    backgroundColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = backgroundColor,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                when (icon) {
                    is ImageVector -> Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
                    is Painter -> Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
                }
            }
        }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun EventLocationRow(locationName: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(0.3f),
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.LocationOn,
                null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            locationName,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ParticipantsSection(
    participationRatio: Float,
    currentParticipants: Int,
    maxParticipants: Int,
    spotsLeft: Int,
    event: FirestoreRepository.Event,
    userId: String,
    repository: FirestoreRepository,
    onRefreshNeeded: () -> Unit
) {
    val viewModel: EventViewModel = viewModel(
        key = "event_${event.id}", // Add unique key based on event ID
        factory = EventViewModelFactory(repository, userId)
    )
    val uiState by viewModel.uiState.collectAsState()

    // Only run this when the event changes
    LaunchedEffect(event.id) {
        viewModel.determineInitialState(event)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Default.Person,
                    null,
                    modifier = Modifier.size(16.dp),
                )
                    Text("Participants", style = MaterialTheme.typography.labelMedium)
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when {
                    uiState.requestStatus == EventJoinStatus.FULL -> MaterialTheme.colorScheme.errorContainer
                    participationRatio > 0.7f -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = "$currentParticipants/$maxParticipants",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = when {
                        uiState.requestStatus == EventJoinStatus.FULL -> MaterialTheme.colorScheme.onErrorContainer
                        participationRatio > 0.7f -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        LinearProgressIndicator(
            progress = { participationRatio.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = when {
                uiState.requestStatus == EventJoinStatus.FULL -> MaterialTheme.colorScheme.error
                participationRatio > 0.7f -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.secondary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Text(
            text = when {
                uiState.requestStatus == EventJoinStatus.FULL -> "Event is full"
                else -> "$spotsLeft ${if (spotsLeft == 1) "spot" else "spots"} remaining"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (uiState.requestStatus == EventJoinStatus.FULL) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (uiState.requestStatus != EventJoinStatus.HOSTING) {
            Button(
                onClick = { viewModel.handleEventParticipation(event, onRefreshNeeded) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isProcessingRequest &&
                        (uiState.requestStatus == EventJoinStatus.CAN_JOIN ||
                                uiState.requestStatus == EventJoinStatus.JOINED),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (uiState.requestStatus) {
                        EventJoinStatus.JOINED -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                if (uiState.isProcessingRequest) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...")
                } else {
                    Text(
                        when (uiState.requestStatus) {
                            EventJoinStatus.CAN_JOIN -> "Join Event"
                            EventJoinStatus.JOINED -> "Joined (Tap to Leave)"
                            EventJoinStatus.FULL -> "Event Full"
                            EventJoinStatus.HOSTING -> "Hosting"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OpenMapsButton(location: com.google.firebase.firestore.GeoPoint, locationName: String) {
    val context = LocalContext.current

    Button(
        onClick = {
            val locationUri = Uri.parse("geo:${location.latitude},${location.longitude}?q=${Uri.encode(locationName)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, locationUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            try {
                context.startActivity(mapIntent)
            } catch (e: ActivityNotFoundException) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, locationUri))
                } catch (e: ActivityNotFoundException) {
                    // Toast would cause recomposition - better to handle this differently
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
        Icon(painterResource(R.drawable.map), null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Open in Maps", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
    }
}

