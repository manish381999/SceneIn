package com.tie.vibein.profile.persentation.screen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.tabs.TabLayoutMediator
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.R
import com.tie.vibein.databinding.FragmentProfileBinding
import com.tie.vibein.profile.data.models.MyProfileData
import com.tie.vibein.profile.data.models.PublicProfileData
import com.tie.vibein.profile.data.models.StatItem
import com.tie.vibein.profile.persentation.adapter.InterestAdapter
import com.tie.vibein.profile.persentation.adapter.ProfileViewPagerAdapter
import com.tie.vibein.profile.persentation.view_model.ProfileViewModel
import com.tie.vibein.utils.NetworkState

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var currentUserId: String
    private val tabTitles = listOf("Events", "Tickets", "Connections")
    private val tabIcons = listOf(R.drawable.ic_event, R.drawable.ic_ticket, R.drawable.ic_group_)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserId = SP.getString(requireContext(), SP.USER_ID) ?: ""
        if (currentUserId.isEmpty()) {
            Toast.makeText(requireContext(), "Session expired. Please log in.", Toast.LENGTH_SHORT).show()
            // Here you would typically navigate the user back to the login screen
            return
        }

        setupTabs()
        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Fetch the latest profile data from the server every time the fragment becomes visible.
        viewModel.fetchMyProfile(currentUserId)
    }

    private fun observeViewModel() {
        viewModel.myProfileState.observe(viewLifecycleOwner) { state ->
            // --- THIS IS THE CORRECTED LOGIC ---
            val isLoading = state is NetworkState.Loading
            binding.shimmerLayout.isVisible = isLoading

            // Show the main content ONLY when loading is finished.
            // We control the visibility of the containers that hold the real UI.
            binding.mToolBar.isVisible = !isLoading
            binding.scrollView.isVisible = !isLoading
            // --- END OF CORRECTION ---

            when (state) {
                is NetworkState.Success -> {
                    bindProfileData(state.data)
                }
                is NetworkState.Error -> {
                    Toast.makeText(requireContext(), "Error loading profile: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                is NetworkState.Loading -> {
                    // Handled by visibility properties above
                }
            }
        }
    }

    private fun bindProfileData(user: MyProfileData) {
        // Bind data to the views that actually exist in your layout
        binding.tvFullName.text = user.name
        binding.tvUserName.text = user.userName ?: user.name // Show username in the toolbar
        binding.tvAboutYou.text = user.aboutYou

        Glide.with(this)
            .load(user.profilePic)
            .placeholder(R.drawable.ic_profile_placeholder)
            .error(R.drawable.ic_profile_placeholder)
            .into(binding.ivProfile)

        // Bind the new, dynamic stats from the server
        binding.tvEventCount.text = (user.totalEventsHosting + user.totalEventsAttending).toString()
        binding.tvConnectionsCount.text = user.totalConnections.toString()
        binding.tvTicketCount.text = (user.totalTicketsSold + user.totalTicketsBought).toString()

        val flexboxLayoutManager = FlexboxLayoutManager(requireContext()).apply {
            flexDirection = FlexDirection.ROW
            flexWrap = FlexWrap.WRAP
        }
        binding.rvInterested.layoutManager = flexboxLayoutManager
        binding.rvInterested.adapter = InterestAdapter(requireContext(), user.interestNames)
    }

    private fun setupTabs() {
        val adapter = ProfileViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.customView = createTabView(tabTitles[position], tabIcons[position], position == 0)
        }.attach()

        val primaryColor = ContextCompat.getColor(requireContext(), R.color.textPrimary)
        val secondaryColor = ContextCompat.getColor(requireContext(), R.color.textSecondary)

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                tab?.customView?.findViewById<ImageView>(R.id.tab_icon)?.setColorFilter(primaryColor)
                tab?.customView?.findViewById<TextView>(R.id.tab_text)?.setTextColor(primaryColor)
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                tab?.customView?.findViewById<ImageView>(R.id.tab_icon)?.setColorFilter(secondaryColor)
                tab?.customView?.findViewById<TextView>(R.id.tab_text)?.setTextColor(secondaryColor)
            }
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupClickListeners() {

        binding.ivProfile.setOnClickListener {
            // Get the latest user data from the ViewModel's state.
            (viewModel.myProfileState.value as? NetworkState.Success)?.data?.let { user ->
                // --- THIS IS THE DEFINITIVE FIX: No more conversion! ---
                val intent = Intent(requireContext(), ProfilePicturePreviewActivity::class.java).apply {
                    putExtra(ProfilePicturePreviewActivity.EXTRA_USER_ID, user.userId.toString())
                    putExtra(ProfilePicturePreviewActivity.EXTRA_PROFILE_PIC_URL, user.profilePic)
                    putExtra(ProfilePicturePreviewActivity.EXTRA_USERNAME, user.userName)
                    putExtra(ProfilePicturePreviewActivity.EXTRA_FULL_NAME, user.name)
                }
                startActivity(intent)
            }
        }

        binding.btnEditProfile.setOnClickListener {
             val intent = Intent(requireContext(), EditProfileActivity::class.java)
             startActivity(intent)

        }
        binding.llEventCount.setOnClickListener {
            // Retrieve the data directly from the last successful LiveData state
            (viewModel.myProfileState.value as? NetworkState.Success)?.data?.let { user ->
                showStatsSheet("Event Activity", listOf(
                    StatItem(user.totalEventsHosting.toString(), "Events Hosted"),
                    StatItem(user.totalEventsAttending.toString(), "Events Attending")
                ))
            }
        }
        binding.llConnectionsCount.setOnClickListener {
            (viewModel.myProfileState.value as? NetworkState.Success)?.data?.let { user ->
                showStatsSheet("Connections", listOf(
                    StatItem(user.totalConnections.toString(), "Total Connections")
                ))
            }
        }
        binding.llTicketCount.setOnClickListener {
            (viewModel.myProfileState.value as? NetworkState.Success)?.data?.let { user ->
                showStatsSheet("Ticket Activity", listOf(
                    StatItem(user.totalTicketsSold.toString(), "Tickets Sold"),
                    StatItem(user.totalTicketsBought.toString(), "Tickets Bought")
                ))
            }
        }
    }

    private fun showStatsSheet(title: String, stats: List<StatItem>) {
        StatsBottomSheetFragment.newInstance(title, stats).show(childFragmentManager, "StatsBottomSheet")
    }


    private fun createTabView(title: String, iconRes: Int, isSelected: Boolean): View {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.custom_tab, null)
        val icon = view.findViewById<ImageView>(R.id.tab_icon)
        val text = view.findViewById<TextView>(R.id.tab_text)
        icon.setImageResource(iconRes)
        text.text = title
        val color = if (isSelected) ContextCompat.getColor(requireContext(), R.color.textPrimary) else ContextCompat.getColor(requireContext(), R.color.textSecondary)
        icon.setColorFilter(color)
        text.setTextColor(color)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}