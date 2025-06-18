package com.tie.vibein.createEvent.data.models

data class AgeRestrictionResponse(
    val status: String,
    val data: List<AgeRestrictionItem>
)

data class AgeRestrictionItem(
    val id: String,
    val age: String,
    val status: String,
    val created_at: String
)

