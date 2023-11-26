package com.dsd.kosjenka.domain.response_objects

import com.dsd.kosjenka.domain.models.Exercise

data class GetExercisesResponseObject (
    val items: ArrayList<Exercise>,
    val total: Int,
    val page: Int,
    val size: Int,
    val pages: Int
)