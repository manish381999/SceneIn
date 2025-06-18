package com.tie.vibein

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.credentials.presentation.screens.LoginActivity
import com.tie.vibein.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    // 1. Declare the modern ActivityResultLauncher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted.
            Log.d("PERMISSION_DEBUG", "Notification permission granted by user.")
        } else {
            // Permission is denied. The app can continue to function without notifications.
            Log.d("PERMISSION_DEBUG", "Notification permission denied by user.")
        }
        // After the user has responded, proceed with the splash screen logic.
        startAppFlow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start the UI animations immediately
        startAnimations()

        // Handle the permission logic
        askNotificationPermission()
    }

    private fun startAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        binding.logoImage.startAnimation(fadeIn)
        binding.appNameText.startAnimation(slideUp)
    }

    private fun askNotificationPermission() {
        // 2. This is only necessary for API level 33+ (Android 13 and higher).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if permission is already granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is already granted, proceed immediately.
                Log.d("PERMISSION_DEBUG", "Notification permission was already granted.")
                startAppFlow()
            } else {
                // Directly ask for the permission. The result will be handled by the launcher.
                Log.d("PERMISSION_DEBUG", "Requesting notification permission...")
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // For older Android versions (below 13), permission is granted by default.
            // No need to ask, just proceed.
            startAppFlow()
        }
    }

    // 3. This function contains the logic to navigate to the next screen.
    private fun startAppFlow() {
        // Use a Handler to create a delay for the splash screen effect.
        Handler(Looper.getMainLooper()).postDelayed({
            // Check login status from SharedPreferences
            val isLoggedIn = SP.getPreferences(this, SP.LOGIN_STATUS, SP.SP_FALSE) == SP.SP_TRUE

            val nextIntent = if (isLoggedIn) {
                Intent(this, BaseActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }
            startActivity(nextIntent)
            finish() // Finish SplashActivity so the user can't go back to it
        }, 3000) // 3 seconds delay
    }
}