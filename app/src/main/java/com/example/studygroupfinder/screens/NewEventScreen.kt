package com.example.studygroupfinder.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.studygroupfinder.firestore.FirestoreRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import java.util.*
import androidx.compose.foundation.layout.FlowRow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewEventScreen(
    onBackClick: () -> Unit,
    repository: FirestoreRepository = FirestoreRepository(),
    navController: NavController? = null
) {
    // Form state
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedTime by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var maxParticipants by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var locationName by remember { mutableStateOf("") }

    // Location state
    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var showLocationPicker by remember { mutableStateOf(false) }

    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Date/Time pickers
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Predefined tags
    val predefinedTags = listOf(
        "Sports", "Music", "Food", "Art", "Technology",
        "Education", "Business", "Health", "Travel", "Photography",
        "Gaming", "Fitness", "Movies", "Books", "Outdoor",
        "Indoor", "Networking", "Workshop", "Party", "Conference"
    )

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun createEvent() {
        // Validation
        when {
            title.isEmpty() -> error = "Please enter event title"
            description.isEmpty() -> error = "Please enter event description"
            selectedDate == null -> error = "Please select event date"
            selectedTime == null -> error = "Please select event time"
            locationName.isEmpty() -> error = "Please enter location name"
            selectedLocation == null -> error = "Please select location on map"
            maxParticipants.toIntOrNull() == null || maxParticipants.toInt() <= 0 ->
                error = "Please enter a valid number of participants"
            else -> {
                scope.launch {
                    isLoading = true
                    error = null

                    try {
                        // Combine date and time
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = selectedDate!!
                            set(Calendar.HOUR_OF_DAY, selectedTime!!.first)
                            set(Calendar.MINUTE, selectedTime!!.second)
                        }

                        repository.addEvent(
                            title = title,
                            description = description,
                            date = calendar.time,
                            time = "${selectedTime!!.first}:${selectedTime!!.second.toString().padStart(2, '0')}",
                            location = selectedLocation!!,
                            locationName = locationName,
                            maxParticipants = maxParticipants.toInt(),
                            tags = selectedTags.toList()
                        )
                        onBackClick()
                    } catch (e: Exception) {
                        error = e.message ?: "Failed to create event"
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }

    // Show full-screen location picker
    if (showLocationPicker) {
        LocationPickerScreen(
            initialLocation = selectedLocation?.let { OsmGeoPoint(it.latitude, it.longitude) },
            onLocationSelected = { lat, lng, name ->
                selectedLocation = GeoPoint(lat, lng)
                if (name.isNotEmpty()) locationName = name
                showLocationPicker = false
            },
            onDismiss = { showLocationPicker = false }
        )
        return // Return early to show only the location picker
    }

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { dateMillis ->
                selectedDate = dateMillis
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Time picker dialog
    if (showTimePicker) {
        TimePickerDialog(
            onTimeSelected = { hour, minute ->
                selectedTime = Pair(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Event") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error message
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )

                // Date and Time
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = selectedDate?.let {
                                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
                            } ?: "Select Date *"
                        )
                    }

                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = selectedTime?.let { (h, m) ->
                                "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
                            } ?: "Select Time *"
                        )
                    }
                }

                // Max Participants
                OutlinedTextField(
                    value = maxParticipants,
                    onValueChange = { maxParticipants = it },
                    label = { Text("Max Participants *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Tags
                Column {
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        predefinedTags.forEach { tag ->
                            FilterChip(
                                selected = selectedTags.contains(tag),
                                onClick = {
                                    selectedTags = if (selectedTags.contains(tag)) {
                                        selectedTags - tag
                                    } else {
                                        selectedTags + tag
                                    }
                                },
                                label = {
                                    Text(
                                        text = tag,
                                        fontSize = 14.sp,
                                        fontWeight = if (selectedTags.contains(tag)) FontWeight.Medium else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }

                // Location
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Location Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showLocationPicker = true }) {
                            Icon(Icons.Default.LocationOn, "Pick Location")
                        }
                    }
                )

                selectedLocation?.let {
                    Text(
                        text = "Lat: ${"%.6f".format(it.latitude)}, Lng: ${"%.6f".format(it.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Create Event Button
                Button(
                    onClick = { createEvent() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Create Event")
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

typealias OsmGeoPoint = org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    initialLocation: OsmGeoPoint?,
    onLocationSelected: (Double, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLocation by remember { mutableStateOf(initialLocation) }
    var placeName by remember { mutableStateOf("") }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Address>>(emptyList()) }
    var showSearchResults by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<OsmGeoPoint?>(null) }
    var locationPermissionGranted by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (locationPermissionGranted) {
            isLoadingLocation = true
            getCurrentLocation(context) { location ->
                currentLocation = location
                if (location != null) {
                    selectedLocation = location
                    isLoadingLocation = false
                    mapView?.let { map ->
                        map.controller.animateTo(location)
                        map.controller.setZoom(15.0)
                        updateMapMarker(map, location, "Current Location")
                    }
                    scope.launch {
                        val address = getAddressFromLocation(context, location)
                        placeName = address
                        searchQuery = address
                    }
                } else {
                    isLoadingLocation = false
                }
            }
        } else {
            isLoadingLocation = false
        }
    }

    // Initialize OSMDroid configuration and get current location
    LaunchedEffect(Unit) {
        org.osmdroid.config.Configuration.getInstance().load(
            context,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        )
        org.osmdroid.config.Configuration.getInstance().userAgentValue = context.packageName

        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

        locationPermissionGranted = hasLocationPermission

        if (hasLocationPermission) {
            isLoadingLocation = true
            getCurrentLocation(context) { location ->
                currentLocation = location
                if (initialLocation == null && location != null) {
                    selectedLocation = location
                    scope.launch {
                        val address = getAddressFromLocation(context, location)
                        placeName = address
                        searchQuery = address
                    }
                } else if (initialLocation != null) {
                    selectedLocation = initialLocation
                    scope.launch {
                        val address = getAddressFromLocation(context, initialLocation)
                        placeName = address
                        searchQuery = address
                    }
                }
                isLoadingLocation = false
                mapView?.let { map ->
                    (selectedLocation ?: currentLocation)?.let {
                        map.controller.setCenter(it)
                        map.controller.setZoom(15.0)
                        updateMapMarker(map, it, if(selectedLocation != null) "Selected Location" else "Current Location")
                    }
                }
            }
        } else if (initialLocation == null) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            selectedLocation = initialLocation
            scope.launch {
                val address = getAddressFromLocation(context, initialLocation)
                placeName = address
                searchQuery = address
            }
            mapView?.let { map ->
                map.controller.setCenter(initialLocation)
                map.controller.setZoom(15.0)
                updateMapMarker(map, initialLocation, "Selected Location")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Location") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            selectedLocation?.let {
                                onLocationSelected(it.latitude, it.longitude, placeName)
                            }
                        },
                        enabled = selectedLocation != null
                    ) {
                        Text("DONE")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { ctx ->
                    org.osmdroid.views.MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setBuiltInZoomControls(true)
                        setMultiTouchControls(true)
                        mapView = this

                        controller.setZoom(15.0)
                        val locationToShow =
                            selectedLocation ?: currentLocation ?: OsmGeoPoint(37.7749, -122.4194)
                        controller.setCenter(locationToShow)

                        overlays.add(object : org.osmdroid.views.overlay.Overlay() {
                            override fun onSingleTapUp(
                                e: MotionEvent?,
                                mapView: org.osmdroid.views.MapView?
                            ): Boolean {
                                if (e != null && mapView != null) {
                                    val projection = mapView.projection
                                    val geoPoint = projection.fromPixels(
                                        e.x.toInt(),
                                        e.y.toInt()
                                    ) as org.osmdroid.util.GeoPoint
                                    selectedLocation = geoPoint
                                    showSearchResults = false

                                    updateMapMarker(mapView, geoPoint, "Selected Location")

                                    scope.launch {
                                        val address = getAddressFromLocation(context, geoPoint)
                                        placeName = address
                                        searchQuery = address
                                    }
                                    return true
                                }
                                return false
                            }
                        })

                        val initialMarkerLocation = selectedLocation ?: currentLocation
                        initialMarkerLocation?.let { geoPoint ->
                            updateMapMarker(
                                this, geoPoint,
                                when {
                                    selectedLocation != null -> "Selected Location"
                                    currentLocation != null -> "Current Location"
                                    else -> "Default Location"
                                }
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mapView ->
                    selectedLocation?.let { location ->
                        mapView.controller.animateTo(location)
                        if (mapView.zoomLevelDouble < 15.0) {
                            mapView.controller.setZoom(15.0)
                        }
                        updateMapMarker(
                            mapView, location,
                            if (placeName.isNotEmpty()) placeName else "Selected Location"
                        )
                    }
                }
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { query ->
                                searchQuery = query
                                if (query.length > 2) {
                                    isSearching = true
                                    scope.launch {
                                        try {
                                            val geocoder = Geocoder(context, Locale.getDefault())
                                            val addresses = withContext(Dispatchers.IO) {
                                                geocoder.getFromLocationName(query, 5)
                                            }
                                            searchResults = addresses ?: emptyList()
                                            showSearchResults = searchResults.isNotEmpty()
                                        } catch (e: Exception) {
                                            searchResults = emptyList()
                                            showSearchResults = false
                                        } finally {
                                            isSearching = false
                                        }
                                    }
                                } else {
                                    searchResults = emptyList()
                                    showSearchResults = false
                                }
                            },
                            label = { Text("Search Address") },
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                // CHANGE: Add Clear button when text exists and not searching
                                if (isSearching) {
                                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = "" // Clear the text
                                        searchResults = emptyList() // Clear results
                                        showSearchResults = false // Hide results
                                    }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear Search"
                                        )
                                    }
                                } else {
                                    Icon(Icons.Default.Search, "Search")
                                }
                            },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FloatingActionButton(
                            onClick = { /* ... (Same as before) ... */ },
                            modifier = Modifier.size(48.dp),
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                        ) {
                            if (isLoadingLocation) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.LocationOn, "My Location")
                            }
                        }
                    }

                    AnimatedVisibility(visible = showSearchResults) {
                        LazyColumn(
                            modifier = Modifier
                                .heightIn(max = 150.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                        ) {
                            items(searchResults) { address ->
                                Text(
                                    text = address.getAddressLine(0) ?: "Unknown Address",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedLocation =
                                                OsmGeoPoint(address.latitude, address.longitude)
                                            placeName =
                                                address.getAddressLine(0) ?: "Selected Location"
                                            searchQuery = address.getAddressLine(0) ?: ""
                                            showSearchResults = false

                                            mapView?.let { map ->
                                                selectedLocation?.let { geoPoint ->
                                                    map.controller.animateTo(geoPoint)
                                                    map.controller.setZoom(16.0)
                                                    updateMapMarker(map, geoPoint, placeName)
                                                }
                                            }
                                        }
                                        .padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Divider()
                            }
                        }
                    }

                    selectedLocation?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (placeName.isNotEmpty()) placeName else "Tap map or search",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Lat: ${"%.6f".format(it.latitude)}, Lng: ${"%.6f".format(it.longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Helper function to update map marker
private fun updateMapMarker(mapView: org.osmdroid.views.MapView, geoPoint: org.osmdroid.util.GeoPoint, title: String) {
    mapView.overlays.removeAll { it is org.osmdroid.views.overlay.Marker }
    val marker = org.osmdroid.views.overlay.Marker(mapView)
    marker.position = geoPoint
    marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
    marker.title = title
    mapView.overlays.add(marker)
    mapView.invalidate()
}

// Helper function for reverse geocoding
private suspend fun getAddressFromLocation(context: Context, geoPoint: org.osmdroid.util.GeoPoint): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = withContext(Dispatchers.IO) {
            geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
        }
        addresses?.firstOrNull()?.getAddressLine(0) ?: "Selected Location"
    } catch (e: Exception) {
        "Selected Location"
    }
}

// Helper function to get current location (same as before)
private fun getCurrentLocation(
    context: Context,
    onLocationReceived: (OsmGeoPoint?) -> Unit
) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    try {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onLocationReceived(null)
            return
        }

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        var bestLocation: Location? = null

        for (provider in providers) {
            if (locationManager.isProviderEnabled(provider)) {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null && (bestLocation == null || location.accuracy < bestLocation.accuracy)) {
                    bestLocation = location
                }
            }
        }

        if (bestLocation != null) {
            onLocationReceived(OsmGeoPoint(bestLocation.latitude, bestLocation.longitude))
        } else {
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    onLocationReceived(OsmGeoPoint(location.latitude, location.longitude))
                    locationManager.removeUpdates(this)
                }

                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null)
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null)
            } else {
                onLocationReceived(null)
            }
        }
    } catch (e: Exception) {
        onLocationReceived(null)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(datePickerState.selectedDateMillis)
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(timePickerState.hour, timePickerState.minute)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}