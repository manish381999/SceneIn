package com.tie.vibein.tickets.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tie.vibein.tickets.data.models.*
import com.tie.vibein.tickets.data.repository.TicketRepository
import com.tie.vibein.utils.FileUtils
import com.tie.vibein.utils.NetworkState
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody

class TicketViewModel : ViewModel() {
    private val repository = TicketRepository()

    // --- LiveData for each API call, using specific model classes ---
    private val _browseTicketsState = MutableLiveData<NetworkState<List<Ticket>>>()
    val browseTicketsState: LiveData<NetworkState<List<Ticket>>> = _browseTicketsState


    private val _createTicketState = MutableLiveData<NetworkState<GenericApiResponse>>()
    val createTicketState: LiveData<NetworkState<GenericApiResponse>> get() = _createTicketState

    private val _createOrderState = MutableLiveData<NetworkState<CreateOrderResponse>>()
    val createOrderState: LiveData<NetworkState<CreateOrderResponse>> = _createOrderState

    private val _verifyPayoutState = MutableLiveData<NetworkState<PayoutVerificationResponse>>()
    val verifyPayoutState: LiveData<NetworkState<PayoutVerificationResponse>> = _verifyPayoutState

    // A single LiveData for simple success/error actions like reveal, complete, dispute
    private val _transactionActionState = MutableLiveData<NetworkState<GenericApiResponse>>()
    val transactionActionState: LiveData<NetworkState<GenericApiResponse>> = _transactionActionState


    // =================================================================
    // == Functions to Trigger API Calls (Complete Implementation) =====
    // =================================================================

    // --- SELLER FLOW ---

    fun createTicket(
        params: Map<String, RequestBody>,
        ticketFile: MultipartBody.Part?
    ) {
        _createTicketState.value = NetworkState.Loading
        viewModelScope.launch {
            val result = repository.createTicket(params, ticketFile)
            _createTicketState.postValue(result)
        }
    }
    fun verifyUpiPayout(userId: String, vpa: String) {
        _verifyPayoutState.value = NetworkState.Loading
        viewModelScope.launch {
            _verifyPayoutState.value = try {
                val response = repository.verifyUpiPayout(userId, vpa)
                if (response.isSuccessful && response.body()?.status == "success") {
                    NetworkState.Success(response.body()!!)
                } else {
                    NetworkState.Error(response.body()?.message ?: "UPI verification failed.")
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An error occurred.")
            }
        }
    }

    fun verifyBankPayout(userId: String, accountHolderName: String, ifsc: String, accountNumber: String) {
        _verifyPayoutState.value = NetworkState.Loading
        viewModelScope.launch {
            _verifyPayoutState.value = try {
                val response = repository.verifyBankPayout(userId, accountHolderName, ifsc, accountNumber)
                if (response.isSuccessful && response.body()?.status == "success") {
                    NetworkState.Success(response.body()!!)
                } else {
                    NetworkState.Error(response.body()?.message ?: "Bank verification failed.")
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An error occurred.")
            }
        }
    }

    // --- BUYER FLOW ---

    fun fetchBrowseTickets() {
        _browseTicketsState.value = NetworkState.Loading
        viewModelScope.launch {
            _browseTicketsState.value = try {
                val response = repository.browseTickets()
                if (response.isSuccessful && response.body()?.status == "success") {
                    NetworkState.Success(response.body()!!.tickets)
                } else {
                    NetworkState.Error(response.errorBody()?.string() ?: "Failed to load tickets.")
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An error occurred.")
            }
        }
    }

    fun createOrder(ticketId: Int, buyerId: String, buyerName: String, buyerEmail: String, buyerPhone: String) {
        _createOrderState.value = NetworkState.Loading
        viewModelScope.launch {
            _createOrderState.value = try {
                val response = repository.createOrder(ticketId, buyerId, buyerName, buyerEmail, buyerPhone)
                if (response.isSuccessful && response.body()?.status == "success") {
                    NetworkState.Success(response.body()!!)
                } else {
                    NetworkState.Error(response.body()?.message ?: "Could not create order.")
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An error occurred.")
            }
        }
    }

    fun revealTicket(transactionId: Int, currentUserId: String) {
        _transactionActionState.value = NetworkState.Loading
        viewModelScope.launch {
            _transactionActionState.value = try {
                val response = repository.revealTicket(transactionId, currentUserId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    NetworkState.Success(response.body()!!)
                } else {
                    NetworkState.Error(response.body()?.message ?: "Reveal ticket action failed.")
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An error occurred.")
            }
        }
    }

    fun completeTransaction(transactionId: Int, currentUserId: String) {
        _transactionActionState.value = NetworkState.Loading
        viewModelScope.launch {
            _transactionActionState.value = try {
                val response = repository.completeTransaction(transactionId, currentUserId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    NetworkState.Success(response.body()!!)
                } else {
                    NetworkState.Error(response.body()?.message ?: "Complete transaction action failed.")
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An error occurred.")
            }
        }
    }

    fun createDispute(transactionId: Int, currentUserId: String, reason: String) {
        _transactionActionState.value = NetworkState.Loading
        viewModelScope.launch {
            _transactionActionState.value = try {
                val response = repository.createDispute(transactionId, currentUserId, reason)
                if (response.isSuccessful && response.body()?.status == "success") {
                    NetworkState.Success(response.body()!!)
                } else {
                    NetworkState.Error(response.body()?.message ?: "Dispute creation failed.")
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An error occurred.")
            }
        }
    }
}