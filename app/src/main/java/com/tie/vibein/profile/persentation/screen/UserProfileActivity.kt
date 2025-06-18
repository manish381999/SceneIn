package com.tie.vibein.profile.persentation.screen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog // Make sure this is imported
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.R
import com.tie.vibein.chat.persentation.screens.ChatActivity
import com.tie.vibein.databinding.ActivityUserProfileBinding
import com.tie.vibein.discover.data.models.GetUsersResponse
import com.tie.vibein.discover.presentation.viewmodel.DiscoverViewModel
import com.tie.vibein.profile.data.models.ConnectionStatusResponse
import com.tie.vibein.profile.persentation.adapter.InterestAdapter
import com.tie.vibein.profile.persentation.adapter.UserProfileViewPagerAdapter
import com.tie.vibein.profile.presentation.viewmodel.ProfileViewModel
import com.tie.vibein.utils.NetworkState

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private var profileUserId: String? = null
    private lateinit var currentUserId: String
    private val discoverViewModel: DiscoverViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private var currentConnectionStatus: ConnectionStatusResponse? = null
    private var userDetails: GetUsersResponse.User? = null
    private val tabTitles = listOf("Events", "Tickets", "Connections")
    private val tabIcons = listOf(R.drawable.ic_event, R.drawable.ic_ticket, R.drawable.ic_group_)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = SP.getPreferences(this, SP.USER_ID) ?: ""
        profileUserId = intent.getStringExtra("user_id")

        initComponents()
        setupTabs()
        observeViewModels()
        onClickListener()
    }

    private fun initComponents() {
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Error: You are not logged in.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (profileUserId == null || profileUserId == currentUserId) {
            discoverViewModel.fetchUserDetailsById(currentUserId, 1, 1)
        } else {
            discoverViewModel.fetchUserDetailsById(profileUserId!!, 1, 3)
            profileViewModel.checkConnectionStatus(viewerId = currentUserId, profileId = profileUserId!!)
        }
    }

    private fun observeViewModels() {
        // Observer for fetching the user's profile details (no changes here)
        discoverViewModel.userDetailsState.observe(this) { state ->
            when (state) {
                is NetworkState.Loading -> { /* ... */ }
                is NetworkState.Success -> {
                    if (state.data.isNotEmpty()) {
                        userDetails = state.data[0] // Save the user details here
                        bindUserDetails(state.data[0])
                    } else {
                        Toast.makeText(this, "No user found.", Toast.LENGTH_SHORT).show()
                    }
                }
                is NetworkState.Error -> {
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // --- OBSERVER IS CORRECTED TO HANDLE BUTTON STATES ---
        profileViewModel.connectionStatusState.observe(this) { state ->
            when (state) {
                is NetworkState.Loading -> {
                    // During loading, show a disabled "Loading..." button
                    binding.singleButtonLayout.visibility = View.VISIBLE
                    binding.responseButtonsLayout.visibility = View.GONE
                    binding.btnEditProfile.visibility = View.GONE
                    binding.btnConnect.isEnabled = false
                    binding.btnConnect.text = "Loading..."
                }
                is NetworkState.Success -> {
                    // Once successful, re-enable the button and update the UI
                    binding.btnConnect.isEnabled = true
                    currentConnectionStatus = state.data
                    updateConnectButtonUI(state.data)
                }
                is NetworkState.Error -> {
                    // On error, re-enable the button and reset to a default state
                    binding.btnConnect.isEnabled = true
                    updateConnectButtonUI(ConnectionStatusResponse("error", "none", null, null))
                    Toast.makeText(this, "Error checking status: ${state.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observer for the result of an action (connect/disconnect) (no changes here)
        profileViewModel.connectionActionState.observe(this) { state ->
            when (state) {
                is NetworkState.Success -> Toast.makeText(this, state.data, Toast.LENGTH_SHORT).show()
                is NetworkState.Error -> Toast.makeText(this, "Action Failed: ${state.message}", Toast.LENGTH_SHORT).show()
                else -> Unit
            }
        }
    }

    private fun bindUserDetails(user: GetUsersResponse.User) {
        binding.tvUserName.text = user.user_name
        binding.tvFullName.text = user.name
        binding.tvAboutYou.text = user.about_you
        Glide.with(this).load(user.profile_pic).placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).into(binding.ivProfile)
        val totalEvents = user.total_events_hosting + user.total_events_attending
        binding.tvEventCount.text = totalEvents.toString()
        binding.tvConnectionsCount.text = "0"
        binding.tvTicketCount.text = "0"
        binding.rvInterested.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvInterested.adapter = InterestAdapter(this, user.interest)
    }

    private fun updateConnectButtonUI(status: ConnectionStatusResponse) {
        // Hide all interactive layouts first for a clean slate
        binding.singleButtonLayout.visibility = View.GONE
        binding.responseButtonsLayout.visibility = View.GONE
        binding.btnEditProfile.visibility = View.GONE
        binding.btnMessage.visibility = View.GONE

        // Case 1: The user is viewing their own profile
        if (profileUserId == null || profileUserId == currentUserId) {
            binding.btnEditProfile.visibility = View.VISIBLE
            return // Nothing more to do
        }

        // Case 2: The user is viewing someone else's profile
        when (status.connectionStatus) {
            "none" -> {
                // Not connected yet
                binding.singleButtonLayout.visibility = View.VISIBLE
                binding.btnConnect.text = "Connect"
                binding.btnMessage.visibility = View.VISIBLE // <--- CHANGE: Show message button
            }
            "pending" -> {
                if (status.requestSentBy == "me") {
                    // I have sent a request
                    binding.singleButtonLayout.visibility = View.VISIBLE
                    binding.btnConnect.text = "Cancel Request"
                    binding.btnMessage.visibility = View.VISIBLE // <--- CHANGE: Show message button
                } else {
                    // They have sent me a request
                    binding.responseButtonsLayout.visibility = View.VISIBLE
                    // We keep the message button hidden here to avoid clutter with Accept/Decline
                }
            }
            "accepted" -> {
                // We are connected
                binding.singleButtonLayout.visibility = View.VISIBLE
                binding.btnConnect.text = "Connected"
                binding.btnMessage.visibility = View.VISIBLE // <--- This was already correct
            }
            "error", "declined" -> {
                // In case of an error or a declined request, treat it as "none"
                binding.singleButtonLayout.visibility = View.VISIBLE
                binding.btnConnect.text = "Connect"
                binding.btnMessage.visibility = View.VISIBLE // <--- CHANGE: Show message button
            }
        }
    }

    private fun onClickListener() {
        binding.ivSetting.setOnClickListener { /* ... */ }

        binding.btnConnect.setOnClickListener {
            val pId = profileUserId ?: return@setOnClickListener
            when (currentConnectionStatus?.connectionStatus) {
                "none" -> profileViewModel.sendConnectionRequest(senderId = currentUserId, receiverId = pId)
                "pending" -> {
                    if (currentConnectionStatus?.requestSentBy == "me") {
                        profileViewModel.removeConnection(currentUserId = currentUserId, userToDisconnectId = pId)
                    }
                }
                "accepted" -> {
                    // Clicking "Connected" triggers the disconnect confirmation
                    showDisconnectBottomSheet()
                }
            }
        }



        binding.btnMessage.setOnClickListener {
            val pId = profileUserId ?: return@setOnClickListener
            // Use the saved userDetails object to pass full info
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("receiver_id", pId)
                putExtra("receiver_name", userDetails?.name ?: "Chat")
                putExtra("receiver_profile_pic", userDetails?.profile_pic ?: "")
            }
            startActivity(intent)
        }


        binding.btnAccept.setOnClickListener {
            val connId = currentConnectionStatus?.connectionId?.toString() ?: return@setOnClickListener
            val pId = profileUserId ?: return@setOnClickListener
            profileViewModel.respondToConnectionRequest(connId, "accepted", currentUserId, pId)
        }

        binding.btnDecline.setOnClickListener {
            val connId = currentConnectionStatus?.connectionId?.toString() ?: return@setOnClickListener
            val pId = profileUserId ?: return@setOnClickListener
            profileViewModel.respondToConnectionRequest(connId, "declined", currentUserId, pId)
        }

        binding.btnEditProfile.setOnClickListener { /* ... */ }
    }
    private fun showDisconnectBottomSheet() {
        val userName = binding.tvFullName.text.toString()
        val bottomSheet = DisconnectConfirmationBottomSheet.newInstance(userName)

        // Set the listener for the confirmation action
        bottomSheet.onDisconnectConfirmed = onDisconnectConfirmed@{
            val pId = profileUserId ?: return@onDisconnectConfirmed
            profileViewModel.removeConnection(
                currentUserId = currentUserId,
                userToDisconnectId = pId
            )
        }

        // Show the bottom sheet
        bottomSheet.show(supportFragmentManager, "DisconnectConfirmationBottomSheet")
    }

    private fun setupTabs() {
        val adapter = UserProfileViewPagerAdapter(this, profileUserId ?: currentUserId)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.customView = createTabView(tabTitles[position], tabIcons[position], position == 0)
        }.attach()
        val primaryColor = ContextCompat.getColor(this, R.color.textPrimary)
        val secondaryColor = ContextCompat.getColor(this, R.color.textSecondary)
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.customView?.findViewById<ImageView>(R.id.tab_icon)?.setColorFilter(primaryColor)
                tab?.customView?.findViewById<TextView>(R.id.tab_text)?.setTextColor(primaryColor)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.customView?.findViewById<ImageView>(R.id.tab_icon)?.setColorFilter(secondaryColor)
                tab?.customView?.findViewById<TextView>(R.id.tab_text)?.setTextColor(secondaryColor)
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun createTabView(title: String, iconRes: Int, isSelected: Boolean): View {
        val view = LayoutInflater.from(this).inflate(R.layout.custom_tab, null)
        val icon = view.findViewById<ImageView>(R.id.tab_icon)
        val text = view.findViewById<TextView>(R.id.tab_text)
        icon.setImageResource(iconRes)
        text.text = title
        val color = if (isSelected) ContextCompat.getColor(this, R.color.textPrimary) else ContextCompat.getColor(this, R.color.textSecondary)
        icon.setColorFilter(color)
        text.setTextColor(color)
        return view
    }
}