package com.scenein.tickets.presentation.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scenein.tickets.data.models.*
import com.scenein.tickets.data.respository.TicketRepository
import com.scenein.utils.NetworkState
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject

class TicketViewModel : ViewModel() {
    private val repository = TicketRepository()

    // State for parsing OCR text from a ticket
    private val _parseTextState = MutableLiveData<NetworkState<AnalyzedTicketData>>()
    val parseTextState: LiveData<NetworkState<AnalyzedTicketData>> = _parseTextState

    // State for creating a new ticket listing
    private val _createTicketState = MutableLiveData<NetworkState<GenericApiResponse>>()
    val createTicketState: LiveData<NetworkState<GenericApiResponse>> = _createTicketState

    // State for verifying a payout method
    private val _verifyPayoutState = MutableLiveData<NetworkState<PayoutVerificationResponse>>()
    val verifyPayoutState: LiveData<NetworkState<PayoutVerificationResponse>> = _verifyPayoutState

    // State for browsing tickets in the marketplace
    private val _browseTicketsState = MutableLiveData<NetworkState<List<Ticket>>>()
    val browseTicketsState: LiveData<NetworkState<List<Ticket>>> = _browseTicketsState

    // State for fetching a user's purchased and listed tickets
    private val _myActivityState = MutableLiveData<NetworkState<MyActivityResponse>>()
    val myActivityState: LiveData<NetworkState<MyActivityResponse>> = _myActivityState

    // State for creating a payment order
    private val _createOrderState = MutableLiveData<NetworkState<CreateOrderResponse>>()
    val createOrderState: LiveData<NetworkState<CreateOrderResponse>> = _createOrderState

    // State for actions on a purchased ticket (reveal, complete, dispute, relist)
    private val _transactionActionState = MutableLiveData<NetworkState<GenericApiResponse>>()
    val transactionActionState: LiveData<NetworkState<GenericApiResponse>> = _transactionActionState

    // --- NEW: State for actions on a listed ticket (update price, delist) ---
    private val _listingActionState = MutableLiveData<NetworkState<GenericApiResponse>>()
    val listingActionState: LiveData<NetworkState<GenericApiResponse>> = _listingActionState


    // =================================================================
    // == Functions to Trigger API Calls (Complete Implementation) =====
    // =================================================================

    // --- SELLER FLOW ---

    fun parseOcrText(rawText: String) {
        _parseTextState.value = NetworkState.Loading
        viewModelScope.launch {
            _parseTextState.value = try {
                val response = repository.parseOcrText(rawText)
                if (response.isSuccessful && response.body()?.status == "success") {
                    NetworkState.Success(response.body()!!.data)
                } else {
                    NetworkState.Error(response.body()?.message ?: "Could not extract details.")
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An error occurred.")
            }
        }
    }


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



    fun updateTicket(ticketId: String, newPrice: String) {
        _listingActionState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                // We will also rename the repository function for consistency
                val response = repository.updateTicket(ticketId, newPrice)
                if(response.isSuccessful) {
                    _listingActionState.postValue(NetworkState.Success(response.body()!!))
                } else {
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    _listingActionState.postValue(NetworkState.Error(errorMsg))
                }
            } catch (e: Exception) {
                _listingActionState.postValue(e.message?.let { NetworkState.Error(it) })
            }
        }
    }



    // --- NEW: Function to trigger delisting ---
    fun delistTicket(ticketId: String) {
        _listingActionState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                val response = repository.delistTicket(ticketId)
                if(response.isSuccessful) _listingActionState.postValue(NetworkState.Success(response.body()!!))
                else _listingActionState.postValue(NetworkState.Error(JSONObject(response.errorBody()!!.string()).getString("message")))
            } catch (e: Exception) { _listingActionState.postValue(e.message?.let {
                NetworkState.Error(
                    it
                )
            }) }
        }
    }

    fun verifyUpiPayout(vpa: String) {
        _verifyPayoutState.value = NetworkState.Loading
        viewModelScope.launch {
            _verifyPayoutState.value = try {
                val response = repository.verifyUpiPayout(vpa)
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

    fun verifyBankPayout( accountHolderName: String, ifsc: String, accountNumber: String) {
        _verifyPayoutState.value = NetworkState.Loading
        viewModelScope.launch {
            _verifyPayoutState.value = try {
                val response = repository.verifyBankPayout( accountHolderName, ifsc, accountNumber)
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

    fun createOrder(ticketId: String) {
        _createOrderState.value = NetworkState.Loading
        viewModelScope.launch {
            _createOrderState.value = try {
                val response = repository.createOrder(ticketId)
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

    fun fetchMyActivity() {
        _myActivityState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getMyActivity()
                if (response.isSuccessful && response.body()?.status == "success") {
                    _myActivityState.value = NetworkState.Success(response.body()!!)
                } else {
                    NetworkState.Error("Failed to load your tickets.")
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An error occurred.")
            }
        }
    }

    fun revealTicket(transactionId: Int) {
        _transactionActionState.value = NetworkState.Loading
        viewModelScope.launch {
            _transactionActionState.value = try {
                val response = repository.revealTicket(transactionId)
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

    fun completeTransaction(transactionId: Int) {
        _transactionActionState.value = NetworkState.Loading
        viewModelScope.launch {
            _transactionActionState.value = try {
                val response = repository.completeTransaction(transactionId)
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

    fun createDispute(transactionId: Int, reason: String) {
        _transactionActionState.value = NetworkState.Loading
        viewModelScope.launch {
            _transactionActionState.value = try {
                val response = repository.createDispute(transactionId, reason)
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

    fun relistTicket(transactionId: Int, newSellingPrice: String) {
        _transactionActionState.value = NetworkState.Loading
        viewModelScope.launch {
            _transactionActionState.value = try {
                val response = repository.relistTicket(transactionId, newSellingPrice)
                if (response.isSuccessful && response.body()?.status == "success") {
                    NetworkState.Success(response.body()!!)
                } else {
                    // Try to parse a specific error message from the backend JSON response
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    NetworkState.Error(errorMsg)
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An error occurred during relist.")
            }
        }
    }
}