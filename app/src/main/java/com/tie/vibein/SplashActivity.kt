package com.tie.vibein

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.credentials.presentation.screens.LoginActivity
import com.tie.vibein.utils.EdgeToEdgeUtils

// Assuming BaseActivity is in this package, otherwise add the correct import
// import com.tie.vibein.main.BaseActivity

class SplashActivity : AppCompatActivity() {

    // This flag determines if the splash screen should remain visible.
    private var keepSplashScreenOn = true

    // Modern ActivityResultLauncher for handling the permission request result.
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("PERMISSION_DEBUG", "Notification permission granted by user.")
        } else {
            Log.d("PERMISSION_DEBUG", "Notification permission denied by user.")
        }
        // Permission dialog has been dismissed, so we can proceed.
        navigateToNextScreen()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install the splash screen. This MUST be called before super.onCreate().
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        // 2. Set the condition to keep the splash screen visible.
        // It will stay on screen as long as `keepSplashScreenOn` is true.
        splashScreen.setKeepOnScreenCondition { keepSplashScreenOn }

        // 3. Start the permission check and navigation logic.
        handleAppStartup()
    }

    private fun handleAppStartup() {
        // For Android 13 (API 33) and above, we need to request notification permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if the permission has already been granted.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // Permission is already granted, proceed directly.
                Log.d("PERMISSION_DEBUG", "Notification permission was already granted.")
                navigateToNextScreen()
            } else {
                // Permission has not been granted, so request it.
                // The splash screen will remain visible until the user responds.
                Log.d("PERMISSION_DEBUG", "Requesting notification permission...")
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // For older Android versions (below 13), no runtime permission is needed.
            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        // We are now ready to move to the next screen, so we can stop keeping the splash screen on.
        keepSplashScreenOn = false

        // Check login status from SharedPreferences.
        val isLoggedIn = SP.getString(this, SP.LOGIN_STATUS, SP.SP_FALSE) == SP.SP_TRUE

        val nextIntent = if (isLoggedIn) {
            // Make sure you have a class named BaseActivity
            Intent(this, BaseActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }

        startActivity(nextIntent)
        finish() // Finish SplashActivity so the user cannot navigate back to it.
    }
}