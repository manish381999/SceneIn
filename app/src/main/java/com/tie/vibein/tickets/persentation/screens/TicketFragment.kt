package com.tie.vibein.tickets.persentation.screens

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tie.vibein.databinding.FragmentTicketBinding

/**
 * This fragment currently displays a "Coming Soon" message for the ticket marketplace feature.
 * The real functionality (ViewPager, Tabs, FAB) can be added back later once the
 * payment gateway is fully activated.
 */
class TicketFragment : Fragment() {

    private var _binding: FragmentTicketBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment using view binding.
        _binding = FragmentTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // No logic is needed here for now, as it's just a static display.
        // When you re-enable the feature, your setupViewPagerWithTabs() and
        // setupClickListeners() calls will go here.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Important: Clear the binding reference to avoid memory leaks.
    }
}