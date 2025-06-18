package com.tie.vibein.profile.persentation.screen

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tie.vibein.R
import com.tie.vibein.databinding.BottomSheetDisconnectConfirmationBinding

class DisconnectConfirmationBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDisconnectConfirmationBinding? = null
    private val binding get() = _binding!!

    var onDisconnectConfirmed: (() -> Unit)? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate your content layout.
        _binding = BottomSheetDisconnectConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    // =========================================================================
    // ============== THIS IS THE NEW, DEFINITIVE FIX ==========================
    // =========================================================================
    // We override onCreateDialog to manually access and force-style the dialog's views.
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // We use a listener to ensure we modify the views AFTER they have been laid out.
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            setupRoundedCorners(bottomSheetDialog)
        }
        return dialog
    }

    private fun setupRoundedCorners(dialog: BottomSheetDialog) {
        // This is the FrameLayout that holds the content of the bottom sheet.
        // It's the view that should have our rounded background.
        val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)

        if (bottomSheet != null) {
            // Apply our custom rounded drawable to the sheet itself.
            bottomSheet.background = ContextCompat.getDrawable(requireContext(), R.drawable.bottom_sheet_background)

            // This is the crucial part. The 'bottomSheet' is placed inside another
            // view by the system (the CoordinatorLayout). We find that parent view
            // and force its background to be transparent. This makes the rounded
            // corners of our 'bottomSheet' visible.
            (bottomSheet.parent as? View)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }
    // ======================= END OF THE FIX ==============================
    // =====================================================================

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userName = arguments?.getString(ARG_USER_NAME) ?: "this user"
        binding.tvMessage.text = "Are you sure you want to remove $userName from your connections?"

        binding.btnConfirmDisconnect.setOnClickListener {
            onDisconnectConfirmed?.invoke()
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_USER_NAME = "user_name"
        fun newInstance(userName: String): DisconnectConfirmationBottomSheet {
            val fragment = DisconnectConfirmationBottomSheet()
            val args = Bundle()
            args.putString(ARG_USER_NAME, userName)
            fragment.arguments = args
            return fragment
        }
    }
}