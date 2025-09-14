package com.scenein.tickets.presentation.screens

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.scenein.utils.SP
import com.scenein.utils.EdgeToEdgeUtils
import org.json.JSONObject

/**
 * This Activity has no UI. Its sole purpose is to launch the Razorpay Checkout.
 * This version uses the correct method signatures for the SDK version 1.6.x.
 */
class CheckoutActivity : AppCompatActivity(), PaymentResultListener {

    private val TAG = "RazorpayCheckout" // Changed to camelCase as per lint warning

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        startPayment()
    }

    private fun startPayment() {
        val orderId = intent.getStringExtra("order_id")
        val amountInPaise = intent.getIntExtra("amount", 0)
        val keyId = intent.getStringExtra("key_id")

        if (keyId.isNullOrEmpty() || orderId.isNullOrEmpty() || amountInPaise == 0) {
            handleError("Payment details are missing. Cannot proceed.")
            return
        }

        val checkout = Checkout()
        checkout.setKeyID(keyId)

        try {
            val options = JSONObject()
            options.put("name", "VibeIn")
            options.put("description", "Ticket Purchase Transaction")
            options.put("theme.color", "#007BFF")

            // --- FIX for setImage ---
            // The image is now set within the options object as a URL.
            // You can host your logo online or convert your drawable to a data URI.
            // For now, we will omit it to ensure compilation. You can add this later.
            // options.put("image", "https://your-domain.com/logo.png");

            options.put("currency", "INR")
            options.put("order_id", orderId)
            options.put("amount", amountInPaise.toString())

            val prefill = JSONObject()
            prefill.put("email", SP.getString(this, SP.USER_EMAIL, ""))
            prefill.put("contact", SP.getString(this, SP.USER_MOBILE, ""))
            options.put("prefill", prefill)

            checkout.open(this, options)

        } catch (e: Exception) {
            handleError("Error preparing payment: ${e.message}")
            e.printStackTrace()
        }
    }

    // --- DEFINITIVE LISTENER IMPLEMENTATIONS ---

    /**
     * This is the PRIMARY success callback for your SDK version.
     * It provides the payment ID.
     */
    override fun onPaymentSuccess(razorpayPaymentId: String) {
        Log.d(TAG, "Payment Successful! Payment ID: $razorpayPaymentId")
        // Set the result to OK so TicketDetailActivity knows the purchase was a success.
        setResult(Activity.RESULT_OK)
        finish()
    }

    /**
     * This is the PRIMARY error callback for your SDK version.
     * It provides an error code and a description.
     */
    override fun onPaymentError(code: Int, response: String) {
        Log.e(TAG, "Payment Failed. Code: $code, Response: $response")
        try {
            // Try to parse a more user-friendly error message.
            val errorJson = JSONObject(response)
            val errorMessage = errorJson.getJSONObject("error").getString("description")
            handleError("Payment Failed: $errorMessage")
        } catch (e: Exception) {
            handleError("Payment Failed. Please try again.")
        }
    }

    /**
     * A helper function to show a Toast and finish the activity.
     */
    private fun handleError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}