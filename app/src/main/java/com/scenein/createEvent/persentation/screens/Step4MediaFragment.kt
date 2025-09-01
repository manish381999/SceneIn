package com.scenein.createEvent.persentation.screens

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.scenein.createEvent.persentation.view_model.CreateEventViewModel
import com.scenein.databinding.FragmentStep4MediaBinding

class Step4MediaFragment : Fragment() {
    private var _binding: FragmentStep4MediaBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: CreateEventViewModel by activityViewModels()

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                sharedViewModel.coverImageUri.value = it
                sharedViewModel.checkAllValidations()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStep4MediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUIListeners()
        setupViewModelObserver()
    }

    private fun setupUIListeners() {
        binding.cardCoverImage.setOnClickListener {
            if (sharedViewModel.coverImageUri.value == null) {
                pickImageLauncher.launch("image/*")
            }
        }

        binding.btnChangeImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnRemoveImage.setOnClickListener {
            sharedViewModel.coverImageUri.value = null
            sharedViewModel.checkAllValidations()
        }
    }

    private fun setupViewModelObserver() {
        sharedViewModel.coverImageUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                binding.ivCoverImage.setImageURI(uri)
                binding.placeholderContainer.visibility = View.GONE
                binding.imageActionsContainer.visibility = View.VISIBLE
                binding.imageOverlay.visibility = View.VISIBLE
            } else {
                binding.ivCoverImage.setImageURI(null)
                binding.placeholderContainer.visibility = View.VISIBLE
                binding.imageActionsContainer.visibility = View.GONE
                binding.imageOverlay.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}