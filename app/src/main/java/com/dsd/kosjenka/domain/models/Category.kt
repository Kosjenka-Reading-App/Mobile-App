package com.dsd.kosjenka.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    var category: String?
): Parcelable