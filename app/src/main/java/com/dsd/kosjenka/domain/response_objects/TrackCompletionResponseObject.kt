package com.dsd.kosjenka.domain.response_objects

data class TrackCompletionResponseObject(
    val completion: Int,
    val exercise_id: Int,
    val position: Int,
    val time_spent: Int,
    val user_id: Int
)