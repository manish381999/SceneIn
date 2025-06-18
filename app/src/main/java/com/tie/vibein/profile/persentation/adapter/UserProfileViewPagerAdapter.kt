package com.tie.vibein.profile.persentation.adapter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

import com.tie.vibein.profile.persentation.screen.UserProfileConnectionsFragment
import com.tie.vibein.profile.persentation.screen.UserProfileEventsFragment
import com.tie.vibein.profile.persentation.screen.UserProfileTicketsFragment
class UserProfileViewPagerAdapter(
    activity: AppCompatActivity,
    private val userId: String
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        val fragment: Fragment = when (position) {
            0 -> UserProfileEventsFragment()
            1 -> UserProfileTicketsFragment()
            2 -> UserProfileConnectionsFragment()
            else -> throw IllegalStateException("Invalid tab position: $position")
        }
        fragment.arguments = Bundle().apply {
            putString("user_id", userId)
        }
        return fragment
    }
}

