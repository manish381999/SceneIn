package com.scenein.discover.presentation.screens

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.scenein.databinding.ActivityLocationPickerBinding
import com.scenein.utils.EdgeToEdgeUtils

class LocationPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationPickerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        binding = ActivityLocationPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        // TODO: Populate the RecyclerView with a list of cities from your API
        // You'll need a new API endpoint and a simple RecyclerView adapter for this.
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupClickListeners() {
        binding.tvUseCurrentLocation.setOnClickListener {
            // Logic to get current location and save it
            // For now, we just return a success result to trigger a refresh
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}