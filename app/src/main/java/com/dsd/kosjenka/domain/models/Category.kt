package com.dsd.kosjenka.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    val category: String
): Parcelable