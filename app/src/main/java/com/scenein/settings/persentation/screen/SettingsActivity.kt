package com.scenein.settings.persentation.screen

import android.content.Intent
import android.os.Bundle
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.scenein.utils.SP
import com.scenein.utils.SP.ACCOUNT_IS_PRIVATE
import com.scenein.R
import com.scenein.credentials.presentation.screens.LoginActivity
import com.scenein.databinding.ActivitySettingsBinding
import com.scenein.profile.persentation.screen.EditProfileActivity
import com.scenein.settings.data.models.SettingsItem
import com.scenein.settings.persentation.adapter.SettingsAdapter
import com.scenein.settings.persentation.view_model.SettingsViewModel
import com.scenein.tickets.persentation.screens.PayoutSetupActivity
import com.scenein.utils.NetworkState
import com.scenein.utils.ThemeManager
import com.scenein.utils.CustomAlertDialog

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var adapter: SettingsAdapter
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = SP.getString(this, SP.USER_ID) ?: ""
        if (currentUserId.isEmpty()) {
            // If for some reason the user ID is lost, we can't proceed.
            Toast.makeText(this, "Session error. Please log in again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the list when returning to the activity, e.g., to update the theme subtitle
        setupRecyclerView()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = SettingsAdapter(buildSettingsList())
        binding.rvSettings.layoutManager = LinearLayoutManager(this)
        binding.rvSettings.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.settingsActionState.observe(this) { state ->
            // Optionally show a loading state here

            if (state is NetworkState.Success) {
                val response = state.data
                // Always show the success message from the server
                Toast.makeText(this, response.message, Toast.LENGTH_LONG).show()

                // --- THE CORRECT LOGIC ---
                // The API response's 'action' key is the single source of truth.
                when (response.action) {
                    "LOGOUT_SUCCESS", "DEACTIVATE_SUCCESS" -> {
                        // The server has confirmed the action. Now, perform the full local logout.
                        clearDataAndGoToLogin()
                    }
                }
            } else if (state is NetworkState.Error) {
                Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun buildSettingsList(): List<SettingsItem> {
        val currentThemeSubtitle =
            "Current: ${ThemeManager.getCurrentTheme(this).replaceFirstChar { it.uppercase() }}"
        val isAccountPrivate = SP.getBoolean(this, ACCOUNT_IS_PRIVATE, false)
        val areNotificationsPaused = SP.getBoolean(this, SP.NOTIFICATIONS_PAUSED, false)

        return listOf(
            SettingsItem.Header("Account"),
            SettingsItem.NavigableItem(
                "Personal Details",
                "Update your name, username, and email",
                R.drawable.ic_account_circle
            ) {
                startActivity(Intent(this, EditProfileActivity::class.java))
            },

            SettingsItem.NavigableItem(
                title = "My Bookmarks",
                subtitle = "View events you've saved for later",
                iconRes = R.drawable.ic_bookmark_filled,
                action = {
                    startActivity(Intent(this, BookmarksActivity::class.java))
                }
            ),
            SettingsItem.NavigableItem(
                "Payout Methods",
                "Manage your bank account or UPI for payments",
                R.drawable.ic_payment
            ) {
                startActivity(Intent(this, PayoutSetupActivity::class.java))
            },
            SettingsItem.NavigableItem(
                "Account Deletion",
                "Permanently delete your account",
                R.drawable.ic_delete_forever
            ) {
                showAccountDeletionConfirmation()
            },
            SettingsItem.Spacer,

            SettingsItem.Header("Payments"),
            SettingsItem.NavigableItem(
                title = "Ticket Transaction History",
                subtitle = "View your ticket sales and purchases",
                iconRes = R.drawable.ic_ticket, // You'll need an 'history' icon
                action = {
                    startActivity(Intent(this, TicketTransactionHistoryActivity::class.java))
                }
            ),
            SettingsItem.Spacer,
            SettingsItem.Header("Appearance"),
            SettingsItem.NavigableItem("Theme", currentThemeSubtitle, R.drawable.ic_dark_mode) {
                showThemeChooserDialog()
            },
            SettingsItem.Spacer,

            SettingsItem.Header("Notifications"),
            SettingsItem.ToggleItem(
                "Pause All",
                "Temporarily stop all push notifications",
                R.drawable.ic_notifications_off,
                areNotificationsPaused
            ) { isChecked ->
                // --- THIS IS THE FINAL LOGIC ---
                // Save the new state to SharedPreferences so it's remembered.
                SP.saveBoolean(this, SP.NOTIFICATIONS_PAUSED, isChecked)

                // You would add logic in your MyFirebaseMessagingService to check this boolean
                // before showing a notification.
                val state = if (isChecked) "paused" else "resumed"
                Toast.makeText(this, "Notifications have been $state", Toast.LENGTH_SHORT).show()
            },
            SettingsItem.Spacer,

            SettingsItem.Header("Privacy & Safety"),
            SettingsItem.ToggleItem(
                "Private Account",
                "Only your connections can see your profile",
                R.drawable.ic_lock,
                isAccountPrivate
            ) { isChecked ->
                SP.saveBoolean(this, ACCOUNT_IS_PRIVATE, isChecked)
                // This now makes a real API call.
                viewModel.updateAccountPrivacy(isChecked)
            },
            SettingsItem.Spacer,

            SettingsItem.Header("About & Support"),
            SettingsItem.NavigableItem(
                "Privacy Policy",
                "Read how we handle your data",
                R.drawable.ic_privacy
            ) {
                openUrl("https://dreamsquad.fun/privacy.html")
            },
            SettingsItem.NavigableItem(
                "Terms & Conditions",
                "Read our rules and terms of use",
                R.drawable.ic_terms
            ) {
                openUrl("https://dreamsquad.fun/terms.html")
            },
            SettingsItem.Spacer,

            SettingsItem.LogoutItem(username = SP.getString(this, SP.USER_NAME) ?: "user") {
                showLogoutConfirmation()
            }
        )
    }

    private fun showThemeChooserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_theme_chooser, null)
        val dialog = BottomSheetDialog(this, R.style.ThemeOverlay_App_BottomSheetDialog)
        dialog.setContentView(dialogView)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.rgThemeOptions)
        when (ThemeManager.getCurrentTheme(this)) {
            ThemeManager.THEME_LIGHT -> radioGroup.check(R.id.rbLight)
            ThemeManager.THEME_DARK -> radioGroup.check(R.id.rbDark)
            else -> radioGroup.check(R.id.rbSystem)
        }
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedTheme = when (checkedId) {
                R.id.rbLight -> ThemeManager.THEME_LIGHT
                R.id.rbDark -> ThemeManager.THEME_DARK
                else -> ThemeManager.THEME_SYSTEM
            }
            ThemeManager.saveThemePreference(this, selectedTheme)
            ThemeManager.applyTheme(selectedTheme) // This will cause the activity to recreate
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showLogoutConfirmation() {
        CustomAlertDialog.show(
            context = this,
            title = "Log Out?",
            message = "Are you sure you want to log out?",
            positiveButtonText = "Log Out",
            onPositiveClick = {
                // The button click's only job is to call the ViewModel.
                // The observer will handle the rest.
                viewModel.logout()
            }
        )
    }

    private fun showAccountDeletionConfirmation() {
        CustomAlertDialog.show(
            context = this,
            title = "Deactivate Account?",
            message = "Your account will be deactivated and scheduled for deletion in 30 days.",
            positiveButtonText = "Yes, Deactivate",
            onPositiveClick = {
                // The button click's only job is to call the ViewModel.
                viewModel.deleteAccount() // Assuming you have renamed deleteAccount
            }
        )
    }


    private fun clearDataAndGoToLogin() {
        // 1. Clear all SharedPreferences.
        SP.removeAllSharedPreferences(this)

        // 2. Create an intent to go back to the LoginActivity.
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        // 3. This is a final safeguard to ensure all activities in the current task are closed.
        finishAffinity()
    }
    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Could not open link. A web browser is required.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}