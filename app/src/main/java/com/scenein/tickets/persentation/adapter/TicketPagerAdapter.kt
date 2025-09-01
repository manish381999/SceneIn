package com.scenein.tickets.persentation.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.scenein.tickets.persentation.screens.BrowseTicketsFragment
import com.scenein.tickets.persentation.screens.MyTicketsFragment

class TicketPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> BrowseTicketsFragment()
            1 -> MyTicketsFragment()
            else -> throw IllegalStateException("Invalid pager position")
        }
    }
}