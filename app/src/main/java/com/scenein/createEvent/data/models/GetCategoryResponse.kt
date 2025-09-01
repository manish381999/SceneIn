package com.scenein.createEvent.data.models

data class GetCategoryResponse(
    val status: String,
    val message: String,
    val data: List<Category>
)

data class Category(
    val id: String,
    val category_name: String,
    val category_image: String,
    val status: String,
    val created_at: String
)
