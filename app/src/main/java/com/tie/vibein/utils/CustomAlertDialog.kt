package com.tie.vibein.utils.dialogs

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.tie.vibein.databinding.DialogCustomAlertBinding

object CustomAlertDialog {

    /**
     * The definitive function to show a custom, iOS-style alert dialog.
     *
     * @param context The context of the activity or fragment.
     * @param title The title of the dialog.
     * @param message The main message content of the dialog.
     * @param positiveButtonText The text for the primary action button (e.g., "OK", "Confirm").
     * @param onPositiveClick A lambda function to be executed when the positive button is clicked.
     * @param negativeButtonText The text for the secondary/cancel button. Can be null to hide it.
     */
    fun show(
        context: Context,
        title: String,
        message: String,
        positiveButtonText: String = "OK",
        onPositiveClick: () -> Unit,
        negativeButtonText: String? = "Cancel" // Nullable to make it optional
    ) {
        // Inflate the custom layout using ViewBinding
        val binding = DialogCustomAlertBinding.inflate(LayoutInflater.from(context))

        // Create the dialog instance
        val dialog = Dialog(context)
        dialog.setContentView(binding.root)

        // Set the background to be transparent to show our custom rounded shape
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Populate the views with the provided text
        binding.tvAlertTitle.text = title
        binding.tvAlertMessage.text = message
        binding.btnPositive.text = positiveButtonText

        // Handle the negative button visibility
        if (negativeButtonText != null) {
            binding.btnNegative.text = negativeButtonText
            binding.btnNegative.setOnClickListener {
                dialog.dismiss() // Just dismiss the dialog on cancel
            }
        } else {
            // If no negative button text is provided, hide the button.
            binding.btnNegative.visibility = View.GONE
        }

        // Set the click listener for the positive action
        binding.btnPositive.setOnClickListener {
            onPositiveClick.invoke() // Execute the provided action
            dialog.dismiss()         // Dismiss the dialog after the action
        }

        // Show the fully configured dialog to the user
        dialog.show()
    }
}