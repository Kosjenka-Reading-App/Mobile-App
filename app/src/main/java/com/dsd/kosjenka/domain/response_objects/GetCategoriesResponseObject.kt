package com.dsd.kosjenka.domain.response_objects

import com.dsd.kosjenka.domain.models.Category

data class GetCategoriesResponseObject(
    val items: ArrayList<Category>,
    val total: Int,
    val page: Int,
    val size: Int,
    val pages: Int
)