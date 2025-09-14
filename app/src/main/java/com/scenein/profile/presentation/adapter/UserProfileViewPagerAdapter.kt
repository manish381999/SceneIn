package com.scenein.profile.presentation.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.scenein.R
import com.scenein.profile.presentation.screen.UserProfileConnectionsFragment
import com.scenein.profile.presentation.screen.UserProfileEventsFragment
import com.scenein.profile.presentation.screen.UserProfileTicketsFragment

class UserProfileViewPagerAdapter(
    activity: FragmentActivity,
    private val userId: String,
    private val isOwnProfile: Boolean,
    private val areConnected: Boolean
) : FragmentStateAdapter(activity) {

    // These lists hold the tabs that are ACTUALLY visible for the current profile
    val tabs: MutableList<Fragment> = mutableListOf()
    val tabTitles: MutableList<String> = mutableListOf()
    val tabIcons: MutableList<Int> = mutableListOf()

    init {
        // --- This is the definitive, correct visibility logic ---

        // Tab 1: Events (is always visible for everyone)
        tabs.add(UserProfileEventsFragment.newInstance(userId))
        tabTitles.add("Events")
        tabIcons.add(R.drawable.ic_event)

        // Tab 2: Tickets (is ONLY visible if it's the user's own profile)
        if (isOwnProfile) {
            tabs.add(UserProfileTicketsFragment.newInstance(userId))
            tabTitles.add("Tickets")
            tabIcons.add(R.drawable.ic_ticket)
        }

        // Tab 3: Connections (is visible if it's the user's own profile OR if they are connected)
        if (isOwnProfile || areConnected) {
            tabs.add(UserProfileConnectionsFragment.newInstance(userId))
            tabTitles.add("Connections")
            tabIcons.add(R.drawable.ic_group_)
        }
    }

    override fun getItemCount(): Int {
        return tabs.size
    }

    override fun createFragment(position: Int): Fragment {
        return tabs[position]
    }
}