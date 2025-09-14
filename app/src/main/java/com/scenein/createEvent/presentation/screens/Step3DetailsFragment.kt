package com.scenein.createEvent.presentation.screens

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.chip.Chip
import com.scenein.R
import com.scenein.createEvent.data.models.AgeRestrictionItem
import com.scenein.createEvent.presentation.view_model.CreateEventViewModel
import com.scenein.databinding.FragmentStep3DetailsBinding
import com.scenein.utils.NetworkState

class Step3DetailsFragment : Fragment() {
    private var _binding: FragmentStep3DetailsBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: CreateEventViewModel by activityViewModels()

    private val STATE_ERROR = intArrayOf(R.attr.state_error)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStep3DetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModelObservers()
        setupUIListeners()
        restoreStateFromViewModel()

        if (sharedViewModel.ageRestrictionState.value == null) {
            sharedViewModel.fetchAgeRestrictions()
        }
    }

    private fun restoreStateFromViewModel() {
        binding.etMaxParticipants.setText(sharedViewModel.maxParticipants.value)
        binding.etTicketPrice.setText(sharedViewModel.ticketPrice.value)
        if (sharedViewModel.eventType.value == "Paid") {
            binding.rgEventType.check(R.id.rbPaid)
        } else {
            binding.rgEventType.check(R.id.rbFree)
        }
    }

    private fun setupViewModelObservers() {
        sharedViewModel.ageRestrictionState.observe(viewLifecycleOwner) { state ->
            binding.ageProgressBar.isVisible = state is NetworkState.Loading
            binding.chipGroupAge.isVisible = state is NetworkState.Success && state.data.data.isNotEmpty()
            binding.ageErrorLayout.isVisible = state is NetworkState.Error || (state is NetworkState.Success && state.data.data.isEmpty())

            when (state) {
                is NetworkState.Success -> {
                    if (state.data.data.isEmpty()){
                        binding.tvAgeErrorMsg.text = "No age groups found"
                        binding.btnRetryAge.text = "Refresh"
                    } else {
                        populateAgeChips(state.data.data)
                    }
                }
                is NetworkState.Error -> binding.tvAgeErrorMsg.text = state.message
                is NetworkState.Loading -> { /* Handled by isVisible */ }
            }
        }
    }

    private fun populateAgeChips(ages: List<AgeRestrictionItem>) {
        binding.chipGroupAge.removeAllViews()
        val activeAges = ages.filter { it.status == "1" }
        activeAges.forEach { age ->
            val chip = Chip(requireContext()).apply {
                text = age.age
                tag = age.id
                isCheckable = true
                chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.chip_background_selector)
                setTextColor(ContextCompat.getColorStateList(context, R.color.chip_text_color_selector))
                chipCornerRadius = resources.getDimension(R.dimen.dp_20_dsa)
                chipStrokeWidth = 0f
                checkedIcon = null
            }
            binding.chipGroupAge.addView(chip)

            if (sharedViewModel.selectedAge.value?.first == age.id) {
                chip.isChecked = true
            }
        }
    }

    private fun setupUIListeners() {
        binding.btnRetryAge.setOnClickListener {
            sharedViewModel.fetchAgeRestrictions()
        }

        binding.etMaxParticipants.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val countStr = s.toString()
                sharedViewModel.maxParticipants.value = countStr
                validateMaxParticipants(countStr)
                sharedViewModel.checkAllValidations()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.chipGroupAge.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId != View.NO_ID) {
                val selectedChip: Chip = group.findViewById(checkedId)
                val ageId = selectedChip.tag as String
                val ageName = selectedChip.text.toString()
                sharedViewModel.selectedAge.value = Pair(ageId, ageName)
            } else {
                sharedViewModel.selectedAge.value = null
            }
            sharedViewModel.checkAllValidations()
        }

        binding.rgEventType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbPaid) {
                binding.priceContainer.visibility = View.VISIBLE
                sharedViewModel.eventType.value = "Paid"
            } else {
                binding.priceContainer.visibility = View.GONE
                sharedViewModel.eventType.value = "Free"
                binding.etTicketPrice.text?.clear()
            }
            sharedViewModel.checkAllValidations()
        }

        binding.etTicketPrice.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                sharedViewModel.ticketPrice.value = s.toString()
                validatePrice(s.toString())
                sharedViewModel.checkAllValidations()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun validateMaxParticipants(countStr: String) {
        if (countStr.isNotEmpty() && (countStr.toIntOrNull() ?: 0) <= 0) {
            showError(binding.etMaxParticipants, binding.tvMaxParticipantsError, "Must be greater than 0")
        } else {
            clearError(binding.etMaxParticipants, binding.tvMaxParticipantsError)
        }
    }

    private fun validatePrice(priceStr: String) {
        if (sharedViewModel.eventType.value == "Paid") {
            if (priceStr.isNotEmpty() && (priceStr.toDoubleOrNull() ?: 0.0) <= 0) {
                showError(binding.etTicketPrice, binding.tvPriceError, "Price must be greater than 0")
            } else {
                clearError(binding.etTicketPrice, binding.tvPriceError)
            }
        } else {
            clearError(binding.etTicketPrice, binding.tvPriceError)
        }
    }

    private fun showError(editText: EditText, errorTextView: TextView, message: String) {
        editText.background.state = STATE_ERROR
        errorTextView.text = message
        errorTextView.visibility = View.VISIBLE
    }

    private fun clearError(editText: EditText, errorTextView: TextView) {
        editText.background.state = intArrayOf()
        errorTextView.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}