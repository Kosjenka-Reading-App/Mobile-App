package com.dsd.kosjenka.utils.interfaces

import com.dsd.kosjenka.domain.models.Category


interface CategoryFilterListener {
    fun onCategoryFilterSelected(category: Category)
}