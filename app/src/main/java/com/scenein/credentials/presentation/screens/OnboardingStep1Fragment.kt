package com.scenein.credentials.presentation.screens

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.scenein.R
import com.scenein.credentials.presentation.view_model.CredentialViewModel
import com.scenein.credentials.presentation.view_model.OnboardingViewModel
import com.scenein.databinding.FragmentOnboardingStep1Binding
import com.scenein.utils.NetworkState

class OnboardingStep1Fragment : Fragment(R.layout.fragment_onboarding_step1) {

    private var _binding: FragmentOnboardingStep1Binding? = null
    private val binding get() = _binding!!

    private val onboardingViewModel: OnboardingViewModel by activityViewModels()
    private val credentialViewModel: CredentialViewModel by activityViewModels()

    private val handler = Handler(Looper.getMainLooper())
    private var usernameCheckRunnable: Runnable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingStep1Binding.bind(view)

        setupInputListeners()
        setupObservers()
    }

    private fun setupInputListeners() {
        binding.etFullName.doAfterTextChanged {
            onboardingViewModel.fullName.value = it.toString().trim()
            binding.tvFullNameError.isVisible = false
        }
        binding.etEmail.doAfterTextChanged {
            onboardingViewModel.email.value = it.toString().trim()
            binding.tvEmailError.isVisible = false
        }
        binding.etUserName.doAfterTextChanged { s ->
            binding.tvUsernameStatusMessage.text = ""
            binding.tvUsernameStatusMessage.isVisible = false
            usernameCheckRunnable?.let { handler.removeCallbacks(it) }
            usernameCheckRunnable = Runnable {
                val username = s.toString().trim()
                onboardingViewModel.userName.value = username
                if (username.isNotEmpty()) {
                    credentialViewModel.checkUsername(username)
                } else {
                    binding.usernameStatusIcon.isVisible = false
                    binding.usernameStatusProgress.isVisible = false
                    credentialViewModel.clearUsernameCheckState()
                }
            }
            handler.postDelayed(usernameCheckRunnable!!, 500)
        }
    }

    private fun setupObservers() {
        onboardingViewModel.fullNameError.observe(viewLifecycleOwner) { error ->
            binding.tvFullNameError.text = error
            binding.tvFullNameError.isVisible = error != null
        }
        onboardingViewModel.emailError.observe(viewLifecycleOwner) { error ->
            binding.tvEmailError.text = error
            binding.tvEmailError.isVisible = error != null
        }
        onboardingViewModel.userNameError.observe(viewLifecycleOwner) { error ->
            binding.tvUsernameStatusMessage.text = error
            binding.tvUsernameStatusMessage.isVisible = error != null
        }

        credentialViewModel.usernameCheckState.observe(viewLifecycleOwner) { state ->
            binding.usernameStatusProgress.isVisible = state is NetworkState.Loading
            binding.usernameStatusIcon.isVisible = state !is NetworkState.Loading && !binding.etUserName.text.isNullOrBlank()
            binding.tvUsernameStatusMessage.isVisible = state !is NetworkState.Loading

            when (state) {
                is NetworkState.Success -> {
                    val colorRes = if (state.data.available) R.color.colorSuccess else R.color.colorError
                    val iconRes = if (state.data.available) R.drawable.ic_check_circle else R.drawable.ic_error
                    binding.usernameStatusIcon.setImageResource(iconRes)
                    binding.tvUsernameStatusMessage.text = state.data.message
                    binding.tvUsernameStatusMessage.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
                }
                is NetworkState.Error -> {
                    binding.usernameStatusIcon.setImageResource(R.drawable.ic_error)
                    binding.tvUsernameStatusMessage.text = state.message
                    binding.tvUsernameStatusMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorError))
                }
                else -> {
                    binding.tvUsernameStatusMessage.text = ""
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        usernameCheckRunnable?.let { handler.removeCallbacks(it) }
        _binding = null
    }
}