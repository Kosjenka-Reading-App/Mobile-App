package com.dsd.kosjenka.domain.response_objects

data class RegisterResponseObject(
    val account_category: String,
    val email: String,
    val id_account: Int
)