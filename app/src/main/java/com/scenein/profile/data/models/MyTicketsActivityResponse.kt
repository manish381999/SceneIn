package com.scenein.profile.data.models

import com.google.gson.annotations.SerializedName
import com.scenein.tickets.data.models.Ticket

data class MyTicketsActivityResponse(
    @SerializedName("status") val status: String,
    @SerializedName("listed_tickets") val listedTickets: List<Ticket>,
    @SerializedName("purchased_tickets") val purchasedTickets: List<Ticket>

)