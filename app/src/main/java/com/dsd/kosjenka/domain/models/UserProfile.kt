package com.dsd.kosjenka.domain.models

import androidx.annotation.StringRes
import androidx.annotation.DrawableRes

data class UserProfile(
    val profileId: Int,
    val username: String,
    val proficiency: Double) {
}