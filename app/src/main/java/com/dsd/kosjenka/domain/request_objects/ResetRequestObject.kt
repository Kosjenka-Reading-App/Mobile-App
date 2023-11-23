package com.dsd.kosjenka.domain.request_objects

data class ResetPasswordRequest(
    val token: String,
    val password: String
)