package com.tie.vibein.utils

// models/PincodeResponse.kt
data class PincodeResponse(
    val PostOffice: List<PostOffice>?,
    val Status: String
)

data class PostOffice(
    val Name: String,
    val District: String,
    val State: String,
    val Country: String
)

