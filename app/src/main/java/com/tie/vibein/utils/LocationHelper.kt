package com.tie.vibein.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import java.util.*

class LocationHelper(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Global variables for location
    var latitude: Double? = null
    var longitude: Double? = null
    var city: String? = null
    var country: String? = null
    var pincode: String? = null

    sealed class LocationResult {
        data class Success(
            val latitude: Double,
            val longitude: Double,
            val city: String?,
            val country: String?,
            val pincode: String?
        ) : LocationResult()

        data class Error(val message: String) : LocationResult()
    }

    @SuppressLint("MissingPermission")
    fun fetchLocation(callback: (LocationResult) -> Unit) {
        if (!hasLocationPermission()) {
            callback(LocationResult.Error("Location permission not granted"))
            return
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = Priority.PRIORITY_HIGH_ACCURACY
            maxWaitTime = 15000
            numUpdates = 1
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                    val location: Location? = locationResult.lastLocation
                    if (location != null) {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addressList = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                        val address = addressList?.firstOrNull()
                        city = address?.locality
                        country = address?.countryName
                        pincode = address?.postalCode

                        // Set global variables
                        latitude = location.latitude
                        longitude = location.longitude

                        // Log location info
                        Log.d("LocationHelper", "Latitude: $latitude, Longitude: $longitude, City: $city, Country: $country, Pincode: $pincode")

                        callback(
                            LocationResult.Success(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                city = city,
                                country = country,
                                pincode = pincode
                            )
                        )
                    } else {
                        callback(LocationResult.Error("Unable to retrieve location"))
                    }
                }
            },
            Looper.getMainLooper()
        )
    }

    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationPermission(activity: Activity, requestCode: Int) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            requestCode
        )
    }
}
