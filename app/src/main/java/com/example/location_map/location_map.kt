package com.example.location_map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import android.location.Geocoder
import android.os.Build
import java.util.Locale

@Composable
fun LocationMapScreen(modifier: Modifier = Modifier) {

    val defaultLocation = LatLng(42.3505, -71.1054)

    val context = LocalContext.current

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var userLocation by remember { mutableStateOf(defaultLocation) }

    var addressText by remember { mutableStateOf("Address will appear here") }

    var customMarkers by remember { mutableStateOf(listOf<LatLng>()) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    fun loadAddress(latLng: LatLng) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { list ->
                    if (list.isNotEmpty()) {
                        addressText = list[0].getAddressLine(0) ?: "No address found"
                    } else {
                        addressText = "No address found"
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val list = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (!list.isNullOrEmpty()) {
                    addressText = list[0].getAddressLine(0) ?: "No address found"
                } else {
                    addressText = "No address found"
                }
            }
        } catch (e: Exception) {
            addressText = "Error getting address"
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Button(
            onClick = {
                if (!hasPermission) {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val latLng = LatLng(location.latitude, location.longitude)
                            userLocation = latLng
                            cameraPositionState.position =
                                CameraPosition.fromLatLngZoom(latLng, 16f)
                            loadAddress(latLng)
                        }
                    }
                }
            }
        ) {
            Text("Get My Location")
        }

        Text(
            text = addressText,
            modifier = Modifier.padding(8.dp)
        )

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                customMarkers = customMarkers + latLng
            }
        ) {

            val markerState = remember { MarkerState(position = userLocation) }
            markerState.position = userLocation

            Marker(
                state = markerState,
                title = "My Location"
            )

            customMarkers.forEachIndexed { index, latLng ->
                Marker(
                    state = MarkerState(position = latLng),
                    title = "Custom Marker ${index + 1}"
                )
            }
        }
    }
}