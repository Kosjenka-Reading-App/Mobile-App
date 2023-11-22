package com.dsd.kosjenka.utils

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferences
@Inject constructor(@ApplicationContext private val context: Context, private val gson: Gson) {

    private val preferences = context.getSharedPreferences(PREF_FILENAME, Context.MODE_PRIVATE)

    var accessToken: String
        get() = preferences.getString(ACCESS_TOKEN, "").toString()
        set(newToken) = preferences.edit().putString(ACCESS_TOKEN, newToken).apply()

    var refreshToken: String
        get() = preferences.getString(REFRESH_TOKEN, "").toString()
        set(newToken) = preferences.edit().putString(REFRESH_TOKEN, newToken).apply()


    fun clearPreferences() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREF_FILENAME = "com.example.radnikme.pref"
        private const val ACCESS_TOKEN = " ACCESS_TOKEN"
        private const val REFRESH_TOKEN = " REFRESH_TOKEN"
    }
}