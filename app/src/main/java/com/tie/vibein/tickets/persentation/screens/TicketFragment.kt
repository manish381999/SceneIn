package com.tie.vibein.tickets.persentation.screens

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tie.vibein.R
import com.tie.vibein.databinding.FragmentTicketBinding

class TicketFragment : Fragment() {
    private var _binding: FragmentTicketBinding? = null
    private val binding get() = _binding!! // Non-nullable property for safe access


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentTicketBinding.inflate(inflater, container, false)

        return binding.root
    }

}