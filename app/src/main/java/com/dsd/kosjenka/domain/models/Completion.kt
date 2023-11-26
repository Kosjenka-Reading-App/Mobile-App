package com.dsd.kosjenka.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Completion(
    val completion: Int,
    val position: Int,
    val time_spent: Int,
    val user_id: Int,
) : Parcelable