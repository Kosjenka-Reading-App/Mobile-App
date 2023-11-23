package com.dsd.kosjenka.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserProfile(
    val id_user: Int,
    val id_account: Int,
    var username: String,
    var proficiency: Double) : Parcelable {
}