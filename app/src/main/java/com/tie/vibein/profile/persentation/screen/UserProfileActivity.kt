package com.tie.vibein.profile.persentation.screen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.R
import com.tie.vibein.chat.persentation.screens.ChatActivity
import com.tie.vibein.databinding.ActivityUserProfileBinding
import com.tie.vibein.profile.data.models.ConnectionStatusResponse
import com.tie.vibein.profile.data.models.MyProfileData
import com.tie.vibein.profile.data.models.PublicProfileData
import com.tie.vibein.profile.data.models.StatItem
import com.tie.vibein.profile.persentation.adapter.InterestAdapter
import com.tie.vibein.profile.persentation.adapter.UserProfileViewPagerAdapter
import com.tie.vibein.profile.persentation.view_model.ProfileViewModel
import com.tie.vibein.utils.EdgeToEdgeUtils
import com.tie.vibein.utils.NetworkState

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private var profileUserId: String? = null
    private lateinit var currentUserId: String
    private val profileViewModel: ProfileViewModel by viewModels()
    private var currentConnectionStatus: ConnectionStatusResponse? = null

    private var lastFetchedPublicUser: PublicProfileData? = null
    private var lastFetchedMyUser: MyProfileData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = SP.getString(this, SP.USER_ID) ?: ""
        profileUserId = intent.getStringExtra("user_id")

        initComponents()
        observeViewModels()
        onClickListener()
    }

    private fun initComponents() {
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Session expired. Please log in.", Toast.LENGTH_LONG).show()
            finish(); return
        }
        val isViewingOwnProfile = profileUserId.isNullOrEmpty() || profileUserId == currentUserId

        if (isViewingOwnProfile) {
            updateButtonVisibility(isOwnProfile = true)
            profileViewModel.fetchMyProfile(currentUserId)
        } else {
            val targetUserId = profileUserId!!
            profileViewModel.fetchPublicUserProfile(targetUserId)
            profileViewModel.checkConnectionStatus(viewerId = currentUserId, profileId = targetUserId)
        }
    }

    private fun observeViewModels() {
        profileViewModel.myProfileState.observe(this) { state ->
            val isLoading = state is NetworkState.Loading
            binding.shimmerLayout.isVisible = isLoading
            if (isLoading) binding.shimmerLayout.startShimmer() else binding.shimmerLayout.stopShimmer()
            binding.constraintLayout.isVisible = !isLoading

            if (state is NetworkState.Success) {
                lastFetchedMyUser = state.data
                bindMyProfileData(state.data)
                setupTabs(areConnected = true) // Always show all tabs for my own profile
            } else if (state is NetworkState.Error) {
                Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
            }
        }

        profileViewModel.publicProfileState.observe(this) { state ->
            val isLoading = state is NetworkState.Loading
            binding.shimmerLayout.isVisible = isLoading
            if(isLoading) binding.shimmerLayout.startShimmer() else binding.shimmerLayout.stopShimmer()
            binding.constraintLayout.isVisible = !isLoading

            if(state is NetworkState.Success) {
                lastFetchedPublicUser = state.data
                bindPublicProfileData(state.data)
            } else if (state is NetworkState.Error) {
                Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
            }
        }

        profileViewModel.connectionStatusState.observe(this) { state ->
            if (state is NetworkState.Success) {
                currentConnectionStatus = state.data
                updateButtonVisibility(isOwnProfile = false, status = state.data)
                setupTabs(state.data.connectionStatus == "accepted")
            } else if (state is NetworkState.Loading) {
                // To prevent button flicker, just update the text
                binding.btnConnect.isEnabled = false
                binding.btnConnect.text = "Loading..."
            }
        }

        profileViewModel.connectionActionState.observe(this) { state ->
            if (state is NetworkState.Success) {
                Toast.makeText(this, state.data, Toast.LENGTH_SHORT).show()
                // After an action, always re-check the status to update the button UI
                if (!profileUserId.isNullOrEmpty()) {
                    profileViewModel.checkConnectionStatus(currentUserId, profileUserId!!)
                }
            } else if (state is NetworkState.Error) {
                Toast.makeText(this, "Action Failed: ${state.message}", Toast.LENGTH_SHORT).show()
                // Re-enable connect button on failure to allow user to try again
                binding.btnConnect.isEnabled = true
            }
        }
    }

    private fun bindMyProfileData(user: MyProfileData) {
        binding.tvUserName.text = user.userName
        binding.tvFullName.text = user.name
        binding.tvAboutYou.text = user.aboutYou
        Glide.with(this).load(user.profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.ivProfile)
        binding.tvEventCount.text = (user.totalEventsHosting + user.totalEventsAttending).toString()
        binding.tvConnectionsCount.text = user.totalConnections.toString()
        binding.tvTicketCount.text = (user.totalTicketsSold + user.totalTicketsBought).toString()

        val flexboxLayoutManager = FlexboxLayoutManager(this).apply {
            flexDirection = FlexDirection.ROW
            flexWrap = FlexWrap.WRAP
        }
        binding.rvInterested.layoutManager = flexboxLayoutManager
        binding.rvInterested.adapter = InterestAdapter(this, user.interestNames)
    }

    private fun bindPublicProfileData(user: PublicProfileData) {
        binding.tvUserName.text = user.userName
        binding.tvFullName.text = user.name
        binding.tvAboutYou.text = user.aboutYou
        Glide.with(this).load(user.profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.ivProfile)
        binding.tvEventCount.text = (user.totalEventsHosting + user.totalEventsAttending).toString()
        binding.tvConnectionsCount.text = user.totalConnections.toString()
        binding.tvTicketCount.text = (user.totalTicketsSold + user.totalTicketsBought).toString()

        val flexboxLayoutManager = FlexboxLayoutManager(this).apply {
            flexDirection = FlexDirection.ROW
            flexWrap = FlexWrap.WRAP
        }
        binding.rvInterested.layoutManager = flexboxLayoutManager
        binding.rvInterested.adapter = InterestAdapter(this, user.interestNames)
    }

    private fun updateButtonVisibility(isOwnProfile: Boolean, status: ConnectionStatusResponse? = null) {
        // Reset all button container visibilities
        binding.singleButtonLayout.isVisible = false
        binding.responseButtonsLayout.isVisible = false
        binding.btnEditProfile.isVisible = false
        binding.btnMessage.isVisible = false

        if (isOwnProfile) {
            binding.btnEditProfile.isVisible = true

            return // End function here for own profile
        }

        // Logic for viewing other profiles
        binding.btnConnect.isEnabled = true // Re-enable button by default
        when (status?.connectionStatus) {
            "none", "declined" -> {
                binding.singleButtonLayout.isVisible = true
                binding.btnConnect.text = "Connect"
                binding.btnMessage.isVisible = true
            }
            "pending" -> {
                if (status.requestSentBy == "me") {
                    binding.singleButtonLayout.isVisible = true
                    binding.btnConnect.text = "Cancel Request"
                    binding.btnMessage.isVisible = true
                } else {
                    binding.responseButtonsLayout.isVisible = true
                }
            }
            "accepted" -> {
                binding.singleButtonLayout.isVisible = true
                binding.btnConnect.text = "Connected"
                binding.btnMessage.isVisible = true
            }
            "error" -> {
                binding.singleButtonLayout.isVisible = true
                binding.btnConnect.text = "Connect"
            }
        }
    }

    private fun onClickListener() {

        binding.ivProfile.setOnClickListener {
            // Use the locally stored PublicProfileData object
            lastFetchedPublicUser?.let { user ->
                // --- THIS IS THE DEFINITIVE FIX: No more conversion! ---
                val intent = Intent(this, ProfilePicturePreviewActivity::class.java).apply {
                    putExtra(ProfilePicturePreviewActivity.EXTRA_USER_ID, user.userId.toString())
                    putExtra(ProfilePicturePreviewActivity.EXTRA_PROFILE_PIC_URL, user.profilePic)
                    putExtra(ProfilePicturePreviewActivity.EXTRA_USERNAME, user.userName)
                    putExtra(ProfilePicturePreviewActivity.EXTRA_FULL_NAME, user.name)
                }
                startActivity(intent)
            }
        }

        binding.llEventCount.setOnClickListener {
            lastFetchedMyUser?.let { showStatsSheet("Event Activity", listOf(StatItem(it.totalEventsHosting.toString(), "Events Hosted"), StatItem(it.totalEventsAttending.toString(), "Events Attending"))) }
                ?: lastFetchedPublicUser?.let { showStatsSheet("Event Activity", listOf(StatItem(it.totalEventsHosting.toString(), "Events Hosted"), StatItem(it.totalEventsAttending.toString(), "Events Attending"))) }
        }

        binding.llConnectionsCount.setOnClickListener {
            lastFetchedMyUser?.let { showStatsSheet("Connections", listOf(StatItem(it.totalConnections.toString(), "Total Connections"))) }
                ?: lastFetchedPublicUser?.let { showStatsSheet("Connections", listOf(StatItem(it.totalConnections.toString(), "Total Connections"))) }
        }

        binding.llTicketCount.setOnClickListener {
            lastFetchedMyUser?.let { showStatsSheet("Ticket Activity", listOf(StatItem(it.totalTicketsSold.toString(), "Tickets Sold"), StatItem(it.totalTicketsBought.toString(), "Tickets Bought"))) }
                ?: lastFetchedPublicUser?.let { showStatsSheet("Ticket Activity", listOf(StatItem(it.totalTicketsSold.toString(), "Tickets Sold"), StatItem(it.totalTicketsBought.toString(), "Tickets Bought"))) }
        }

        binding.btnConnect.setOnClickListener {
            val pId = profileUserId ?: return@setOnClickListener
            when (currentConnectionStatus?.connectionStatus) {
                "none", "declined" -> profileViewModel.sendConnectionRequest(currentUserId, pId)
                "pending" -> { if (currentConnectionStatus?.requestSentBy == "me") { profileViewModel.removeConnection(currentUserId, pId) } }
                "accepted" -> { showDisconnectBottomSheet() }
            }
        }

        binding.btnMessage.setOnClickListener {
            val pId = profileUserId ?: return@setOnClickListener
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("receiver_id", pId)
                val name = lastFetchedPublicUser?.name ?: lastFetchedMyUser?.name ?: "Chat"
                val pic = lastFetchedPublicUser?.profilePic ?: lastFetchedMyUser?.profilePic ?: ""
                putExtra("receiver_name", name)
                putExtra("receiver_profile_pic", pic)
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

        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }



        binding.ivSetting.setOnClickListener {
            // TODO: Implement navigation to your SettingsActivity
            Toast.makeText(this, "Settings Clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showStatsSheet(title: String, stats: List<StatItem>) {
        StatsBottomSheetFragment.newInstance(title, stats).show(supportFragmentManager, "StatsBottomSheetFragment")
    }

    private fun showDisconnectBottomSheet() {
        val userName = binding.tvFullName.text.toString()
        val bottomSheet = DisconnectConfirmationBottomSheet.newInstance(userName)
        bottomSheet.onDisconnectConfirmed = {
            profileUserId?.let { profileViewModel.removeConnection(currentUserId, it) }
        }
        bottomSheet.show(supportFragmentManager, "DisconnectConfirmationBottomSheet")
    }

    private fun setupTabs(areConnected: Boolean) {
        val isViewingOwnProfile = profileUserId.isNullOrEmpty() || profileUserId == currentUserId
        val targetUserId = profileUserId ?: currentUserId

        // This check prevents recreating the adapter if it already exists
        if (binding.viewPager.adapter == null) {
            val viewPagerAdapter = UserProfileViewPagerAdapter(this, targetUserId, isViewingOwnProfile, areConnected)
            binding.viewPager.adapter = viewPagerAdapter

            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                tab.customView = createTabView(viewPagerAdapter.tabTitles[position], viewPagerAdapter.tabIcons[position], position == 0)
            }.attach()

            binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                val primaryColor = ContextCompat.getColor(this@UserProfileActivity, R.color.textPrimary)
                val secondaryColor = ContextCompat.getColor(this@UserProfileActivity, R.color.textSecondary)
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