package com.scenein


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.scenein.createEvent.persentation.screens.CreateEventActivity
import com.scenein.databinding.BottomSheetCreateOptionsBinding
import com.scenein.tickets.persentation.screens.SellTicketActivity

class CreateOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCreateOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCreateOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionCreateEvent.setOnClickListener {
            // --- THIS IS THE FIX ---
            // Launch the new Activity instead of using NavController
            startActivity(Intent(requireContext(), CreateEventActivity::class.java))
            dismiss()
        }

        binding.optionSellTicket.setOnClickListener {
            startActivity(Intent(requireContext(), SellTicketActivity::class.java))
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}