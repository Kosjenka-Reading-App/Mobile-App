package com.dsd.kosjenka.domain.models

data class UserProfile(
    val profileId: Int,
    val username: String,
    val proficiency: Double,
) {
}