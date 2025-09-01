package com.scenein.utils


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.scenein.databinding.BottomSheetEditPhotoBinding

class EditPhotoBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetEditPhotoBinding? = null
    private val binding get() = _binding!!

    // Define listener interfaces for the actions
    var onCameraClick: (() -> Unit)? = null
    var onGalleryClick: (() -> Unit)? = null
    var onRemoveClick: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetEditPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cameraButton.setOnClickListener {
            onCameraClick?.invoke()
            dismiss()
        }
        binding.galleryButton.setOnClickListener {
            onGalleryClick?.invoke()
            dismiss()
        }
        binding.removeButton.setOnClickListener {
            onRemoveClick?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Static instance constructor
    companion object {
        fun newInstance(): EditPhotoBottomSheet = EditPhotoBottomSheet()
    }
}