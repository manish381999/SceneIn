package com.scenein.chat.presentation.adapter // Or your adapter's package

import com.scenein.chat.data.model.Message

// This sealed class represents any item that can appear in our chat list.
sealed class ChatItem {
    // A unique ID for DiffUtil to work correctly.
    abstract val id: String

    // This data class will wrap our existing Message model.
    data class MessageItem(val message: Message) : ChatItem() {
        override val id: String = message.tempId ?: message.messageId
    }

    // This data class will represent the date separator.
    data class DateItem(val date: String) : ChatItem() {
        override val id: String = date
    }
}