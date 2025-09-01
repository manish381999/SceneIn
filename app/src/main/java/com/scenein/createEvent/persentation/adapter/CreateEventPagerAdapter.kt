package com.scenein.createEvent.persentation.adapter // (Or your correct package)

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.scenein.createEvent.persentation.screens.Step1BasicsFragment
import com.scenein.createEvent.persentation.screens.Step2TimePlaceFragment
import com.scenein.createEvent.persentation.screens.Step3DetailsFragment
import com.scenein.createEvent.persentation.screens.Step4MediaFragment

const val TOTAL_STEPS = 4

// --- THE FIX IS HERE: The constructor now takes a FragmentActivity ---
class CreateEventPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = TOTAL_STEPS

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> Step1BasicsFragment()
            1 -> Step2TimePlaceFragment()
            2 -> Step3DetailsFragment()
            else -> Step4MediaFragment()
        }
    }
}