package com.scenein.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import java.util.*

class LocationHelper(private val context: Context) {

    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // A single, comprehensive data class for the location result
    sealed class LocationResult {
        data class Success(
            val location: android.location.Location,
            val latitude: Double,
            val longitude: Double,
            val city: String?,
            val locality: String?,
            val subLocality: String?,
            val country: String?,
            val pincode: String?
        ) : LocationResult()
        data class Error(val message: String) : LocationResult()
    }

    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationPermission(fragment: Fragment, requestCode: Int) {
        fragment.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode)
    }

    // In LocationHelper.kt

    fun fetchLocation(callback: (LocationResult) -> Unit) {
        if (!hasLocationPermission()) {
            callback(LocationResult.Error("Location permission not granted."))
            return
        }

        val locationRequest = LocationRequest.create().apply {
            priority = Priority.PRIORITY_HIGH_ACCURACY
            numUpdates = 1
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                        fusedLocationClient.removeLocationUpdates(this)
                        val location = locationResult.lastLocation ?: run {
                            callback(LocationResult.Error("Failed to get last location."))
                            return
                        }
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            if (addresses.isNullOrEmpty()) {
                                callback(LocationResult.Error("No address found for coordinates."))
                                return
                            }

                            val address = addresses[0]

                            // --- THIS IS THE FIX ---
                            // 1. Create the 'resultLocation' variable from the address data
                            val resultLocation = android.location.Location("Geocoder").apply {
                                latitude = address.latitude
                                longitude = address.longitude
                            }

                            callback(
                                LocationResult.Success(
                                    location = resultLocation, // 2. Pass the created variable here
                                    latitude = address.latitude,
                                    longitude = address.longitude,
                                    city = address.locality,
                                    locality = address.featureName,
                                    subLocality = address.subLocality,
                                    country = address.countryName,
                                    pincode = address.postalCode
                                )
                            )
                        } catch (e: Exception) {
                            callback(LocationResult.Error("Geocoder service failed: ${e.message}"))
                        }
                    }
                },
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            callback(LocationResult.Error("Location permission error: ${e.message}"))
        }
    }
}