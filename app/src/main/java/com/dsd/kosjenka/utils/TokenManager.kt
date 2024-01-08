package com.dsd.kosjenka.utils

import com.dsd.kosjenka.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

class TokenManager(private var sharedPreferences: SharedPreferences) {

    companion object {
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }

    // Retrieve the access token from SharedPreferences
    var accessToken: String?
        get() = sharedPreferences.accessToken
        set(value) {sharedPreferences.accessToken = value}

    // Retrieve the refresh token from SharedPreferences
    var refreshToken: String?
        get() = sharedPreferences.refreshToken
        set(value) {sharedPreferences.refreshToken = value}

    fun deleteToken() {
        sharedPreferences.accessToken = null
        sharedPreferences.refreshToken = null
    }
}