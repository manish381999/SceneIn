package com.tie.vibein.tickets.persentation.screens

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import com.tie.vibein.databinding.FragmentTicketBinding
import com.tie.vibein.tickets.persentation.adapter.TicketPagerAdapter

class TicketFragment : Fragment() {
    private var _binding: FragmentTicketBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPagerWithTabs()
        setupClickListeners()

        // --- THIS IS THE FINAL PIECE OF THE LOGIC ---
        // Check if the arguments bundle (passed from BaseActivity) contains our index.
        val initialTabIndex = arguments?.getInt("initial_tab_index", 0) ?: 0

        // After a small delay to ensure the UI is fully laid out,
        // switch to the requested tab.
        binding.viewPager.post {
            binding.viewPager.setCurrentItem(initialTabIndex, false) // false for no smooth scroll
        }
        // --- END OF LOGIC ---
    }

    private fun setupViewPagerWithTabs() {
        binding.viewPager.adapter = TicketPagerAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Browse Tickets"
                1 -> "My Tickets"
                else -> null
            }
        }.attach()
    }

    private fun setupClickListeners() {
        binding.fabSellTicket.setOnClickListener {
            startActivity(Intent(requireContext(), SellTicketActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}