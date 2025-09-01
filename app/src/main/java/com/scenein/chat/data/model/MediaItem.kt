package com.scenein.chat.data.model

import java.io.Serializable

// This data class will bundle all info needed for the media viewer.
// It must be Serializable to be passed in an Intent.
data class MediaItem(
    val url: String,
    val senderName: String,
    val timestamp: String
) : Serializable