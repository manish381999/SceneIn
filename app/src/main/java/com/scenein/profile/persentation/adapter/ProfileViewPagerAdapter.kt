package com.scenein.profile.persentation.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.scenein.profile.persentation.screen.ConnectionsFragment
import com.scenein.profile.persentation.screen.EventsFragment
import com.scenein.profile.persentation.screen.TicketsFragment

class ProfileViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> EventsFragment()
            1 -> TicketsFragment()
            2 -> ConnectionsFragment()
            else -> throw IllegalStateException("Invalid tab position: $position")
        }
    }
}
