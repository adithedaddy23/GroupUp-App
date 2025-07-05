package com.example.studygroupfinder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class LocationService(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private var locationCallback: LocationCallback? = null
    private var timeoutJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun requestCurrentLocation(
        onLocationUpdate: (GeoPoint) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // First try the modern getCurrentLocation API if available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getCurrentLocationWithModernApi(onLocationUpdate, onError)
        } else {
            // Fallback to location updates for older devices
            requestFreshLocationUpdates(onLocationUpdate, onError)
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun getCurrentLocationWithModernApi(
        onLocationUpdate: (GeoPoint) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val currentLocationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0)
            .setDurationMillis(15000)
            .build()

        try {
            if (hasLocationPermission()) {
                val cancellationToken = CancellationTokenSource()

                timeoutJob = scope.launch {
                    delay(30000)
                    cancellationToken.cancel()
                    onError(Exception("Location request timeout. Please check if location services are enabled."))
                }

                fusedLocationClient.getCurrentLocation(
                    currentLocationRequest,
                    cancellationToken.token
                ).addOnSuccessListener { location ->
                    timeoutJob?.cancel()
                    if (location != null) {
                        onLocationUpdate(GeoPoint(location.latitude, location.longitude))
                    } else {
                        requestFreshLocationUpdates(onLocationUpdate, onError)
                    }
                }.addOnFailureListener { exception ->
                    timeoutJob?.cancel()
                    requestFreshLocationUpdates(onLocationUpdate, onError)
                }
            } else {
                throw SecurityException("Location permission not granted")
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestFreshLocationUpdates(
        onLocationUpdate: (GeoPoint) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (!isLocationEnabled()) {
            onError(Exception("Location services disabled. Please enable GPS or network location."))
            return
        }

        // Try last known location first as immediate fallback
        tryLastKnownLocation(onLocationUpdate) { lastLocationSuccess ->
            if (!lastLocationSuccess) {
                // Only start location updates if last known location failed
                startLocationUpdates(onLocationUpdate, onError)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun tryLastKnownLocation(
        onLocationUpdate: (GeoPoint) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        try {
            if (hasLocationPermission()) {
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                    if (lastLocation != null) {
                        // Check if the last known location is reasonably recent (within 5 minutes)
                        val locationAge = System.currentTimeMillis() - lastLocation.time
                        val maxAge = 5 * 60 * 1000 // 5 minutes in milliseconds

                        if (locationAge < maxAge && lastLocation.accuracy < 200f) {
                            onLocationUpdate(GeoPoint(lastLocation.latitude, lastLocation.longitude))
                            onComplete(true)
                            return@addOnSuccessListener
                        }
                    }
                    onComplete(false)
                }.addOnFailureListener {
                    onComplete(false)
                }
            } else {
                onComplete(false)
            }
        } catch (e: Exception) {
            onComplete(false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(
        onLocationUpdate: (GeoPoint) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000 // Reduced interval for faster response
        ).apply {
            setMinUpdateIntervalMillis(2000) // Faster minimum interval
            setWaitForAccurateLocation(false) // Don't wait for high accuracy on older devices
            setMinUpdateDistanceMeters(0f) // Accept any location update
        }.build()

        locationCallback = object : LocationCallback() {
            private var hasReceivedLocation = false

            override fun onLocationResult(locationResult: LocationResult) {
                if (hasReceivedLocation) return // Prevent multiple calls

                locationResult.lastLocation?.let { location ->
                    // More lenient accuracy requirements for older devices
                    val acceptableAccuracy = 200f // Increased threshold for older devices

                    // Accept location if it's within acceptable accuracy OR if it's the first location after 10 seconds
                    val shouldAcceptLocation = location.accuracy <= acceptableAccuracy ||
                            location.accuracy == 0f ||
                            (System.currentTimeMillis() - location.time) < 10000

                    if (shouldAcceptLocation) {
                        hasReceivedLocation = true
                        timeoutJob?.cancel()
                        onLocationUpdate(GeoPoint(location.latitude, location.longitude))
                        stopLocationUpdates()
                    }
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                if (!locationAvailability.isLocationAvailable && !hasReceivedLocation) {
                    // Final fallback to any last known location
                    tryLastKnownLocationFallback(onLocationUpdate, onError)
                }
            }
        }

        try {
            if (hasLocationPermission()) {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback as LocationCallback,
                    Looper.getMainLooper()
                )

                // Reduced timeout for older devices (20 seconds instead of 30)
                timeoutJob = scope.launch {
                    delay(20000)
                    if (locationCallback != null) {
                        // Try one more time with last known location before failing
                        tryLastKnownLocationFallback(onLocationUpdate, onError)
                    }
                }
            } else {
                throw SecurityException("Location permission not granted")
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun tryLastKnownLocationFallback(
        onLocationUpdate: (GeoPoint) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                stopLocationUpdates()
                if (lastLocation != null) {
                    onLocationUpdate(GeoPoint(lastLocation.latitude, lastLocation.longitude))
                } else {
                    onError(Exception("Unable to get location. Please ensure location services are enabled and try again."))
                }
            }.addOnFailureListener {
                stopLocationUpdates()
                onError(Exception("Location request timeout. Please check if location services are enabled."))
            }
        } catch (e: Exception) {
            stopLocationUpdates()
            onError(e)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        timeoutJob?.cancel()
        timeoutJob = null
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

@Composable
fun rememberLocationService(): LocationService {
    val context = LocalContext.current
    return remember { LocationService(context) }
}

@Composable
fun LocationPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                when {
                    hasLocationPermission(context) -> {
                        onPermissionGranted()
                    }
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        context as Activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) -> {
                        // Show rationale and request permissions
                        launcher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }
                    else -> {
                        launcher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}