package com.tie.vibein.tickets.persentation.screens

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.databinding.ActivitySellTicketBinding
import com.tie.vibein.tickets.data.models.AnalyzedTicketData
import com.tie.vibein.tickets.presentation.viewmodel.TicketViewModel
import com.tie.vibein.utils.EdgeToEdgeUtils
import com.tie.vibein.utils.NetworkState
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SellTicketActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySellTicketBinding
    private val viewModel: TicketViewModel by viewModels()
    private var selectedTicketUri: Uri? = null
    private val calendar: Calendar = Calendar.getInstance()

    private var eventDateForApi: String = ""
    private var eventTimeForApi: String = ""
    private var isPayoutMethodVerified = false
    private var currentTicketCount = 1

    private var analyzedData: AnalyzedTicketData? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedTicketUri = it
            processSelectedFile(it)
        }
    }

    private val payoutSetupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Payout method updated!", Toast.LENGTH_SHORT).show()
            checkUserPayoutStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        binding = ActivitySellTicketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        observeViewModel()
        showUiState(UiState.UPLOAD_PROMPT)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupClickListeners() {
        binding.btnSelectFile.setOnClickListener { filePickerLauncher.launch("*/*") }
        binding.tvEventDate.setOnClickListener { showDatePicker() }
        binding.tvEventTime.setOnClickListener { showTimePicker() }
        binding.btnListTicket.setOnClickListener { handleListTicket() }
        binding.btnChangePayout.setOnClickListener {
            payoutSetupLauncher.launch(Intent(this, PayoutSetupActivity::class.java))
        }

        // --- Ticket Counter Listeners ---
        binding.btnIncrementTickets.setOnClickListener {
            currentTicketCount++
            binding.tvTicketCount.text = currentTicketCount.toString()
        }
        binding.btnDecrementTickets.setOnClickListener {
            if (currentTicketCount > 1) {
                currentTicketCount--
                binding.tvTicketCount.text = currentTicketCount.toString()
            }
        }
    }

    private fun processSelectedFile(uri: Uri) {
        showUiState(UiState.ANALYZING)
        try {
            val image = InputImage.fromFilePath(this, uri)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                .addOnSuccessListener { visionText ->
                    if (visionText.text.isNotBlank()) {
                        Log.d("ML_KIT_OCR_RAW_TEXT", visionText.text)
                        viewModel.parseOcrText(visionText.text)
                    } else {
                        showUiState(UiState.ERROR, "We couldn't read any text from your ticket.")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MLKitError", "On-device OCR failed", e)
                    showUiState(UiState.ERROR, "Text recognition failed.")
                }
        } catch (e: IOException) {
            e.printStackTrace()
            showUiState(UiState.ERROR, "Failed to open the selected file.")
        }
    }

    private fun observeViewModel() {
        viewModel.parseTextState.observe(this) { state ->
            when (state) {
                is NetworkState.Success -> {
                    this.analyzedData = state.data
                    populateForm(state.data)
                    checkUserPayoutStatus()
                    showUiState(UiState.FORM_READY)
                }
                is NetworkState.Error -> showUiState(UiState.ERROR, state.message)
                is NetworkState.Loading -> showUiState(UiState.ANALYZING)
            }
        }

        viewModel.createTicketState.observe(this) { state ->
            binding.progressBar.isVisible = state is NetworkState.Loading
            binding.btnListTicket.isEnabled = state !is NetworkState.Loading
            binding.btnListTicket.text = if (state is NetworkState.Loading) "" else "List My Ticket"
            when (state) {
                is NetworkState.Success -> {
                    Toast.makeText(this, state.data.message, Toast.LENGTH_LONG).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                is NetworkState.Error -> Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    private fun populateForm(data: AnalyzedTicketData) {
        binding.etEventName.setText(data.eventName)
        binding.etEventVenue.setText(data.eventVenue)

        val originalPrice = data.originalPrice?.toDoubleOrNull() ?: 0.0
        val sellingPriceLabel = binding.tvYourPriceLabel

        if (originalPrice > 0.0) {
            binding.etOriginalPrice.setText(data.originalPrice)
            binding.etOriginalPrice.isEnabled = false
            binding.etSellingPrice.setText(data.originalPrice)
            sellingPriceLabel.text = "Your Price (Max ₹${data.originalPrice})"
        } else {
            binding.etOriginalPrice.setText("")
            binding.etOriginalPrice.hint = "Enter face value (₹)"
            binding.etOriginalPrice.isEnabled = true
            sellingPriceLabel.text = "Your Price (₹)"
        }

        currentTicketCount = data.numberOfTickets ?: 1
        binding.tvTicketCount.text = currentTicketCount.toString()

        data.eventDate?.takeIf { it.isNotBlank() }?.let { dateStr ->
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            val outputFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
            try {
                inputFormat.parse(dateStr)?.let { binding.tvEventDate.text = outputFormat.format(it) }
            } catch (e: Exception) { binding.tvEventDate.text = dateStr }
        }
        eventDateForApi = data.eventDate ?: ""

        data.eventTime?.takeIf { it.isNotBlank() }?.let { timeStr ->
            val inputFormat = SimpleDateFormat("HH:mm:ss", Locale.ROOT)
            val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            try {
                inputFormat.parse(timeStr)?.let { binding.tvEventTime.text = outputFormat.format(it) }
            } catch (e: Exception) { binding.tvEventTime.text = timeStr }
        }
        eventTimeForApi = data.eventTime ?: ""

        binding.dynamicFieldsContainer.isVisible = data.requiresSeatNumber
        if (data.requiresSeatNumber) {
            binding.etTicketType.setText(data.ticketType)
            binding.etSeatNumber.setText(data.seatNumber)
        }
    }

    private fun checkUserPayoutStatus() {
        val isVerified = SP.getBoolean(this, SP.IS_PAYOUT_VERIFIED, false)
        val payoutInfo = SP.getString(this, SP.PAYOUT_INFO_DISPLAY, "No payout method has been added.")
        isPayoutMethodVerified = isVerified
        if (isVerified) {
            binding.tvPayoutMethodInfo.text = "Payouts will be sent to:\n$payoutInfo"
            binding.btnChangePayout.text = "Change Method"
        } else {
            binding.tvPayoutMethodInfo.text = "A verified payout method is required to list this ticket."
            binding.btnChangePayout.text = "Add Payout Method"
        }
        binding.btnListTicket.isEnabled = isVerified
    }

    private fun handleListTicket() {
        if (!validateInput()) return

        val params = mutableMapOf<String, RequestBody>().apply {
            put("seller_id", createPartFromString(SP.getString(this@SellTicketActivity, SP.USER_ID) ?: ""))
            put("eventName", createPartFromString(binding.etEventName.text.toString()))
            put("eventVenue", createPartFromString(binding.etEventVenue.text.toString()))
            put("eventCity", createPartFromString(analyzedData?.eventCity ?: ""))
            put("eventDate", createPartFromString(eventDateForApi))
            put("eventTime", createPartFromString(eventTimeForApi))
            put("originalPrice", createPartFromString(binding.etOriginalPrice.text.toString()))
            put("sellingPrice", createPartFromString(binding.etSellingPrice.text.toString()))
            put("ticketType", createPartFromString(binding.etTicketType.text.toString()))
            put("seatNumber", createPartFromString(binding.etSeatNumber.text.toString()))
            put("source", createPartFromString("upload"))
            put("numberOfTickets", createPartFromString(currentTicketCount.toString()))
        }

        val ticketFilePart = selectedTicketUri?.let { uri ->
            try {
                contentResolver.openInputStream(uri)?.use {
                    MultipartBody.Part.createFormData("ticket_file", getFileNameFromUri(uri), it.readBytes().toRequestBody(contentResolver.getType(uri)?.toMediaTypeOrNull()))
                }
            } catch (e: Exception) { null }
        }
        if (ticketFilePart == null) { Toast.makeText(this, "Could not read ticket file.", Toast.LENGTH_SHORT).show(); return }
        viewModel.createTicket(params, ticketFilePart)
    }

    private fun validateInput(): Boolean {
        var isValid = true
        if (!isPayoutMethodVerified) {
            Toast.makeText(this, "Please add a verified payout method.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.etEventName.text.isNullOrBlank()) {
            binding.etEventName.error = "Event name cannot be empty."
            isValid = false
        }
        val originalPrice = binding.etOriginalPrice.text.toString().toDoubleOrNull() ?: 0.0
        if (originalPrice <= 0) {
            binding.etOriginalPrice.error = "Please enter the ticket's original price"
            isValid = false
        }
        val sellingPrice = binding.etSellingPrice.text.toString().toDoubleOrNull() ?: 0.0
        if (sellingPrice <= 0) {
            binding.etSellingPrice.error = "Please enter a valid price"
            isValid = false
        }
        if (sellingPrice > originalPrice) {
            binding.etSellingPrice.error = "Cannot exceed original price"
            isValid = false
        }
        if (selectedTicketUri == null) {
            Toast.makeText(this, "A ticket file must be uploaded.", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        return isValid
    }

    private fun showDatePicker() {
        val listener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            binding.tvEventDate.text = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(calendar.time)
            eventDateForApi = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(calendar.time)
        }
        val dialog = DatePickerDialog(this, listener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        dialog.datePicker.minDate = System.currentTimeMillis() - 1000
        dialog.show()
    }

    private fun showTimePicker() {
        val listener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            binding.tvEventTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
            eventTimeForApi = SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(calendar.time)
        }
        TimePickerDialog(this, listener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
    }

    private fun createPartFromString(string: String): RequestBody = string.toRequestBody("text/plain".toMediaTypeOrNull())

    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = "ticket_file.tmp" // A sensible default
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FileNameError", "Could not get file name from URI", e)
        }
        return fileName
    }

    private enum class UiState { UPLOAD_PROMPT, ANALYZING, FORM_READY, ERROR }

    private fun showUiState(state: UiState, msg: String = "") {
        binding.uploadContainer.isVisible = state == UiState.UPLOAD_PROMPT || state == UiState.ERROR
        binding.analysisProgressBar.isVisible = state == UiState.ANALYZING
        binding.formScrollView.isVisible = state == UiState.FORM_READY
        binding.bottomButtonContainer.isVisible = state == UiState.FORM_READY
        when(state) {
            UiState.UPLOAD_PROMPT -> { binding.tvUploadPrompt.text = "Begin by uploading your ticket file"; binding.btnSelectFile.text = "Select Ticket File" }
            UiState.ERROR -> { binding.analysisProgressBar.visibility = View.GONE; binding.tvUploadPrompt.text = msg; binding.btnSelectFile.text = "Try Another File" }
            else -> {}
        }
    }
}