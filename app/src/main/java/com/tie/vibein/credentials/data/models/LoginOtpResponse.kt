package com.tie.vibein.credentials.data.models

data class LoginOtpResponse(
    val status: String,
    val message: String,
    val otp: String? = null // only in dev/testing
)

