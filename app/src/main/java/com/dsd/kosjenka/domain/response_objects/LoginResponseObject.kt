package com.dsd.kosjenka.domain.response_objects

data class LoginResponseObject(
    val access_token: String,
    val refresh_token: String,
)