package com.scenein

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.scenein.databinding.ActivityBaseBinding
import com.scenein.utils.EdgeToEdgeUtils
import com.scenein.utils.ThemeManager

class BaseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBaseBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)

        binding = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge
        EdgeToEdgeUtils.setUpEdgeToEdge(this)

        initComponents()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun initComponents() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        NavigationUI.setupWithNavController(binding.bottomNavigation, navController)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.createMenuItem) {
                CreateOptionsBottomSheet().show(supportFragmentManager, "CreateOptionsBottomSheet")
                return@setOnItemSelectedListener false
            } else {
                NavigationUI.onNavDestinationSelected(item, navController)
                return@setOnItemSelectedListener true
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null || !intent.hasExtra("NAVIGATE_TO")) return

        when (intent.getStringExtra("NAVIGATE_TO")) {
            "TICKETS" -> {
                binding.bottomNavigation.selectedItemId = R.id.ticketFragment
                if (intent.hasExtra("TICKETS_SUB_TAB")) {
                    val subTabIndex = intent.getIntExtra("TICKETS_SUB_TAB", 0)
                    val bundle = Bundle().apply { putInt("initial_tab_index", subTabIndex) }
                    navController.navigate(R.id.ticketFragment, bundle)
                }
            }
        }
        intent.removeExtra("NAVIGATE_TO")
    }
}
