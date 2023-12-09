package com.dsd.kosjenka.data.remote

import com.dsd.kosjenka.utils.SharedPreferences
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // If token has been saved, add it to the request
        sharedPreferences.let {
            requestBuilder.addHeader("Authorization", "Bearer ${it.accessToken}")
        }
        val request = requestBuilder.build()
        return chain.proceed(request)
    }

}