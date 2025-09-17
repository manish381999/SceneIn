package com.scenein.profile.data.models

import com.google.gson.annotations.SerializedName
import com.scenein.tickets.data.models.Ticket

data class MyTicketsActivityResponse(
    @SerializedName("status") val status: String,
    @SerializedName("listedTickets") val listedTickets: List<Ticket> = emptyList(),
    @SerializedName("purchasedTickets") val purchasedTickets: List<Ticket> = emptyList()
)
