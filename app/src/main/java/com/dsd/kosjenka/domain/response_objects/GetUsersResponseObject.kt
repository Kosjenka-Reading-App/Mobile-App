package com.dsd.kosjenka.domain.response_objects

import com.dsd.kosjenka.domain.models.UserProfile

data class GetUsersResponseObject (
    val items: ArrayList<UserProfile>,
    val total: Int,
    val page: Int,
    val size: Int,
    val pages: Int
)