package com.dsd.kosjenka.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Exercise(
    val id: Int,
    val title: String,
    val complexity: String,
    val category: List<Category>,
    val completion: Completion?,
    val date: String,
    val text: String? = null
) : Parcelable



