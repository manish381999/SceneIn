package com.tie.vibein.profile.persentation.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.R
import com.tie.vibein.databinding.FragmentProfileBinding
import com.tie.vibein.profile.persentation.adapter.InterestAdapter
import com.tie.vibein.profile.persentation.adapter.ProfileViewPagerAdapter

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val tabTitles = listOf("Events", "Tickets", "Connections")
    private val tabIcons = listOf(
        R.drawable.ic_event,
        R.drawable.ic_ticket,
        R.drawable.ic_group_
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initComponents()
        setupTabs()
    }

    private fun initComponents() {
        val context = requireContext()

        val fullName = SP.getPreferences(context, SP.FULL_NAME)
        val userName = SP.getPreferences(context, SP.USER_NAME)
        val aboutYou = SP.getPreferences(context, SP.USER_ABOUT_YOU)
        val profilePicUrl = SP.getPreferences(context, SP.USER_PROFILE_PIC)
        val interestNames = SP.getInterestNames(context, SP.USER_INTEREST_NAMES)

        binding.tvFullName.text = fullName
        binding.tvUserName.text =userName
        binding.tvAboutYou.text = aboutYou
//        binding.tvUserName.text = userName

        Glide.with(this)
            .load(profilePicUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .into(binding.ivProfile)

        val flexboxLayoutManager = FlexboxLayoutManager(context).apply {
            flexDirection = FlexDirection.ROW
            flexWrap = FlexWrap.WRAP
        }

        binding.rvInterested.layoutManager = flexboxLayoutManager
        binding.rvInterested.adapter = InterestAdapter(context, interestNames)
    }

    private fun setupTabs() {
        val adapter = ProfileViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.customView = createTabView(tabTitles[position], tabIcons[position], position == 0)
        }.attach()

        val primaryColor = ContextCompat.getColor(requireContext(), R.color.textPrimary)
        val secondaryColor = ContextCompat.getColor(requireContext(), R.color.textSecondary)

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
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.custom_tab, null)
        val icon = view.findViewById<ImageView>(R.id.tab_icon)
        val text = view.findViewById<TextView>(R.id.tab_text)

        icon.setImageResource(iconRes)
        text.text = title

        val color = if (isSelected)
            ContextCompat.getColor(requireContext(), R.color.textPrimary)
        else
            ContextCompat.getColor(requireContext(), R.color.textSecondary)

        icon.setColorFilter(color)
        text.setTextColor(color)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
