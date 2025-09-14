package com.scenein.credentials.presentation.screens

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.scenein.R
import com.scenein.createEvent.presentation.view_model.CreateEventViewModel
import com.scenein.credentials.presentation.adapter.CategoryAdapter
import com.scenein.credentials.presentation.view_model.OnboardingViewModel
import com.scenein.databinding.FragmentOnboardingStep3Binding
import com.scenein.utils.NetworkState

class OnboardingStep3Fragment : Fragment(R.layout.fragment_onboarding_step3) {

    private var _binding: FragmentOnboardingStep3Binding? = null
    private val binding get() = _binding!!

    private val onboardingViewModel: OnboardingViewModel by activityViewModels()
    private val eventViewModel: CreateEventViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingStep3Binding.bind(view)

        binding.rvInterested.layoutManager = GridLayoutManager(requireContext(), 3)

        setupObservers()
        eventViewModel.fetchCategories() // Fetch categories if not already loaded
    }

    private fun setupObservers() {
        eventViewModel.categoryState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is NetworkState.Success -> {
                    binding.rvInterested.adapter = CategoryAdapter(
                        context = requireContext(),
                        categories = state.data.data,
                        initialSelectedIds = mutableListOf(),
                    ) { selectedIds ->
                        onboardingViewModel.selectedCategoryIdsString.value = selectedIds.joinToString(",")
                        // Clear error once user selects something
                        if (selectedIds.isNotEmpty()) {
                            onboardingViewModel.categoryError.value = null
                        }
                    }
                }
                is NetworkState.Error -> {
                    Toast.makeText(requireContext(), "Failed to load categories: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                else -> { /* Handle Loading state if needed */ }
            }
        }

        onboardingViewModel.categoryError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}