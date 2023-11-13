package com.dsd.kosjenka.domain.request_objects

data class CreateUserRequestObject(
    val username: String,
    val proficiency: Int
)