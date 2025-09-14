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
import com.scenein.createEvent.data.models.Category
import com.scenein.createEvent.presentation.view_model.CreateEventViewModel
import com.scenein.databinding.FragmentStep1BasicsBinding
import com.scenein.utils.NetworkState

class Step1BasicsFragment : Fragment() {
    private var _binding: FragmentStep1BasicsBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: CreateEventViewModel by activityViewModels()

    private val STATE_ERROR = intArrayOf(R.attr.state_error)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStep1BasicsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModelObservers()
        setupUIListeners()
        restoreStateFromViewModel()

        if (sharedViewModel.categoryState.value == null) {
            sharedViewModel.fetchCategories()
        }
    }

    private fun restoreStateFromViewModel() {
        binding.etEventName.setText(sharedViewModel.eventName.value)
        binding.etEventDescription.setText(sharedViewModel.eventDescription.value)
    }

    private fun setupViewModelObservers() {
        sharedViewModel.categoryState.observe(viewLifecycleOwner) { state ->
            binding.categoryProgressBar.isVisible = state is NetworkState.Loading
            binding.chipGroupCategory.isVisible = state is NetworkState.Success && state.data.data.isNotEmpty()
            binding.categoryErrorLayout.isVisible = state is NetworkState.Error || (state is NetworkState.Success && state.data.data.isEmpty())

            when (state) {
                is NetworkState.Success -> {
                    if (state.data.data.isEmpty()) {
                        binding.tvCategoryErrorMsg.text = "No categories found"
                        binding.btnRetryCategory.text = "Refresh"
                    } else {
                        populateCategoryChips(state.data.data)
                    }
                }
                is NetworkState.Error -> binding.tvCategoryErrorMsg.text = state.message
                is NetworkState.Loading -> { /* Handled by isVisible */ }
            }
        }
    }

    private fun populateCategoryChips(categories: List<Category>) {
        binding.chipGroupCategory.removeAllViews()
        val activeCategories = categories.filter { it.status == "1" }
        activeCategories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category.category_name
                tag = category.id
                isCheckable = true
                chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.chip_background_selector)
                setTextColor(ContextCompat.getColorStateList(context, R.color.chip_text_color_selector))
                chipCornerRadius = resources.getDimension(R.dimen.dp_20_dsa)
                chipStrokeWidth = 0f
                checkedIcon = null
            }
            binding.chipGroupCategory.addView(chip)
            if (sharedViewModel.selectedCategory.value?.first == category.id) {
                chip.isChecked = true
            }
        }
    }

    private fun setupUIListeners() {
        binding.btnRetryCategory.setOnClickListener { sharedViewModel.fetchCategories() }

        binding.etEventName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                sharedViewModel.eventName.value = s.toString()
                validateEventName(s.toString())
                sharedViewModel.checkAllValidations()
            }
        })

        binding.etEventDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                sharedViewModel.eventDescription.value = s.toString()
                validateDescription(s.toString())
                sharedViewModel.checkAllValidations()
            }
        })

        binding.chipGroupCategory.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId != View.NO_ID) {
                val selectedChip: Chip = group.findViewById(checkedId)
                val categoryId = selectedChip.tag as String
                val categoryName = selectedChip.text.toString()
                sharedViewModel.selectedCategory.value = Pair(categoryId, categoryName)
                binding.tvCategoryError.visibility = View.GONE
            } else {
                sharedViewModel.selectedCategory.value = null
            }
            sharedViewModel.checkAllValidations()
        }
    }

    private fun validateEventName(name: String) {
        if (name.isNotEmpty() && name.length < 3) {
            showError(binding.etEventName, binding.tvEventNameError, "Name must be at least 3 characters")
        } else {
            clearError(binding.etEventName, binding.tvEventNameError)
        }
    }

    private fun validateDescription(desc: String) {
        if (desc.isNotEmpty() && desc.length < 10) {
            showError(binding.etEventDescription, binding.tvDescriptionError, "Description must be at least 10 characters")
        } else {
            clearError(binding.etEventDescription, binding.tvDescriptionError)
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