package com.tie.vibein

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.tie.vibein.databinding.ActivityBaseBinding
import com.tie.vibein.utils.EdgeToEdgeUtils

class BaseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBaseBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this) // Correctly sets up edge-to-edge
        binding = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initComponents()

        // --- THIS IS THE DEFINITIVE FIX: Handle incoming navigation requests ---
        handleIntent(intent)
        // --- END OF FIX ---
    }

    /**
     * This function is called when a new intent is delivered to an existing activity.
     * For example, if the app is already open and the user taps a notification.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Ensure we handle the new navigation request.
        if (intent != null) {
            handleIntent(intent)
        }
    }

    private fun initComponents() {
        navController = Navigation.findNavController(this, R.id.navHostFragment)
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController)
    }

    /**
     * This is the "brain" of our deep link navigation.
     * It reads the "extras" from the Intent and performs the correct navigation action.
     */
    private fun handleIntent(intent: Intent) {
        // Check if the intent has our custom "NAVIGATE_TO" extra
        if (intent.hasExtra("NAVIGATE_TO")) {
            when (intent.getStringExtra("NAVIGATE_TO")) {
                "TICKETS" -> {
                    // Navigate to the TicketsFragment (R.id.ticketFragment is from your nav_graph.xml)
                    binding.bottomNavigation.selectedItemId = R.id.ticketFragment

                    // Now, check if there's a sub-tab to select
                    if (intent.hasExtra("TICKETS_SUB_TAB")) {
                        val subTabIndex = intent.getIntExtra("TICKETS_SUB_TAB", 0)
                        // To communicate this to the TicketFragment, we will need to
                        // pass this as an argument in the navigation action.
                        // For simplicity now, we can rely on a shared object or a
                        // temporary static variable if you don't use SafeArgs.

                        // Example of a simple way to pass this info:
                        val bundle = Bundle().apply {
                            putInt("initial_tab_index", subTabIndex)
                        }
                        navController.navigate(R.id.ticketFragment, bundle)
                    }
                }

//                "CHATS" -> {
//                    binding.bottomNavigation.selectedItemId = R.id.chatFragment // Or your correct ID
//                }
                // Add more cases for other destinations like "PROFILE", "EVENTS" etc.
            }

            // Clear the extra so it's not processed again if the activity is recreated
            intent.removeExtra("NAVIGATE_TO")
        }
    }
}