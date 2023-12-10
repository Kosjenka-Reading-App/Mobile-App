package com.dsd.kosjenka.domain.response_objects

import com.dsd.kosjenka.domain.models.Exercise

data class ExerciseResponseObject(
    val items: List<Exercise>,
    val page: Int,
    val pages: Int,
    val size: Int,
    val total: Int
)