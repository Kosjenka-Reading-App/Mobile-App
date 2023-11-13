package com.dsd.kosjenka.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor : Interceptor {

    lateinit var sharedPreferences: SharedPreferences

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // If token has been saved, add it to the request
        sharedPreferences?.let {
            requestBuilder.addHeader("Authorization", "Bearer ${it.accessToken}")
        }
        val request = requestBuilder.build()
        val response: Response = chain.proceed(request)
        return response
    }

}