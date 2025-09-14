package com.scenein.credentials.presentation.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.scenein.credentials.presentation.screens.OnboardingStep1Fragment
import com.scenein.credentials.presentation.screens.OnboardingStep2Fragment
import com.scenein.credentials.presentation.screens.OnboardingStep3Fragment


const val NUM_STEPS = 3

class OnboardingPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = NUM_STEPS

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OnboardingStep1Fragment()
            1 -> OnboardingStep2Fragment()
            2 -> OnboardingStep3Fragment()
            else -> throw IllegalStateException("Invalid fragment position: $position")
        }
    }
}