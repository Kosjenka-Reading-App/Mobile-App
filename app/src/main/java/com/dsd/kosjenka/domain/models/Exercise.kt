package com.dsd.kosjenka.domain.models

data class Exercise(
    val category: List<Category>,
    val complexity: String,
    val id: Int,
    val title: String
)