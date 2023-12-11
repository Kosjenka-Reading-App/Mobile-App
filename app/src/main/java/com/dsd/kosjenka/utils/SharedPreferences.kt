package com.dsd.kosjenka.utils

import android.content.Context
import com.dsd.kosjenka.domain.models.Category
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

    var userId: String
        get() = preferences.getString(USER_ID, "").toString()
        set(newToken) = preferences.edit().putString(USER_ID, newToken).apply()

    var isLoggedIn: Boolean
        get() = preferences.getBoolean(IS_LOGGED_IN, false)
        set(newToken) = preferences.edit().putBoolean(IS_LOGGED_IN, newToken).apply()

    var categories: List<Category>
        get() {
            val json = preferences.getString(CATEGORIES, null)
            return if (json != null)
                gson.fromJson(json, object : TypeToken<List<Category>>() {}.type)
            else
                emptyList()
        }
        set(newValue) {
            val json = gson.toJson(newValue)
            preferences.edit().putString(CATEGORIES, json).apply()
        }

    fun clearPreferences() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREF_FILENAME = "com.example.radnikme.pref"
        private const val ACCESS_TOKEN = " ACCESS_TOKEN"
        private const val REFRESH_TOKEN = " REFRESH_TOKEN"
        private const val USER_ID = " USER_ID"
        private const val IS_LOGGED_IN = " IS_LOGGED_IN"
        private const val CATEGORIES = "CATEGORIES"
    }
}