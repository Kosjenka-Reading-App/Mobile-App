package com.dsd.kosjenka.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Exercise(
    val category: List<Category>,
    val complexity: String,
    val id: Int,
    val title: String,
) : Parcelable